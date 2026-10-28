package com.shanthan.products.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.shanthan.products.entity.Product;
import com.shanthan.products.exception.DuplicateProductNameException;
import com.shanthan.products.model.CreateProductRequest;
import com.shanthan.products.model.ProductResponse;
import com.shanthan.products.repository.ProductRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class ProductServiceTest {

	private final ProductRepository productRepository = mock(ProductRepository.class);

	private final ProductService productService = new ProductService(productRepository);

	@ParameterizedTest
	@CsvSource(delimiter = '|', quoteCharacter = '\'', value = {
			"P1                       | P1",
			"p1                       | P1",
			"' P1 '                   | P1",
			"'\t p1\n'                | P1",
			"Some Product Name        | SOME PRODUCT NAME",
			"'some  product'          | SOME  PRODUCT",
	})
	void normalizeNameTrimsLeadingAndTrailingWhitespaceAndUppercases(String name, String expected) {
		assertThat(ProductService.normalizeName(name)).isEqualTo(expected);
	}

	@Test
	void normalizeNameDoesNotDependOnTheDefaultLocale() {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.of("tr"));
			assertThat(ProductService.normalizeName("pi")).isEqualTo("PI");
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	void createSavesTheNormalizedName() {
		given(productRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.create(new CreateProductRequest(" p1 "));

		ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
		verify(productRepository).save(saved.capture());
		assertThat(saved.getValue().getName()).isEqualTo("P1");
		assertThat(response.name()).isEqualTo("P1");
	}

	@Test
	void createReportsADuplicateNameWhenTheUniqueConstraintIsViolated() {
		given(productRepository.save(any())).willThrow(integrityViolation(ProductService.UNIQUE_VIOLATION));

		assertThatThrownBy(() -> productService.create(new CreateProductRequest(" p1 ")))
				.isInstanceOf(DuplicateProductNameException.class)
				.hasMessage("A product named P1 already exists.");
	}

	@Test
	void createRethrowsOtherIntegrityViolationsUnchanged() {
		DataIntegrityViolationException otherViolation = integrityViolation("23514"); // check_violation
		given(productRepository.save(any())).willThrow(otherViolation);

		assertThatThrownBy(() -> productService.create(new CreateProductRequest("P1"))).isSameAs(otherViolation);
	}

	@Test
	void listReturnsTheProductsInTheRepositoryOrder() {
		Product newer = mock(Product.class);
		given(newer.getId()).willReturn(UUID.fromString("019a2b3c-4d5e-7f00-8000-000000000002"));
		given(newer.getName()).willReturn("P2");
		Product older = mock(Product.class);
		given(older.getId()).willReturn(UUID.fromString("019a2b3c-4d5e-7f00-8000-000000000001"));
		given(older.getName()).willReturn("P1");
		given(productRepository.findAllByOrderByIdDesc()).willReturn(List.of(newer, older));

		assertThat(productService.list()).containsExactly(
				new ProductResponse(newer.getId(), "P2"),
				new ProductResponse(older.getId(), "P1"));
	}

	private static DataIntegrityViolationException integrityViolation(String sqlState) {
		return new DataIntegrityViolationException("could not execute statement",
				new SQLException("violation", sqlState));
	}
}
