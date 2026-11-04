package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.base.BaseUploadsTest;
import biz.icecat.icedatav2.models.api.ApiUpload;
import biz.icecat.icedatav2.service.impl.DefaultUploadsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class DefaultUploadsServiceTest extends BaseUploadsTest {

    @Test
    void failedSecondTransferRollsBackTheEntireBatchIncludingTemporaryFiles() throws Exception {
        MockMultipartFile failingUpload = new MockMultipartFile("files[]", "failingUpload.csv", "text/csv", new byte[]{2}) {
            @Override
            public void transferTo(Path target) throws IOException {
                Files.writeString(target, "partial upload");
                throw new IOException("Simulated disk failure");
            }
        };
        assertThatThrownBy(() -> uploadsService.store(List.of(multipartFile("first.csv"), failingUpload)))
                .isInstanceOf(IOException.class).hasMessageContaining("disk failure");
        try (Stream<Path> storedFiles = Files.list(directory)) {
            assertThat(storedFiles.toList()).isEmpty();
        }
    }

    @Test
    void windowsPathsAreRemovedAndUnicodeUrlsSurviveStorageRecreation() throws Exception {
        ApiUpload storedUpload = uploadsService.store(List.of(multipartFile("C:\\fakepath\\звіт.CSV"))).get(0);
        assertThat(storedUpload.getName()).isEqualTo("звіт.CSV");
        assertThat(storedUpload.getUrl()).contains("%D0").doesNotContain("fakepath");
        assertThat(new DefaultUploadsService(directory.toString()).list()).containsExactly(storedUpload);
    }

    @Test
    void acceptsExactlyTwentyMiB() throws Exception {
        byte[] content = new byte[20 * 1024 * 1024];
        java.util.Arrays.fill(content, (byte) 'x');
        content[1] = ',';
        MockMultipartFile file = new MockMultipartFile("files[]", "boundary.csv", "text/csv", content);
        assertThat(uploadsService.store(List.of(file)).get(0).getSize()).isEqualTo(20 * 1024 * 1024);
    }

    @Test
    void quotaIncludesEarlierUploadsNestedFilesAndTheWholeBatch() throws Exception {
        long limit = 5L * 1024 * 1024 * 1024;
        Path filler = Files.createDirectories(directory.resolve("other")).resolve("existing.bin");
        try (var sparse = new java.io.RandomAccessFile(filler.toFile(), "rw")) {
            sparse.setLength(limit - 6);
        }
        var first = multipartFile("first.csv");
        uploadsService.store(List.of(first));
        assertThatThrownBy(() -> uploadsService.store(List.of(first, first)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(507));
        assertThat(uploadsService.list()).hasSize(1);
        uploadsService.store(List.of(first));
        assertThat(uploadsService.list()).hasSize(2);
    }

    @Test
    void newAndPreviouslyStoredUploadsHaveNoExecuteBits() throws Exception {
        uploadsService.store(List.of(multipartFile("first.csv")));
        Path stored;
        try (var paths = Files.list(directory)) {
            stored = paths.findFirst().orElseThrow();
        }
        assertThat(Files.isExecutable(stored)).isFalse();
        Files.setPosixFilePermissions(stored, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        new DefaultUploadsService(directory.toString());
        assertThat(Files.isExecutable(stored)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"csv", "doc", "docx", "xls", "xlsx"})
    void renamedExecutableIsRejectedAndEarlierFilesAreRemoved(String extension) throws Exception {
        var fake = new MockMultipartFile("files[]", "report." + extension, "text/csv",
                new byte[]{0x7f, 'E', 'L', 'F', 0});
        assertThatThrownBy(() -> uploadsService.store(List.of(multipartFile("valid.csv"), fake)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        try (var paths = Files.list(directory)) {
            assertThat(paths.toList()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello world", "a,b\n1", "a,b\n1,\"unterminated", "a,b\n1,2\u0000"})
    void rejectsMalformedCsv(String content) {
        var file = new MockMultipartFile("files[]", "report.csv", "text/csv",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> uploadsService.store(List.of(file))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptsQuotedUtf8CsvWithBomSemicolonsAndEmbeddedNewlines() throws IOException {
        var file = new MockMultipartFile("files[]", "report.csv", "application/octet-stream",
                "\uFEFFid;name\r\n1;\"слово;\n\"\"test\"\"\"\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(uploadsService.store(List.of(file))).hasSize(1);
    }

    @Test
    void acceptsOfficeDocumentsAndRejectsSwappedExtensions() throws Exception {
        try (var xlsx = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var docx = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             var xls = new org.apache.poi.hssf.usermodel.HSSFWorkbook()) {
            xlsx.createSheet();
            docx.createParagraph();
            xls.createSheet();
            var bytes = new java.io.ByteArrayOutputStream();
            xlsx.write(bytes);
            verifyOffice(bytes.toByteArray(), "xlsx", "docx");
            bytes.reset();
            docx.write(bytes);
            verifyOffice(bytes.toByteArray(), "docx", "xlsx");
            bytes.reset();
            xls.write(bytes);
            verifyOffice(bytes.toByteArray(), "xls", "doc");
        }
    }

    private void verifyOffice(byte[] bytes, String extension, String wrongExtension) throws IOException {
        uploadsService.store(List.of(new MockMultipartFile("files[]", "report." + extension, null, bytes)));
        assertThatThrownBy(() -> uploadsService.store(List.of(
                new MockMultipartFile("files[]", "report." + wrongExtension, null, bytes))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"csv", ".csv", "a.php.csv.exe", "a.csv/", "C:\\fakepath\\.csv"})
    void invalidNamesNeverCreateFiles(String name) throws Exception {
        assertThatThrownBy(() -> uploadsService.store(List.of(multipartFile(name)))).isInstanceOf(ResponseStatusException.class);
        assertThat(uploadsService.list()).isEmpty();
    }

    @Test
    void multibyteFilenameLengthIsLimitedBeforeAnyFileIsWritten() throws Exception {
        assertThatThrownBy(() -> uploadsService.store(List.of(multipartFile("first.csv"), multipartFile("я".repeat(100) + ".csv"))))
                .isInstanceOf(ResponseStatusException.class);
        try (Stream<Path> storedFiles = Files.list(directory)) {
            assertThat(storedFiles.toList()).isEmpty();
        }
    }

}
