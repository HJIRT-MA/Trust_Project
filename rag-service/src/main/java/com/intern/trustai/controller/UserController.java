package com.intern.trustai.controller;

import com.intern.trustai.dto.RoleUpdateRequest;
import com.intern.trustai.dto.UserResponse;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    public UserController(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        RealmResource realmResource = keycloak.realm(targetRealm);
        List<UserRepresentation> users = realmResource.users().list();

        List<UserResponse> userResponses = new ArrayList<>();
        for (UserRepresentation user : users) {
            UserResource userResource = realmResource.users().get(user.getId());
            List<String> roles = userResource.roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .filter(name -> name.equals("admin") || name.equals("analyst") || name.equals("viewer"))
                    .collect(Collectors.toList());

            userResponses.add(UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(roles)
                    .build());
        }

        return ResponseEntity.ok(userResponses);
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> updateUserRoles(@PathVariable String userId, @RequestBody RoleUpdateRequest request) {
        RealmResource realmResource = keycloak.realm(targetRealm);
        UserResource userResource = realmResource.users().get(userId);

        // Remove all relevant current roles
        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
        List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                .filter(r -> r.getName().equals("admin") || r.getName().equals("analyst") || r.getName().equals("viewer"))
                .collect(Collectors.toList());
        if (!rolesToRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(rolesToRemove);
        }

        // Add new roles
        List<RoleRepresentation> rolesToAdd = new ArrayList<>();
        for (String roleName : request.getRoles()) {
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            if (role != null) {
                rolesToAdd.add(role);
            }
        }
        
        if (!rolesToAdd.isEmpty()) {
            userResource.roles().realmLevel().add(rolesToAdd);
        }

        return ResponseEntity.ok("Rôles mis à jour avec succès");
    }
}
