package biz.icecat.icedatav2.configuration.mappings;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TestConfigPoc {

    private Map<String, List<Config>> mappings = new HashMap<>();
    private String version;

    @Data
    public static class Config {
        private String mapping;
        private boolean enabled;
    }
}
