import test from 'node:test';
import {jsonResponseViews} from '../launchpad/js/json-inspector.js';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {requestSummary, validateRequest} from '../launchpad/js/request/state.js';
import {
    jsonUrl,
    liveAssets,
    liveProperties,
    productUrl,
    sharedRequest,
    shareUrl,
    xmlUrl
} from '../launchpad/js/request/links.js';
import {fetchJson} from '../launchpad/js/request/api.js';
import {embedCode} from '../launchpad/js/request/embed.js';

const sample = () => ({
    username: 'openicecat-live', appkey: '', brand: 'Dell', mpn: '5587-9709',
    ean: '5397184119709', productId: '56195846', identifier: 'brandMpn', format: 'json', language: 'EN',
    languageName: 'English', output: 'productxml', repository: 'freexml/', assets: []
});

test('validation identifies each missing active field and ignores inactive identifiers', () => {
    assert.deepEqual(validateRequest({...sample(), brand: ' ', mpn: '', username: ''}),
        {brand: 'Enter a brand.', mpn: 'Enter a manufacturer part number.', username: 'Enter your Icecat username.'});
    assert.deepEqual(validateRequest({...sample(), identifier: 'ean', brand: '', mpn: ''}), {});
    assert.deepEqual(Object.keys(validateRequest({...sample(), identifier: 'id', productId: ''})), ['productId']);
});

test('XML and product pages do not require an account; product pages require ID mode', () => {
    assert.deepEqual(validateRequest({...sample(), format: 'xml', username: ''}), {});
    assert.deepEqual(validateRequest({...sample(), format: 'fo', identifier: 'id', username: ''}), {});
    assert.ok(validateRequest({...sample(), format: 'fo'}).identifierType);
});

test('granular validation and targets agree on available blocks', () => {
    const state = {...sample(), format: 'liveGranular', assets: ['generalinfo', 'related']};
    assert.ok(validateRequest(state).assets);
    state.assets.push('gallery');
    assert.deepEqual(validateRequest(state), {});
    assert.deepEqual(liveAssets(state), {gallery: '#gallery'});
});

test('JSON identifiers, account and content survive reserved URL characters', () => {
    const state = {
        ...sample(),
        brand: 'A&B + 中文',
        mpn: 'x;/?#= y',
        appkey: 'key+&=',
        assets: ['title', 'videos', 'tours3d']
    };
    const params = new URL(jsonUrl(state)).searchParams;
    assert.equal(params.get('Brand'), state.brand);
    assert.equal(params.get('ProductCode'), state.mpn);
    assert.equal(params.get('app_key'), state.appkey);
    assert.equal(params.get('content'), 'title,multimedia,');
    assert.equal(new URL(jsonUrl({...state, identifier: 'ean'})).searchParams.get('GTIN'), state.ean);
    assert.equal(new URL(jsonUrl({...state, identifier: 'id'})).searchParams.get('icecat_id'), state.productId);
});

test('XML uses escaped semicolon parameters and the selected output', () => {
    assert.equal(xmlUrl({...sample(), mpn: 'a;b&c', brand: 'A/B', output: 'productcsv'}),
        'https://data.icecat.biz/xml_s3/xml_server3.cgi?lang=EN;prod_id=a%3Bb%26c;vendor=A%2FB;output=productcsv');
    assert.equal(productUrl({...sample(), language: 'NL'}), 'https://icecat.biz/nl/p/a/b/desc-56195846.html');
});

test('shared links round-trip active state, special characters and auto-submit action', () => {
    const state = {
        ...sample(), identifier: 'ean', ean: 'a+&/#', username: 'A&B', appkey: '+secret&',
        format: 'liveGranular', language: 'NL', repository: 'level4/', assets: ['gallery', 'featuregroups', 'related']
    };
    const url = new URL(shareUrl(state, 'https://demo.test/?stale=true#old', state.format));
    assert.equal(url.hash, '');
    assert.equal(url.searchParams.has('brand'), false);
    assert.equal(url.searchParams.has('stale'), false);
    const restored = sharedRequest(url.search, sample());
    for (const key of ['identifier', 'ean', 'username', 'appkey', 'format', 'language', 'repository']) {
        assert.equal(restored.state[key], state[key]);
    }
    assert.deepEqual(restored.state.assets, ['gallery', 'featuregroups']);
    assert.equal(restored.autoSubmit, true);
    assert.equal(new URL(shareUrl({...state, format: 'xml'}, url, 'xml')).searchParams.has('app_key'), false);
});

test('legacy aliases work and unknown URL values do not select unsupported modes', () => {
    const restored = sharedRequest('?radio=ean&requestType=liveGranular&content=multimedia,description&lang=nl', sample());
    assert.deepEqual(restored.state.assets, ['videos', 'tours3d', 'marketingtext']);
    assert.equal(restored.state.language, 'NL');
    assert.equal(restored.state.format, 'liveGranular');
    const unknown = sharedRequest('?radio=__proto__&format=constructor&output=other&repo=bad&requestType=constructor', sample());
    assert.equal(unknown.state.identifier, 'brandMpn');
    assert.equal(unknown.state.format, 'json');
    assert.equal(unknown.state.output, 'productxml');
    assert.equal(unknown.autoSubmit, false);
});

test('summary describes the captured request, including language and XML output', () => {
    const sent = {...sample()};
    const edited = {...sent, brand: 'Changed', language: 'NL'};
    assert.notEqual(edited.brand, sent.brand);
    assert.deepEqual(requestSummary(sent), {
        product: 'Brand + MPN · Dell / 5587-9709',
        language: 'English (EN)',
        response: 'JSON'
    });
    assert.equal(requestSummary({...sent, format: 'xml', output: 'productcsv'}).response, 'Product CSV');
});

test('full and granular embed code execute with the same arguments as page requests', () => {
    for (const format of ['liveFull', 'liveGranular']) {
        const state = {...sample(), format, brand: '</script> A"&', assets: ['gallery', 'featuregroups']};
        const code = embedCode(state);
        const scripts = [...code.matchAll(/<script>([\s\S]*?)<\/script>/g)];
        assert.equal(scripts.length, 1);
        let listener;
        let received;
        vm.runInNewContext(scripts[0][1], {
            window: {
                addEventListener(name, callback) {
                    assert.equal(name, 'liveload');
                    listener = callback;
                }
            },
            IcecatLive: {
                getDatasheet(...args) {
                    received = args;
                }
            }
        });
        listener();
        assert.deepEqual(JSON.parse(JSON.stringify(received)), [format === 'liveFull' ? '#content' : liveAssets(state), liveProperties(state), 'EN']);
        assert.ok(code.includes('live-current-2.js'));
    }
});


test('transport reports API errors and rejects invalid JSON instead of rendering it', async t => {
    const fetch = t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({message: 'No access'}), {status: 403}));
    await assert.rejects(fetchJson('https://example.test'), /No access/);
    fetch.mock.mockImplementation(async () => new Response('<html>failure</html>', {status: 502}));
    await assert.rejects(fetchJson('https://example.test'), /HTTP 502/);
    fetch.mock.mockImplementation(async () => new Response('invalid JSON', {status: 200}));
    await assert.rejects(fetchJson('https://example.test'), /invalid JSON response/);
    fetch.mock.mockImplementation(async () => new Response(JSON.stringify({data: {id: 1}})));
    assert.deepEqual(await fetchJson('https://example.test'), {data: {id: 1}});
});


test('JSON views retain the complete envelope and every valid falsy data value', () => {
    for (const data of [null, false, 0, '', [], {Title: 'Product'}]) {
        const response = {data, message: 'OK', metadata: {version: 2}};
        const views = jsonResponseViews(response);
        assert.equal(views.data, data);
        assert.equal(views.full, response);
        assert.equal(views.full.metadata.version, 2);
    }
});

test('responses without a data member expose only the full response', () => {
    for (const response of [null, false, 0, 'message', [], {message: 'No product', error: 'NOT_FOUND'}]) {
        assert.deepEqual(Object.keys(jsonResponseViews(response)), ['full']);
        assert.equal(jsonResponseViews(response).full, response);
    }
    const inherited = Object.create({data: 'inherited'});
    inherited.message = 'Own fields only';
    assert.deepEqual(Object.keys(jsonResponseViews(inherited)), ['full']);
});
