package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseUploadsIT;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

class UploadsIT extends BaseUploadsIT {

    @Test
    void uploadListDownloadAndCleanupWorkOverHttpWithUnicodeNames() throws Exception {
        verifyUploadRoundTrip("звіт.CSV", "id,name\n1,тест".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void servletRejectsAnOversizeFileWithUsefulErrorAndStoresNothing() throws Exception {
        verifyRejectedUpload(multipartFiles("large.csv", new byte[1025]), 413);
    }

    @Test
    void servletAlsoEnforcesTheCombinedRequestSize() throws Exception {
        MultiValueMap<String, Object> files = multipartFiles("one.csv", new byte[900]);
        files.addAll(multipartFiles("two.csv", new byte[900]));
        files.addAll(multipartFiles("three.csv", new byte[900]));
        files.addAll(multipartFiles("four.csv", new byte[900]));

        verifyRejectedUpload(files, 413);
    }

    @Test
    void mixedValidAndInvalidFilesAreRejectedAsOneBatch() throws Exception {
        MultiValueMap<String, Object> files = multipartFiles("valid.csv", new byte[]{1});
        files.addAll(multipartFiles("script.php", new byte[]{1}));

        verifyRejectedUpload(files, 400);
    }

    @Test
    void absentMultipartFieldAndWrongContentTypeAreRejected() throws Exception {
        MultiValueMap<String, Object> invalidMultipartFields = new LinkedMultiValueMap<>();
        invalidMultipartFields.add("other", "value");

        verifyRejectedUpload(invalidMultipartFields, 400);
        verifyWrongContentTypeRejected();
    }
}
