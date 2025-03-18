package biz.icecat.icedatav2.models.refs.suppliers;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static biz.icecat.icedatav2.utils.FieldUtils.*;

@Data
@Accessors(chain = true)
public class Supplier implements XmlElement<Supplier> {

    private Long supplierId;
    private String supplierName;
    private boolean isSponsor;
    private String brandLogo;
    private List<SupplierMapping> supplierMappings = new ArrayList<>();

    @Override
    public List<XmlAttributeBiConsumer<Supplier, ?>> getAttributeProcessors() {
        return List.of(
                new XmlAttributeBiConsumer<>("ID", Supplier::setSupplierId, mapToLong),
                new XmlAttributeBiConsumer<>("Name", Supplier::setSupplierName, Function.identity()),
                new XmlAttributeBiConsumer<>("LogoPic", Supplier::setBrandLogo, Function.identity()),
                new XmlAttributeBiConsumer<>("Sponsor", Supplier::setSponsor, mapToBool)
        );
    }
}
