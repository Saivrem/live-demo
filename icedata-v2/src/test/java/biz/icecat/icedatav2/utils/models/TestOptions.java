package biz.icecat.icedatav2.utils.models;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

import java.util.List;

@Data
@Accessors(chain = true)
public class TestOptions {

    private String endpoint;
    private HttpStatusCode statusCode;
    private String expectedResponse;
    private List<UrlParam> urlParams;
    private String requestBodyFile;
    private HttpHeaders headers;

    public static TestOptions of(String endpoint, int code, String expectedResponse, List<UrlParam> urlParams, String requestBodyFile) {
        return new TestOptions().setEndpoint(endpoint)
                .setStatusCode(HttpStatusCode.valueOf(code))
                .setExpectedResponse(expectedResponse)
                .setUrlParams(urlParams)
                .setRequestBodyFile(requestBodyFile);
    }

    public static TestOptions of(String endpoint, int code, String expectedResponse, List<UrlParam> urlParams, String requestBodyFile, HttpHeaders headers) {
        return of(endpoint, code, expectedResponse, urlParams, requestBodyFile).setHeaders(headers);
    }
}
