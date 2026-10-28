package com.shanthan.products.model;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// The length is checked only once the name is present, so a blank name gets exactly one error.
@GroupSequence({ CreateProductRequest.class, CreateProductRequest.LengthCheck.class })
public record CreateProductRequest(
		@NotNull(message = "Name is required")
		// Needs at least one character that is neither Java whitespace (what strip() removes) nor a
		// Unicode space separator (e.g. no-break space U+00A0), so a spaces-only name is rejected.
		@Pattern(regexp = "(?s).*[^\\p{javaWhitespace}\\p{Z}].*", message = "Name is required")
		@Size(max = 50, message = "Name must be at most {max} characters", groups = LengthCheck.class)
		String name) {

	interface LengthCheck {
	}
}
