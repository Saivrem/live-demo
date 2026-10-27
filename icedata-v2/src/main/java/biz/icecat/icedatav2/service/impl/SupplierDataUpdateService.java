package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.aspect.Measured;
import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.mapping.converters.SupplierConverter;
import biz.icecat.icedatav2.repository.SupplierRepository;
import biz.icecat.icedatav2.sax.SupplierHandler;
import biz.icecat.icedatav2.service.DataUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SupplierDataUpdateService extends DataUpdateService {

    private final SupplierRepository repository;
    private final SupplierConverter converter;
    private final SupplierCsvExportService csvExportService;

    @Value("${files.suppliers-list}")
    private String suppliersListFile;

    public SupplierDataUpdateService(ApplicationProperties properties, SupplierRepository repository, SupplierConverter converter, SupplierCsvExportService csvExportService) {
        super(properties);
        this.repository = repository;
        this.converter = converter;
        this.csvExportService = csvExportService;
    }

    @Override
    @Measured(methodName = "Update Suppliers info")
    public int update() {
        SupplierHandler handler = new SupplierHandler(batchSize,
                (suppliers -> repository.saveAll(converter.domainsListToListOfEntities(suppliers)))
        );

        int processed = processFile(handler, suppliersListFile, "suppliers_list.xml");
        if (processed > 0) {
            try {
                csvExportService.export();
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Could not export suppliers CSV", exception);
            }
        }
        return processed;
    }
}
