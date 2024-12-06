package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import biz.icecat.icedatav2.sax.LanguageHandler;
import biz.icecat.icedatav2.service.DataUpdateService;
import biz.icecat.icedatav2.utils.SaxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static biz.icecat.icedatav2.utils.LoadingUtils.getDownloadedFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LanguagesDataUpdateService implements DataUpdateService {

    private final ApplicationProperties properties;
    private final LanguagesRepository repository;

    @Value("${files.language-list-file}")
    private String languagesFile;

    @Override
    public int update() {
        LanguageHandler handler = new LanguageHandler(1, repository::saveAll);
        SAXParser saxParser = SaxUtils.getParser();

        try {
            Path languages = getDownloadedFile(languagesFile, "languages_list.xml", properties);

            saxParser.parse(Files.newInputStream(languages), handler);
            return handler.getProcessed();
        } catch (SAXException saxEx) {
            log.warn("Failed to configure parser for {}", this.getClass().getSimpleName());
        } catch (IOException e) {
            log.warn("Failed to process suppliers list");
        }

        return 0;
    }
}
