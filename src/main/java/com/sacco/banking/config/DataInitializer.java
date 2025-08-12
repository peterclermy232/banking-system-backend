package com.sacco.banking.config;

import com.sacco.banking.entity.Role;
import com.sacco.banking.enums.RoleName;
import com.sacco.banking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        if (roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            Role adminRole = new Role(RoleName.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName(RoleName.ROLE_MEMBER).isEmpty()) {
            Role memberRole = new Role(RoleName.ROLE_MEMBER);
            roleRepository.save(memberRole);
        }
    }
}
