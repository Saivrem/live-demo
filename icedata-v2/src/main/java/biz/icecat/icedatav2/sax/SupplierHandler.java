package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.models.refs.suppliers.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static biz.icecat.icedatav2.utils.SaxUtils.populateFields;

// TODO Very POC implementation, refactor!
@Slf4j
@RequiredArgsConstructor
public class SupplierHandler extends DefaultHandler  implements CountedHandler<Supplier> {

    private static final String SUPPLIER = "Supplier";

    @Getter
    private int processed = 0;
    private final int batchSize;
    private final Consumer<List<Supplier>> consumer;
    private final List<Supplier> entities = new ArrayList<>();
    private Supplier currentEntity;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase(SUPPLIER)) {
            currentEntity = new Supplier();
            populateFields(currentEntity, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(SUPPLIER) && currentEntity != null) {
            entities.add(currentEntity);
            currentEntity = null;

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
