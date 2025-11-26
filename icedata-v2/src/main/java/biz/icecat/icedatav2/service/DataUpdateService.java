package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.sax.CountedHandler;
import biz.icecat.icedatav2.utils.SaxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static biz.icecat.icedatav2.utils.LoadingUtils.getDownloadedFile;

@Slf4j
@RequiredArgsConstructor
public abstract class DataUpdateService {
    protected final ApplicationProperties properties;
    @Value("${icedata-v2.batch-size}")
    protected Integer batchSize;

    public abstract int update();

    protected int processFile(DefaultHandler handler, String targetFile, String extractedFileName) {
        SAXParser saxParser = SaxUtils.getParser();
        try (InputStream is = Files.newInputStream(
                getDownloadedFile(targetFile, extractedFileName, properties)
        )) {
            saxParser.parse(is, handler);
        } catch (SAXException saxEx) {
            log.warn("Failed to configure parser for {}", this.getClass().getSimpleName(), saxEx);
        } catch (IOException e) {
            log.warn("Failed to process {}", extractedFileName, e);
        }
        SaxUtils.removeParser();
        return getProcessed(handler);
    }

    private int getProcessed(DefaultHandler handler) {
        if (handler instanceof CountedHandler<?> ch) {
            return ch.getProcessed();
        }
        log.warn("Handler {} is not support count operation", handler.getClass().getSimpleName());
        return 0;
    }
}
