package com.sudoku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.dto.LoginRequest;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.dto.UpdateProfileRequest;
import com.sudoku.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sudoku-auth-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registrationCreatesAccountWithHashedPasswordAndSession() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest("player" + suffix, "Space Player", suffix + "@example.com", "a-strong-password");

        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("player" + suffix))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();

        assertTrue(userRepository.findByUsernameIgnoreCase("player" + suffix)
            .orElseThrow().getPasswordHash().startsWith("$2a$"));
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(suffix + "@example.com"));
    }

    @Test
    void loginAcceptsEmailAndPassword() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        register("solver" + suffix, suffix + "@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(suffix + "@example.com", "a-strong-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(suffix + "@example.com"));
    }

    @Test
    void duplicateUsernameOrEmailIsRejected() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        register("duplicate" + suffix, suffix + "@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RegisterRequest(
                                "DUPLICATE" + suffix, "Another Player", "other" + suffix + "@example.com", "a-strong-password"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RegisterRequest(
                                "other" + suffix, "Another Player", suffix + "@EXAMPLE.COM", "a-strong-password"))))
                .andExpect(status().isConflict());
    }

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/auth/me").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new UpdateProfileRequest("Pilot", "pilot@example.com"))))
            .andExpect(status().isUnauthorized());
        }

        @Test
        void csrfTokenEndpointProvidesSpaToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
        }

        @Test
        void logoutInvalidatesAuthenticatedSession() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new RegisterRequest(
                    "logout" + suffix, "Logout Pilot", suffix + "@example.com", "a-strong-password"))))
            .andExpect(status().isCreated())
            .andReturn();
        MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new RegisterRequest(username, "Test Player", email, "a-strong-password"))))
                .andExpect(status().isCreated());
    }
}