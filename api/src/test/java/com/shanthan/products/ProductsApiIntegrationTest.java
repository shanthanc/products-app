package com.shanthan.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductsApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void emptyTable() {
		jdbcClient.sql("delete from products").update();
	}

	@Test
	void createStoresTheProductWithADatabaseGeneratedUuidV7() throws Exception {
		String response = mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "P1"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("P1"))
				.andReturn().getResponse().getContentAsString();

		UUID id = UUID.fromString(JsonPath.read(response, "$.id"));
		assertThat(id.version()).isEqualTo(7);

		List<Map<String, Object>> rows = jdbcClient.sql("select id, name from products").query().listOfRows();
		assertThat(rows).containsExactly(Map.of("id", id, "name", "P1"));
	}

	@Test
	void databaseRejectsANullName() {
		assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values (null)").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsAnEmptyName() {
		assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values ('')").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void createStoresTheNameTrimmedAndUppercasedKeepingSpacesBetweenWords() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": " Some  Product Name\\t"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("SOME  PRODUCT NAME"));

		assertThat(jdbcClient.sql("select name from products").query(String.class).single())
				.isEqualTo("SOME  PRODUCT NAME");
	}

	@Test
	void createRejectsANameThatIsTheSameAfterNormalization() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "P1"}"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": " p1 "}"""))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("A product named P1 already exists."));

		assertThat(jdbcClient.sql("select name from products").query(String.class).list()).containsExactly("P1");
	}

	// Names reach the database as bound parameters, never as SQL text, so SQL in a name is stored as plain text.
	@Test
	void sqlInANameIsStoredAsTextAndNotExecuted() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "x'); DROP TABLE products; --"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("X'); DROP TABLE PRODUCTS; --"));

		assertThat(jdbcClient.sql("select name from products").query(String.class).single())
				.isEqualTo("X'); DROP TABLE PRODUCTS; --");
	}

	@Test
	void databaseRejectsANameLongerThan50Characters() {
		jdbcClient.sql("insert into products (name) values (?)").param("A".repeat(50)).update();

		assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values (?)").param("B".repeat(51)).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsADuplicateName() {
		jdbcClient.sql("insert into products (name) values ('P1')").update();

		assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values ('P1')").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsANameThatIsNotNormalized() {
		for (String unnormalized : List.of("p1", " P1", "P1 ", " ")) {
			assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values (?)").param(unnormalized).update())
					.as("'%s'", unnormalized)
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}
}
