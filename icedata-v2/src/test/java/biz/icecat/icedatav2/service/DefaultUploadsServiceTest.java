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
        MockMultipartFile file = new MockMultipartFile("files[]", "boundary.xlsx", "application/octet-stream", new byte[20 * 1024 * 1024]);
        assertThat(uploadsService.store(List.of(file)).get(0).getSize()).isEqualTo(20 * 1024 * 1024);
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
