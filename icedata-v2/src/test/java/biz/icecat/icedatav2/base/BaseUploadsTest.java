package biz.icecat.icedatav2.base;

import biz.icecat.icedatav2.service.UploadsService;
import biz.icecat.icedatav2.service.impl.DefaultUploadsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public abstract class BaseUploadsTest {

    @TempDir
    protected Path directory;

    protected UploadsService uploadsService;

    @BeforeEach
    void initializeUploadStorage() throws IOException {
        uploadsService = new DefaultUploadsService(directory.toString());
    }

    protected MockMultipartFile multipartFile(String filename) {
        return new MockMultipartFile("files[]", filename, "text/csv", new byte[]{1});
    }
}
