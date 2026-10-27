package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.models.entity.SupplierEntity;
import biz.icecat.icedatav2.repository.SupplierRepository;
import biz.icecat.icedatav2.utils.CsvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierCsvExportService {

    private final SupplierRepository repository;
    @Value("${share.directory:share}")
    private String shareDirectory;

    public synchronized void export() throws IOException {
        List<SupplierEntity> suppliers = repository.findAll(Sort.by("supplierId"));
        Path directory = Path.of(shareDirectory);
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".suppliers-", ".tmp");
        try {
            writeCsv(temporary, suppliers);
            // Apache runs as a different user and needs read access to the published file.
            if (Files.getFileStore(temporary).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-r--r--"));
            }
            Files.move(temporary, directory.resolve("SuppliersList.csv"), ATOMIC_MOVE, REPLACE_EXISTING);
            log.info("Exported {} suppliers to SuppliersList.csv", suppliers.size());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void writeCsv(Path file, List<SupplierEntity> suppliers) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(CsvUtils.toCsvHeader(SupplierEntity.class));
            writer.write("\r\n");
            for (String row : suppliers.stream()
                    .map(CsvUtils::toCsvRow)
                    .toList()) {
                writer.write(row);
                writer.write("\r\n");
            }
        }
    }
}
