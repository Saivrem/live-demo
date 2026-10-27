package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.models.refs.languages.Language;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static biz.icecat.icedatav2.utils.SaxUtils.processAttributes;

@Slf4j
@RequiredArgsConstructor
public class LanguageHandler extends DefaultHandler implements CountedHandler<Language> {

    private static final String LANGUAGE = "Language";

    @Getter
    private int processed = 0;
    private final int batchSize;
    private final Consumer<List<Language>> consumer;
    private final List<Language> entities = new ArrayList<>();
    private Language currentEntity;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals(LANGUAGE)) {
            currentEntity = new Language();
            processAttributes(currentEntity, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(LANGUAGE) && currentEntity != null) {
            entities.add(currentEntity);

            if (entities.size() == batchSize) {
                processed += flush(consumer, entities);
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!entities.isEmpty()) {
            processed += flush(consumer, entities);
        }
    }
}
