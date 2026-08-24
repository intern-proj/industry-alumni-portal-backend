package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.UserAdminCreateRequestDto;
import com.portal.userprofileservice.dto.request.UserAdminUpdateRequestDto;
import com.portal.userprofileservice.dto.response.PageResponseDto;
import com.portal.userprofileservice.dto.response.UserAdminResponseDto;
import com.portal.userprofileservice.model.AccountStatus;
import com.portal.userprofileservice.model.UserRole;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    UserAdminResponseDto createManagementOrAdminUser(UserAdminCreateRequestDto dto);

    UserAdminResponseDto getUserById(String userId);

    PageResponseDto<UserAdminResponseDto> getAllUsers(String query, UserRole role, AccountStatus status, Pageable pageable);

    UserAdminResponseDto updateUser(String userId, UserAdminUpdateRequestDto dto);

    UserAdminResponseDto updateAccountStatus(String userId, AccountStatus status);

    void deleteUser(String userId);
}
