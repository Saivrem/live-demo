package biz.icecat.icedatav2.models.refs.suppliers;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class SupplierMapping implements XmlElement<SupplierMapping> {

    private long recordId;
    private long supplierId;
    private String mappedSupplierName;

    @Override
    public List<XmlAttributeBiConsumer<SupplierMapping, ?>> getAttributeProcessors() {
        return List.of();
    }
}
