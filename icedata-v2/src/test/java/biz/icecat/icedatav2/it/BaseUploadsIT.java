package biz.icecat.icedatav2.it;

import biz.icecat.icedatav2.service.UploadsService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static biz.icecat.icedatav2.utils.TestUtils.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;

/** Real Tomcat multipart parsing: MockMvc does not enforce servlet upload limits. */
@TestPropertySource(properties = {
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=3KB"
})
public abstract class BaseUploadsIT extends BaseIT {

    @Autowired
    private UploadsService uploadsService;

    @AfterEach
    void cleanup() throws Exception {
        uploadsService.cleanup();
    }

    protected void verifyUploadRoundTrip(String name, byte[] content) throws Exception {
        ResponseEntity<String> response = upload(multipartFiles(name, content));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode uploadedFile = objectMapper.readTree(response.getBody()).get(0);
        assertThat(uploadedFile.get("name").asText()).isEqualTo(name);
        URI downloadUri = URI.create(serverUrl() + uploadedFile.get("url").asText());
        ResponseEntity<byte[]> downloadResponse = restTemplate.getForEntity(downloadUri, byte[].class);
        assertThat(downloadResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(downloadResponse.getBody()).isEqualTo(content);
        assertThat(downloadResponse.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(downloadResponse.getHeaders().getContentDisposition().getFilename()).isEqualTo(name);
        assertThat(objectMapper.readTree(restTemplate.getForObject(endpoint(), String.class))).hasSize(1);
        uploadsService.cleanup();
        assertThat(restTemplate.getForEntity(downloadUri, String.class).getStatusCode().value()).isEqualTo(404);
    }

    protected void verifyRejectedUpload(MultiValueMap<String, Object> files, int status) throws Exception {
        ResponseEntity<String> response = upload(files);
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        if (status == 413) {
            assertThat(response.getBody()).contains("message");
        }
        assertThat(uploadsService.list()).isEmpty();
    }

    protected void verifyWrongContentTypeRejected() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        assertThat(restTemplate.postForEntity(endpoint(), new HttpEntity<>("{}", headers), String.class)
                .getStatusCode().value()).isEqualTo(415);
        assertThat(uploadsService.list()).isEmpty();
    }

    private String endpoint() {
        return buildRequestUrl("uploads");
    }

    protected MultiValueMap<String, Object> multipartFiles(String name, byte[] content) {
        MultiValueMap<String, Object> multipartFiles = new LinkedMultiValueMap<>();
        multipartFiles.add("files[]", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return name;
            }
        });
        return multipartFiles;
    }

    private ResponseEntity<String> upload(MultiValueMap<String, Object> files) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.postForEntity(endpoint(), new HttpEntity<>(files, headers), String.class);
    }
}
