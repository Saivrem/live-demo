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

// TODO Very POC implementation, refactor!
@Slf4j
@RequiredArgsConstructor
public class SupplierHandler extends DefaultHandler {

    private final int batchSize;
    private final Consumer<List<SupplierEntity>> consumer;
    private final List<SupplierEntity> suppliers = new ArrayList<>();
    @Getter
    private int totalSuppliers = 0;
    private SupplierEntity currentSupplier;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("supplier")) {
            Long id = Long.valueOf(attributes.getValue("ID"));
            String name = attributes.getValue("Name");
            String logoUrl = attributes.getValue("LogoPic");
            String sponsor = attributes.getValue("Sponsor");

            int isSponsor = Integer.parseInt(sponsor != null ? sponsor : "0");

            currentSupplier = new SupplierEntity().setSupplierId(id)
                    .setSupplierName(name)
                    .setBrandLogo(logoUrl)
                    .setIsSponsor(isSponsor);
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

    private void flush() {
        totalSuppliers += suppliers.size();

        consumer.accept(suppliers);
        log.debug("{}: processed {} entries", getClass().getSimpleName(), suppliers.size());
        suppliers.clear();
    }
}
