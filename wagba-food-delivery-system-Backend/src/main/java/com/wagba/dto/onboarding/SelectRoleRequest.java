package com.wagba.dto.onboarding;

import com.wagba.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class SelectRoleRequest {

	@NotNull
	private UserRole role;
	
	public UserRole getRole()
	{
		return role;
	}
	
	public void setRole(UserRole role) {
        this.role = role;
    }
}
