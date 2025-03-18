package biz.icecat.icedatav2.models.refs;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;

import java.util.List;

public interface XmlElement<T> {

    List<XmlAttributeBiConsumer<T, ?>> getAttributeProcessors();
}
