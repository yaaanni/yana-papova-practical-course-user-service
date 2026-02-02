package com.example.UserService.controller;

import com.example.UserService.dto.user.UserByIdResponse;
import com.example.UserService.dto.user.UserRequest;
import com.example.UserService.dto.user.UserResponse;
import com.example.UserService.security.service.UserSecurityService;
import com.example.UserService.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSecurityService userSecurityService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and principal.userId != null and #id == principal.userId)")
    @GetMapping("/{id}")
    public ResponseEntity<UserByIdResponse> getUserById(@PathVariable Long id) {
        UserByIdResponse response = userService.getUserById(id);
        System.out.println(response);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isOwner(#email, principal.userId())")
    @GetMapping("/email/{email}")
    public ResponseEntity<UserByIdResponse> getUserByEmail(@PathVariable String  email) {
        UserByIdResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) String name, @RequestParam(required = false) String surname) {
        Page<UserResponse> users = userService.findAll(page, size, name, surname);
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@RequestBody UserRequest request, @PathVariable Long id) {
        UserResponse response = userService.update(request, id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable Long id) {
        UserResponse response = userService.activate(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable Long id) {
        UserResponse response = userService.deactivate(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
