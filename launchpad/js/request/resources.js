export function createResourcesMenu(menu, repository, language) {
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


    function refresh() {
        const base = 'https://data.icecat.biz/export/' + repository.value;
        const lang = encodeURIComponent(language.value);
        const groups = {
            indexDropdownBody: indexFiles.map(name => [name, base + lang + '/' + name]),
            refDropdownBody: [...refs.map(name => [name, base + 'refs/' + name]),
                ['SuppliersList.csv', '/share/SuppliersList.csv'], ['supplier_mapping.xml', base + lang + '/supplier_mapping.xml']],
            documentsBody: Object.entries(documentLinks).map(([name, path]) => [name, 'https://iceclog.com/' + path])
        };
        Object.entries(groups).forEach(([id, links]) => {
            const items = links.map(([name, href]) => {
                const item = document.createElement('a');
                item.className = 'dropdown-item';
                item.href = href;
                item.target = '_blank';
                item.rel = 'noopener noreferrer';
                item.textContent = name;
                return item;
            });
            menu.querySelector('#' + id).replaceChildren(...items);
        });
    }

    repository.addEventListener('change', refresh);
    language.addEventListener('change', refresh);
    document.addEventListener('click', event => {
        if (!menu.contains(event.target)) menu.open = false;
    });
    menu.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            menu.open = false;
            menu.querySelector('summary').focus();
        }
    });
    refresh();
    return {refresh};
}
