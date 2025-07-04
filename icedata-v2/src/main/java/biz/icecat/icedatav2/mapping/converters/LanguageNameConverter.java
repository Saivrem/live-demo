package biz.icecat.icedatav2.mapping.converters;

import biz.icecat.icedatav2.models.api.ApiLanguageName;
import biz.icecat.icedatav2.models.entity.LanguageNameEntity;
import biz.icecat.icedatav2.models.refs.languages.LanguageName;
import biz.icecat.icedatav2.utils.DateUtils;
import org.springframework.stereotype.Component;

import static biz.icecat.icedatav2.utils.DateUtils.parseToLong;

@Component
public class LanguageNameConverter implements Converter<LanguageNameEntity, LanguageName, ApiLanguageName> {

    @Override
    public LanguageNameEntity apiToEntity(ApiLanguageName api) {
        return new LanguageNameEntity()
                .setLangNameId(api.getLangNameId())
                .setTranslationLangId(api.getTranslationLangId())
                .setTargetLangId(api.getTargetLangId())
                .setNameTranslation(api.getNameTranslation())
                .setUpdated(parseToLong(api.getUpdated()));
    }

    @Override
    public ApiLanguageName entityToApi(LanguageNameEntity entity) {
        return new ApiLanguageName()
                .setLangNameId(entity.getLangNameId())
                .setTranslationLangId(entity.getTranslationLangId())
                .setTargetLangId(entity.getTargetLangId())
                .setNameTranslation(entity.getNameTranslation())
                .setUpdated(DateUtils.format(entity.getUpdated(), DateUtils.UTC));
    }

    @Override
    public LanguageName entityToDomain(LanguageNameEntity entity) {
        return new LanguageName()
                .setLangNameId(entity.getLangNameId())
                .setTranslationLangId(entity.getTranslationLangId())
                .setTargetLangId(entity.getTargetLangId())
                .setNameTranslation(entity.getNameTranslation())
                .setUpdated(entity.getUpdated());
    }

    @Override
    public LanguageNameEntity domainToEntity(LanguageName domain) {
        return new LanguageNameEntity()
                .setLangNameId(domain.getLangNameId())
                .setTranslationLangId(domain.getTranslationLangId())
                .setTargetLangId(domain.getTargetLangId())
                .setNameTranslation(domain.getNameTranslation())
                .setUpdated(domain.getUpdated());
    }
}
