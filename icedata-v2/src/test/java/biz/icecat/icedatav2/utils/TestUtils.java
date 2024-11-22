package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.it.BaseIT;
import biz.icecat.icedatav2.utils.models.UrlParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@UtilityClass

public class TestUtils {

    public static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());

    public static boolean isBuildRunningInDockerContainer() {
        String buildRunningInContainerEnvVar = System.getenv("BUILD_RUNNING_IN_CONTAINER");
        return StringUtils.isNotBlank(buildRunningInContainerEnvVar) && Boolean.parseBoolean(buildRunningInContainerEnvVar);
    }

    public static int randomPort() {
        for (int i = 0; i < 100; i++) {
            int port = RandomUtils.nextInt(10000, 65535);
            try (ServerSocket ignored = new ServerSocket(port)) {
                return port;
            } catch (IOException ignored) {
                // ignore
            }
        }
        throw new IllegalStateException("no free port found");
    }

    @SneakyThrows
    public static String readResourceFile(String path) {
        if (path == null) {
            return null;
        }
        byte[] encoded = Files.readAllBytes(Paths.get("src/test/resources/" + path));
        return new String(encoded);
    }

    @SneakyThrows
    public static byte[] objectToJson(Object object) {
        objectMapper.findAndRegisterModules();
        return objectMapper.writeValueAsBytes(object);
    }

    @SneakyThrows
    public static List<JsonNode> readTestCases(String path) {
        try (InputStream is = getResourceInputStream(path)) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }

    public static InputStream getResourceInputStream(String path) {
        return BaseIT.class.getClassLoader().getResourceAsStream(path);
    }


    public static URI buildUri(String url, List<UrlParam> params) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url);
        if (params != null) {
            params.forEach(param -> uriComponentsBuilder.queryParam(param.param(), param.value()));
        }
        return uriComponentsBuilder.build().toUri();
    }

    public static HttpHeaders buildHeaders(Map<String, String> map) {
        HttpHeaders headers = new HttpHeaders();
        map.forEach(headers::add);
        return headers;
    }
}
