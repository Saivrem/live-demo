package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.persistence.repository.SupplierRepository;
import biz.icecat.icedatav2.sax.SupplierHandler;
import biz.icecat.icedatav2.service.DataUpdateService;
import biz.icecat.icedatav2.utils.FileUtils;
import biz.icecat.icedatav2.utils.LoadingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static biz.icecat.icedatav2.utils.FileUtils.TEMP_DIR;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierDataUpdateService implements DataUpdateService {

    @Value("${icedata-v2.batch-size}")
    private Integer batchSize;
    private final ApplicationProperties properties;
    private final SupplierRepository repository;

    // TODO think about whole flow, logging and handling
    @Override
    public int update() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            SupplierHandler handler = new SupplierHandler(batchSize, repository::saveAll);

            Path suppliersList = getSuppliersList();

            saxParser.parse(Files.newInputStream(suppliersList), handler);

            return handler.getTotalSuppliers();
        } catch (ParserConfigurationException | SAXException saxEx) {
            log.warn("Failed to configure parser for {}", this.getClass().getSimpleName());
        } catch (IOException e) {
            log.warn("Failed to process suppliers list");
        }

        return 0;
    }

    private Path getSuppliersList() throws IOException {
        Path gzippedSuppliersList = Path.of(TEMP_DIR, properties.getSuppliersListFile());

        Files.createDirectories(gzippedSuppliersList);

        LoadingUtils.authenticate(properties.getServiceUserName(), properties.getServicePassword());
        LoadingUtils.downloadUrl(properties.getBaseUrl() + properties.getSuppliersListFile(), gzippedSuppliersList);

        Path unzippedTempFile = Path.of(TEMP_DIR, "suppliers_list.xml");
        FileUtils.unGzip(gzippedSuppliersList, unzippedTempFile);
        Files.deleteIfExists(gzippedSuppliersList);

        return unzippedTempFile;
    }
}
