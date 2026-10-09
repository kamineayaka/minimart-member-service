package com.minimart.member.user;

import java.time.Instant;

public record IssuedToken(String token, String tokenType, Instant expiresAt) {

	@Override
	public String toString() {
		return "IssuedToken[tokenType=" + tokenType + ", expiresAt=" + expiresAt + "]";
	}
}
