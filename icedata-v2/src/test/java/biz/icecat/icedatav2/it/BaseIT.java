package biz.icecat.icedatav2.it;

import biz.icecat.icedatav2.IcedataV2Application;
import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.utils.containers.mysql.MySqlContainer;
import biz.icecat.icedatav2.utils.models.TestOptions;
import biz.icecat.icedatav2.utils.models.UrlParam;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Network;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static biz.icecat.icedatav2.utils.TestConstants.URL_PATTERN;
import static biz.icecat.icedatav2.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BaseIT.BaseTestConfiguration.class)
@ContextConfiguration(classes = {IcedataV2Application.class}, loader = SpringBootContextLoader.class)
public class BaseIT {

    private static final Class<?> TEST_CLASS = BaseIT.class;

    public static final TestRestTemplate restTemplate = new TestRestTemplate();

    @ClassRule
    public static MySqlContainer mySqlContainer = MySqlContainer.create(TEST_CLASS);

    @ClassRule
    public static Network network = Network.newNetwork();

    @Autowired
    private ApplicationProperties properties;

    @LocalServerPort
    private int serverPort;

    static {
        mySqlContainer.withNetwork(network).start();
    }

    protected void verifyGetFlow(TestOptions options) {
        ResponseEntity<String> response = queryEndpointGet(
                options.getEndpoint(),
                options.getUrlParams(),
                options.getHeaders());

        assertEquals(options.getStatusCode(), response.getStatusCode());
        assertResponse(options, response);
    }

    protected void verifyPostFlow(TestOptions options) {
        ResponseEntity<String> response = queryEndpointPost(
                options.getEndpoint(), options.getHeaders(),
                readResourceFile(options.getRequestBodyFile()));

        assertEquals(options.getStatusCode(), response.getStatusCode());
        assertResponse(options, response);
    }

    public ResponseEntity<String> queryEndpointGet(String endpoint, List<UrlParam> params, HttpHeaders headers) {
        URI uri = buildUri(buildRequestUrl(endpoint), params);

        RequestEntity<Void> request = RequestEntity.get(uri)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .build();

        return restTemplate.exchange(request, String.class);
    }

    public ResponseEntity<String> queryEndpointPost(String endpoint, HttpHeaders headers, String body) {
        RequestEntity<String> request = RequestEntity.post(buildRequestUrl(endpoint))
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);

        return restTemplate.exchange(request, String.class);
    }

    private String buildRequestUrl(String endpoint) {
        return URL_PATTERN.formatted(properties.getIcedataBaseUrl(),
                serverPort,
                properties.getApiPath(),
                endpoint);
    }

    @TestConfiguration
    public static class BaseTestConfiguration {

        @Bean
        @Primary
        public Clock testClock() {
            return Clock.fixed(Instant.ofEpochMilli(1731870252907L), ZoneOffset.UTC);
        }
    }
}


