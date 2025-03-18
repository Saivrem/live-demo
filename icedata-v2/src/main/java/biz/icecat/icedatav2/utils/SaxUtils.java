package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import lombok.experimental.UtilityClass;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.List;

@UtilityClass
public class SaxUtils {

    private final static ThreadLocal<SAXParser> THREAD_LOCAL_PARSER = ThreadLocal.withInitial(() -> {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            return factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Error creating SAXParser", e);
        }
    });

    public static SAXParser getParser() {
        return THREAD_LOCAL_PARSER.get();
    }

    /**
     * Populate fields of passed Supplier entity from attributes
     *
     * @param object        {@link T} is a generic type calculated from {@link XmlAttributeBiConsumer} passed into method
     * @param attributes    {@link Attributes} of current XML element
     * @param processorList a list of {@link XmlAttributeBiConsumer} for a particular attributes set
     */
    public static <T> void populateFields(T object, Attributes attributes, List<XmlAttributeBiConsumer<T, ?>> processorList) {
        processorList.forEach(processor -> {
            String value = attributes.getValue(processor.xmlAttributeName());
            if (value != null) {
                processor.apply(object, value);
            }
        });
    }
}
