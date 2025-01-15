package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.mapping.converters.LanguageConverter;
import biz.icecat.icedatav2.models.api.ApiLanguage;
import biz.icecat.icedatav2.models.entity.LanguageEntity;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import biz.icecat.icedatav2.service.LanguagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLanguagesService implements LanguagesService {

    private final LanguagesRepository repository;
    private final LanguageConverter converter;

    @Override
    public List<ApiLanguage> getLanguages() {
        List<LanguageEntity> findAll = repository.findAll();
        return converter.toListOfApis(findAll);
    }

    @Override
    public ApiLanguage getLanguageById(Long id) {
        return repository.findById(id)
                .map(converter::entityToApi)
                .orElse(null);
    }
}
