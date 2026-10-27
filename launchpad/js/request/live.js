// Existing staging and SDK lifecycle, isolated from form state and request feedback.
export function createLiveRenderer(content, template) {
    let requestNumber = 0;
    let sdkPromise;

    function loadSdk() {
        if (window.IcecatLive) return Promise.resolve();
        if (!sdkPromise) {
            const script = document.createElement('script');
            script.src = 'https://live.icecat.biz/js/live-current-2.js';
            sdkPromise = new Promise((resolve, reject) => {
                script.onload = () => window.IcecatLive ? resolve() : reject(new Error('Icecat Live did not initialize. Please retry.'));
                script.onerror = () => reject(new Error('Could not load Icecat Live. Check the connection and retry.'));
                document.head.append(script);
            }).catch(reason => {
                sdkPromise = null;
                script.remove();
                throw reason;
            });
        }
        return sdkPromise;
    }

    async function render(assets, properties, language) {
        const stage = document.createElement('div');
        stage.id = 'live-request-' + (++requestNumber);
        stage.className = 'live-result live-staging';
        stage.style.width = content.getBoundingClientRect().width + 'px';
        content.closest('.result-panel').append(stage);
        let target = '#' + stage.id;
        if (assets) {
            stage.dataset.blockCount = String(Object.keys(assets).length);
            stage.append(document.importNode(template.content, true));
            stage.querySelectorAll('[id]').forEach(element => {
                element.id = stage.id + '-' + element.id;
            });
            target = Object.fromEntries(Object.entries(assets).map(([name, selector]) => [name, '#' + stage.id + '-' + selector.slice(1)]));
        }
        try {
            await loadSdk();
            await window.IcecatLive.getDatasheet(target, properties, language);
            stage.classList.remove('live-staging');
            stage.style.removeProperty('width');
            return stage;
        } catch (reason) {
            stage.remove();
            throw reason;
        }
    }

    return {render};
}
