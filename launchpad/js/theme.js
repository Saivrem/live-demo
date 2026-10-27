(() => {
    const storageKey = 'launchpad-theme';
    const root = document.documentElement;
    const systemTheme = window.matchMedia('(prefers-color-scheme: dark)');
    const themeNames = {light: 'Ayu Light', dark: 'Ayu Dark'};
    let preference = null;
    try {
        preference = localStorage.getItem(storageKey);
    } catch {
        // The theme also works when browser storage is unavailable.
    }
    if (!Object.hasOwn(themeNames, preference)) preference = null;

    function applyTheme(theme) {
        root.dataset.theme = theme;
        document.querySelectorAll('[data-theme-toggle]').forEach(button => {
            button.setAttribute('aria-label', 'Switch to ' + themeNames[theme === 'dark' ? 'light' : 'dark']);
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
        document.querySelectorAll('[data-classic-version]').forEach(link => {
            link.search = window.location.search;
        });
        const tooltip = document.createElement('div');
        tooltip.id = 'headerTooltip';
        tooltip.className = 'header-tooltip';
        tooltip.setAttribute('role', 'tooltip');
        tooltip.hidden = true;
        document.body.append(tooltip);
        let tooltipTarget;

        function hideTooltip() {
            tooltip.hidden = true;
            if (tooltipTarget) tooltipTarget.removeAttribute('aria-describedby');
            tooltipTarget = null;
        }

        function showTooltip(target) {
            hideTooltip();
            tooltipTarget = target;
            tooltip.textContent = target.dataset.tooltip;
            tooltip.hidden = false;
            target.setAttribute('aria-describedby', tooltip.id);
            const bounds = target.getBoundingClientRect();
            const width = tooltip.offsetWidth;
            const left = Math.max(8, Math.min(bounds.left + bounds.width / 2 - width / 2, window.innerWidth - width - 8));
            const below = bounds.bottom + 10;
            tooltip.style.left = left + 'px';
            tooltip.style.top = (below + tooltip.offsetHeight < window.innerHeight - 8 ? below : Math.max(8, bounds.top - tooltip.offsetHeight - 10)) + 'px';
        }

        document.querySelectorAll('.site-header [data-tooltip]').forEach(target => {
            target.addEventListener('pointerenter', () => showTooltip(target));
            target.addEventListener('pointerleave', hideTooltip);
            target.addEventListener('focus', () => showTooltip(target));
            target.addEventListener('blur', hideTooltip);
            target.addEventListener('click', hideTooltip);
        });
        window.addEventListener('resize', hideTooltip);
        document.addEventListener('scroll', hideTooltip, true);
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') hideTooltip();
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
        preference = Object.hasOwn(themeNames, event.newValue) ? event.newValue : null;
        applyTheme(preference || (systemTheme.matches ? 'dark' : 'light'));
    });
})();
