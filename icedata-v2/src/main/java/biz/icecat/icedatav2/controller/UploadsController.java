package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiUpload;
import biz.icecat.icedatav2.service.UploadsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("uploads")
@RequiredArgsConstructor
public class UploadsController {

    private final UploadsService uploadsService;

    @GetMapping
    public List<ApiUpload> getUploads() throws IOException {
        return uploadsService.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ApiUpload> uploadFiles(@RequestParam("files[]") List<MultipartFile> files) throws IOException {
        return uploadsService.store(files);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("name") String storedFilename) {
        Path storedFile = uploadsService.find(storedFilename);
        String originalFilename = uploadsService.originalFilename(storedFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(originalFilename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(storedFile));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> invalidUpload(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", exception.getReason()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> storageFailure(IOException exception) {
        log.error("Upload storage operation failed", exception);
        return ResponseEntity.internalServerError().body(Map.of("message", "File storage is temporarily unavailable."));
    }
}
