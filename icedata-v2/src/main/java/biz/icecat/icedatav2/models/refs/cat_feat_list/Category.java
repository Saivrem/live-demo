package biz.icecat.icedatav2.models.refs.cat_feat_list;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeProcessor;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static biz.icecat.icedatav2.utils.FieldUtils.mapToLong;

@Data
public class Category implements XmlElement<Category> {

    private Long id;
    private String uncatid;
    private String intName;
    List<Feature> featuresList = new ArrayList<>();

    @Override
    public List<XmlAttributeProcessor<Category, ?>> getAttributeProcessors() {
        return List.of(
                new XmlAttributeProcessor<>("ID", Category::setId, mapToLong),
                new XmlAttributeProcessor<>("UNCATID", Category::setUncatid, Function.identity())
        );
    }
}
