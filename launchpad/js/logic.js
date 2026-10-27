/**
 * Global variable for link copying
 * @type {string}
 */
let linkBuilder = "";
let codeStash = "";
let indent = "    ";

/**
 * Everything below should start only when the index page is fully loaded
 */
document.addEventListener('DOMContentLoaded', async function () {

    /**
     * Protocol
     * @type {string}
     */
    const protocol = "https://";

    /**
     * Code domain which is used in all links
     * @type {string}
     */
    const coreDomain = "icecat.biz/";

    const iceCLog = "iceclog.com/";

    /**
     * Link beginning for jsonCall
     * @type {string}
     */
    const jsonDomain = "live." + coreDomain + "api?";

    /**
     * Link beginning for XML_S3 call
     * @type {string}
     */
    const xmlDomain = "data." + coreDomain + "xml_s3/xml_server3.cgi?";

    /**
     * Link start for dropdowns;
     * @type {string}
     */
    const dataDomain = "data." + coreDomain + "export/";

    /**
     * FO link ending
     * @type {string}
     */
    const foLinkPart = "/p/a/b/desc-";

    /**
     *
     * @type {{
     * checkBoxes: NodeListOf<Element>,
     * appKey: HTMLElement,
     * language: HTMLElement,
     * fields: NodeListOf<Element>
     *     }}
     */
    const inputFields = {
        appKey: document.getElementById("appkey"),
        fields: document.querySelectorAll('input[type=text]'),
        checkBoxes: document.querySelectorAll('.asset-option input[type=checkbox]'),
        language: document.getElementById("lang")
    };

    /**
     * Radio buttons;
     * @type {{ean: HTMLElement,
     * productId: HTMLElement,
     * mpn: HTMLElement}}
     */
    const identifierType = document.getElementById('identifierType');
    const requestFormat = document.getElementById('requestFormat');
    const requestForm = document.getElementById('requestForm');
    let requestBusy = false;
    let liveRequestNumber = 0;
    let liveSdkPromise;

    /**
     * Buttons on the page
     * @type {{
     * output: HTMLElement,
     * liveGranular: HTMLElement,
     * checkAllBoxes: HTMLElement,
     * liveFull: HTMLElement,
     * liveGranularGetCode: HTMLElement,
     * clearButton: HTMLElement,
     * jsonLink: HTMLElement,
     * json: HTMLElement,
     * xmls3: HTMLElement,
     * foButton: HTMLElement,
     * liveFullGetCode: HTMLElement,
     * copyLink: HTMLElement
     * }}
     */
    const buttons = {
        json: document.getElementById('jsoncall'),
        jsonLink: document.getElementById('jsoncall2'),
        code: document.getElementById('getRequestCode'),
        copyLink: document.getElementById('copyLinkButton'),
        checkAllBoxes: document.getElementById('selectAllCheck'),
        output: document.getElementById('output'),
        clearButton: document.getElementById('clear')
    };

    /**
     * areas of the page
     * @type {{
     * mainContainer: HTMLElement,
     * refsDropdown: HTMLElement,
     * popUpContent: HTMLElement,
     * popUp: HTMLElement,
     * liveTemplate: Element,
     * share: HTMLElement,
     * indexDropdown: HTMLElement,
     * documentsDropdown: HTMLElement,
     * liveArea: HTMLElement,
     * trash: HTMLElement,
     * getCodeModal : HTMLElement
     * }}
     */
    const areas = {
        liveArea: document.getElementById("content"),
        liveTemplate: document.querySelector("#liveDiv"),
        mainContainer: document.getElementById('content'),
        popUpContent: document.getElementById("popUpContent"),
        popUp: document.getElementById("popUp"),
        trash: document.getElementById("copyBox"),
        indexDropdown: document.getElementById("indexDropdown"),
        refsDropdown: document.getElementById("refDropdown"),
        documentsDropdown: document.getElementById("documents"),
        getCodeModal: document.getElementById("getCodeModalBody")
    };

    /**
     * Response strings for popUp messages;
     * @type {{
     * brandOrMpnIsNotSet: string,
     * success: string,
     * idIsNotSet: string,
     * onlyId: string,
     * nullResponse: string,
     * notImplementedYet: string,
     * userNameIsNotSet: string,
     * eanIsNotSet: string
     * }}
     */
    const responses = {
        nullResponse: "No fields set",
        notImplementedYet: "This functionality is under construction",
        success: "Success",
        userNameIsNotSet: "Username is empty",
        idIsNotSet: "Product ID is not set",
        eanIsNotSet: "Product EAN is not set",
        brandOrMpnIsNotSet: "Product Brand or MPN are not set",
        onlyId: "Only call by ID is allowed"
    };

    /**
     *
     * @type {string[]}
     */
    const indexFiles = [
        "files.index.xml.gz",
        "files.index.csv.gz",
        "daily.index.xml.gz",
        "daily.index.csv.gz"
    ];

    const refs = [
        "CampaignsList.xml",
        "CategoriesList.xml.gz",
        "CategoryFeatureIntervalsList.xml",
        "CategoryFeaturesList.xml.gz",
        "DistributorList.xml.gz",
        "FeatureGroupsList.xml.gz",
        "FeatureLogosList.xml.gz",
        "FeatureValuesVocabularyList.xml.gz",
        "FeaturesList.xml.gz",
        "LanguageList.xml.gz",
        "LogisticsDataList.xml",
        "MeasuresList.xml.gz",
        "RelationsList.xml",
        "SupplierProductFamiliesListRequest.xml.gz",
        "SuppliersList.xml.gz"
    ];

    const documentLinks = {
        'XML': 'open-catalog-interface-oci-open-icecat-xml-and-full-icecat-xml-repositories/',
        'XML_S3': 'downloading-individual-product-xmls/',
        'JSON': 'manual-for-icecat-json-product-requests/',
        'Live': 'icecat-live-real-time-product-data-in-your-app/',
        'Live Granular': 'icecat-live-manual-for-granular-call/',
        'Product Story': 'product-story-manual-how-to-embed-rich-content-into-your-website/',
        'CSV': 'icecat-csv/',
        'URL': 'icecat-url-import-implementation-notes/',
        'Digital Asset Types': 'digital-asset-types-as-represented-in-icecats-open-catalog-interfaces/',
        'Push-In API': 'manual-for-icecat-push-api-api-in/',
        'Brand Permission Request': 'permission-required-from-full-icecat-brands-to-see-a-products-rich-media-assets-on-icecat-biz/',
        'Personalized interface': 'manuals-personalized-interface-files-and-catalogs'
    }

    const resourceRepository = document.getElementById('resourceRepository');
    const formats = {
        json: { action: 'Show JSON', help: 'View JSON here, or open the response in a new tab.', execute: previewJson },
        xml: { action: 'Open XML / CSV', help: 'XML and CSV responses open in a new tab.', execute: openXml },
        liveFull: { action: 'Show Icecat Live', help: 'Render the complete Icecat product datasheet below.', execute: executeFullLive },
        liveGranular: { action: 'Show selected blocks', help: 'Choose the product content blocks to render below.', execute: executeGranularLive },
        fo: { action: 'Open product page', help: 'Available when searching by Icecat product ID. Opens in a new tab.', execute: openProductPage }
    };

    function refreshResourceLinks() {
        dropdownSetter('indexDropdownBody');
        dropdownSetter('refDropdownBody');
        dropdownSetter('documentsBody');
    }
    [areas.indexDropdown, areas.refsDropdown, areas.documentsDropdown].forEach(group => {
        group.addEventListener('toggle', () => { if (group.open) refreshResourceLinks(); });
    });
    resourceRepository.addEventListener('change', refreshResourceLinks);
    inputFields.language.addEventListener('change', refreshResourceLinks);
    const resourcesMenu = document.getElementById('resourcesMenu');
    document.addEventListener('click', event => {
        if (!resourcesMenu.contains(event.target)) resourcesMenu.open = false;
    });
    resourcesMenu.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            resourcesMenu.open = false;
            resourcesMenu.querySelector('summary').focus();
        }
    });

    function updateRequestUi() {
        const names = { brandMpn: ['brand', 'mpn'], ean: ['ean'], id: ['productId'] }[identifierType.value];
        enableInputFields(names);
        document.querySelectorAll('[data-identifier-field]').forEach(field => {
            field.hidden = !names.includes(field.dataset.identifierField);
        });
        const format = requestFormat.value;
        buttons.json.textContent = requestBusy ? 'Loading…' : formats[format].action;
        buttons.jsonLink.hidden = format !== 'json';
        buttons.code.hidden = !['liveFull', 'liveGranular'].includes(format);
        document.getElementById('xmlOutputField').hidden = format !== 'xml';
        document.getElementById('formatHelp').textContent = formats[format].help;
        document.getElementById('contentSettings').hidden = !['json', 'liveGranular'].includes(format);
        document.getElementById('contentSettingsLabel').textContent = format === 'liveGranular' ? 'Select content blocks' : 'Choose JSON fields · optional';
        document.getElementById('contentHelp').textContent = format === 'liveGranular'
            ? 'Select at least one block to display.' : 'Leave all unchecked to request the full JSON response.';
        inputFields.checkBoxes.forEach(checkbox => {
            const unavailable = format === 'liveGranular' && ['generalinfoCheck', 'imageCheck', 'relatedCheck'].includes(checkbox.id);
            checkbox.disabled = unavailable;
            checkbox.closest('.asset-option').hidden = unavailable;
        });
        const available = [...inputFields.checkBoxes].filter(checkbox => !checkbox.disabled);
        const checked = available.filter(checkbox => checkbox.checked).length;
        buttons.checkAllBoxes.checked = checked === available.length;
        buttons.checkAllBoxes.indeterminate = checked > 0 && checked < available.length;
        requestFormat.disabled = requestBusy;
        identifierType.disabled = requestBusy;
        requestForm.querySelectorAll('.request-toolbar button').forEach(button => { button.disabled = requestBusy; });
        if (format === 'fo' && identifierType.value !== 'id') buttons.json.disabled = true;
    }
    identifierType.addEventListener('change', updateRequestUi);
    requestFormat.addEventListener('change', () => {
        updateRequestUi();
        if (requestFormat.value === 'liveGranular') document.getElementById('contentSettings').open = true;
    });
    buttons.checkAllBoxes.addEventListener('change', event => { checkAll(event); updateRequestUi(); });
    inputFields.checkBoxes.forEach(checkbox => checkbox.addEventListener('change', updateRequestUi));
    requestForm.addEventListener('input', () => { linkBuilder = ''; });
    requestForm.addEventListener('change', () => { linkBuilder = ''; });
    buttons.clearButton.addEventListener('click', () => { document.location.search = ''; });
    document.getElementById('retryRequest').addEventListener('click', () => requestForm.requestSubmit());
    requestForm.addEventListener('submit', event => {
        event.preventDefault();
        if (requestBusy || validateRadio() === 'null') return;
        try {
            if (!['xml', 'fo'].includes(requestFormat.value) && !notEmpty(getInputFieldsValues().username)) {
                document.getElementById('accountSettings').open = true;
                document.getElementById('username').focus();
                throw new Error(responses.userNameIsNotSet);
            }
            formats[requestFormat.value].execute();
        } catch (reason) {
            error(reason);
        }
    });
    buttons.jsonLink.addEventListener('click', () => {
        try {
            if (validateRadio() === 'null') return;
            const link = jsonLinkBuilder();
            if (!link) return;
            buildLinkForShare('json');
            window.open(link, '_blank', 'noopener');
            setFeedback('Opening JSON in a new tab.', 'success');
        } catch (reason) { error(reason); }
    });
    buttons.code.addEventListener('click', () => {
        try {
            if (validateRadio() === 'null') return;
            if (requestFormat.value === 'liveGranular' && Object.keys(getAssets()).length === 0) {
                throw new Error('Select at least one content block.');
            }
            areas.getCodeModal.innerText = requestFormat.value === 'liveGranular' ? getGranularLiveCode() : getFullLiveCode();
            document.getElementById('codeCopyStatus').textContent = '';
            new bootstrap.Modal(document.getElementById('getCodeModal')).show();
        } catch (reason) { error(reason); }
    });
    document.getElementById('copyEmbedCode').addEventListener('click', async () => {
        try {
            await copyLink(areas.getCodeModal.textContent);
            document.getElementById('codeCopyStatus').textContent = 'Embed code copied.';
        } catch {
            document.getElementById('codeCopyStatus').textContent = 'Could not copy. Select the code and copy it manually.';
        }
    });
    buttons.copyLink.addEventListener('click', async () => {
        try {
            if (!linkBuilder) buildLinkForShare(null);
            await copyLink(linkBuilder);
            setFeedback('Request link copied.', 'success');
        } catch (reason) { error(reason); }
    });

    function setFeedback(message, state) {
        areas.popUp.hidden = false;
        areas.popUp.setAttribute('role', state === 'error' ? 'alert' : 'status');
        areas.popUp.dataset.state = state;
        areas.popUpContent.textContent = message;
        document.getElementById('retryRequest').hidden = state !== 'error' || requestBusy;
    }
    function beginRequest(message) {
        requestBusy = true;
        areas.mainContainer.setAttribute('aria-busy', 'true');
        setFeedback(message, 'loading');
        updateRequestUi();
    }
    function finishRequest() {
        requestBusy = false;
        areas.mainContainer.setAttribute('aria-busy', 'false');
        updateRequestUi();
        document.getElementById('retryRequest').hidden = areas.popUp.dataset.state !== 'error';
    }
    function requestSucceeded(message) {
        document.getElementById('resultSummary').textContent = message;
        setFeedback(message, 'success');
        scroll('content');
    }
    function previewJson() {
        const requestLanguage = inputFields.language.value;
        const link = jsonLinkBuilder();
        if (!link) return;
        buildLinkForShare('json');
        beginRequest('Loading JSON. The previous result stays visible until the new response is ready.');
        $.getJSON(link).done(result => {
            const viewer = new JsonInspector(result.data === undefined ? result : result.data);
            areas.mainContainer.replaceChildren(viewer.getContainer());
            requestSucceeded('JSON response · ' + requestLanguage);
        }).fail(error).always(finishRequest);
    }
    function openXml() {
        const fields = getInputFieldsValues();
        let params = 'lang=' + encodeURIComponent(inputFields.language.value) + ';';
        switch (validateRadio()) {
            case 'id': params += 'icecat_id=' + encodeURIComponent(fields.productId); break;
            case 'ean': params += 'ean_upc=' + encodeURIComponent(fields.ean); break;
            case 'brandMpn': params += 'prod_id=' + encodeURIComponent(fields.mpn) + ';vendor=' + encodeURIComponent(fields.brand); break;
            default: return;
        }
        buildLinkForShare('xml');
        window.open(protocol + xmlDomain + params + ';output=' + encodeURIComponent(buttons.output.value), '_blank', 'noopener');
        setFeedback('Opening ' + buttons.output.selectedOptions[0].textContent + ' in a new tab.', 'success');
    }
    function openProductPage() {
        if (validateRadio() !== 'id') { popUp(responses.onlyId); return; }
        buildLinkForShare('fo');
        window.open(protocol + coreDomain + inputFields.language.value.toLowerCase() + foLinkPart
            + encodeURIComponent(getInputFieldsValues().productId) + '.html', '_blank', 'noopener');
        setFeedback('Opening the Icecat product page in a new tab.', 'success');
    }
    function executeFullLive() {
        const properties = getRequestProperties();
        properties.Type = 'html';
        buildLinkForShare('liveFull');
        liveFullExecute(properties, inputFields.language.value);
    }
    function executeGranularLive() {
        const assets = getAssets();
        if (Object.keys(assets).length === 0) { popUp('Select at least one content block.'); return; }
        const properties = getRequestProperties();
        buildLinkForShare('liveGranular');
        liveGranularExecute(assets, properties, inputFields.language.value);
    }

    function stringifyRequestProperties(requestProperties) {
        let builder = new StringBuilder('');
        for (const [key, value] of Object.entries(requestProperties)) {
            if (key !== 'selectAll') {
                builder.append(indent).append(indent).append(indent)
                    .append(JSON.stringify(key)).append(': ')
                    .append(JSON.stringify(value).replace(/</g, '\\u003c')).append(',\n');
            }
        }
        return builder.toString();
    }

//Helper functions

    /**
     * get Assets for Live granular call;
     * @returns {{}}
     */
    function getAssets() {
        let assets = {};
        inputFields.checkBoxes.forEach(function (element) {
            if (element.checked && !element.disabled) {
                let key = element.id.replace('Check', '');
                assets[key] = "#" + key;
            }
        });
        return assets;
    }

    /**
     * @returns returns an Object with the list of all text fields on HTML page {{}}
     */
    function getInputFieldsValues() {
        let resultSet = {};
        inputFields.fields.forEach(function (element) {
            resultSet[element.name] = element.value;
        });
        resultSet["appkey"] = document.getElementById("appkey").value;
        return resultSet;
    }

    /**
     * @returns Object with properties for Live call {{}}
     */
    function getRequestProperties() {
        let inputFieldsValues = getInputFieldsValues();
        let requestProperties = {};
        if (notEmpty(inputFieldsValues["username"])) {
            requestProperties['UserName'] = inputFieldsValues['username'];
            switch (validateRadio()) {
                case "id":
                    requestProperties['IcecatProductId'] = inputFieldsValues["productId"];
                    break;
                case "ean":
                    requestProperties['GTIN'] = inputFieldsValues['ean'];
                    break;
                case "brandMpn":
                    requestProperties['Brand'] = inputFieldsValues['brand'];
                    requestProperties['PartCode'] = inputFieldsValues['mpn'];
                    break;
                default:
                    throw new Error('Enter a product identifier.');
            }
        } else {
            document.getElementById('accountSettings').open = true;
            throw new Error(responses.userNameIsNotSet);
        }
        return requestProperties;
    }

    /**
     * Function to init dropdown menu
     * @param field string argument which refers to what menu should be initiated;
     */
    function dropdownSetter(field) {

        let targetMenu = $("#" + field);
        let link;

        let lang = inputFields.language.value;
        targetMenu.html("");

        if (field === "refDropdownBody") {
            refs.forEach((ref) => {
                link = protocol + dataDomain + getRepo() + "refs/" + ref;
                appendMenuItem(ref);
            });
            link = "share/SuppliersList.csv";
            appendMenuItem("SuppliersList.csv");
            link = protocol + dataDomain + getRepo() + lang + "/supplier_mapping.xml";
            appendMenuItem("supplier_mapping.xml");
        } else if (field === "indexDropdownBody") {
            indexFiles.forEach((file) => {
                link = protocol + dataDomain + getRepo() + lang + "/" + file;
                appendMenuItem(file);
            });
        } else if (field === 'documentsBody') {
            for (const [key, value] of Object.entries(documentLinks)) {
                link = protocol + iceCLog + value;
                appendMenuItem(key);
            }
        }

        /**
         * Shared function within this scope to prevent code duplication,
         * appends menu item to dropdown
         * @param file string parameter with the file name;
         */
        function appendMenuItem(file) {
            const item = document.createElement('a');
            item.className = 'dropdown-item';
            item.href = link;
            item.target = '_blank';
            item.rel = 'noopener noreferrer';
            item.textContent = file;
            targetMenu[0].append(item);
        }

    }

    /**
     * Live full request executor
     * @param requestFields Object with properties (username, product identifiers);
     * @param requestLang lang field, should be predefined;
     */
    function liveFullExecute(requestFields, requestLang) {
        renderLive(null, requestFields, requestLang);
    }

    function liveGranularExecute(assets, requestFields, requestLang) {
        renderLive(assets, requestFields, requestLang);
    }

    function loadLiveSdk() {
        if (window.IcecatLive) return Promise.resolve();
        if (!liveSdkPromise) {
            const script = document.createElement('script');
            script.src = 'https://live.icecat.biz/js/live-current-2.js';
            liveSdkPromise = new Promise((resolve, reject) => {
                script.onload = () => window.IcecatLive ? resolve() : reject(new Error('Icecat Live did not initialize. Please retry.'));
                script.onerror = () => reject(new Error('Could not load Icecat Live. Check the connection and retry.'));
                document.head.append(script);
            }).catch(reason => { liveSdkPromise = null; script.remove(); throw reason; });
        }
        return liveSdkPromise;
    }

    async function renderLive(assets, requestFields, requestLang) {
        const stage = document.createElement('div');
        stage.id = 'live-request-' + (++liveRequestNumber);
        stage.className = 'live-result live-staging';
        stage.style.width = areas.mainContainer.getBoundingClientRect().width + 'px';
        document.querySelector('.result-panel').append(stage);
        let target = '#' + stage.id;
        if (assets) {
            stage.dataset.blockCount = String(Object.keys(assets).length);
            stage.append(document.importNode(areas.liveTemplate.content, true));
            stage.querySelectorAll('[id]').forEach(element => { element.id = stage.id + '-' + element.id; });
            target = Object.fromEntries(Object.entries(assets).map(([name, selector]) => [name, '#' + stage.id + '-' + selector.slice(1)]));
        }
        beginRequest('Loading Icecat Live. The previous result stays visible until the response is ready.');
        try {
            await loadLiveSdk();
            await IcecatLive.getDatasheet(target, requestFields, requestLang);
            stage.classList.remove('live-staging');
            stage.style.removeProperty('width');
            areas.mainContainer.replaceChildren(stage);
            requestSucceeded((assets ? 'Selected Live blocks' : 'Icecat Live datasheet') + ' · ' + requestLang);
        } catch (reason) {
            stage.remove();
            error(reason);
        } finally {
            finishRequest();
        }
    }

    /**
     * enable\disable input fields depending on passed names
     * @param names
     */
    function enableInputFields(names) {
        inputFields.fields.forEach(field => {
            if (names.includes(field.name)) {
                field.disabled = false;
            } else if (field.name !== 'username') {
                field.disabled = true;
            }
        })
    }

    /**
     * Error message wrapper, used in catch().
     * @param object - reason for error;
     */
    function error(object) {
        const message = typeof object === 'string' ? object
            : object?.responseJSON?.message || object?.message || (object?.status ? 'Request failed (HTTP ' + object.status + ').' : 'Could not load the response. Check the connection and try again.');
        setFeedback(message, 'error');
    }

    function popUp(message) {
        setFeedback(message, 'error');
    }

    function scroll(id) {
        const target = document.getElementById(id) || areas.mainContainer;
        target.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
    }

    /**
     * Radio button validator
     * @returns {string}
     */
    function validateRadio() {
        const fields = getInputFieldsValues();
        const mode = identifierType.value;
        if (mode === 'id' && notEmpty(fields.productId)) return 'id';
        if (mode === 'ean' && notEmpty(fields.ean)) return 'ean';
        if (mode === 'brandMpn' && notEmpty(fields.brand) && notEmpty(fields.mpn)) return 'brandMpn';
        popUp(mode === 'id' ? responses.idIsNotSet : mode === 'ean' ? responses.eanIsNotSet : responses.brandOrMpnIsNotSet);
        const missing = mode === 'id' ? 'productId' : mode === 'ean' ? 'ean' : !notEmpty(fields.brand) ? 'brand' : 'mpn';
        document.getElementById(missing).focus();
        return 'null';
    }

    /**
     * Helper for JSON granular call;
     * @returns {string}
     */
    function getContent() {
        let content = "";
        inputFields.checkBoxes.forEach(function (element) {
            if (element.checked && !element.disabled) {
                let asset = element.id.replace('Check', '');
                if (equals(asset, "videos") || equals(asset, "tours3d")) {
                    if (!content.includes("multimedia")) {
                        content += "multimedia,";
                    }
                } else {
                    content += asset + ",";
                }
            }
        });
        return content;
    }

    /**
     * Helper which is building JSON request link;
     * @returns {string}
     */
    function jsonLinkBuilder() {
        let link = protocol + jsonDomain;
        let fields = getInputFieldsValues();
        if (notEmpty(fields["username"])) {
            link += "shopname=" + encodeURIComponent(fields["username"]);
            if (notEmpty(fields["appkey"])) {
                link += "&app_key=" + encodeURIComponent(fields["appkey"]);
            }
        } else {
            popUp(responses.userNameIsNotSet, 1500);
            throw new Error(responses.userNameIsNotSet);
        }

        link += "&lang=" + (inputFields.language.value);

        switch (validateRadio()) {
            case "id":
                link += "&icecat_id=" + encodeURIComponent(fields["productId"]);
                break;
            case "ean":
                link += "&GTIN=" + encodeURIComponent(fields["ean"]);
                break;
            case "brandMpn":
                link += "&Brand=" + encodeURIComponent(fields["brand"]) + "&ProductCode=" + encodeURIComponent(fields["mpn"]);
                break;
            default:
                return;
        }

        link += "&content=" + getContent();

        return link;
    }

    /**
     * Makes all checkboxes checked\unchecked
     * @param event
     */
    function checkAll(event) {
        inputFields.checkBoxes.forEach(function (element) {
            if (!element.disabled) element.checked = !!event.target.checked;
        });
    }

    /** Radio button validator
     * @returns {string}
     */
    function getRepo() {
        return resourceRepository.value;
    }

    /**
     * Checks if object value is empty string;
     * @param object
     * @returns {boolean}
     */
    function notEmpty(object) {
        return object != null && String(object).trim() !== '';
    }

    /***
     * Simple equal function, I'd like to suggest usage for string
     * @param object1 first object
     * @param object2 second object
     * @returns {boolean}
     */
    function equals(object1, object2) {
        return object1 === object2;
    }

    /**
     * Function to create the request link, may be shared with others
     * if some requestType button was pressed before copying - requestType will me mentioned in link
     * so, being copied to the browser's address bar you will not only get the same settings for
     * input fields but the same request will be immediately performed.
     *
     * The link is stored in variable linkBuilder;
     */
    function buildLinkForShare(requestType) {
        const fields = getInputFieldsValues();
        const params = new URLSearchParams();
        if (requestType !== 'xml') {
            if (notEmpty(fields.username)) params.set('username', fields.username);
            if (notEmpty(inputFields.appKey.value)) params.set('app_key', inputFields.appKey.value);
        }
        const names = { brandMpn: ['brand', 'mpn'], ean: ['ean'], id: ['productId'] }[identifierType.value];
        names.forEach(name => { if (notEmpty(fields[name])) params.set(name, fields[name]); });
        params.set('radio', identifierType.value);
        params.set('lang', inputFields.language.value);
        params.set('format', requestFormat.value);
        params.set('repo', resourceRepository.value);
        if (requestFormat.value === 'xml') params.set('output', buttons.output.value);
        const content = [...inputFields.checkBoxes].filter(item => item.checked && !item.disabled)
            .map(item => item.id.replace('Check', '')).join(',');
        if (content) params.set('content', content);
        if (requestType) params.set('requestType', requestType);
        linkBuilder = document.location.origin + document.location.pathname + '?' + params.toString();
    }

    /**
     * Builds text for liveGranularCall
     * @returns {string|*}
     */
    function getGranularLiveCode() {
        let assets = getAssets();
        let code = new StringBuilder(`<!-- Put this into BODY -->\n`);

        if (Object.keys(assets).length > 0) {
            let divs = new StringBuilder("");
            for (const [key, value] of Object.entries(assets)) {
                if (key !== 'selectAll') {
                    divs.append(`<div id="` + value.substring(1) + `"></div>\n`);
                }
            }
            code.append(divs.toString())
                .append(`<script type="text/javascript">\n`)
                .append(indent)
                .append(`window.addEventListener('liveload', function() {\n`)
                .append(indent).append(indent)
                .append(`IcecatLive.getDatasheet(\n`)
                .append(indent).append(indent).append("{\n")
                .append(stringifyRequestProperties(assets));

            code.removeCharAtIndex(code.toString().length - 1);

            code.append(indent).append(indent).append("},\n")
                .append(indent).append(indent).append("{\n")
                .append(stringifyRequestProperties(getRequestProperties()));

            code.removeCharAtIndex(code.toString().length - 1);

            code.append(indent).append(indent).append("}, \"")
                .append(inputFields.language.value)
                .append("\")\n")
                .append(indent).append("});\n")
                .append(`</script>`)
                .append(`\n<script src='https://live.icecat.biz/js/live-current-2.js'></script>\n`);
            codeStash = code.toString();
            return code.toString();
        } else {
            return responses.nullResponse;
        }
    }

    function getFullLiveCode() {
        const properties = stringifyRequestProperties(getRequestProperties());
        const code = new StringBuilder('<div id="content"></div>\n<script>\n');
        code.append("window.addEventListener('liveload', function() {\n")
            .append(indent).append("IcecatLive.getDatasheet('#content', {\n")
            .append(properties);
        code.removeCharAtIndex(code.toString().length - 1);
        code.append(indent).append('}, ').append(JSON.stringify(inputFields.language.value))
            .append(');\n});\n</script>\n')
            .append('<script src="https://live.icecat.biz/js/live-current-2.js"></script>\n');
        codeStash = code.toString();
        return codeStash;
    }

    /**
     * Copies linkBuilder's content into the buffer, if variable is not set, buildLinkForShare(null) will be issues first
     */
    async function copyLink(text) {
        try {
            if (navigator.clipboard) { await navigator.clipboard.writeText(text); return; }
        } catch { /* Fall back for browsers that deny clipboard access. */ }
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.append(textArea);
        try {
            textArea.select();
            if (!document.execCommand('copy')) throw new Error('Clipboard access was denied.');
        } finally { textArea.remove(); }
    }

    /**
     * Reads shared link and setting up params
     */
    function readParams() {
        const urlParams = new URLSearchParams(window.location.search);
        if (notEmpty(urlParams.toString())) {
            inputFields.fields.forEach((element) => {
                let tempValue = urlParams.get(element.name);
                element.value = tempValue != null ? tempValue : "";
            });

            const sharedLanguage = urlParams.get('lang');
            if (sharedLanguage && Array.from(inputFields.language.options).some(option => option.value === sharedLanguage.toUpperCase())) {
                inputFields.language.value = sharedLanguage.toUpperCase();
            }

            let content = urlParams.get("content");
            if (content !== null) {
                let assets = {};
                content.split(",").forEach((contentPart) => {
                    switch (contentPart) {
                        case "multimedia":
                            assets["videosCheck"] = "videosCheck";
                            assets["tours3dCheck"] = "tours3dCheck";
                            break;
                        case "description":
                            assets["marketingtextCheck"] = "marketingtextCheck";
                            break;
                        case "generalinfo":
                            assets["generalinfoCheck"] = "generalinfoCheck";
                            break;
                        default:
                            assets[contentPart + "Check"] = contentPart + "Check";
                            break;
                    }
                });
                inputFields.checkBoxes.forEach((element) => {
                    if (element.id === assets[element.id]) {
                        element.checked = true;
                    }
                });
            }
            const sharedIdentifier = urlParams.get('radio');
            identifierType.value = ['brandMpn', 'ean', 'id'].includes(sharedIdentifier) ? sharedIdentifier : 'brandMpn';
            const sharedFormat = urlParams.get('format') || urlParams.get('requestType');
            if (Object.keys(formats).includes(sharedFormat)) requestFormat.value = sharedFormat;
            if (['freexml/', 'level4/'].includes(urlParams.get('repo'))) resourceRepository.value = urlParams.get('repo');
            if (urlParams.get("output") !== null) {
                buttons.output.value = urlParams.get("output");
            }
            if (urlParams.get("app_key") !== null) {
                inputFields.appKey.value = urlParams.get("app_key");
            }
            updateRequestUi();
            if (inputFields.appKey.value || fieldsHaveCustomAccount()) document.getElementById('accountSettings').open = true;
            if (requestFormat.value === 'liveGranular' || content) document.getElementById('contentSettings').open = true;
            if (Object.keys(formats).includes(urlParams.get('requestType'))) requestForm.requestSubmit();
        }
    }

    function fieldsHaveCustomAccount() {
        return document.getElementById('username').value !== 'openicecat-live';
    }

    updateRequestUi();
    const languageStatus = document.getElementById('languageStatus');
    try {
        const response = await fetch('/icedata/api/v2/languages');
        if (!response.ok) throw new Error('Language request failed');
        const languages = await response.json();
        if (!Array.isArray(languages) || languages.length === 0) throw new Error('No languages available');
        const options = languages
            .sort((a, b) => a.langName.localeCompare(b.langName))
            .map(language => new Option(language.langName, language.langCode.toUpperCase()));
        inputFields.language.replaceChildren(...options);
        if (options.some(option => option.value === 'EN')) inputFields.language.value = 'EN';
        languageStatus.textContent = '';
    } catch (error) {
        languageStatus.textContent = 'Could not load languages. English is available; reload to retry.';
    }
    inputFields.language.disabled = false;
    readParams();
    updateRequestUi();
});

class StringBuilder {
    string;

    constructor(str) {
        this.string = str;
    }

    append(suffix) {
        this.string += suffix;
        return this;
    }

    removeCharAtIndex(index) {
        this.string = this.string.substring(0, index - 1) + this.string.substring(index, this.string.length);
    }

    toString() {
        return this.string;
    }
}

