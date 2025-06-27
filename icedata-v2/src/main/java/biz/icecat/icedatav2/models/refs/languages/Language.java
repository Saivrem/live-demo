package biz.icecat.icedatav2.models.refs.languages;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static biz.icecat.icedatav2.utils.FieldUtils.mapDateTimeToLong;
import static biz.icecat.icedatav2.utils.FieldUtils.mapToLong;

@Data
@Accessors(chain = true)
public class Language implements XmlElement<Language> {

    private Long langId;
    private String intLangName;
    private String shortCode;
    private Long updated;

    private List<LanguageName> names = new ArrayList<>();

    @Override
    public List<XmlAttributeBiConsumer<Language, ?>> getAttributeProcessors() {
        return List.of(
                new XmlAttributeBiConsumer<>("ID", Language::setLangId, mapToLong),
                new XmlAttributeBiConsumer<>("Code", Language::setIntLangName, Function.identity()),
                new XmlAttributeBiConsumer<>("ShortCode", Language::setShortCode, Function.identity()),
                new XmlAttributeBiConsumer<>("Updated", Language::setUpdated, mapDateTimeToLong)
        );
    }
}
