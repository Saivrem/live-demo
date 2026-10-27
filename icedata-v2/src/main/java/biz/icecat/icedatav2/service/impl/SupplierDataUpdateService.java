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

    @Value("${files.suppliers-list}")
    private String suppliersListFile;

    public SupplierDataUpdateService(ApplicationProperties properties, SupplierRepository repository, SupplierConverter converter) {
        super(properties);
        this.repository = repository;
        this.converter = converter;
    }

    @Override
    @Measured(methodName = "Update Suppliers info")
    public int update() {
        SupplierHandler handler = new SupplierHandler(batchSize,
                (suppliers -> repository.saveAll(converter.domainsListToListOfEntities(suppliers)))
        );

        return processFile(handler, suppliersListFile, "suppliers_list.xml");
    }
}
