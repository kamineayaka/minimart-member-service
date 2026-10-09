package com.minimart.member.web;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.minimart.api.error.ApiError;
import com.minimart.api.error.ApiFieldError;
import com.minimart.api.error.ErrorCodes;
import com.minimart.api.http.CorrelationIdHolder;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
		List<ApiFieldError> fields = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(validation(fields));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest().body(ApiError.of(ErrorCodes.VALIDATION_ERROR, "Request body is not readable",
				CorrelationIdHolder.get()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException ex) {
		return ResponseEntity.badRequest()
				.body(validation(List.of(new ApiFieldError(ex.getParameterName(), "must be present"))));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.badRequest()
				.body(validation(List.of(new ApiFieldError(ex.getName(), "must be a valid value"))));
	}

	@ExceptionHandler(MemberException.class)
	ResponseEntity<ApiError> member(MemberException ex) {
		HttpStatus status = switch (ex) {
			case MemberException.NotFound ignored -> HttpStatus.NOT_FOUND;
			case MemberException.Conflict ignored -> HttpStatus.CONFLICT;
			case MemberException.Unauthenticated ignored -> HttpStatus.UNAUTHORIZED;
		};
		List<ApiFieldError> fields = List.of();
		if (ex instanceof MemberException.Conflict conflict && conflict.field() != null) {
			fields = List.of(new ApiFieldError(conflict.field(), ex.getMessage()));
		}
		return ResponseEntity.status(status)
				.body(new ApiError(ex.code(), ex.getMessage(), CorrelationIdHolder.get(), Instant.now(), fields));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		log.error("unhandled request failure", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("INTERNAL_ERROR", "Unexpected error", CorrelationIdHolder.get()));
	}

	private static ApiError validation(List<ApiFieldError> fields) {
		return new ApiError(ErrorCodes.VALIDATION_ERROR, "Request validation failed", CorrelationIdHolder.get(), Instant.now(),
				fields);
	}
}
