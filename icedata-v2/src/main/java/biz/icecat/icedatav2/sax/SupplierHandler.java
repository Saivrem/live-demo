package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.persistence.entity.SupplierEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

// TODO Very POC implementation, refactor!
@Slf4j
public class SupplierHandler extends DefaultHandler {

    @Getter
    private final List<SupplierEntity> suppliers = new ArrayList<>();
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
        }
    }
}
