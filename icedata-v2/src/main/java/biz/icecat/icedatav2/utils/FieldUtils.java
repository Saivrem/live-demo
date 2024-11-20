package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.persistence.entity.SupplierEntity;
import biz.icecat.icedatav2.service.extractors.XmlAttributeBiConsumer;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.function.Function;

@UtilityClass
public class FieldUtils {
    public static Function<String, Long> mapToLong = (string) -> {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            return null;
        }
    };

    public static Function<String, Integer> mapToInt = (string) -> {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    };

    public static Function<String, Integer> mapToIntOrZero = mapToInt.andThen(num -> num != null ? num : 0);

    public static final List<XmlAttributeBiConsumer<SupplierEntity, ?>> SUPPLIER_ATTRIBUTES_PROCESSOR = List.of(
            new XmlAttributeBiConsumer<>("ID", SupplierEntity::setSupplierId, mapToLong),
            new XmlAttributeBiConsumer<>("Name", SupplierEntity::setSupplierName, Function.identity()),
            new XmlAttributeBiConsumer<>("LogoPic", SupplierEntity::setBrandLogo, Function.identity()),
            new XmlAttributeBiConsumer<>("Sponsor", SupplierEntity::setIsSponsor, mapToIntOrZero)
    );
}
