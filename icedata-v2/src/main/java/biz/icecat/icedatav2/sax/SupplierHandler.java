package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.persistence.entity.SupplierEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static biz.icecat.icedatav2.utils.FieldUtils.*;

// TODO Very POC implementation, refactor!
@Slf4j
@RequiredArgsConstructor
public class SupplierHandler extends DefaultHandler {
    private static final String SUPPLIER = "Supplier";

    private final int batchSize;
    private final Consumer<List<SupplierEntity>> consumer;
    private final List<SupplierEntity> suppliers = new ArrayList<>();
    @Getter
    private int processedSuppliersNumber = 0;
    private SupplierEntity currentSupplier;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase(SUPPLIER)) {
            currentSupplier = new SupplierEntity();
            populateFields(currentSupplier, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("Supplier") && currentSupplier != null) {
            suppliers.add(currentSupplier);
            currentSupplier = null;

            if (suppliers.size() == batchSize) {
                flush();
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (!suppliers.isEmpty()) {
            flush();
        }
    }

    /**
     * Populate fields of passed Supplier entity from attributes
     * @param supplier {@link SupplierEntity} current supplier entity
     * @param attributes {@link Attributes} of current XML element
     */
    private void populateFields(SupplierEntity supplier, Attributes attributes) {
        SUPPLIER_ATTRIBUTES_PROCESSOR.forEach(processor -> {
            String value = attributes.getValue(processor.xmlAttributeName());
            if (value != null) {
                processor.apply(supplier, value);
            }
        });
    }

    /**
     * Passing current suppliers batch to consumer
     */
    private void flush() {
        consumer.accept(suppliers);
        processedSuppliersNumber += suppliers.size();
        log.debug("{}: processed {} entries", getClass().getSimpleName(), suppliers.size());
        suppliers.clear();
    }
}
