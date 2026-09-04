package com.portal.user_service.controller;

import com.portal.user_service.dto.request.UserAdminCreateRequestDto;
import com.portal.user_service.dto.request.UserAdminUpdateRequestDto;
import com.portal.user_service.dto.response.ApiResponseDto;
import com.portal.user_service.dto.response.PageResponseDto;
import com.portal.user_service.dto.response.UserAdminResponseDto;
import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserRole;
import com.portal.user_service.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<UserAdminResponseDto>> createManagementOrAdminUser(
            @Valid @RequestBody UserAdminCreateRequestDto requestDto) {
        UserAdminResponseDto created = adminUserService.createManagementOrAdminUser(requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "Management/Admin user created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserAdminResponseDto>> getUserById(@PathVariable String userId) {
        UserAdminResponseDto user = adminUserService.getUserById(userId);
        return ResponseEntity.ok(ApiResponseDto.success(user, "User details retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<UserAdminResponseDto>>> getAllUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponseDto<UserAdminResponseDto> users = adminUserService.getAllUsers(query, role, status, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(users, "Users retrieved successfully"));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserAdminResponseDto>> updateUser(
            @PathVariable String userId,
            @RequestBody UserAdminUpdateRequestDto requestDto) {
        UserAdminResponseDto updated = adminUserService.updateUser(userId, requestDto);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "User updated successfully"));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponseDto<UserAdminResponseDto>> updateAccountStatus(
            @PathVariable String userId,
            @RequestParam AccountStatus status) {
        UserAdminResponseDto updated = adminUserService.updateAccountStatus(userId, status);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "User status updated successfully"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteUser(@PathVariable String userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "User deleted successfully"));
    }
}
