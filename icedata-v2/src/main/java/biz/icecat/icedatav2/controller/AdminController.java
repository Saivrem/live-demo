package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiAdmin;
import biz.icecat.icedatav2.service.impl.LanguagesDataUpdateService;
import biz.icecat.icedatav2.service.impl.SupplierDataUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final LanguagesDataUpdateService languages;
    private final SupplierDataUpdateService suppliers;

    @GetMapping
    public ResponseEntity<ApiAdmin> initDatabase() {
        int languageUpdates = languages.update();
        int supplierUpdates = suppliers.update();

        return ResponseEntity.ok(new ApiAdmin(supplierUpdates, languageUpdates));
    }
}
