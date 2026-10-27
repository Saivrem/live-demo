const rows = document.getElementById('statusRows');
const message = document.getElementById('statusMessage');
const refresh = document.getElementById('refreshStatus');
const languageFilter = document.getElementById('languageFilter');
const repositoryFilter = document.getElementById('repositoryFilter');
let entries = [];

function timestamp(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toISOString().replace('T', ' ').slice(0, 19);
}

function renderStatus() {
    rows.replaceChildren();
    const selected = entries.filter(entry => entry.langCode.toUpperCase().includes(languageFilter.value.trim().toUpperCase())
        && (!repositoryFilter.value || entry.repository === repositoryFilter.value));
    for (const entry of selected) {
        const row = document.createElement('tr');
        const states = { TODAY: ['Today', 'today'], YESTERDAY: ['Yesterday', 'yesterday'], OLD: ['Older', 'old'] };
        let [label, color] = states[entry.freshness] || ['Unknown', 'unknown'];
        if (entry.result !== 'OK') [label, color] = ['Check failed', 'old'];
        if (entry.checkOverdue) [label, color] = ['Check overdue', 'unknown'];
        for (const value of [entry.langCode, entry.repository, entry.format.toUpperCase(), label,
            timestamp(entry.lastModified), timestamp(entry.checkedAt),
            entry.result.replaceAll('_', ' ') + (entry.httpStatus ? ` · HTTP ${entry.httpStatus}` : '')]) {
            const cell = document.createElement('td');
            cell.textContent = value;
            row.append(cell);
        }
        const badge = document.createElement('span');
        badge.className = `badge ${color}`;
        badge.textContent = label;
        row.children[3].replaceChildren(badge);
        rows.append(row);
    }
    message.textContent = entries.length ? `Showing ${selected.length} of ${entries.length} files.`
        : 'No checks saved yet. Import languages and wait for the first monitoring sweep.';
}

async function loadStatus() {
    refresh.disabled = true;
    message.textContent = 'Loading saved status…';
    try {
        const response = await fetch('/icedata/api/v2/daily-indexes', { cache: 'no-store' });
        if (!response.ok) throw new Error(`Could not load status (HTTP ${response.status}).`);
        const data = await response.json();
        if (!Array.isArray(data)) throw new Error('Unexpected status response.');
        entries = data;
        const selectedRepo = repositoryFilter.value;
        repositoryFilter.replaceChildren(new Option('All available', ''));
        [...new Set(entries.map(entry => entry.repository))].sort()
            .forEach(repo => repositoryFilter.add(new Option(repo, repo)));
        if ([...repositoryFilter.options].some(option => option.value === selectedRepo)) repositoryFilter.value = selectedRepo;
        renderStatus();
    } catch (error) {
        entries = [];
        rows.replaceChildren();
        message.textContent = error.message;
    } finally {
        refresh.disabled = false;
    }
}

languageFilter.addEventListener('input', renderStatus);
repositoryFilter.addEventListener('change', renderStatus);
refresh.addEventListener('click', loadStatus);
loadStatus();
