package biz.icecat.icedatav2.mapping.converters;

import biz.icecat.icedatav2.models.api.ApiSupplier;
import biz.icecat.icedatav2.models.entity.SupplierEntity;
import biz.icecat.icedatav2.models.refs.suppliers.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplierConverter implements Converter<SupplierEntity, Supplier, ApiSupplier> {

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

    @Override
    public Supplier entityToDomain(SupplierEntity entity) {
        return new Supplier().setSupplierId(entity.getSupplierId())
                .setSupplierName(entity.getSupplierName())
                .setBrandLogo(entity.getBrandLogo())
                .setSponsor(entity.getIsSponsor() == 1)
                .setSupplierMappings(
                        supplierMappingConverter.entityListToDomainList(entity.getSupplierMappingEntityList())
                );
    }

    @Override
    public SupplierEntity domainToEntity(Supplier domain) {
        return new SupplierEntity().setSupplierId(domain.getSupplierId())
                .setSupplierName(domain.getSupplierName())
                .setBrandLogo(domain.getBrandLogo())
                .setIsSponsor(domain.isSponsor() ? 1 : 0)
                .setSupplierMappingEntityList(
                        supplierMappingConverter.domainsListToListOfEntities(domain.getSupplierMappings())
                );
    }
}
