package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiSupplier;
import biz.icecat.icedatav2.service.impl.DefaultSuppliersService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import biz.icecat.icedatav2.configuration.SecurityConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static biz.icecat.icedatav2.utils.TestUtils.objectMapper;
import static biz.icecat.icedatav2.utils.TestUtils.objectToJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// in case I'll add some auth there is excludeFilters field
// e.g. excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = MyAwesomeFilter.class)}
@WebMvcTest(value = SuppliersController.class)
@ExtendWith(MockitoExtension.class)
@Import(SecurityConfiguration.class)
class SuppliersControllerTest {

    private final static String SUPPLIERS_ENDPOINT = "/suppliers";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DefaultSuppliersService suppliersService;

    private final static List<ApiSupplier> allSuppliers = new ArrayList<>();
    private final static ApiSupplier sponsor = new ApiSupplier().setSupplierId(1L).setSupplierName("Sponsor").setIsSponsor(1).setBrandLogo("Logo");
    private final static ApiSupplier notSponsor = new ApiSupplier().setSupplierId(2L).setSupplierName("NotSponsor").setIsSponsor(0).setBrandLogo("Logo");

    static {
        allSuppliers.add(sponsor);
        allSuppliers.add(notSponsor);
    }


    @Test
    @SneakyThrows
    void getAllSuppliers() {
        when(suppliersService.findAll()).thenReturn(allSuppliers);

        MvcResult mvcResult = mockMvc.perform(get(SUPPLIERS_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn();

        List<ApiSupplier> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(allSuppliers);
    }

    @Test
    void createSupplier() {
        when(suppliersService.createSupplier(notSponsor)).thenReturn(notSponsor);
        SuppliersController suppliersController = new SuppliersController(suppliersService);

        ResponseEntity<ApiSupplier> response = suppliersController.createSupplier(notSponsor);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(notSponsor);
    }

    @Test
    @SneakyThrows
    void shouldRejectSupplierCreationWithoutChangingData() {
        mockMvc.perform(post(SUPPLIERS_ENDPOINT).with(csrf())
                        .content(objectToJson(notSponsor))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(suppliersService);
    }

    @Test
    @WithMockUser(authorities = "OTHER_AUTHORITY")
    void shouldRejectUserWithoutAdminAuthority() throws Exception {
        shouldRejectSupplierCreationWithoutChangingData();
    }

    @Test
    @WithMockUser(authorities = SecurityConfiguration.ADMIN_AUTHORITY)
    void shouldCreateSupplierWithAdminAuthority() throws Exception {
        when(suppliersService.createSupplier(notSponsor)).thenReturn(notSponsor);

        mockMvc.perform(post(SUPPLIERS_ENDPOINT).with(csrf())
                        .content(objectToJson(notSponsor))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(suppliersService).createSupplier(notSponsor);
    }

    @Test
    @WithMockUser(authorities = SecurityConfiguration.ADMIN_AUTHORITY)
    void shouldRejectAdministrativeWriteWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(SUPPLIERS_ENDPOINT)
                        .content(objectToJson(notSponsor))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(suppliersService);
    }

}
