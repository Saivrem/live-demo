package biz.icecat.icedatav2.models.refs.cat_feat_list;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.Data;

import java.util.List;

import static biz.icecat.icedatav2.utils.FieldUtils.mapToBool;
import static biz.icecat.icedatav2.utils.FieldUtils.mapToLong;

@Data
public class Feature implements XmlElement<Feature> {

    private Long id;
    private Long categoryFeatureGroupId;
    private Long categoryFeatureId;
    private boolean isHidden;
    private boolean isMandatory;
    private String intName;

    @Override
    public List<XmlAttributeBiConsumer<Feature, ?>> getAttributeProcessors() {
        return List.of(
                new XmlAttributeBiConsumer<>("ID", Feature::setId, mapToLong),
                new XmlAttributeBiConsumer<>("CategoryFeature_ID", Feature::setCategoryFeatureId, mapToLong),
                new XmlAttributeBiConsumer<>("CategoryFeatureGroup_ID", Feature::setCategoryFeatureGroupId, mapToLong),
                new XmlAttributeBiConsumer<>("IsHidden", Feature::setHidden, mapToBool),
                new XmlAttributeBiConsumer<>("Mandatory", Feature::setMandatory, mapToBool)
        );
    }
}
