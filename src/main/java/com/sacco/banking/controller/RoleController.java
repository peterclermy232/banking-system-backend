package com.sacco.banking.controller;

import com.sacco.banking.entity.Role;
import com.sacco.banking.enums.RoleName;
import com.sacco.banking.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieve all available roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{roleName}")
    @Operation(summary = "Get role by name", description = "Retrieve a role by its enum name")
    public ResponseEntity<Role> getRoleByName(@PathVariable String roleName) {
        RoleName name;
        try {
            name = RoleName.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        return roleRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
