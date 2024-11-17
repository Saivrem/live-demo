package biz.icecat.icedatav2.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@UtilityClass
public class FileUtils {

    public static final String TEMP_DIR = "tmp/";

    /**
     * Unpacks GZ file;
     *
     * @param inputFile  input (gzipped) file;
     * @param resultFile result file;
     */
    public static void unGzip(Path inputFile, Path resultFile) {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(inputFile))) {
            Files.copy(gzipInputStream, resultFile, REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Can't process {}", inputFile.getFileName());
        }
    }
}
