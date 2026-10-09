package com.minimart.member.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@NotBlank @Size(min = 3, max = 64) @Pattern(regexp = LoginNames.PATTERN, message = "must be 3-64 letters, digits, '.', '_' or '-'") String loginName,
		@NotBlank @Size(min = 8, max = 72) String password) {

	public RegisterUserRequest {
		loginName = LoginNames.normalize(loginName);
	}

	@Override
	public String toString() {
		return "RegisterUserRequest[loginName=" + loginName + "]";
	}
}
