package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.scheduler.InitialDatabaseImport;
import biz.icecat.icedatav2.scheduler.DatabaseUpdateTask;
import org.springframework.boot.DefaultApplicationArguments;
import biz.icecat.icedatav2.mapping.converters.SupplierConverter;
import biz.icecat.icedatav2.models.entity.SupplierEntity;
import biz.icecat.icedatav2.utils.CsvUtils;
import biz.icecat.icedatav2.service.impl.SupplierDataUpdateService;
import biz.icecat.icedatav2.repository.SupplierRepository;
import biz.icecat.icedatav2.service.impl.SupplierCsvExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.xml.sax.helpers.DefaultHandler;
import org.springframework.data.domain.Sort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplierCsvExportServiceTest {
    @TempDir
    Path directory;

    private final SupplierRepository repository = mock(SupplierRepository.class);

    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void supplierImportExportsOnlyWhenRecordsWereProcessed(int processed) throws Exception {
        var exporter = mock(SupplierCsvExportService.class);
        var importer = new SupplierDataUpdateService(mock(ApplicationProperties.class), repository,
                mock(SupplierConverter.class), exporter) {
            @Override
            protected int processFile(DefaultHandler handler, String source, String extracted) {
                return processed;
            }
        };
        ReflectionTestUtils.setField(importer, "batchSize", 1000);

        assertThat(importer.update()).isEqualTo(processed);

        if (processed > 0) verify(exporter).export();
        else verifyNoInteractions(exporter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"initial", "scheduled"})
    void completedImportPublishesCsvForBothEntryPoints(String entryPoint) throws Exception {
        SupplierCsvExportService csvExporter = exporter(directory);
        SupplierDataUpdateService importer = new SupplierDataUpdateService(mock(ApplicationProperties.class),
                repository, mock(SupplierConverter.class), csvExporter) {
            @Override
            protected int processFile(DefaultHandler handler, String source, String extracted) {
                when(repository.findAll(Sort.by("supplierId"))).thenReturn(List.of(
                        new SupplierEntity().setSupplierId(1L).setSupplierName("Imported")));
                return 1;
            }
        };
        ReflectionTestUtils.setField(importer, "batchSize", 1000);

        if ("initial".equals(entryPoint)) {
            new InitialDatabaseImport(List.of(importer)).run(new DefaultApplicationArguments("--initial-start"));
        } else {
            new DatabaseUpdateTask(List.of(importer)).updateDatabase();
        }

        assertThat(Files.readString(directory.resolve("SuppliersList.csv")))
                .isEqualTo("\"supplier_id\",\"supplier_name\",\"is_sponsor\",\"brand_logo\"\r\n\"1\",\"Imported\",\"0\",\"\"\r\n");
    }

    @Test
    void csvRowEnclosesNullEmptyAndWhitespaceFieldsWithoutChangingTheirContents() {
        assertThat(CsvUtils.toCsvRow(new SupplierEntity())).isEqualTo("\"\",\"\",\"0\",\"\"");
        assertThat(CsvUtils.toCsvRow(new SupplierEntity().setSupplierId(7L).setSupplierName("  Brand\t ")
                .setBrandLogo("C:\\logos\\brand.png")))
                .isEqualTo("\"7\",\"  Brand\t \",\"0\",\"C:\\logos\\brand.png\"");
    }

    @Test
    void exportsDatabaseColumnsInUtf8AndEscapesCsvText() throws Exception {
        when(repository.findAll(Sort.by("supplierId"))).thenReturn(List.of(
                new SupplierEntity().setSupplierId(1L).setSupplierName("Бренд, \"One\"\r\nTwo")
                        .setIsSponsor(1).setBrandLogo("https://example.com/logo.png"),
                new SupplierEntity().setSupplierId(2L).setSupplierName("Other").setIsSponsor(0)));

        exporter(directory).export();

        assertThat(Files.readString(directory.resolve("SuppliersList.csv"))).isEqualTo(
                "\"supplier_id\",\"supplier_name\",\"is_sponsor\",\"brand_logo\"\r\n"
                        + "\"1\",\"Бренд, \"\"One\"\"\r\nTwo\",\"1\",\"https://example.com/logo.png\"\r\n"
                        + "\"2\",\"Other\",\"0\",\"\"\r\n");
        verify(repository).findAll(Sort.by("supplierId"));
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactly("SuppliersList.csv");
        }
    }

    @Test
    void exportReadsExistingDatabaseAndNextExportReplacesIt() throws Exception {
        when(repository.findAll(Sort.by("supplierId"))).thenReturn(
                List.of(new SupplierEntity().setSupplierId(1L).setSupplierName("Old")), List.of());
        var service = exporter(directory.resolve("share"));
        service.export();
        assertThat(Files.readString(directory.resolve("share/SuppliersList.csv"))).contains("Old");

        service.export();

        assertThat(Files.readString(directory.resolve("share/SuppliersList.csv")))
                .isEqualTo("\"supplier_id\",\"supplier_name\",\"is_sponsor\",\"brand_logo\"\r\n");
    }

    @Test
    void failedExportKeepsPreviousFileAndRemovesTemporaryFile() throws Exception {
        Files.writeString(directory.resolve("SuppliersList.csv"), "previous CSV");
        SupplierEntity brokenSupplier = null; // Invalid database row must not replace the published CSV.
        when(repository.findAll(Sort.by("supplierId"))).thenReturn(java.util.Collections.singletonList(brokenSupplier));

        assertThatThrownBy(() -> exporter(directory).export())
                .isInstanceOf(NullPointerException.class);

        assertThat(Files.readString(directory.resolve("SuppliersList.csv"))).isEqualTo("previous CSV");
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).containsExactly("SuppliersList.csv");
        }
    }
    private SupplierCsvExportService exporter(Path directory) {
        SupplierCsvExportService service = new SupplierCsvExportService(repository);
        ReflectionTestUtils.setField(service, "shareDirectory", directory.toString());
        return service;
    }

}
