package biz.icecat.icedatav2.service.extractors;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Record dedicated for extracting attribute value, transforming it to particular type and setting it to particular object
 * @param xmlAttributeName name of XML attribute
 * @param setter setter method of Target class
 * @param mapper mapping function to convert string value to Target type
 * @param <T> Target Class
 * @param <R> Mapped value Type
 */
public record XmlAttributeBiConsumer<T, R>(String xmlAttributeName, BiConsumer<T, R> setter,
                                           Function<String, R> mapper) {
    public void apply(T target, String value) {
        R mappedValue = mapper.apply(value);
        setter.accept(target, mappedValue);
    }
}
