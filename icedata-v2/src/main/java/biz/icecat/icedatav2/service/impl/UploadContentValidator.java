package biz.icecat.icedatav2.service.impl;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipFile;

final class UploadContentValidator {
    private static final String DOCUMENT_REL =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument";

    static {
        ZipSecureFile.setMaxEntrySize(64L * 1024 * 1024);
    }

    static void validate(Path file, String filename) throws IOException {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        boolean valid;
        if (extension.equals("csv")) {
            try {
                valid = isCsv(Files.readString(file));
            } catch (CharacterCodingException invalidEncoding) {
                valid = false;
            }
        } else {
            try {
                valid = switch (extension) {
                    case "doc", "xls" -> isLegacyOffice(file, extension.equals("doc"));
                    case "docx", "xlsx" -> isOpenXml(file, extension.equals("docx"));
                    default -> false;
                };
            } catch (Exception invalidDocument) {
                valid = false;
            }
        }
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content does not match the ." + extension + " format.");
        }
    }

    private static boolean isLegacyOffice(Path file, boolean word) throws IOException {
        try (var fs = new POIFSFileSystem(file.toFile(), true)) {
            var root = fs.getRoot();
            String stream = "WordDocument";
            if (!word) {
                stream = root.hasEntry("Workbook") ? "Workbook" : "Book";
            }
            try (var input = root.createDocumentInputStream(stream)) {
                if (word) {
                    if (input.available() < 32 || input.readUShort() != 0xa5ec) return false;
                    input.skipNBytes(8);
                    int flags = input.readUShort();
                    return root.hasEntry((flags & 0x0200) == 0 ? "0Table" : "1Table");
                }
                // BIFF workbook-global BOF record, followed by the version and substream type.
                return input.available() >= 8 && input.readUShort() == 0x0809 && input.readUShort() >= 4
                        && input.readUShort() >= 0x0500 && input.readUShort() == 0x0005;
            }
        }
    }

    private static boolean isOpenXml(Path file, boolean word) throws Exception {
        try (var header = new DataInputStream(Files.newInputStream(file))) {
            if (header.readInt() != 0x504b0304) return false;
        }
        // Reject malformed central directories and bound the total expansion before opening the package.
        try (var archive = new ZipFile(file.toFile())) {
            if (archive.size() > 1000) return false;
            long remaining = 128L * 1024 * 1024;
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                long size = entries.nextElement().getSize();
                if (size < 0 || size > remaining) return false;
                remaining -= size;
            }
        }
        try (var pkg = OPCPackage.open(file.toFile(), PackageAccess.READ)) {
            var relations = pkg.getRelationshipsByType(DOCUMENT_REL);
            boolean strict = relations.size() == 0;
            if (strict) {
                relations = pkg.getRelationshipsByType("http://purl.oclc.org/ooxml/officeDocument/relationships/officeDocument");
            }
            if (relations.size() != 1) return false;
            PackagePart part = pkg.getPart(relations.getRelationship(0));
            String family = word ? "wordprocessingml" : "spreadsheetml";
            String type = word ? "document" : "sheet";
            if (part == null || !part.getContentType().equals(
                    "application/vnd.openxmlformats-officedocument." + family + "." + type + ".main+xml")) return false;
            String namespace = strict ? "http://purl.oclc.org/ooxml/" + family + "/main"
                    : "http://schemas.openxmlformats.org/" + family + "/2006/main";
            var factory = XMLInputFactory.newDefaultFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            try (var input = part.getInputStream()) {
                var xml = factory.createXMLStreamReader(input);
                try {
                    if (xml.nextTag() != XMLStreamConstants.START_ELEMENT
                            || !(word ? "document" : "workbook").equals(xml.getLocalName())
                            || !namespace.equals(xml.getNamespaceURI())) {
                        return false;
                    }
                    while (xml.hasNext()) xml.next();
                    return true;
                } finally {
                    xml.close();
                }
            }
        }
    }

    // Require tabular evidence. Quotes may contain delimiters, escaped quotes and newlines.
    private static boolean isCsv(String text) {
        if (text.chars().anyMatch(c -> Character.isISOControl(c) && c != '\r' && c != '\n' && c != '\t')) {
            return false;
        }
        return isDelimited(text, ',') || isDelimited(text, ';') || isDelimited(text, '\t');
    }

    private static boolean isDelimited(String text, char delimiter) {
        boolean quoted = false, startOfField = true, afterQuote = false;
        int columns = 1, expected = 0;
        int start = text.startsWith("\uFEFF") ? 1 : 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') i++;
                    else {
                        quoted = false;
                        afterQuote = true;
                    }
                }
            } else if (c == delimiter) {
                columns++;
                startOfField = true;
                afterQuote = false;
            } else if (c == '\r' || c == '\n') {
                if (columns < 2 || (expected != 0 && columns != expected)) return false;
                expected = columns;
                columns = 1;
                startOfField = true;
                afterQuote = false;
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                if (i + 1 == text.length()) return true;
            } else {
                if (afterQuote || (c == '"' && !startOfField)) return false;
                quoted = c == '"';
                startOfField = false;
            }
        }
        return !quoted && columns >= 2 && (expected == 0 || columns == expected);
    }
}
