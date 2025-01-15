package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.models.api.ApiLanguage;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface LanguagesService {
    List<@Valid ApiLanguage> getLanguages();

    ApiLanguage getLanguageById(Long id);
}
