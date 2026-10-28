package com.shanthan.products.model;

// One entry of the "errors" property on a 400 validation ProblemDetail.
public record FieldViolation(String field, String message) {
}
