package com.ecommerce.authservice.config;

import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        // Initialize default roles if they don't exist
        if (roleRepository.count() == 0) {
            List<String> defaultRoles = Arrays.asList("USER", "ADMIN", "SELLER");
            
            for (String roleName : defaultRoles) {
                Role role = new Role(roleName);
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
            
            log.info("Roles initialized successfully");
        }
    }
}
