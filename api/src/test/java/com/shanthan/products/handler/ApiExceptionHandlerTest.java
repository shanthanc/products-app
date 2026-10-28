package com.shanthan.products.handler;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Exercises the handler through a test-only controller, so it's checked independently of the product endpoints.
@WebMvcTest(ApiExceptionHandlerTest.ThrowingController.class)
@Import(ApiExceptionHandlerTest.ThrowingController.class)
class ApiExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@RestController
	@RequestMapping("/test")
	static class ThrowingController {

		record Body(@NotBlank(message = "Name is required") String name) {
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("connection to db-secret-host refused");
		}

		@PostMapping("/validated")
		void validated(@Valid @RequestBody Body body) {
		}
	}

	@Test
	void unexpectedExceptionReturnsAGeneric500WithoutInternalDetails() throws Exception {
		mockMvc.perform(get("/test/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value(ApiExceptionHandler.UNEXPECTED_ERROR_DETAIL))
				.andExpect(jsonPath("$.instance").value("/test/unexpected"))
				.andExpect(content().string(not(containsString("db-secret-host"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	void validationFailureListsTheFieldErrors() throws Exception {
		mockMvc.perform(post("/test/validated").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").value("Name is required"));
	}

	@Test
	void springMvcErrorsUseTheSameProblemFormat() throws Exception {
		mockMvc.perform(get("/test/no-such-path"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.instance").value("/test/no-such-path"));
	}
}
