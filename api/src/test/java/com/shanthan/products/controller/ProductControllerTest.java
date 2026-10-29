package com.shanthan.products.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shanthan.products.model.CreateProductRequest;
import com.shanthan.products.model.ProductResponse;
import com.shanthan.products.service.ProductService;
import java.util.List;
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
			{"name": "\\u001c"}""" })
	void createRejectsAMissingOrBlankName(String body) throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("Invalid request content."))
				.andExpect(jsonPath("$.instance").value("/api/v1/products"))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				// Blank names with tabs or other non-space whitespace also break the allowed characters.
				.andExpect(jsonPath("$.errors[*].message", hasItem("Name is required")));

		verifyNoInteractions(productService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "1P", "_P1", "-P1", "&P1", "  1P", " -P1", "Kid's Chair", "4.5L Pot", "Café", "a\\tb",
			"a\\nb", "P1\\n", "\\u00a0P1", "P1\\u200b" })
	void createRejectsANameThatDoesNotStartWithALetterOrHasOtherCharacters(String name) throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + name + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors.length()").value(1))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message")
						.value("Name must start with a letter and contain only letters, numbers, spaces and - & _"));

		verifyNoInteractions(productService);
	}

	@Test
	void createAcceptsLettersDigitsSpacesAndTheAllowedSymbolsWithSurroundingSpaces() throws Exception {
		given(productService.create(any())).willReturn(new ProductResponse(UUID.randomUUID(), "SALT & PEPPER-T_2"));

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "  Salt & Pepper-T_2  "}"""))
				.andExpect(status().isCreated());
	}

	@Test
	void createRejectsANameLongerThan50Characters() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"" + "a".repeat(51) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].message").value("Name must not exceed 50 characters"));

		verifyNoInteractions(productService);
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
	void listReturns200WithTheProductsInTheServiceOrder() throws Exception {
		UUID newer = UUID.fromString("019a2b3c-4d5e-7f00-8000-000000000002");
		UUID older = UUID.fromString("019a2b3c-4d5e-7f00-8000-000000000001");
		given(productService.list()).willReturn(List.of(new ProductResponse(newer, "P2"), new ProductResponse(older, "P1")));

		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(newer.toString()))
				.andExpect(jsonPath("$[0].name").value("P2"))
				.andExpect(jsonPath("$[1].id").value(older.toString()))
				.andExpect(jsonPath("$[1].name").value("P1"));
	}

	@Test
	void listErrorsReturnAGeneric500() throws Exception {
		given(productService.list()).willThrow(new IllegalStateException("connection to db-secret-host refused"));

		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Something went wrong. Please try again."))
				.andExpect(content().string(not(containsString("db-secret-host"))));
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
