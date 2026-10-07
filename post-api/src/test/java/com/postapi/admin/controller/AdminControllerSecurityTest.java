package com.postapi.admin.controller;

import com.postapi.admin.service.AdminService;
import com.postapi.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void normalUser_shouldNotAccessAdminEndpoints() throws Exception {

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/posts/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminService);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void admin_shouldAccessAdminEndpoints() throws Exception {

        UUID postId = UUID.randomUUID();

        when(adminService.getUsers(any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/posts/{id}", postId))
                .andExpect(status().isOk());

        verify(adminService).deletePost(postId);
    }

    @Test
    void unauthenticatedUser_shouldNotAccessAdminEndpoint() throws Exception {

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}