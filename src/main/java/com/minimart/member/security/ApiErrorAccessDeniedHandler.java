package com.minimart.member.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.minimart.api.error.ApiError;
import com.minimart.api.http.CorrelationIdHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper json;

	public ApiErrorAccessDeniedHandler(ObjectMapper json) {
		this.json = json;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException {
		ApiErrorWriter.write(json, response, HttpStatus.FORBIDDEN.value(),
				ApiError.of("FORBIDDEN", "Access is denied", CorrelationIdHolder.get()));
	}
}
