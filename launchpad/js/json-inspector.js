/* Read-only JSON inspection for the modern Launchpad. No HTML from the response is interpreted. */
class JsonInspector {
    constructor(value) {
        this.raw = JSON.stringify(value, null, 2);
        this.query = '';
        this.mode = 'tree';
        this.root = this.createNode('$', value, '$', 0);
        this.container = this.element('section', 'json-inspector');
        this.container.setAttribute('aria-label', 'JSON response viewer');
        const toolbar = this.element('div', 'json-toolbar');
        const modes = this.element('div', 'json-modes');
        modes.setAttribute('role', 'group');
        modes.setAttribute('aria-label', 'JSON display mode');
        this.treeButton = this.button('Tree', () => this.setMode('tree'));
        this.rawButton = this.button('Raw', () => this.setMode('raw'));
        modes.append(this.treeButton, this.rawButton);
        this.search = this.element('input', 'json-search');
        this.search.type = 'search';
        this.search.placeholder = 'Find a key or value…';
        this.search.setAttribute('aria-label', 'Find a JSON key or value');
        this.search.addEventListener('input', () => {
            clearTimeout(this.searchTimer);
            this.searchTimer = setTimeout(() => {
                this.query = this.search.value.trim().toLowerCase();
                this.render();
                this.setMode('tree');
            }, 150);
        });
        this.collapseButton = this.button('Collapse', () => {
            this.tree.querySelectorAll('details').forEach(detail => {
                detail.open = false;
            });
        });
        toolbar.append(modes, this.search, this.collapseButton,
            this.button('Copy JSON', () => this.copy(this.raw, 'JSON copied.')),
            this.button('Download', () => this.download()));
        this.status = this.element('p', 'json-status');
        this.status.setAttribute('role', 'status');
        this.status.setAttribute('aria-live', 'polite');
        this.tree = this.element('div', 'json-tree');
        this.rawView = this.element('pre', 'json-raw');
        this.rawView.tabIndex = 0;
        this.rawView.setAttribute('aria-label', 'Raw JSON');
        this.rawView.textContent = this.raw;
        this.container.append(toolbar, this.status, this.tree, this.rawView);
        this.render();
        this.setMode('tree');
    }

    getContainer() {
        return this.container;
    }

    element(tag, className, text) {
        const element = document.createElement(tag);
        element.className = className;
        if (text !== undefined) element.textContent = text;
        return element;
    }

    button(text, action) {
        const button = this.element('button', 'json-button', text);
        button.type = 'button';
        button.addEventListener('click', action);
        return button;
    }

    createNode(key, value, path, depth) {
        const branch = value !== null && typeof value === 'object';
        const node = {key, value, path, depth, branch, children: [], ownMatch: false, match: true};
        if (branch) {
            node.children = Object.entries(value).map(([name, child]) => this.createNode(
                name, child, path + (Array.isArray(value) ? '[' + name + ']' : '[' + JSON.stringify(name) + ']'), depth + 1));
        }
        return node;
    }

    setMode(mode) {
        this.mode = mode;
        this.treeButton.setAttribute('aria-pressed', String(mode === 'tree'));
        this.rawButton.setAttribute('aria-pressed', String(mode === 'raw'));
        this.tree.hidden = mode !== 'tree';
        this.rawView.hidden = mode !== 'raw';
        this.collapseButton.disabled = mode !== 'tree';
        this.status.textContent = mode === 'raw'
            ? 'Raw JSON · copy or download the displayed payload.' : this.treeStatus;
    }

    findMatches(node) {
        node.ownMatch = Boolean(this.query) && (node.key.toLowerCase().includes(this.query)
            || (!node.branch && String(node.value).toLowerCase().includes(this.query)));
        let count = node.ownMatch ? 1 : 0;
        node.children.forEach(child => {
            count += this.findMatches(child);
        });
        node.match = !this.query || count > 0;
        return count;
    }

    render() {
        const count = this.findMatches(this.root);
        this.tree.replaceChildren();
        this.treeStatus = this.query
            ? count + ' matching field' + (count === 1 ? '' : 's') + ' · matching branches are shown below.'
            : 'Select a branch to explore it. Hover a row or focus its actions to copy a value or path.';
        this.status.textContent = this.treeStatus;
        if (!this.root.match) {
            this.tree.append(this.element('p', 'json-empty', 'No matching keys or values. Try another search.'));
            return;
        }
        this.tree.append(this.renderNode(this.root, false));
    }

    appendText(parent, text, className) {
        const span = this.element('span', className);
        const index = this.query ? text.toLowerCase().indexOf(this.query) : -1;
        if (index < 0) span.textContent = text;
        else {
            span.append(document.createTextNode(text.slice(0, index)),
                this.element('mark', 'json-match', text.slice(index, index + this.query.length)),
                document.createTextNode(text.slice(index + this.query.length)));
        }
        parent.append(span);
    }

    renderNode(node, inheritedMatch) {
        const item = this.element('div', 'json-item');
        const actions = this.element('div', 'json-row-actions');
        const copyValue = this.button('Value', () => this.copy(JSON.stringify(node.value, null, 2), 'Value copied.'));
        copyValue.setAttribute('aria-label', 'Copy value at ' + node.path);
        copyValue.title = 'Copy value as JSON';
        const copyPath = this.button('Path', () => this.copy(node.path, 'Path copied: ' + node.path));
        copyPath.setAttribute('aria-label', 'Copy path ' + node.path);
        copyPath.title = node.path;
        actions.append(copyValue, copyPath);
        const label = this.element(node.branch ? 'summary' : 'div', 'json-row');
        label.title = node.path;
        this.appendText(label, node.key, 'json-key');
        label.append(document.createTextNode(': '));
        if (node.branch) {
            const array = Array.isArray(node.value);
            label.append(this.element('span', 'json-count', (array ? '[' : '{') + node.children.length
                + (array ? ' items]' : ' fields}')));
            const detail = this.element('details', 'json-branch');
            detail.append(label);
            let loaded = false;
            const load = () => {
                if (loaded) return;
                loaded = true;
                const children = this.element('div', 'json-children');
                const showAll = inheritedMatch || node.ownMatch;
                const visible = node.children.filter(child => showAll || child.match);
                let offset = 0;
                const more = this.button('', () => showBatch());
                const showBatch = () => {
                    more.remove();
                    const end = Math.min(offset + 100, visible.length);
                    for (; offset < end; offset++) children.append(this.renderNode(visible[offset], showAll));
                    if (offset < visible.length) {
                        more.textContent = 'Show next ' + Math.min(100, visible.length - offset) + ' · ' + (visible.length - offset) + ' remaining';
                        children.append(more);
                    }
                };
                showBatch();
                detail.append(children);
            };
            detail.addEventListener('toggle', () => {
                if (detail.open) load();
            });
            detail.open = this.query ? (!inheritedMatch && !node.ownMatch) : node.depth < 1;
            if (detail.open) load();
            item.append(detail, actions);
        } else {
            const type = node.value === null ? 'null' : typeof node.value;
            this.appendText(label, JSON.stringify(node.value), 'json-value json-' + type);
            item.append(label, actions);
        }
        return item;
    }

    async copy(text, message) {
        try {
            if (navigator.clipboard && window.isSecureContext) await navigator.clipboard.writeText(text);
            else {
                const previous = document.activeElement;
                const input = this.element('textarea', 'json-copy-buffer');
                input.value = text;
                document.body.append(input);
                input.select();
                const copied = document.execCommand('copy');
                input.remove();
                if (previous) previous.focus();
                if (!copied) throw new Error('Clipboard unavailable');
            }
            this.status.textContent = message;
        } catch {
            this.status.textContent = 'Could not access the clipboard. Use Raw to select and copy the JSON.';
        }
    }

    download() {
        const url = URL.createObjectURL(new Blob([this.raw + '\n'], {type: 'application/json;charset=utf-8'}));
        const link = this.element('a', '');
        link.href = url;
        link.download = 'icecat-response.json';
        this.container.append(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        this.status.textContent = 'JSON download started.';
    }
}
