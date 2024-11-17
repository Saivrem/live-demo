package biz.icecat.icedatav2.models.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApiSupplierMapping {
    @NotNull
    private Long recordId;
    @NotNull
    private Long supplierId;
    @NotBlank
    private String mappedSupplierName;
}
