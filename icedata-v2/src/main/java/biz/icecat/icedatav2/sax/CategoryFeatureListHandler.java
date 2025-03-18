package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.models.refs.cat_feat_list.Category;
import biz.icecat.icedatav2.models.refs.cat_feat_list.Feature;
import biz.icecat.icedatav2.models.refs.cat_feat_list.Name;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileWriter;
import java.nio.file.Path;

import static biz.icecat.icedatav2.utils.SaxUtils.populateFields;

/**
 * This is WIP handler, currently not in use
 */
@Slf4j
public class CategoryFeatureListHandler extends DefaultHandler {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static JsonFactory jsonFactory = new JsonFactory();
    private final static JsonGenerator jsonGenerator;

    private final static FileWriter writer;

    static {
        try {
            Path report = Path.of(System.getProperty("user.home"), "Downloads", "report.json");
            writer = new FileWriter(report.toFile());
            jsonGenerator = jsonFactory.createGenerator(writer);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartArray();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private Category currentCategory;
    private Feature currentFeature;
    private boolean inFeatureGroup = false;
    private boolean inFeature = false;


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "Category" -> {
                currentCategory = new Category();
                populateFields(currentCategory, attributes, currentCategory.getAttributeProcessors());
            }
            case "Feature" -> {
                inFeature = true;
                currentFeature = new Feature();
                populateFields(currentFeature, attributes, currentFeature.getAttributeProcessors());
            }
            case "FeatureGroup" -> inFeatureGroup = true;
            case "Name" -> {
                Name name = new Name();
                populateFields(name, attributes, name.getAttributeProcessors());
                if (name.getLangId() == 1) {
                    if (inFeature) {
                        currentFeature.setIntName(name.getValue());
                    } else if (!inFeatureGroup) {
                        currentCategory.setIntName(name.getValue());
                    }
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "Category" -> {
                if (!currentCategory.getFeaturesList().isEmpty()) {
                    objectMapper.writeValue(jsonGenerator, currentCategory);
                }
            }
            case "Feature" -> {
                inFeature = false;
                if (isMandatoryFeature()) {
                    currentCategory.getFeaturesList().add(currentFeature);
                }
            }
            case "FeatureGroup" -> inFeatureGroup = false;
        }
    }

    @Override
    @SneakyThrows
    public void endDocument() throws SAXException {
        jsonGenerator.writeEndArray();
    }

    private boolean isMandatoryFeature() {
        return currentFeature != null && currentFeature.isMandatory();
    }
}
