package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.sax.CountedHandler;
import biz.icecat.icedatav2.utils.SaxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.SAXParser;

import static biz.icecat.icedatav2.utils.LoadingUtils.getDownloadedFile;

@Slf4j
@RequiredArgsConstructor
public abstract class DataUpdateService {

    protected final ApplicationProperties properties;
    @Value("${icedata-v2.batch-size}")
    protected Integer batchSize;
    @Value("${icedata-v2.temp-directory:tmp}")
    private String tempDirectory;

    public abstract int update();

    protected int processFile(DefaultHandler handler, String targetFile, String extractedFileName) {
        SAXParser saxParser = SaxUtils.getParser();
        try (InputStream downloadedFile = Files.newInputStream(
                getDownloadedFile(targetFile, extractedFileName, properties, Path.of(tempDirectory))
        )) {
            saxParser.parse(downloadedFile, handler);
        } catch (SAXException parserException) {
            log.warn("Failed to configure parser for {}", this.getClass().getSimpleName(), parserException);
        } catch (IOException downloadException) {
            log.warn("Failed to process {}", extractedFileName, downloadException);
        } finally {
            SaxUtils.removeParser();
        }
        return getProcessed(handler);
    }

    private int getProcessed(DefaultHandler handler) {
        if (handler instanceof CountedHandler<?> countedHandler) {
            return countedHandler.getProcessed();
        }
        log.warn("Handler {} is not support count operation", handler.getClass().getSimpleName());
        return 0;
    }
}
