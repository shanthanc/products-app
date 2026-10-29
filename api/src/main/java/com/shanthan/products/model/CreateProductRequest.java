package com.shanthan.products.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
		@NotBlank(message = "Name is required")
		// After optional leading spaces: a letter, then only letters, digits, spaces and - & _. Spaces-only
		// and empty names also match, so they get just the "required" error.
		@Pattern(regexp = " *([A-Za-z][A-Za-z0-9 &_-]*)?",
				message = "Name must start with a letter and contain only letters, numbers, spaces and - & _")
		@Size(max = 50, message = "Name must not exceed 50 characters")
		String name) {
}
