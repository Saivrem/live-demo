package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.models.api.ApiSupplier;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface SuppliersService {

    List<@Valid ApiSupplier> findAll();

    ApiSupplier createSupplier(@Valid ApiSupplier apiSupplier);
}
