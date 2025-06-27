package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiLanguage;
import biz.icecat.icedatav2.service.impl.DefaultLanguagesService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static biz.icecat.icedatav2.utils.TestConstants.LANGUAGES_ENDPOINT;
import static biz.icecat.icedatav2.utils.TestUtils.objectMapper;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LanguagesController.class)
@ExtendWith(MockitoExtension.class)
class LanguagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DefaultLanguagesService languagesService;

    private final static List<ApiLanguage> allLanguages = new ArrayList<>();
    private final static ApiLanguage english = new ApiLanguage()
            .setLangId(1L)
            .setLangName("English")
            .setLangCode("en")
            .setUpdated("2023-01-01");
    private final static ApiLanguage spanish = new ApiLanguage()
            .setLangId(2L)
            .setLangName("Spanish")
            .setLangCode("es")
            .setUpdated("2023-01-01");

    static {
        allLanguages.add(english);
        allLanguages.add(spanish);
    }

    @Test
    @SneakyThrows
    void getAllLanguages() {
        when(languagesService.getLanguages(null)).thenReturn(allLanguages);

        MvcResult mvcResult = mockMvc.perform(get(LANGUAGES_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn();

        List<ApiLanguage> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(allLanguages);
    }

    @Test
    @SneakyThrows
    void getLanguageById() {
        List<ApiLanguage> languages = List.of(english);
        when(languagesService.getLanguages(List.of(1L))).thenReturn(languages);

        MvcResult mvcResult = mockMvc.perform(get(LANGUAGES_ENDPOINT).param("id", "1"))
                .andExpect(status().isOk())
                .andReturn();

        List<ApiLanguage> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(languages);
    }

    @Test
    @SneakyThrows
    void getLanguageById_NotFound() {
        when(languagesService.getLanguages(List.of(999L))).thenReturn(List.of());

        mockMvc.perform(get(LANGUAGES_ENDPOINT).param("id", "999"))
                .andExpect(status().isOk());
    }
}