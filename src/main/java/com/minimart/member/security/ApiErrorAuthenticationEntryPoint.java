package com.minimart.member.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.minimart.api.error.ApiError;
import com.minimart.api.http.CorrelationIdHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper json;

	public ApiErrorAuthenticationEntryPoint(ObjectMapper json) {
		this.json = json;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		ApiErrorWriter.write(json, response, HttpStatus.UNAUTHORIZED.value(),
				ApiError.of("UNAUTHENTICATED", "Authentication is required", CorrelationIdHolder.get()));
	}
}
