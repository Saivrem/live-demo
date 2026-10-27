export const identifierFields = {brandMpn: ['brand', 'mpn'], ean: ['ean'], id: ['productId']};
export const assetNames = ['title', 'gallery', 'generalinfo', 'essentialinfo', 'featuregroups', 'marketingtext',
    'bulletpoints', 'manuals', 'featurelogos', 'productstory', 'related', 'reasonstobuy', 'videos', 'tours3d', 'reviews'];
export const outputNames = {productxml: 'Product XML', metaxml: 'Meta XML', productcsv: 'Product CSV'};
export const formats = {
    json: {name: 'JSON', action: 'Show JSON', help: 'View JSON here, or open the response in a new tab.'},
    xml: {name: 'XML / CSV', action: 'Open XML / CSV', help: 'XML and CSV responses open in a new tab.'},
    liveFull: {
        name: 'Icecat Live · full',
        action: 'Show Icecat Live',
        help: 'Render the complete Icecat product datasheet below.'
    },
    liveGranular: {
        name: 'Icecat Live · selected blocks',
        action: 'Show selected blocks',
        help: 'Choose the product content blocks to render below.'
    },
    fo: {
        name: 'Icecat product page',
        action: 'Open product page',
        help: 'Available when searching by Icecat product ID. Opens in a new tab.'
    }
};

export function selectedAssets(state) {
    return state.assets.filter(name => assetNames.includes(name)
        && (state.format !== 'liveGranular' || !['generalinfo', 'image', 'related'].includes(name)));
}

export function validateRequest(state) {
    const errors = {};
    if (state.format === 'fo' && state.identifier !== 'id') {
        errors.identifierType = 'Choose Icecat product ID to open a product page.';
    }
    const messages = {
        brand: 'Enter a brand.', mpn: 'Enter a manufacturer part number.',
        ean: 'Enter an EAN or GTIN.', productId: 'Enter an Icecat product ID.'
    };
    identifierFields[state.identifier].forEach(name => {
        if (!state[name].trim()) errors[name] = messages[name];
    });
    if (!['xml', 'fo'].includes(state.format) && !state.username.trim()) errors.username = 'Enter your Icecat username.';
    if (state.format === 'liveGranular' && selectedAssets(state).length === 0) errors.assets = 'Select at least one content block.';
    return errors;
}

export function requestSummary(state) {
    const product = state.identifier === 'brandMpn' ? 'Brand + MPN · ' + state.brand + ' / ' + state.mpn
        : state.identifier === 'ean' ? 'EAN / GTIN · ' + state.ean : 'Icecat ID · ' + state.productId;
    return {
        product,
        language: state.languageName ? state.languageName + ' (' + state.language + ')' : state.language,
        response: state.format === 'xml' ? outputNames[state.output] : formats[state.format].name
    };
}
