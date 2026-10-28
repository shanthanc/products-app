package com.shanthan.products.handler;

import com.shanthan.products.exception.DuplicateProductNameException;
import com.shanthan.products.model.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Every error leaves the API as a ProblemDetail (application/problem+json); see the API contract in docs/PLAN.md.
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	static final String UNEXPECTED_ERROR_DETAIL = "Something went wrong. Please try again.";

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
				.toList();
		ProblemDetail body = ex.getBody();
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	// Spring MVC exceptions (bad JSON, validation, wrong content type, ...) all pass through here.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		String target = request instanceof ServletWebRequest servletRequest
				? servletRequest.getHttpMethod() + " " + servletRequest.getRequest().getRequestURI()
				: request.getDescription(false);
		if (statusCode.is5xxServerError()) {
			log.error("{} -> {}", target, statusCode.value(), ex);
		}
		else {
			log.warn("{} -> {}: {}", target, statusCode.value(), ex.getMessage());
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	@ExceptionHandler(DuplicateProductNameException.class)
	public ProblemDetail handleDuplicateName(DuplicateProductNameException ex, HttpServletRequest request) {
		log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), HttpStatus.CONFLICT.value(),
				ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("{} {} -> {}", request.getMethod(), request.getRequestURI(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_DETAIL);
	}
}
