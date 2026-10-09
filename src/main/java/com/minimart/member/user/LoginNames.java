package com.minimart.member.user;

import java.util.Locale;

public final class LoginNames {

	public static final String PATTERN = "^[\\p{L}0-9][\\p{L}0-9._-]{2,63}$";

	private LoginNames() {
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return null;
		}
		return raw.trim().toLowerCase(Locale.ROOT);
	}
}
