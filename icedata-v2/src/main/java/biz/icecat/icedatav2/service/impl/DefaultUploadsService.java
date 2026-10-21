package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.models.api.ApiUpload;
import biz.icecat.icedatav2.service.UploadsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class DefaultUploadsService implements UploadsService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xls", "xlsx", "docx", "doc");
    private static final int MAX_FILENAME_BYTES = 180;
    private static final int STORED_FILENAME_PREFIX_LENGTH = 37;

    private final Path uploadDirectory;

    public DefaultUploadsService(@Value("${uploads.directory:uploads}") String directoryPath) throws IOException {
        this.uploadDirectory = Path.of(directoryPath).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDirectory);
    }

    @Override
    public synchronized List<ApiUpload> store(List<MultipartFile> files) throws IOException {
        List<String> filenames = validateFiles(files);
        List<Path> storedFiles = new ArrayList<>();

        try {
            for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
                Path storedFile = uploadDirectory.resolve(UUID.randomUUID() + "-" + filenames.get(fileIndex));
                storedFiles.add(storedFile);
                saveFile(files.get(fileIndex), storedFile);
            }

            List<ApiUpload> uploads = new ArrayList<>();
            for (Path storedFile : storedFiles) {
                uploads.add(toApiUpload(storedFile));
            }
            return uploads;
        } catch (IOException | RuntimeException exception) {
            rollbackFiles(storedFiles, exception);
            throw exception;
        }
    }

    @Override
    public synchronized List<ApiUpload> list() throws IOException {
        List<ApiUpload> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(uploadDirectory)) {
            for (Path path : paths.filter(this::isUpload).sorted(Comparator.comparing(Path::toString)).toList()) {
                result.add(toApiUpload(path));
            }
        }
        return result;
    }

    @Override
    public Path find(String name) {
        Path path = uploadDirectory.resolve(name).normalize();
        if (!uploadDirectory.equals(path.getParent()) || !isUpload(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found.");
        }
        return path;
    }

    @Override
    public synchronized void cleanup() throws IOException {
        try (Stream<Path> paths = Files.list(uploadDirectory)) {
            for (Path path : paths.filter(this::isUpload).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Override
    public String originalFilename(Path storedFile) {
        return storedFile.getFileName().toString().substring(STORED_FILENAME_PREFIX_LENGTH);
    }

    private List<String> validateFiles(List<MultipartFile> files) {
        if (files.isEmpty()) {
            throw badRequest("Choose at least one file.");
        }

        List<String> filenames = new ArrayList<>();
        for (MultipartFile file : files) {
            filenames.add(validateFilename(file));
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Each file must be at most 20 MB.");
            }
        }
        return filenames;
    }

    private String validateFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank() || file.isEmpty()) {
            throw badRequest("Empty file.");
        }

        String normalizedFilename = originalFilename.replace('\\', '/');
        String filename = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1)
                .replaceAll("[^\\p{L}\\p{N}._-]", "_");
        int extensionSeparatorIndex = filename.lastIndexOf('.');
        if (extensionSeparatorIndex <= 0) {
            throw badRequest("File must have a name and an allowed extension.");
        }
        if (filename.getBytes(StandardCharsets.UTF_8).length > MAX_FILENAME_BYTES) {
            throw badRequest("File name is too long.");
        }

        String extension = filename.substring(extensionSeparatorIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw badRequest("Accepted formats: csv, xls, xlsx, docx, doc.");
        }
        return filename;
    }

    private void saveFile(MultipartFile file, Path storedFile) throws IOException {
        // Publish the file only after its transfer has completed.
        Path temporaryFile = Files.createTempFile(uploadDirectory, ".upload-", ".tmp");
        try {
            file.transferTo(temporaryFile);
            Files.move(temporaryFile, storedFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void rollbackFiles(List<Path> storedFiles, Exception originalException) {
        for (Path storedFile : storedFiles) {
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException cleanupException) {
                originalException.addSuppressed(cleanupException);
            }
        }
    }

    private boolean isUpload(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && path.getFileName().toString().matches("[0-9a-f-]{36}-.+\\.(?i:csv|xls|xlsx|docx|doc)");
    }

    private ApiUpload toApiUpload(Path path) throws IOException {
        String storedName = path.getFileName().toString();
        return new ApiUpload(originalFilename(path), "/icedata/api/v2/uploads/" +
                UriUtils.encodePathSegment(storedName, StandardCharsets.UTF_8),
                Files.size(path));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
