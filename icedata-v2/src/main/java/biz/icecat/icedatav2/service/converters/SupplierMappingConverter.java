package biz.icecat.icedatav2.service.converters;

import biz.icecat.icedatav2.models.api.ApiSupplierMapping;
import biz.icecat.icedatav2.persistence.entity.SupplierMappingEntity;
import org.springframework.stereotype.Component;

@Component
public class SupplierMappingConverter implements Converter<SupplierMappingEntity, ApiSupplierMapping> {
    @Override
    public SupplierMappingEntity apiToEntity(ApiSupplierMapping api) {
        return new SupplierMappingEntity().setSupplierId(api.getSupplierId())
                .setMappedSupplierName(api.getMappedSupplierName())
                .setRecordId(api.getRecordId());
    }

    @Override
    public ApiSupplierMapping entityToApi(SupplierMappingEntity entity) {
        return new ApiSupplierMapping().setSupplierId(entity.getSupplierId())
                .setMappedSupplierName(entity.getMappedSupplierName())
                .setRecordId(entity.getRecordId());
    }
}
