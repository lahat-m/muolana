package com.lahat.muolana.auth.web;

import com.lahat.muolana.auth.domain.AuthService;
import com.lahat.muolana.auth.domain.RegisterCmd;
import com.lahat.muolana.auth.domain.UpdateUserCmd;
import com.lahat.muolana.auth.domain.UserVM;
import com.lahat.muolana.auth.web.dtos.RegisterRequest;
import com.lahat.muolana.auth.web.dtos.UpdateUserRequest;
import com.lahat.muolana.auth.web.dtos.UserResponse;
import com.lahat.muolana.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("@ss.isAdmin()")
class AdminUsersController {

    private final AuthService authService;

    AdminUsersController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        UserVM user = authService.createAdminUser(new RegisterCmd(registerRequest.email(), registerRequest.password(), registerRequest.fullName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping
    ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserVM> page = authService.listUsers(role, isActive, pageable);
        return ResponseEntity.ok(PageResponse.from(page.map(this::toResponse)));
    }

    @GetMapping("/{userId}")
    ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(toResponse(authService.getUserById(userId)));
    }

    @PatchMapping("/{userId}")
    ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
                                            @RequestBody UpdateUserRequest updateUserRequest) {
        UpdateUserCmd updateUserCommand = new UpdateUserCmd(
                updateUserRequest.role() != null ? com.lahat.muolana.auth.domain.Role.valueOf(updateUserRequest.role().toUpperCase()) : null,
                updateUserRequest.isActive()
        );
        return ResponseEntity.ok(toResponse(authService.updateUser(userId, updateUserCommand)));
    }

    private UserResponse toResponse(UserVM userVm) {
        return new UserResponse(
                userVm.id(), userVm.email(), userVm.fullName(), userVm.role(), userVm.isActive(),
                userVm.createdAt() != null ? userVm.createdAt().toString() : null
        );
    }
}
