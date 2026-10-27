package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.configuration.SecurityConfiguration;
import biz.icecat.icedatav2.models.api.ApiDailyIndex;
import biz.icecat.icedatav2.service.impl.DailyIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DailyIndexController.class)
@Import(SecurityConfiguration.class)
class DailyIndexControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private DailyIndexService service;

    @Test
    void publicStatusReadsSavedDataWithoutTriggeringPolling() throws Exception {
        when(service.findAll()).thenReturn(List.of(new ApiDailyIndex().setRepository("freexml")
                .setLangCode("EN").setFormat("xml").setResult("OK").setFreshness("TODAY")));
        mvc.perform(get("/daily-indexes")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repository").value("freexml"))
                .andExpect(jsonPath("$[0].freshness").value("TODAY"));
        verify(service).findAll();
        verifyNoMoreInteractions(service);
    }
}
