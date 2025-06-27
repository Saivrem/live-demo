package biz.icecat.icedatav2.mapping.converters;

import biz.icecat.icedatav2.models.api.ApiLanguage;
import biz.icecat.icedatav2.models.entity.LanguageEntity;
import biz.icecat.icedatav2.models.refs.languages.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static biz.icecat.icedatav2.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
public class LanguageConverter implements Converter<LanguageEntity, Language, ApiLanguage> {

    private final LanguageNameConverter languageNameConverter;

    @Override
    public LanguageEntity apiToEntity(ApiLanguage api) {
        return new LanguageEntity().setLangId(api.getLangId())
                .setIntLangName(api.getLangName())
                .setShortCode(api.getLangCode())
                .setUpdated(parseToLong(api.getUpdated()))
                .setNames(languageNameConverter.toListOfEntities(api.getLanguageNames()));
    }

    @Override
    public ApiLanguage entityToApi(LanguageEntity entity) {
        return new ApiLanguage()
                .setLangId(entity.getLangId())
                .setLangName(entity.getIntLangName())
                .setLangCode(entity.getShortCode())
                .setUpdated(format(entity.getUpdated(), UTC))
                .setLanguageNames(languageNameConverter.toListOfApis(entity.getNames()));
    }

    @Override
    public Language entityToDomain(LanguageEntity entity) {
        return new Language().setLangId(entity.getLangId())
                .setIntLangName(entity.getIntLangName())
                .setShortCode(entity.getShortCode())
                .setUpdated(entity.getUpdated())
                .setNames(languageNameConverter.entityListToDomainList(entity.getNames()));
    }

    @Override
    public LanguageEntity domainToEntity(Language domain) {
        return new LanguageEntity().setLangId(domain.getLangId())
                .setIntLangName(domain.getIntLangName())
                .setShortCode(domain.getShortCode())
                .setUpdated(domain.getUpdated())
                .setNames(languageNameConverter.domainsListToListOfEntities(domain.getNames()));
    }
}
