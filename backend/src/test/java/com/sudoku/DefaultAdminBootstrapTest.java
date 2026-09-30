package com.sudoku;

import com.sudoku.config.DefaultAdminBootstrap;
import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:default-admin-bootstrap-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class DefaultAdminBootstrapTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultAdminBootstrap defaultAdminBootstrap;

    @Test
    @DisplayName("Bootstrap ensures the admin user exists with hardcoded password admin")
    void testAdminBootstrapCreatesHardcodedAdmin() {
        defaultAdminBootstrap.run(null);

        User admin = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        assertEquals("admin", admin.getUsername());
        assertEquals("Administrator", admin.getDisplayName());
        assertTrue(passwordEncoder.matches("admin", admin.getPasswordHash()));
    }
}
