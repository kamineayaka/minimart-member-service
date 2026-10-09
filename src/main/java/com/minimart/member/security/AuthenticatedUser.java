package com.minimart.member.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.minimart.member.web.MemberException;

@Component
public class AuthenticatedUser {

	public long id() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			throw unauthenticated();
		}
		String name = authentication.getName();
		if (name == null || name.isBlank()) {
			throw unauthenticated();
		}
		try {
			return Long.parseLong(name);
		}
		catch (NumberFormatException ex) {
			throw unauthenticated();
		}
	}

	private static MemberException.Unauthenticated unauthenticated() {
		return new MemberException.Unauthenticated("UNAUTHENTICATED", "Authentication is required");
	}
}
