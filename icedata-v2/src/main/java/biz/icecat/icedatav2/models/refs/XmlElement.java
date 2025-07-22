package biz.icecat.icedatav2.models.refs;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeProcessor;

import java.util.List;

public interface XmlElement<T> {

    List<XmlAttributeProcessor<T, ?>> getAttributeProcessors();
}
