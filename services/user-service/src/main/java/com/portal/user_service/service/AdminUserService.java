package com.portal.user_service.service;

import com.portal.user_service.dto.request.UserAdminCreateRequestDto;
import com.portal.user_service.dto.request.UserAdminUpdateRequestDto;
import com.portal.user_service.dto.response.PageResponseDto;
import com.portal.user_service.dto.response.UserAdminResponseDto;
import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserRole;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    UserAdminResponseDto createManagementOrAdminUser(UserAdminCreateRequestDto dto);

    UserAdminResponseDto getUserById(String userId);

    PageResponseDto<UserAdminResponseDto> getAllUsers(String query, UserRole role, AccountStatus status, Pageable pageable);

    UserAdminResponseDto updateUser(String userId, UserAdminUpdateRequestDto dto);

    UserAdminResponseDto updateAccountStatus(String userId, AccountStatus status);

    void deleteUser(String userId);
}
