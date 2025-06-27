package biz.icecat.icedatav2.mapping.converters;

import biz.icecat.icedatav2.models.api.ApiSupplierMapping;
import biz.icecat.icedatav2.models.entity.SupplierMappingEntity;
import biz.icecat.icedatav2.models.refs.suppliers.SupplierMapping;
import org.springframework.stereotype.Component;

@Component
public class SupplierMappingConverter implements Converter<SupplierMappingEntity, SupplierMapping, ApiSupplierMapping> {
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

    @Override
    public SupplierMapping entityToDomain(SupplierMappingEntity entity) {
        return new SupplierMapping().setSupplierId(entity.getSupplierId())
                .setMappedSupplierName(entity.getMappedSupplierName())
                .setRecordId(entity.getRecordId());
    }

    @Override
    public SupplierMappingEntity domainToEntity(SupplierMapping domain) {
        return new SupplierMappingEntity().setSupplierId(domain.getSupplierId())
                .setMappedSupplierName(domain.getMappedSupplierName())
                .setRecordId(domain.getRecordId());
    }
}
