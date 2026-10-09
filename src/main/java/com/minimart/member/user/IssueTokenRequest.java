package com.minimart.member.user;

import jakarta.validation.constraints.NotBlank;

public record IssueTokenRequest(@NotBlank String loginName, @NotBlank String password) {

	public IssueTokenRequest {
		loginName = LoginNames.normalize(loginName);
	}

	@Override
	public String toString() {
		return "IssueTokenRequest[loginName=" + loginName + "]";
	}
}
