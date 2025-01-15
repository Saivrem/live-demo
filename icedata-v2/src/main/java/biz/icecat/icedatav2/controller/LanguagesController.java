package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiLanguage;
import biz.icecat.icedatav2.service.LanguagesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    @GetMapping
    public ResponseEntity<List<ApiLanguage>> getLanguages() {
        List<@Valid ApiLanguage> languages = languagesService.getLanguages();
        return ResponseEntity.ok(languages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiLanguage> getLanguageById(@PathVariable("id") Long id) {
        ApiLanguage language = languagesService.getLanguageById(id);
        if (language != null) {
            return ResponseEntity.ok(language);
        }
        return ResponseEntity.status(NOT_FOUND).build();
    }
}
