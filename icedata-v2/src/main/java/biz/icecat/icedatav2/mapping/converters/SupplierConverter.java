package biz.icecat.icedatav2.mapping.converters;

import biz.icecat.icedatav2.models.api.ApiSupplier;
import biz.icecat.icedatav2.models.entity.SupplierEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplierConverter implements Converter<SupplierEntity, ApiSupplier> {

    private final SupplierMappingConverter supplierMappingConverter;

    @Override
    public SupplierEntity apiToEntity(ApiSupplier api) {
        return new SupplierEntity().setSupplierId(api.getSupplierId())
                .setSupplierName(api.getSupplierName())
                .setBrandLogo(api.getBrandLogo())
                .setIsSponsor(api.getIsSponsor())
                .setSupplierMappingEntityList(
                        supplierMappingConverter.toListOfEntities(api.getSupplierMappings())
                );
    }

    @Override
    public ApiSupplier entityToApi(SupplierEntity entity) {
        return new ApiSupplier().setSupplierId(entity.getSupplierId())
                .setSupplierName(entity.getSupplierName())
                .setBrandLogo(entity.getBrandLogo())
                .setIsSponsor(entity.getIsSponsor())
                .setSupplierMappings(
                        supplierMappingConverter.toListOfApis(entity.getSupplierMappingEntityList())
                );
    }
}
