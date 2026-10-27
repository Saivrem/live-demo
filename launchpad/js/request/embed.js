import {liveAssets, liveProperties} from './links.js';

export function embedCode(state) {
    const stringify = value => JSON.stringify(value, null, 4).replace(/</g, '\\u003c');
    const assets = state.format === 'liveGranular' ? liveAssets(state) : null;
    const properties = liveProperties(state);
    const containers = assets ? Object.values(assets).map(selector => '<div id="' + selector.slice(1) + '"></div>').join('\n')
        : '<div id="content"></div>';
    return containers + '\n<script>\n'
        + "window.addEventListener('liveload', function() {\n"
        + '    IcecatLive.getDatasheet(' + (assets ? stringify(assets) : "'#content'") + ', '
        + stringify(properties) + ', ' + stringify(state.language) + ');\n'
        + '});\n</script>\n<script src="https://live.icecat.biz/js/live-current-2.js"></script>\n';
}
