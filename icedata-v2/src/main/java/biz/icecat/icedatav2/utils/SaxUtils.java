package biz.icecat.icedatav2.utils;

import lombok.experimental.UtilityClass;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
}
