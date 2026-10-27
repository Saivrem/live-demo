import {assetNames, formats, identifierFields, outputNames, selectedAssets} from './state.js';

export function jsonUrl(state) {
    const params = new URLSearchParams({shopname: state.username, lang: state.language});
    if (state.appkey.trim()) params.set('app_key', state.appkey);
    if (state.identifier === 'id') params.set('icecat_id', state.productId);
    else if (state.identifier === 'ean') params.set('GTIN', state.ean);
    else {
        params.set('Brand', state.brand);
        params.set('ProductCode', state.mpn);
    }
    const content = [...new Set(selectedAssets(state).map(name => ['videos', 'tours3d'].includes(name) ? 'multimedia' : name))];
    params.set('content', content.length ? content.join(',') + ',' : '');
    return 'https://live.icecat.biz/api?' + params;
}

export function xmlUrl(state) {
    let product;
    if (state.identifier === 'id') product = 'icecat_id=' + encodeURIComponent(state.productId);
    else if (state.identifier === 'ean') product = 'ean_upc=' + encodeURIComponent(state.ean);
    else product = 'prod_id=' + encodeURIComponent(state.mpn) + ';vendor=' + encodeURIComponent(state.brand);
    return 'https://data.icecat.biz/xml_s3/xml_server3.cgi?lang=' + encodeURIComponent(state.language)
        + ';' + product + ';output=' + encodeURIComponent(state.output);
}

export function productUrl(state) {
    return 'https://icecat.biz/' + state.language.toLowerCase() + '/p/a/b/desc-' + encodeURIComponent(state.productId) + '.html';
}

export function liveProperties(state) {
    const properties = {UserName: state.username};
    if (state.identifier === 'id') properties.IcecatProductId = state.productId;
    else if (state.identifier === 'ean') properties.GTIN = state.ean;
    else {
        properties.Brand = state.brand;
        properties.PartCode = state.mpn;
    }
    return properties;
}

export function liveAssets(state) {
    return Object.fromEntries(selectedAssets(state).map(name => [name, '#' + name]));
}

export function shareUrl(state, baseUrl, requestType = null) {
    const params = new URLSearchParams();
    if (requestType !== 'xml') {
        if (state.username.trim()) params.set('username', state.username);
        if (state.appkey.trim()) params.set('app_key', state.appkey);
    }
    identifierFields[state.identifier].forEach(name => {
        if (state[name].trim()) params.set(name, state[name]);
    });
    params.set('radio', state.identifier);
    params.set('lang', state.language);
    params.set('format', state.format);
    params.set('repo', state.repository);
    if (state.format === 'xml') params.set('output', state.output);
    const assets = selectedAssets(state);
    if (assets.length) params.set('content', assets.join(','));
    if (requestType) params.set('requestType', requestType);
    const url = new URL(baseUrl);
    url.search = params.toString();
    url.hash = '';
    return url.href;
}

// Keep existing shared links, including legacy requestType and content aliases, usable.
export function sharedRequest(search, defaults) {
    const params = new URLSearchParams(search);
    if (!params.size) return {state: defaults, autoSubmit: false};
    const state = {...defaults, assets: []};
    ['username', 'brand', 'mpn', 'ean', 'productId'].forEach(name => {
        state[name] = params.get(name) || '';
    });
    state.appkey = params.get('app_key') || '';
    state.identifier = Object.hasOwn(identifierFields, params.get('radio')) ? params.get('radio') : 'brandMpn';
    const format = params.get('format') || params.get('requestType');
    if (Object.hasOwn(formats, format)) state.format = format;
    if (params.get('lang')) state.language = params.get('lang').toUpperCase();
    if (['freexml/', 'level4/'].includes(params.get('repo'))) state.repository = params.get('repo');
    if (Object.hasOwn(outputNames, params.get('output'))) state.output = params.get('output');
    const aliases = {multimedia: ['videos', 'tours3d'], description: ['marketingtext']};
    state.assets = [...new Set((params.get('content') || '').split(',').flatMap(name => aliases[name] || [name]))]
        .filter(name => assetNames.includes(name));
    return {state, autoSubmit: Object.hasOwn(formats, params.get('requestType'))};
}
