package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.base.BaseSaxTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SaxUtilsTest extends BaseSaxTest {

    @TempDir
    Path directory;

    @Test
    void externalEntitiesCannotReadLocalFiles() throws Exception {
        Path secret = directory.resolve("secret.txt");
        Files.writeString(secret, "SECRET_SHOULD_NOT_BE_READ");
        String xml = "<!DOCTYPE root [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]><root>&xxe;</root>";
        StringBuilder content = new StringBuilder();
        SaxUtils.getParser().parse(xmlStream(xml), new DefaultHandler() {
            @Override
            public void characters(char[] characters, int start, int length) {
                content.append(characters, start, length);
            }
        });
        assertThat(content.toString()).doesNotContain("SECRET_SHOULD_NOT_BE_READ");
    }

    @Test
    void icecatDocumentsMayDeclareAnExternalDtdWithoutLoadingIt() throws Exception {
        String xml = "<!DOCTYPE ICECAT-interface SYSTEM \"" + directory.resolve("missing.dtd").toUri()
                + "\"><ICECAT-interface><Language ID=\"1\"/></ICECAT-interface>";
        List<String> languageIds = new ArrayList<>();
        SaxUtils.getParser().parse(xmlStream(xml), new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if (qName.equals("Language")) {
                    languageIds.add(attributes.getValue("ID"));
                }
            }
        });
        assertThat(languageIds).containsExactly("1");
    }

}
