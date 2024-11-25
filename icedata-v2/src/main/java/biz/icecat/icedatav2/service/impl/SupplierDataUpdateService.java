package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.repository.SupplierRepository;
import biz.icecat.icedatav2.sax.SupplierHandler;
import biz.icecat.icedatav2.service.DataUpdateService;
import biz.icecat.icedatav2.utils.SaxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static biz.icecat.icedatav2.utils.FileUtils.*;
import static biz.icecat.icedatav2.utils.LoadingUtils.authenticate;
import static biz.icecat.icedatav2.utils.LoadingUtils.downloadUrl;
import static java.nio.file.Files.deleteIfExists;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierDataUpdateService implements DataUpdateService {

    private final ApplicationProperties properties;
    private final SupplierRepository repository;
    @Value("${icedata-v2.batch-size}")
    private Integer batchSize;
    @Value("${files.suppliers-list}")
    private String suppliersList;

    // TODO think about whole flow, logging and handling
    @Override
    public int update() {
        try {
            SupplierHandler handler = new SupplierHandler(batchSize, repository::saveAll);
            SAXParser saxParser = SaxUtils.getParser();

            Path suppliersList = getSuppliersList();
            saxParser.parse(Files.newInputStream(suppliersList), handler);

            return handler.getProcessedSuppliersNumber();
        } catch (SAXException saxEx) {
            log.warn("Failed to configure parser for {}", this.getClass().getSimpleName());
        } catch (IOException e) {
            log.warn("Failed to process suppliers list");
        }

        return 0;
    }

    private Path getSuppliersList() throws IOException {
        Path gzippedSuppliersList = Path.of(TEMP_DIR, suppliersList);
        createTempFile(gzippedSuppliersList);

        authenticate(properties.getServiceUserName(), properties.getServicePassword());
        downloadUrl(properties.resolveIcecatLink(suppliersList), gzippedSuppliersList);

        Path unzippedTempFile = Path.of(TEMP_DIR, "suppliers_list.xml");
        unGzip(gzippedSuppliersList, unzippedTempFile);
        deleteIfExists(gzippedSuppliersList);

        return unzippedTempFile;
    }
}
