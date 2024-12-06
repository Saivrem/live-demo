package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static biz.icecat.icedatav2.utils.FileUtils.*;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@UtilityClass
public class LoadingUtils {

    /**
     * V2 version of Download URL
     *
     * @param link String for URL
     * @param path Path to result file.
     * @throws IOException Exception is thrown upper
     */
    public static void downloadUrl(String link, Path path) throws IOException {
        InputStream stream = new URL(link).openStream();
        Files.copy(stream, path, REPLACE_EXISTING);
    }

    /**
     * Auth method
     * TODO refactor to use more recent technologies
     *
     * @param userName username
     * @param passWord password
     */
    public static void authenticate(String userName, String passWord) {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, passWord.toCharArray());
            }
        });
    }

    @NotNull
    public static Path getDownloadedFile(String file, String unGzipped, ApplicationProperties properties) throws IOException {
        Path gzippedLanguages = Path.of(TEMP_DIR, file);
        createTempFile(gzippedLanguages);

        authenticate(properties.getServiceUserName(), properties.getServicePassword());
        downloadUrl(properties.resolveIcecatLink(file), gzippedLanguages);

        Path unzippedTempFile = Path.of(TEMP_DIR, unGzipped);
        unGzip(gzippedLanguages, unzippedTempFile);
        deleteIfExists(gzippedLanguages);

        return unzippedTempFile;
    }
}
