package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeProcessor;
import biz.icecat.icedatav2.models.refs.XmlElement;
import lombok.experimental.UtilityClass;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

@UtilityClass
public class SaxUtils {

    private static final ThreadLocal<SAXParser> THREAD_LOCAL_PARSER = ThreadLocal.withInitial(() -> {
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            parserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parserFactory.setXIncludeAware(false);
            SAXParser saxParser = parserFactory.newSAXParser();
            saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return saxParser;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Error creating SAXParser", e);
        }
    });

    public static SAXParser getParser() {
        return THREAD_LOCAL_PARSER.get();
    }

    public void removeParser() {
        THREAD_LOCAL_PARSER.remove();
    }

    /**
     * Populate fields of passed Supplier entity from attributes
     *
     * @param object     {@link T} is a generic type calculated from {@link XmlAttributeProcessor} passed into method
     * @param attributes {@link Attributes} of current XML element
     */
    public static <T extends XmlElement<T>> void processAttributes(T object, Attributes attributes) {
        if (object == null) return;
        object.getAttributeProcessors().forEach(p -> {
            String value = attributes.getValue(p.attrName());
            if (value != null) {
                p.process(object, value);
            }
        });
    }
}
