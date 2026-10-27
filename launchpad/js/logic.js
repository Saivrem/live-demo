import {createRequestForm} from './request/form.js';
import {formats, requestSummary} from './request/state.js';
import {jsonUrl, liveAssets, liveProperties, productUrl, sharedRequest, shareUrl, xmlUrl} from './request/links.js';
import {fetchJson, loadLanguages} from './request/api.js';
import {createResourcesMenu} from './request/resources.js';
import {createLiveRenderer} from './request/live.js';
import {embedCode} from './request/embed.js';
import {copyText} from './request/clipboard.js';

const form = createRequestForm(document.getElementById('requestForm'));
const content = document.getElementById('content');
const feedback = document.getElementById('popUp');
const retry = document.getElementById('retryRequest');
const live = createLiveRenderer(content, document.getElementById('liveDiv'));
const resources = createResourcesMenu(document.getElementById('resourcesMenu'),
    document.getElementById('resourceRepository'), form.control('lang'));
let busy = false;
let sharedRequestType = null;
let validationFeedback = false;

function setFeedback(message, state, canRetry = false) {
    validationFeedback = false;
    feedback.hidden = false;
    feedback.setAttribute('role', state === 'error' ? 'alert' : 'status');
    feedback.dataset.state = state;
    document.getElementById('popUpContent').textContent = message;
    retry.hidden = !canRetry || busy;
}

function showError(reason) {
    setFeedback(reason?.message || 'Could not load the response. Check the connection and try again.', 'error', true);
}

function setBusy(value) {
    busy = value;
    form.setBusy(value);
    content.setAttribute('aria-busy', String(value));
}

function validRequest(state) {
    if (form.validate(state)) return true;
    setFeedback('Check the highlighted fields before sending the request.', 'error');
    validationFeedback = true;
    return false;
}

function showResult(node, state) {
    content.replaceChildren(node);
    document.getElementById('resultSummary').textContent = formats[state.format].name + ' response · ' + state.language;
    const summary = requestSummary(state);
    const details = document.getElementById('resultRequestDetails');
    details.replaceChildren(...Object.entries(summary).map(([key, value]) => {
        const item = document.createElement('div');
        const term = document.createElement('dt');
        term.textContent = {product: 'Product', language: 'Language', response: 'Response'}[key];
        const description = document.createElement('dd');
        description.textContent = value;
        item.append(term, description);
        return item;
    }));
    details.hidden = false;
    setFeedback(formats[state.format].name + ' response loaded.', 'success');
    content.scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
        block: 'start'
    });
}

async function executeRequest(state, newTab = false) {
    if (busy || !validRequest(state)) return;
    sharedRequestType = state.format;
    if (newTab || ['xml', 'fo'].includes(state.format)) {
        const url = newTab ? jsonUrl(state) : state.format === 'xml' ? xmlUrl(state) : productUrl(state);
        window.open(url, '_blank', 'noopener');
        const summary = requestSummary(state);
        setFeedback('Opening ' + summary.response + ' in a new tab · ' + summary.product + ' · ' + summary.language, 'success');
        return;
    }
    setBusy(true);
    setFeedback('Loading ' + formats[state.format].name + '. The previous result stays visible until the response is ready.', 'loading');
    try {
        let result;
        if (state.format === 'json') {
            const response = await fetchJson(jsonUrl(state));
            const viewer = new JsonInspector(response?.data === undefined ? response : response.data);
            result = viewer.getContainer();
        } else {
            const properties = liveProperties(state);
            if (state.format === 'liveFull') properties.Type = 'html';
            result = await live.render(state.format === 'liveGranular' ? liveAssets(state) : null, properties, state.language);
        }
        showResult(result, state);
    } catch (reason) {
        showError(reason);
    } finally {
        setBusy(false);
        retry.hidden = feedback.dataset.state !== 'error';
    }
}

form.element.addEventListener('submit', event => {
    event.preventDefault();
    executeRequest(form.read());
});

function formChanged() {
    sharedRequestType = null;
    if (validationFeedback && !form.hasErrors()) {
        feedback.hidden = true;
        validationFeedback = false;
    }
}

form.element.addEventListener('input', formChanged);
form.element.addEventListener('change', formChanged);
document.getElementById('resourceRepository').addEventListener('change', () => {
    sharedRequestType = null;
});
form.buttons.jsonLink.addEventListener('click', () => executeRequest(form.read(), true));
form.buttons.reset.addEventListener('click', () => {
    document.location.search = '';
});
retry.addEventListener('click', () => form.element.requestSubmit());
form.buttons.copy.addEventListener('click', async () => {
    try {
        await copyText(shareUrl(form.read(), document.location.href, sharedRequestType));
        setFeedback('Request link copied.', 'success');
    } catch (reason) {
        showError(reason);
    }
});
form.buttons.code.addEventListener('click', () => {
    const state = form.read();
    if (!validRequest(state)) return;
    document.getElementById('getCodeModalBody').textContent = embedCode(state);
    document.getElementById('codeCopyStatus').textContent = '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('getCodeModal')).show();
});
document.getElementById('copyEmbedCode').addEventListener('click', async () => {
    try {
        await copyText(document.getElementById('getCodeModalBody').textContent);
        document.getElementById('codeCopyStatus').textContent = 'Embed code copied.';
    } catch {
        document.getElementById('codeCopyStatus').textContent = 'Could not copy. Select the code and copy it manually.';
    }
});

await loadLanguages(form.control('lang'), document.getElementById('languageStatus'));
const shared = sharedRequest(window.location.search, form.read());
form.restore(shared.state);
resources.refresh();
if (shared.autoSubmit) form.element.requestSubmit();
