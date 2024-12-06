package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.mapping.extractors.XmlAttributeBiConsumer;
import biz.icecat.icedatav2.models.entity.LanguageEntity;
import biz.icecat.icedatav2.models.entity.SupplierEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static biz.icecat.icedatav2.utils.FieldUtils.mapDateTimeToLong;
import static biz.icecat.icedatav2.utils.FieldUtils.mapToLong;

@Slf4j
@RequiredArgsConstructor
public class LanguageHandler extends DefaultHandler {

    private static final String LANGUAGE = "Language";

    private static final List<XmlAttributeBiConsumer<LanguageEntity, ?>> LANGUAGE_ATTRIBUTES_PROCESSOR = List.of(
            new XmlAttributeBiConsumer<>("ID", LanguageEntity::setLangId, mapToLong),
            new XmlAttributeBiConsumer<>("Code", LanguageEntity::setIntLangName, Function.identity()),
            new XmlAttributeBiConsumer<>("ShortCode", LanguageEntity::setShortCode, Function.identity()),
            new XmlAttributeBiConsumer<>("Updated", LanguageEntity::setUpdated, mapDateTimeToLong)
    );

    @Getter
    private int processed = 0;
    private final int batchSize;
    private final Consumer<List<LanguageEntity>> consumer;
    private final List<LanguageEntity> languages = new ArrayList<>();

    private LanguageEntity currentLanguage;


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals(LANGUAGE)) {
            currentLanguage = new LanguageEntity();
            populateFields(currentLanguage, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(LANGUAGE) && currentLanguage != null) {
            languages.add(currentLanguage);
            currentLanguage = null;

            if (languages.size() == batchSize) {
                flush();
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!languages.isEmpty()) {
            flush();
        }
    }

    /**
     * Populate fields of passed Supplier entity from attributes
     *
     * @param language   {@link SupplierEntity} current language entity
     * @param attributes {@link Attributes} of current XML element
     */
    private void populateFields(LanguageEntity language, Attributes attributes) {
        LANGUAGE_ATTRIBUTES_PROCESSOR.forEach(processor -> {
            String value = attributes.getValue(processor.xmlAttributeName());
            if (value != null) {
                processor.apply(language, value);
            }
        });
    }

    /**
     * Passing current suppliers batch to consumer
     */
    private void flush() {
        try {consumer.accept(languages);} catch (Exception e) {
            System.out.println(1);
        }
        processed += languages.size();
        log.debug("{}: processed {} entries", getClass().getSimpleName(), languages.size());
        languages.clear();
    }
}
