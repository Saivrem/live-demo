package biz.icecat.icedatav2.models.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ApiSupplier {

    @NotNull
    private Long supplierId;
    @NotBlank
    private String supplierName;
    @NotNull
    private Integer isSponsor;
    private String brandLogo;

    private List<ApiSupplierMapping> supplierMappings;
}
