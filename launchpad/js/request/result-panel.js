import {formats, requestSummary} from './state.js';

export function createResultPanel(panel, requestForm) {
    const content = panel.querySelector('#content');
    const heading = panel.querySelector('.result-heading');
    const expand = panel.querySelector('#expandResult');
    const back = panel.querySelector('#backToForm');
    let expanded = false;
    let previousScroll = 0;
    let previousFocus;
    let background = [];
    const previousRole = panel.getAttribute('role');

    const headingSize = new ResizeObserver(() => {
        panel.style.setProperty('--result-heading-height', heading.getBoundingClientRect().height + 'px');
    });
    headingSize.observe(heading);

    function setExpanded(value) {
        if (value === expanded) return;
        expanded = value;
        if (expanded) {
            previousScroll = window.scrollY;
            previousFocus = document.activeElement;
            const offset = Math.max(0, -panel.getBoundingClientRect().top);
            background = [...document.querySelectorAll('.site-header, .site-footer, .request-panel, .page-introduction')]
                .filter(element => !panel.contains(element)).map(element => [element, element.inert]);
            background.forEach(([element]) => { element.inert = true; });
            panel.classList.add('result-panel-expanded');
            document.body.classList.add('result-expanded');
            panel.setAttribute('role', 'dialog');
            panel.setAttribute('aria-modal', 'true');
            panel.scrollTop = offset;
            expand.focus({preventScroll: true});
        } else {
            panel.classList.remove('result-panel-expanded');
            document.body.classList.remove('result-expanded');
            background.forEach(([element, inert]) => { element.inert = inert; });
            panel.removeAttribute('aria-modal');
            if (previousRole === null) panel.removeAttribute('role');
            else panel.setAttribute('role', previousRole);
            previousFocus?.focus({preventScroll: true});
            window.scrollTo({top: previousScroll, behavior: 'instant'});
        }
        expand.textContent = expanded ? 'Exit expanded view' : 'Expand result';
        expand.setAttribute('aria-expanded', String(expanded));
    }

    expand.addEventListener('click', () => setExpanded(!expanded));
    back.addEventListener('click', () => {
        setExpanded(false);
        requestForm.querySelector('[type="submit"]').focus({preventScroll: true});
        requestForm.scrollIntoView({behavior: 'auto', block: 'start'});
    });
    document.addEventListener('keydown', event => {
        // Let SDK dialogs outside the result and their own keyboard handling keep working.
        if (!expanded || event.defaultPrevented || !panel.contains(event.target)) return;
        if (event.key === 'Escape') { event.preventDefault(); setExpanded(false); }
        if (event.key !== 'Tab') return;
        const focusable = [...panel.querySelectorAll('a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), summary, [tabindex]:not([tabindex="-1"])')]
            .filter(element => element.getClientRects().length && !element.closest('[inert]'));
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    });

    function show(node, state) {
        content.replaceChildren(node);
        panel.querySelector('#resultSummary').textContent = formats[state.format].name + ' response · ' + state.language;
        const details = panel.querySelector('#resultRequestDetails');
        details.replaceChildren(...Object.entries(requestSummary(state)).map(([key, value]) => {
            const item = document.createElement('div');
            const term = document.createElement('dt');
            term.textContent = {product: 'Product', language: 'Language', response: 'Response'}[key];
            const description = document.createElement('dd');
            description.textContent = value;
            item.append(term, description);
            return item;
        }));
        details.hidden = false;
        expand.disabled = false;
        if (expanded) panel.scrollTo({top: 0, behavior: 'auto'});
        else panel.scrollIntoView({behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start'});
    }

    return {show};
}
