package com.shanthan.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.test.json.JsonCompareMode;
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
	void listReturnsAnEmptyArrayWhenThereAreNoProducts() throws Exception {
		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("[]", JsonCompareMode.STRICT));
	}

	@Test
	void listReturnsEverySavedProductNewestFirst() throws Exception {
		for (String name : List.of("P1", "P2")) {
			mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \"" + name + "\"}"))
					.andExpect(status().isCreated());
		}
		Map<String, UUID> ids = jdbcClient.sql("select name, id from products").query()
				.listOfRows().stream()
				.collect(Collectors.toMap(row -> (String) row.get("name"), row -> (UUID) row.get("id")));

		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(ids.get("P2").toString()))
				.andExpect(jsonPath("$[0].name").value("P2"))
				.andExpect(jsonPath("$[1].id").value(ids.get("P1").toString()))
				.andExpect(jsonPath("$[1].name").value("P1"));
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
				{"name": "  Some  Product Name  "}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("SOME  PRODUCT NAME"));

		assertThat(jdbcClient.sql("select name from products").query(String.class).single())
				.isEqualTo("SOME  PRODUCT NAME");
	}

	// A name using every allowed symbol passes both the API and the database checks (a mismatch would be a 500).
	@Test
	void createStoresANameWithTheAllowedSymbols() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "  Salt & Pepper-T_2  "}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("SALT & PEPPER-T_2"));

		assertThat(jdbcClient.sql("select name from products").query(String.class).single())
				.isEqualTo("SALT & PEPPER-T_2");
	}

	@Test
	void createAllowsANameThatAlreadyExists() throws Exception {
		for (String name : List.of("P1", " p1 ")) {
			mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \"" + name + "\"}"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.name").value("P1"));
		}

		List<Map<String, Object>> rows = jdbcClient.sql("select id, name from products order by id desc").query().listOfRows();
		assertThat(rows).hasSize(2).allSatisfy(row -> assertThat(row.get("name")).isEqualTo("P1"));
		assertThat(rows.get(0).get("id")).isNotEqualTo(rows.get(1).get("id"));

		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(rows.get(0).get("id").toString()))
				.andExpect(jsonPath("$[0].name").value("P1"))
				.andExpect(jsonPath("$[1].id").value(rows.get(1).get("id").toString()))
				.andExpect(jsonPath("$[1].name").value("P1"));
	}

	// Quotes, parentheses and semicolons aren't allowed in a name, so SQL in a name is rejected before it reaches the database.
	@Test
	void sqlInANameIsRejectedAndNotExecuted() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "x'); DROP TABLE products; --"}"""))
				.andExpect(status().isBadRequest());

		assertThat(jdbcClient.sql("select count(*) from products").query(Long.class).single()).isZero();
	}

	@Test
	void databaseRejectsANameLongerThan50Characters() {
		jdbcClient.sql("insert into products (name) values (?)").param("A".repeat(50)).update();

		assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values (?)").param("B".repeat(51)).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void databaseRejectsANameThatDoesNotStartWithALetterOrHasOtherCharacters() {
		jdbcClient.sql("insert into products (name) values ('SALT & PEPPER-T_2  3')").update();

		for (String name : List.of("1P", "_P1", "-P1", "&P1", "KID'S CHAIR", "4.5L POT", "CAF\u00c9", "A\tB", "\u00a0P1",
				"P1\u200b")) {
			assertThatThrownBy(() -> jdbcClient.sql("insert into products (name) values (?)").param(name).update())
					.as("'%s'", name)
					.isInstanceOf(DataIntegrityViolationException.class);
		}
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
