package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.models.api.ApiSupplier;
import biz.icecat.icedatav2.models.entity.SupplierEntity;
import biz.icecat.icedatav2.repository.SupplierRepository;
import biz.icecat.icedatav2.service.SuppliersService;
import biz.icecat.icedatav2.mapping.converters.SupplierConverter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSuppliersService implements SuppliersService {

    private final SupplierRepository repository;
    private final SupplierConverter converter;

    @Override
    public List<ApiSupplier> findAll() {
        List<SupplierEntity> all = repository.findAll();
        return converter.toListOfApis(all);
    }

    @Override
    @Transactional
    public ApiSupplier createSupplier(ApiSupplier apiSupplier) {
        SupplierEntity supplierEntity = converter.apiToEntity(apiSupplier);

        SupplierEntity saved = repository.save(supplierEntity);
        return converter.entityToApi(saved);
    }
}
