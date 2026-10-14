package com.safehome.backend.domain.auth;

import com.safehome.backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "safehome.auth.enforce=true",
    "safehome.legacy-admin-api.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void signupLoginAuthorizationAndLogout() throws Exception {
        String email = "auth-" + UUID.randomUUID() + "@example.test";
        String password = "LongLocalPassword-2026!";
        String signup = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"등록자\",\"phone\":\"010-1234-5678\"}";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).isNotEqualTo(password);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"incorrect-password\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
        String body = login.getResponse().getContentAsString();
        String token = body.replaceFirst(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
        assertThat(token).isNotBlank();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value(email));
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsLocalFrontendCorsPreflight() throws Exception {
        mvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Methods",
                org.hamcrest.Matchers.containsString("POST")))
            .andExpect(header().string("Access-Control-Allow-Headers",
                org.hamcrest.Matchers.containsStringIgnoringCase("content-type")));
    }

    @Test
    void blocksLegacyAdminApiEvenForAuthenticatedUser() throws Exception {
        String email = "legacy-" + UUID.randomUUID() + "@example.test";
        String password = "LongLocalPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                    + "\",\"name\":\"등록자\"}"))
            .andExpect(status().isCreated());
        String token = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
            .replaceFirst(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mvc.perform(get("/api/risk-analyses").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
