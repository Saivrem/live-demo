package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiSupplier;
import biz.icecat.icedatav2.service.SuppliersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v2/suppliers")
@RequiredArgsConstructor
public class SuppliersController {

    private final static HttpHeaders headers;

    static {
        headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
    }

    private final SuppliersService suppliersService;

    @GetMapping
    public ResponseEntity<?> getAllSuppliers() {
        List<ApiSupplier> supplierEntities = suppliersService.findAll();

        return ResponseEntity.ok().headers(headers).body(supplierEntities);
    }

    // TODO Add authorization
    @PostMapping
    public ResponseEntity<?> createSupplier(@RequestBody ApiSupplier apiSupplier) {
        log.info("Received {}", apiSupplier);
        ApiSupplier created = suppliersService.createSupplier(apiSupplier);
        if (created != null) {
            return ResponseEntity.ok().headers(headers).body(created);
        }

        return ResponseEntity.badRequest().build();
    }
}
