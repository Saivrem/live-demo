package biz.icecat.icedatav2.models.refs.languages;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeProcessor;
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
    public List<XmlAttributeProcessor<Language, ?>> getAttributeProcessors() {
        return List.of(
                new XmlAttributeProcessor<>("ID", Language::setLangId, mapToLong),
                new XmlAttributeProcessor<>("Code", Language::setIntLangName, Function.identity()),
                new XmlAttributeProcessor<>("ShortCode", Language::setShortCode, Function.identity()),
                new XmlAttributeProcessor<>("Updated", Language::setUpdated, mapDateTimeToLong)
        );
    }
}
