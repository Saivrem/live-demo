package biz.icecat.icedatav2.it;

import biz.icecat.icedatav2.scheduler.DatabaseUpdateTask;
import biz.icecat.icedatav2.models.entity.LanguageEntity;
import biz.icecat.icedatav2.models.entity.SupplierEntity;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import biz.icecat.icedatav2.repository.SupplierRepository;
import biz.icecat.icedatav2.service.impl.LanguagesDataUpdateService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(value = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseDataImportIT extends BaseIT {

    private static final Map<String, byte[]> SOURCE_DOCUMENTS = new ConcurrentHashMap<>();
    private static final HttpServer SOURCE_SERVER;
    private static final Path DOWNLOAD_DIRECTORY;

    static {
        try {
            DOWNLOAD_DIRECTORY = Files.createTempDirectory("icedata-import-it-");
            SOURCE_SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SOURCE_SERVER.createContext("/", exchange -> {
                byte[] compressedDocument = SOURCE_DOCUMENTS.get(exchange.getRequestURI().getPath());
                if (compressedDocument == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    exchange.sendResponseHeaders(200, compressedDocument.length);
                    exchange.getResponseBody().write(compressedDocument);
                }
                exchange.close();
            });
            SOURCE_SERVER.start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void importSource(DynamicPropertyRegistry registry) {
        registry.add("icedata-v2.icecat-base-url", () -> "http://127.0.0.1:" + SOURCE_SERVER.getAddress().getPort() + "/");
        registry.add("icedata-v2.icecat-repo", () -> "test/");
        registry.add("icedata-v2.batch-size", () -> "2");
        registry.add("icedata-v2.temp-directory", () -> DOWNLOAD_DIRECTORY.toString());
    }

    @Autowired
    private DatabaseUpdateTask databaseUpdateTask;
    @Autowired
    private LanguagesDataUpdateService languagesDataUpdateService;
    @Autowired
    private LanguagesRepository languagesRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void sourceDocuments() throws Exception {
        SOURCE_DOCUMENTS.clear();
        String languageRecords = IntStream.rangeClosed(1, 5)
                .mapToObj(recordId -> """
                        <Language ID="%d" Code="Language %d" ShortCode="en" Updated="2024-01-02T06:04:05+03:00"/>
                        """.formatted(recordId, recordId))
                .collect(Collectors.joining());
        SOURCE_DOCUMENTS.put("/test/refs/LanguageList.xml.gz", gzip("LanguageList", languageRecords));
        supplierDocument("Original");
    }

    @AfterAll
    static void stopSourceAndRemoveOnlyTestDownloads() throws Exception {
        SOURCE_SERVER.stop(0);
        try (Stream<Path> paths = Files.walk(DOWNLOAD_DIRECTORY)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    protected void updateDatabase() {
        databaseUpdateTask.updateDatabase();
    }

    protected void importLanguages() {
        assertThat(languagesDataUpdateService.update()).isEqualTo(5);
    }

    protected void makeLanguagesSourceUnavailable() {
        SOURCE_DOCUMENTS.remove("/test/refs/LanguageList.xml.gz");
    }

    protected void verifyImportedCounts() {
        assertThat(languagesRepository.count()).isEqualTo(5);
        assertThat(supplierRepository.count()).isEqualTo(5);
    }

    protected void verifyImportedLanguage() {
        LanguageEntity language = languagesRepository.findById(5L).orElseThrow();
        assertThat(language.getIntLangName()).isEqualTo("Language 5");
        assertThat(language.getUpdated()).isEqualTo(Instant.parse("2024-01-02T03:04:05Z").toEpochMilli());
    }

    protected void verifyImportedSupplier(String prefix) {
        SupplierEntity supplier = supplierRepository.findById(5L).orElseThrow();
        assertThat(supplier.getSupplierName()).isEqualTo(prefix + " 5");
        assertThat(supplier.getIsSponsor()).isEqualTo(1);
    }

    protected void supplierDocument(String prefix) throws Exception {
        String supplierRecords = IntStream.rangeClosed(1, 5)
                .mapToObj(recordId -> """
                        <Supplier ID="%d" Name="%s %d" Sponsor="1" LogoPic="logo.png"/>
                        """.formatted(recordId, prefix, recordId))
                .collect(Collectors.joining());
        SOURCE_DOCUMENTS.put("/test/refs/SuppliersList.xml.gz", gzip("SuppliersList", supplierRecords));
    }

    private byte[] gzip(String root, String content) throws Exception {
        ByteArrayOutputStream compressedDocument = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressedDocument)) {
            gzip.write(("<ICECAT-interface><" + root + ">" + content + "</" + root + "></ICECAT-interface>")
                    .getBytes(StandardCharsets.UTF_8));
        }
        return compressedDocument.toByteArray();
    }
}
