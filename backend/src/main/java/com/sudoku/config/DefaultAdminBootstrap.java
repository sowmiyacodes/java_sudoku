package com.sudoku.config;

import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminBootstrap.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = userRepository.findByUsernameIgnoreCase(ADMIN_USERNAME).orElseGet(() -> {
            User created = new User();
            created.setUsername(ADMIN_USERNAME);
            created.setDisplayName("Administrator");
            created.setEmail("admin@sudoku.local");
            created.setActive(true);
            return created;
        });

        admin.setUsername(ADMIN_USERNAME);
        admin.setDisplayName("Administrator");
        admin.setEmail("admin@sudoku.local");
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setActive(true);

        userRepository.save(admin);
        log.info("Default admin account is ready: username=admin password=admin");
    }
}
