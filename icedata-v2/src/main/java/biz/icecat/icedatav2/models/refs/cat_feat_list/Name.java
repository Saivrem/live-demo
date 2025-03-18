package biz.icecat.icedatav2.models.refs.cat_feat_list;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

import static biz.icecat.icedatav2.utils.FieldUtils.mapToLong;

@Data
public class Name implements XmlElement<Name> {
    private Long id;
    private Long langId;
    private String value;

    @Override
    public List<XmlAttributeBiConsumer<Name, ?>> getAttributeProcessors() {
        return List.of(
                new XmlAttributeBiConsumer<>("ID", Name::setId, mapToLong),
                new XmlAttributeBiConsumer<>("langid", Name::setLangId, mapToLong),
                new XmlAttributeBiConsumer<>("Value", Name::setValue, Function.identity())
        );
    }
}
