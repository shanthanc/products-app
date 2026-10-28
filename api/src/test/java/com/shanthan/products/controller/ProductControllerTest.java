package com.shanthan.products.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shanthan.products.exception.DuplicateProductNameException;
import com.shanthan.products.model.CreateProductRequest;
import com.shanthan.products.model.ProductResponse;
import com.shanthan.products.service.ProductService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void createReturns201WithTheCreatedProduct() throws Exception {
		UUID id = UUID.fromString("019a2b3c-4d5e-7f00-8000-000000000001");
		given(productService.create(new CreateProductRequest("P1"))).willReturn(new ProductResponse(id, "P1"));

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "P1"}"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.name").value("P1"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", """
			{"name": null}""", """
			{"name": ""}""", """
			{"name": "   "}""", """
			{"name": "\\t\\n"}""", """
			{"name": "\\u00a0\\u2007\\u202f"}""", """
			{"name": "\\u001c"}""" })
	void createRejectsAMissingOrBlankName(String body) throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Invalid request content."))
				.andExpect(jsonPath("$.instance").value("/api/v1/products"))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").value("Name is required"));

		verifyNoInteractions(productService);
	}

	@Test
	void createRejectsANameLongerThan50Characters() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + "a".repeat(51) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").value("Name must be at most 50 characters"));

		verifyNoInteractions(productService);
	}

	@Test
	void createRejectsABlankNameLongerThan50CharactersWithOnlyTheRequiredError() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + " ".repeat(51) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].message").value("Name is required"));

		verifyNoInteractions(productService);
	}

	@ParameterizedTest
	@ValueSource(strings = { """
			{"name": "p1\\n"}""", """
			{"name": "a\\nb"}""", """
			{"name": "\\r\\np1"}""" })
	void createAcceptsANameContainingLineBreaks(String body) throws Exception {
		given(productService.create(any())).willReturn(new ProductResponse(UUID.randomUUID(), "P1"));

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	@Test
	void createAcceptsANameOfExactly50Characters() throws Exception {
		given(productService.create(any())).willReturn(new ProductResponse(UUID.randomUUID(), "A".repeat(50)));

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + "a".repeat(50) + "\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void createRejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(productService);
	}

	@Test
	void createRejectsANonJsonContentType() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.TEXT_PLAIN).content("P1"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(415));

		verifyNoInteractions(productService);
	}

	@Test
	void createReturns409WhenTheNameAlreadyExists() throws Exception {
		given(productService.create(any())).willThrow(new DuplicateProductNameException("P1", null));

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": " p1 "}"""))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("A product named P1 already exists."))
				.andExpect(jsonPath("$.instance").value("/api/v1/products"));
	}

	@Test
	void unexpectedErrorsReturnAGeneric500WithoutInternalDetails() throws Exception {
		given(productService.create(any())).willThrow(new IllegalStateException("connection to db-secret-host refused"));

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "P1"}"""))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("Something went wrong. Please try again."))
				.andExpect(content().string(not(containsString("db-secret-host"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}
}
