package com.example.AuthService.startup;

import com.example.AuthService.entity.Role;
import com.example.AuthService.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {
    private final RoleRepository roleRepo;

    @Override
    public void run(String... args) {
        createIfMissing("USER");
        createIfMissing("MODERATOR");
        createIfMissing("ADMIN");
    }

    private void createIfMissing(String name) {
        roleRepo.findByName(name).orElseGet(() -> roleRepo.save(Role.builder().name(name).build()));
    }
}
