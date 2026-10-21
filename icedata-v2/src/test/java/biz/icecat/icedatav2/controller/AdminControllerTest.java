package biz.icecat.icedatav2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    @Test
    void retiredAdminCannotBeCalledThroughAnyMethodOrSubpath() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminController()).build();
        for (String path : new String[]{"/admin", "/admin/", "/admin/init"}) {
            for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD}) {
                mockMvc.perform(request(method, path)).andExpect(status().isNotFound());
            }
        }
    }
}
