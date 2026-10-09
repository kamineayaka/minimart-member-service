package com.minimart.member.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minimart.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("minimart.jwt.secret must be at least 32 bytes");
		}
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw new IllegalArgumentException("minimart.jwt.ttl must be positive");
		}
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("minimart.jwt.issuer must not be blank");
		}
	}
}
