package biz.icecat.icedatav2.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "icedata-v2")
public class ApplicationProperties {

    @NotBlank
    private String baseUrl;
    @NotBlank
    private String suppliersListFile;
    @NotBlank
    private String suppliersMappingFile;
    @NotBlank
    private String languageListFile;
    @NotBlank
    private String serviceUserName;
    @NotBlank
    private String servicePassword;
}
