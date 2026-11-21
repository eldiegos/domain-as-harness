package io.github.eldiegos.domainasharness;

import com.jayway.jsonpath.JsonPath;
import io.github.eldiegos.domainasharness.dominio.EnrollmentDraft;
import io.github.eldiegos.domainasharness.dominio.EnrollmentId;
import io.github.eldiegos.domainasharness.dominio.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * La ida y vuelta: los datos entran por todos los caminos de escritura de la API (alta, importación,
 * borrador y cambios) y lo que la API devuelve como matrícula tiene que volver a pasar la aduana del
 * dominio con los mismos datos con los que entró.
 *
 * <p>Las pruebas de arquitectura miran la forma del código; esta mira el resultado. Si un cambio en la
 * API, en los casos de uso o en el almacén devuelve algo que el dominio rechazaría, o cambia los datos
 * por el camino, falla aunque el dominio no se haya tocado. Revalida también las matrículas que dan de
 * alta las demás pruebas, porque la aplicación comparte el almacén entre ellas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Ida y vuelta: lo que la API devuelve como matrícula vuelve a pasar la aduana")
class IdaYVueltaTest {

	@Autowired
	private MockMvc mvc;

	/** Lo que debería devolver la API para cada documento, según lo que se envió. */
	private final Map<String, EnrollmentDraft> esperadas = new LinkedHashMap<>();

	@Test
	@DisplayName("Cada matrícula que devuelve la API pasa otra vez la aduana y conserva sus datos")
	void idaYVuelta() throws Exception {
		// La ida, por cada camino de escritura.
		long propio = alta(EnrollmentDraft.home(nif(40000001), "Ana Ida", "ana.ida@example.com", "GII", 60, 2));
		long movilidad = alta(EnrollmentDraft.mobility("FRI40001", "Lea Ida", "lea.ida@example.com", "GADE", 30,
			"FR", "Lyon", "Université Lyon 2", "S1"));

		EnrollmentDraft csv1 = EnrollmentDraft.home(nif(40000002), "Luis Ida", "luis.ida@example.com", "GDER", 72, 3);
		EnrollmentDraft csv2 = EnrollmentDraft.mobility("DEI40002", "Lukas Ida", "lukas.ida@example.de", "GPSI", 60,
			"DE", "Berlin", "Freie Universität", "ANUAL");
		String fichero = linea(csv1) + "\n" + linea(csv2);
		enviar(post("/api/enrollments/import").contentType("text/csv;charset=UTF-8").content(fichero), 200);
		esperadas.put(csv1.document(), csv1);
		esperadas.put(csv2.document(), csv2);

		EnrollmentDraft borrador = EnrollmentDraft.home(nif(40000003), "Eva Ida", "eva.ida@example.com", "GPSI", 30, 1);
		String draft = enviar(post("/api/drafts").contentType(MediaType.APPLICATION_JSON).content(json(borrador)), 201);
		enviar(post("/api/drafts/" + JsonPath.read(draft, "$.id") + "/submission"), 201);
		esperadas.put(borrador.document(), borrador);

		cambiar(propio, "email", "{\"email\": \"ana.vuelta@example.com\"}");
		cambiar(propio, "credits", "{\"credits\": 66}");
		cambiar(propio, "degree", "{\"degree\": \"GADE\"}");
		cambiar(propio, "previous-course", "{\"previousCourse\": 3}");
		esperadas.put(nif(40000001), EnrollmentDraft.home(nif(40000001), "Ana Ida", "ana.vuelta@example.com",
			"GADE", 66, 3));
		cambiar(movilidad, "stay", "{\"period\": \"ANUAL\", \"credits\": 54}");
		cambiar(movilidad, "origin", "{\"originCountry\": \"IT\", \"originCity\": \"Bologna\", "
			+ "\"homeUniversity\": \"Università di Bologna\"}");
		esperadas.put("FRI40001", EnrollmentDraft.mobility("FRI40001", "Lea Ida", "lea.ida@example.com", "GADE", 54,
			"IT", "Bologna", "Università di Bologna", "ANUAL"));

		// La vuelta: todo lo que la API devuelve como matrícula.
		List<Map<String, Object>> todas = JsonPath.read(enviar(get("/api/enrollments"), 200), "$");
		List<String> fallos = new ArrayList<>();
		for (Map<String, Object> vista : todas) {
			fallos.addAll(revisar(vista));
			Object id = vista.get("id");
			Map<String, Object> una = JsonPath.read(enviar(get("/api/enrollments/" + id), 200), "$");
			if (!una.equals(vista)) {
				fallos.add("GET /api/enrollments/" + id + " no coincide con el listado: " + una + " frente a " + vista);
			}
		}
		for (String documento : esperadas.keySet()) {
			if (todas.stream().noneMatch(v -> documento.equals(v.get("document")))) {
				fallos.add("la matrícula " + documento + " entró y no sale en el listado");
			}
		}
		assertTrue(fallos.isEmpty(), String.join("\n", fallos));
	}

	/** Revalida una vista con la aduana del dominio y, si la envió esta prueba, la compara con lo enviado. */
	private List<String> revisar(Map<String, Object> vista) {
		List<String> fallos = new ArrayList<>();
		EnrollmentDraft vuelta = new EnrollmentDraft(texto(vista.get("type")), texto(vista.get("document")),
			texto(vista.get("fullName")), texto(vista.get("email")), texto(vista.get("degree")),
			texto(vista.get("credits")), texto(vista.get("previousCourse")), texto(vista.get("originCountry")),
			texto(vista.get("originCity")), texto(vista.get("homeUniversity")), texto(vista.get("period")));
		long id = ((Number) vista.get("id")).longValue();
		if (id < 1) {
			fallos.add("matrícula " + id + ": su identificador no lo emitió el almacén");
			return fallos;
		}
		if (vuelta.submit(new EnrollmentId(id)) instanceof Result.Rejected<?> r) {
			fallos.add("matrícula " + id + " (" + vuelta.document() + ") no pasa la aduana: " + r.reasons());
		}
		EnrollmentDraft esperada = esperadas.get(vuelta.document());
		if (esperada != null && !mismosDatos(esperada, vuelta)) {
			fallos.add("matrícula " + id + ": entró " + esperada + " y salió " + vuelta);
		}
		return fallos;
	}

	private static boolean mismosDatos(EnrollmentDraft a, EnrollmentDraft b) {
		return Objects.equals(a, b);
	}

	private long alta(EnrollmentDraft d) throws Exception {
		String r = enviar(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(json(d)), 201);
		esperadas.put(d.document(), d);
		return ((Number) JsonPath.read(r, "$.id")).longValue();
	}

	private void cambiar(long id, String campo, String cuerpo) throws Exception {
		enviar(put("/api/enrollments/" + id + "/" + campo).contentType(MediaType.APPLICATION_JSON).content(cuerpo), 200);
	}

	private String enviar(MockHttpServletRequestBuilder peticion, int estado) throws Exception {
		var respuesta = mvc.perform(peticion).andReturn().getResponse();
		assertEquals(estado, respuesta.getStatus(), respuesta.getContentAsString(StandardCharsets.UTF_8));
		return respuesta.getContentAsString(StandardCharsets.UTF_8);
	}

	private static String json(EnrollmentDraft d) {
		return """
			{"type": %s, "document": %s, "fullName": %s, "email": %s, "degree": %s, "credits": %s,
			 "previousCourse": %s, "originCountry": %s, "originCity": %s, "homeUniversity": %s, "period": %s}
			""".formatted(q(d.type()), q(d.document()), q(d.fullName()), q(d.email()), q(d.degree()), q(d.credits()),
			q(d.previousCourse()), q(d.originCountry()), q(d.originCity()), q(d.homeUniversity()), q(d.period()));
	}

	private static String linea(EnrollmentDraft d) {
		return String.join(";", d.type(), d.document(), d.fullName(), d.email(), d.degree(), d.credits(),
			vacio(d.previousCourse()), vacio(d.originCountry()), vacio(d.originCity()), vacio(d.homeUniversity()),
			vacio(d.period()));
	}

	private static String q(String s) {
		return s == null ? "null" : "\"" + s + "\"";
	}

	private static String vacio(String s) {
		return s == null ? "" : s;
	}

	private static String texto(Object o) {
		return o == null ? null : o.toString();
	}

	static String nif(int n) {
		return String.format("%08d", n) + "TRWAGMYFPDXBNJZSQVHLCKE".charAt(n % 23);
	}
}
