package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiLanguage;
import biz.icecat.icedatav2.service.LanguagesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("languages")
@RequiredArgsConstructor
public class LanguagesController {

    // TODO extract somewhere
    private final static HttpHeaders headers;
    private final LanguagesService languagesService;

    static {
        headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
    }

    @GetMapping()
    public ResponseEntity<List<ApiLanguage>> getLanguages(@RequestParam(value = "id", required = false) List<Long> id) {
        List<@Valid ApiLanguage> languages = languagesService.getLanguages(id);
        return ResponseEntity.ok(languages);
    }
}
