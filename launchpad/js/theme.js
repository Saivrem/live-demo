(() => {
    const storageKey = 'launchpad-theme';
    const root = document.documentElement;
    const systemTheme = window.matchMedia('(prefers-color-scheme: dark)');
    let preference = null;
    try {
        preference = localStorage.getItem(storageKey);
    } catch {
        // The theme also works when browser storage is unavailable.
    }
    if (preference !== 'light' && preference !== 'dark') preference = null;

    function applyTheme(theme) {
        root.dataset.theme = theme;
        document.querySelectorAll('[data-theme-toggle]').forEach(button => {
            const label = theme === 'dark' ? 'Switch to Ayu Light' : 'Switch to Ayu Dark';
            button.setAttribute('aria-label', label);
            button.setAttribute('title', label);
            button.setAttribute('aria-pressed', String(theme === 'dark'));
        });
    }

    applyTheme(preference || (systemTheme.matches ? 'dark' : 'light'));
    document.addEventListener('DOMContentLoaded', () => {
        applyTheme(root.dataset.theme);
        document.querySelectorAll('[data-theme-toggle]').forEach(button => {
            button.addEventListener('click', () => {
                preference = root.dataset.theme === 'dark' ? 'light' : 'dark';
                applyTheme(preference);
                try {
                    localStorage.setItem(storageKey, preference);
                } catch {
                    // Keep the selection for this page even if it cannot be saved.
                }
            });
        });
        document.querySelectorAll('[data-copyright-year]').forEach(element => {
            element.textContent = new Date().getFullYear();
        });
    });
    systemTheme.addEventListener('change', event => {
        if (!preference) applyTheme(event.matches ? 'dark' : 'light');
    });
    window.addEventListener('storage', event => {
        if (event.key !== storageKey && event.key !== null) return;
        preference = event.newValue === 'light' || event.newValue === 'dark' ? event.newValue : null;
        applyTheme(preference || (systemTheme.matches ? 'dark' : 'light'));
    });
})();
