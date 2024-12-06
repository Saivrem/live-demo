package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.mapping.converters.LanguageConverter;
import biz.icecat.icedatav2.models.api.ApiLanguage;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("languages")
@RequiredArgsConstructor
public class LanguagesController {

    // TODO extract somewhere
    private final static HttpHeaders headers;
    // TODO extract to service
    private final LanguagesRepository repository;
    private final LanguageConverter converter;

    static {
        headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
    }

    @GetMapping
    public ResponseEntity<List<ApiLanguage>> getLanguages() {
        return ResponseEntity.ok(converter.toListOfApis(repository.findAll()));
    }

    @GetMapping("{id}")
    public ResponseEntity<ApiLanguage> getLanguageById(@PathVariable String id) {
        return ResponseEntity.ok(new ApiLanguage());
    }
}
