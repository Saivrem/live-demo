package biz.icecat.icedatav2.it;

import biz.icecat.icedatav2.IcedataV2Application;
import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.utils.containers.mysql.MySqlContainer;
import biz.icecat.icedatav2.utils.models.TestOptions;
import biz.icecat.icedatav2.utils.models.UrlParam;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.ClassRule;
import org.junit.jupiter.api.Assertions;
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

import static biz.icecat.icedatav2.utils.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

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
    ApplicationProperties properties;

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

        Assertions.assertEquals(options.getStatusCode(), response.getStatusCode());

        if (options.getExpectedResponse() != null) {
            assertFlow(options, response);
        }
    }

    protected void verifyPostFlow(TestOptions options) {
        ResponseEntity<String> response = queryEndpointPost(
                options.getEndpoint(), options.getHeaders(),
                readResourceFile(options.getRequestBodyFile()));

        Assertions.assertEquals(options.getStatusCode(), response.getStatusCode());
        if (options.getExpectedResponse() != null) {
            assertFlow(options, response);
        }
    }

    private <A, E> void assertFlow(TestOptions options, ResponseEntity<String> response) {
        E expected = loadEntities(readResourceFile(options.getExpectedResponse()));
        A actual = loadEntities(response.getBody());

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Deprecated(since = "22.10.2024")
    private <T> T loadExpected(String pathToExpected) {
        return loadEntities(readResourceFile(pathToExpected));
    }

    @Deprecated(since = "22.10.2024")
    private <T> T loadActual(String actualBody) {
        return loadEntities(actualBody);
    }

    @SneakyThrows
    private <T> T loadEntities(String body) {
        return objectMapper.readValue(body, new TypeReference<>() {
        });
    }

    protected ResponseEntity<String> queryEndpointGet(String endpoint, List<UrlParam> params, HttpHeaders headers) {
        URI uri = buildUri(buildRequestUrl(endpoint), params);

        RequestEntity<Void> request = RequestEntity.get(uri)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .build();

        return restTemplate.exchange(request, String.class);
    }

    protected ResponseEntity<String> queryEndpointPost(String endpoint, HttpHeaders headers, String body) {
        RequestEntity<String> request = RequestEntity.post(buildRequestUrl(endpoint))
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);

        return restTemplate.exchange(request, String.class);
    }

    private String buildRequestUrl(String endpoint) {
        return "%s:%d%s%s".formatted(properties.getIcedataBaseUrl(),
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


