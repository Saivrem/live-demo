import {formats, identifierFields, selectedAssets, validateRequest} from './state.js';

export function createRequestForm(element) {
    const control = id => element.querySelector('#' + id);
    const fields = ['username', 'appkey', 'brand', 'mpn', 'ean', 'productId'];
    const checkboxes = [...element.querySelectorAll('.asset-option input[type=checkbox]')];
    const buttons = {
        submit: control('jsoncall'), jsonLink: control('jsoncall2'), code: control('getRequestCode'),
        copy: control('copyLinkButton'), reset: control('clear')
    };
    let busy = false;
    const errorFields = [...fields, 'identifierType', 'assets'];
    const errors = new Map();
    errorFields.forEach(name => {
        const input = control(name === 'assets' ? 'selectAllCheck' : name);
        const message = document.createElement('span');
        message.id = name + 'Error';
        message.className = 'field-error';
        message.hidden = true;
        if (name === 'assets') control('contentHelp').after(message);
        else input.closest('.field').append(message);
        errors.set(name, {input, message});
    });

    function read() {
        const state = Object.fromEntries(fields.map(name => [name, control(name).value]));
        state.identifier = control('identifierType').value;
        state.format = control('requestFormat').value;
        state.language = control('lang').value;
        state.languageName = control('lang').selectedOptions[0]?.textContent || '';
        state.output = control('output').value;
        state.repository = document.getElementById('resourceRepository').value;
        state.assets = checkboxes.filter(input => input.checked).map(input => input.id.replace('Check', ''));
        return state;
    }

    function clearError(name) {
        const field = errors.get(name);
        if (!field) return;
        field.input.removeAttribute('aria-invalid');
        const describedBy = (field.input.getAttribute('aria-describedby') || '').split(' ')
            .filter(id => id && id !== field.message.id);
        if (describedBy.length) field.input.setAttribute('aria-describedby', describedBy.join(' '));
        else field.input.removeAttribute('aria-describedby');
        field.message.textContent = '';
        field.message.hidden = true;
    }

    function clearErrors() {
        errors.forEach((_, name) => clearError(name));
    }

    function validate(state) {
        clearErrors();
        const messages = validateRequest(state);
        Object.entries(messages).forEach(([name, text]) => {
            const field = errors.get(name);
            field.message.textContent = text;
            field.message.hidden = false;
            field.input.setAttribute('aria-invalid', 'true');
            field.input.setAttribute('aria-describedby', [field.input.getAttribute('aria-describedby'), field.message.id].filter(Boolean).join(' '));
            if (name === 'username') control('accountSettings').open = true;
            if (name === 'assets') control('contentSettings').open = true;
        });
        const first = Object.keys(messages)[0];
        if (first) errors.get(first).input.focus();
        return !first;
    }

    function update() {
        const state = read();
        const names = identifierFields[state.identifier];
        element.querySelectorAll('[data-identifier-field]').forEach(label => {
            const enabled = names.includes(label.dataset.identifierField);
            label.hidden = !enabled;
            label.querySelector('input').disabled = !enabled;
        });
        buttons.submit.textContent = busy ? 'Loading…' : formats[state.format].action;
        buttons.jsonLink.hidden = state.format !== 'json';
        buttons.code.hidden = !['liveFull', 'liveGranular'].includes(state.format);
        control('xmlOutputField').hidden = state.format !== 'xml';
        control('formatHelp').textContent = formats[state.format].help;
        control('contentSettings').hidden = !['json', 'liveGranular'].includes(state.format);
        control('contentSettingsLabel').textContent = state.format === 'liveGranular' ? 'Select content blocks' : 'Choose JSON fields · optional';
        control('contentHelp').textContent = state.format === 'liveGranular'
            ? 'Select at least one block to display.' : 'Leave all unchecked to request the full JSON response.';
        checkboxes.forEach(input => {
            const unavailable = state.format === 'liveGranular' && ['generalinfoCheck', 'imageCheck', 'relatedCheck'].includes(input.id);
            input.disabled = unavailable;
            input.closest('.asset-option').hidden = unavailable;
        });
        const available = checkboxes.filter(input => !input.disabled);
        const count = available.filter(input => input.checked).length;
        control('selectAllCheck').checked = count === available.length;
        control('selectAllCheck').indeterminate = count > 0 && count < available.length;
        control('requestFormat').disabled = busy;
        control('identifierType').disabled = busy;
        Object.values(buttons).forEach(button => {
            button.disabled = busy;
        });
    }

    function restore(state) {
        fields.forEach(name => {
            control(name).value = state[name];
        });
        control('identifierType').value = state.identifier;
        control('requestFormat').value = state.format;
        if ([...control('lang').options].some(option => option.value === state.language)) control('lang').value = state.language;
        control('output').value = state.output;
        document.getElementById('resourceRepository').value = state.repository;
        checkboxes.forEach(input => {
            input.checked = state.assets.includes(input.id.replace('Check', ''));
        });
        control('accountSettings').open = Boolean(state.appkey) || state.username !== 'openicecat-live';
        control('contentSettings').open = state.format === 'liveGranular' || state.assets.length > 0;
        update();
    }

    control('identifierType').addEventListener('change', () => {
        clearErrors();
        update();
    });
    control('requestFormat').addEventListener('change', () => {
        clearErrors();
        update();
        if (read().format === 'liveGranular') control('contentSettings').open = true;
    });
    control('selectAllCheck').addEventListener('change', event => {
        checkboxes.forEach(input => {
            if (!input.disabled) input.checked = event.target.checked;
        });
        if (selectedAssets(read()).length) clearError('assets');
        update();
    });
    checkboxes.forEach(input => input.addEventListener('change', () => {
        if (selectedAssets(read()).length) clearError('assets');
        update();
    }));
    element.addEventListener('input', event => {
        const name = event.target.id;
        if (errors.has(name) && !validateRequest(read())[name]) clearError(name);
    });

    update();
    return {
        element, buttons, control, read, restore, validate,
        hasErrors() {
            return [...errors.values()].some(field => !field.message.hidden);
        },
        setBusy(value) {
            busy = value;
            update();
        }
    };
}
