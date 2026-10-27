export async function fetchJson(url) {
    const response = await fetch(url);
    let body;
    try {
        body = await response.json();
    } catch {
        throw new Error(response.ok ? 'The server returned an invalid JSON response.' : 'Request failed (HTTP ' + response.status + ').');
    }
    if (!response.ok) throw new Error(body?.message || body?.detail || 'Request failed (HTTP ' + response.status + ').');
    return body;
}

export async function loadLanguages(select, status) {
    select.disabled = true;
    try {
        const languages = await fetchJson('/icedata/api/v2/languages');
        if (!Array.isArray(languages) || !languages.length) throw new Error('No languages available');
        const options = languages.sort((a, b) => a.langName.localeCompare(b.langName))
            .map(language => new Option(language.langName, language.langCode.toUpperCase()));
        select.replaceChildren(...options);
        if (options.some(option => option.value === 'EN')) select.value = 'EN';
        status.textContent = '';
    } catch {
        status.textContent = 'Could not load languages. English is available; reload to retry.';
    } finally {
        select.disabled = false;
    }
}
