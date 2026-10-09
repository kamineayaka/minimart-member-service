package com.minimart.member.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

import com.minimart.api.error.ApiError;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

final class ApiErrorWriter {

	private ApiErrorWriter() {
	}

	static void write(ObjectMapper json, HttpServletResponse response, int status, ApiError error) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		json.writeValue(response.getOutputStream(), error);
	}
}
