package biz.icecat.icedatav2.controller;

import org.junit.jupiter.api.Test;
import biz.icecat.icedatav2.configuration.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfiguration.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminRequiresAuthorityForEveryMethodAndSubpath() throws Exception {
        for (String path : new String[]{"/admin", "/admin/", "/admin/init"}) {
            for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD}) {
                mockMvc.perform(request(method, path).with(csrf())).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    @WithMockUser(authorities = "OTHER_AUTHORITY")
    void adminRejectsUserWithoutAuthority() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/admin/init"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = SecurityConfiguration.ADMIN_AUTHORITY)
    void authorizedRequestReachesRetiredController() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/admin/init"))
                .andExpect(status().isNotFound());
    }

}
