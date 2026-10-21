package biz.icecat.icedatav2.it;

import biz.icecat.icedatav2.IcedataV2Application;
import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.utils.models.TestOptions;
import biz.icecat.icedatav2.utils.models.UrlParam;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static biz.icecat.icedatav2.utils.TestConstants.URL_PATTERN;
import static biz.icecat.icedatav2.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("it")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BaseIT.BaseTestConfiguration.class)
@ContextConfiguration(classes = {IcedataV2Application.class}, loader = SpringBootContextLoader.class)
public class BaseIT {

    public static final TestRestTemplate restTemplate = new TestRestTemplate();
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("icedata_test")
            .withUsername("icedata_test")
            .withPassword("test_password");
    private static final Path UPLOADS;

    static {
        MYSQL.start();
        try {
            UPLOADS = Files.createTempDirectory("icedata-it-uploads-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (Stream<Path> paths = Files.walk(UPLOADS)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            } catch (IOException ignored) {
                // Isolated temporary test files; never touch application uploadsService.
            }
        }));
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
        registry.add("uploads.directory", () -> UPLOADS.toString());
        registry.add("uploads.cleanup-cron", () -> "-");
    }

    @Autowired
    private ApplicationProperties properties;

    @LocalServerPort
    private int serverPort;

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

    protected String serverUrl() {
        return properties.getIcedataBaseUrl() + ":" + serverPort;
    }

    protected String buildRequestUrl(String endpoint) {
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


