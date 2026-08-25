package com.wagba.dto.auth;

import com.wagba.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class SelectRoleRequest {

    private Long userId;

    private String email;

    @NotNull(message = "Role is required")
    private UserRole role;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
