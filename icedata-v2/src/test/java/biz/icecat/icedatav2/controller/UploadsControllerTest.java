package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.base.BaseUploadsTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.util.List;

import static biz.icecat.icedatav2.utils.TestUtils.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadsControllerTest extends BaseUploadsTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(new UploadsController(uploadsService)).build();
    }

    @Test
    void uploadListAndDownloadPreserveContentAndAvoidOverwriting() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files[]", "products.csv", "text/csv", "id,name\n1,test".getBytes());
        String response = mockMvc.perform(multipart("/uploads").file(file).file(file))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("products.csv"))
                .andReturn().getResponse().getContentAsString();
        JsonNode uploads = objectMapper.readTree(response);
        assertThat(uploads.get(0).get("url")).isNotEqualTo(uploads.get(1).get("url"));
        mockMvc.perform(get("/uploads")).andExpect(jsonPath("$.length()").value(2));
        String url = uploads.get(0).get("url").asText().replace("/icedata/api/v2", "");
        mockMvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(content().bytes(file.getBytes()));
    }

    @Test
    void invalidBatchLeavesNoFilesBehind() throws Exception {
        mockMvc.perform(multipart("/uploads")
                        .file(new MockMultipartFile("files[]", "valid.csv", "text/csv", new byte[]{1}))
                        .file(new MockMultipartFile("files[]", "script.php", "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest());
        assertThat(uploadsService.list()).isEmpty();
        mockMvc.perform(multipart("/uploads").file(new MockMultipartFile("files[]", "empty.csv", "text/csv", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOversizeFiles() throws Exception {
        mockMvc.perform(multipart("/uploads").file(new MockMultipartFile(
                        "files[]", "large.csv", "text/csv", new byte[20 * 1024 * 1024 + 1])))
                .andExpect(status().isPayloadTooLarge());
        assertThat(uploadsService.list()).isEmpty();
    }

    @Test
    void traversalFilenameStaysInsideStorageAndSymlinksAreNotExposed() throws Exception {
        uploadsService.store(List.of(new MockMultipartFile("files[]", "../../outside.csv", "text/csv", new byte[]{1})));
        assertThat(uploadsService.list()).singleElement().satisfies(upload -> assertThat(upload.getName()).isEqualTo("outside.csv"));
        String linkName = "00000000-0000-0000-0000-000000000000-secret.csv";
        Files.createSymbolicLink(directory.resolve(linkName), directory.resolve("../secret.csv"));
        mockMvc.perform(get("/uploads/" + linkName)).andExpect(status().isNotFound());
        mockMvc.perform(get("/uploads/not-found.csv")).andExpect(status().isNotFound());
        assertThat(uploadsService.list()).hasSize(1);
    }

    @Test
    void cleanupDeletesUploadsButKeepsOtherFiles() throws Exception {
        uploadsService.store(List.of(new MockMultipartFile("files[]", "test.docx", "application/octet-stream", new byte[]{1})));
        Files.writeString(directory.resolve("keep.txt"), "keep");
        uploadsService.cleanup();
        assertThat(uploadsService.list()).isEmpty();
        assertThat(directory.resolve("keep.txt")).exists();
    }
}
