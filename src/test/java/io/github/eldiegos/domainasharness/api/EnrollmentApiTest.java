package io.github.eldiegos.domainasharness.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La frontera HTTP. Cada prueba usa sus propios documentos, porque la aplicación comparte el almacén
 * en memoria entre pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API: la frontera HTTP")
class EnrollmentApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("Un alta válida devuelve 201 con la matrícula")
	void altaValida() throws Exception {
		mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"type": "PROPIO", "document": "%s", "fullName": " Ana Pérez ", "email": "ana@example.com",
				 "degree": "GII", "credits": 60, "previousCourse": 2}
				""".formatted(nif(10000001))))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.fullName").value("Ana Pérez"))
			.andExpect(jsonPath("$.originCountry").doesNotExist());
	}

	@Test
	@DisplayName("Un alta inválida devuelve 422 con todos los motivos")
	void altaInvalida() throws Exception {
		mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"type": "MOVILIDAD", "document": "x1", "fullName": "Lea", "email": "lea@",
				 "degree": "GADE", "credits": "cuarenta", "originCountry": "ES", "period": "VERANO"}
				"""))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.reasons", hasSize(6)))
			.andExpect(jsonPath("$.reasons[0]").value("correo no válido"));
	}

	@Test
	@DisplayName("Un documento repetido devuelve 409")
	void documentoRepetido() throws Exception {
		String body = """
			{"type": "PROPIO", "document": "%s", "fullName": "Luis", "email": "luis@example.com",
			 "degree": "GDER", "credits": 72, "previousCourse": 3}
			""".formatted(nif(10000002));
		mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isCreated());
		mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Un cambio que rompe la regla de los créditos devuelve 422 y la matrícula no cambia")
	void cambioInvalido() throws Exception {
		String location = mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"type": "MOVILIDAD", "document": "M3000001", "fullName": "Lukas Weber", "email": "lukas@example.de",
				 "degree": "GADE", "credits": 30, "originCountry": "DE", "originCity": "Berlin",
				 "homeUniversity": "Freie Universitat", "period": "S1"}
				"""))
			.andReturn().getResponse().getHeader("Location");

		mvc.perform(put(location + "/credits").contentType(MediaType.APPLICATION_JSON).content("{\"credits\": 40}"))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.reasons[0]").value("un alumno de movilidad con periodo S1 se matricula de 12 a 30 ECTS"));
		mvc.perform(get(location)).andExpect(jsonPath("$.credits").value(30));

		mvc.perform(put(location + "/stay").contentType(MediaType.APPLICATION_JSON)
				.content("{\"period\": \"ANUAL\", \"credits\": 40}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.credits").value(40));
	}

	@Test
	@DisplayName("La importación CSV es todo o nada")
	void importacion() throws Exception {
		String bad = "PROPIO;%s;Luis;luis@example.com;GDER;72;3;;;;%nPROPIO;mal formado%n".formatted(nif(10000003));
		mvc.perform(post("/api/enrollments/import").contentType("text/csv").content(bad))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.reasons[0]").value("línea 2: se esperaban 11 campos y hay 2"));

		String good = "PROPIO;%s;Luis;luis@example.com;GDER;72;3;;;;%n".formatted(nif(10000003));
		mvc.perform(post("/api/enrollments/import").contentType("text/csv").content(good))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enrolled").value(1));
	}

	@Test
	@DisplayName("El borrador se guarda incompleto, no se envía hasta que cumple las reglas, y entonces se da de alta")
	void borrador() throws Exception {
		String location = mvc.perform(post("/api/drafts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\": \"MOVILIDAD\", \"fullName\": \"Marie Dubois\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		mvc.perform(post(location + "/submission"))
			.andExpect(status().is(422))
			.andExpect(jsonPath("$.reasons", hasSize(8)));

		mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content("""
				{"type": "MOVILIDAD", "document": "FR998877", "fullName": "Marie Dubois", "email": "marie@example.fr",
				 "degree": "GDER", "credits": 60, "originCountry": "FR", "originCity": "Lyon",
				 "homeUniversity": "Universite Lyon 2", "period": "ANUAL"}
				"""))
			.andExpect(status().isOk());

		mvc.perform(post(location + "/submission"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.document").value("FR998877"));
		mvc.perform(get(location)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("El carné exige el pago: 409 sin pagar, 201 después")
	void carne() throws Exception {
		String location = mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("""
				{"type": "PROPIO", "document": "%s", "fullName": "Eva Ruiz", "email": "eva@example.com",
				 "degree": "GPSI", "credits": 60, "previousCourse": 1}
				""".formatted(nif(10000004))))
			.andReturn().getResponse().getHeader("Location");

		mvc.perform(post(location + "/student-card")).andExpect(status().isConflict());
		mvc.perform(post(location + "/payment")).andExpect(status().isNoContent());
		mvc.perform(post(location + "/student-card"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.holder").value("Eva Ruiz"));
	}

	/** Un NIF válido para el número dado: las 8 cifras y su letra de control. */
	private static String nif(int number) {
		return "%08d%c".formatted(number, "TRWAGMYFPDXBNJZSQVHLCKE".charAt(number % 23));
	}
}
