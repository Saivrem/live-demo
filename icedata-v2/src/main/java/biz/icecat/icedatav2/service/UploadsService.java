package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.models.api.ApiUpload;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface UploadsService {

    List<ApiUpload> store(List<MultipartFile> files) throws IOException;

    List<ApiUpload> list() throws IOException;

    Path find(String storedFilename);

    String originalFilename(Path storedFile);

    void cleanup() throws IOException;
}
