package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.aspect.Measured;
import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.mapping.converters.LanguageConverter;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import biz.icecat.icedatav2.sax.LanguageHandler;
import biz.icecat.icedatav2.service.DataUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LanguagesDataUpdateService extends DataUpdateService {

    private final LanguagesRepository repository;
    private final LanguageConverter converter;

    @Value("${files.language-list-file}")
    private String languagesFile;

    public LanguagesDataUpdateService(ApplicationProperties properties, LanguagesRepository repository, LanguageConverter converter) {
        super(properties);
        this.repository = repository;
        this.converter = converter;
    }

    @Override
    @Measured(methodName = "Update Languages")
    public int update() {
        LanguageHandler handler = new LanguageHandler(batchSize,
                languages -> repository.saveAll(converter.domainsListToListOfEntities(languages))
        );

        return processFile(handler, languagesFile, "languages_list.xml");
    }
}
