package io.github.eldiegos.domainasharness.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("El puesto fronterizo: un Email inválido no puede existir")
class EmailTest {

	@Test
	@DisplayName("parse es para lo que llega de fuera: normaliza o devuelve vacío")
	void parse() {
		assertEquals(Optional.of(new Email("ana@example.com")), Email.parse("  ana@example.com "));
		assertEquals(Optional.empty(), Email.parse("ana@"));
		assertEquals(Optional.empty(), Email.parse(null));
	}

	@Test
	@DisplayName("El constructor es para el código de dentro: un correo inválido es un error y lanza")
	void constructor() {
		assertThrows(IllegalArgumentException.class, () -> new Email("ana@"));
		assertThrows(NullPointerException.class, () -> new Email(null));
	}

	@Test
	@DisplayName("Sin atajos: la reflexión no puede cambiar el campo de un record")
	void sinReflexion() throws Exception {
		Email email = new Email("ana@example.com");
		Field value = Email.class.getDeclaredField("value");
		value.setAccessible(true);

		assertThrows(IllegalAccessException.class, () -> value.set(email, "ana@"));
		assertEquals("ana@example.com", email.value());
	}

	/** Un record serializable con la misma regla: Email no lo es, porque nadie lo serializa. */
	record SerializableEmail(String value) implements Serializable {
		SerializableEmail {
			if (value == null || !value.contains("@")) {
				throw new IllegalArgumentException("correo no válido");
			}
		}
	}

	@Test
	@DisplayName("Sin atajos: si el record es serializable, Java lo deserializa con su constructor")
	void deserializacionNativa() throws Exception {
		byte[] bytes;
		try (var out = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(out)) {
			oos.writeObject(new SerializableEmail("ana@example.com"));
			oos.flush();
			bytes = out.toByteArray();
		}
		// Se manipulan los bytes: el valor guardado pasa a ser inválido («ana_example.com»).
		String datos = new String(bytes, StandardCharsets.ISO_8859_1).replace("ana@example.com", "ana_example.com");
		byte[] manipulados = datos.getBytes(StandardCharsets.ISO_8859_1);

		try (var in = new ObjectInputStream(new ByteArrayInputStream(manipulados))) {
			assertThrows(InvalidObjectException.class, in::readObject);
		}
	}

	@Test
	@DisplayName("Sin atajos: Jackson también construye el record con su constructor")
	void deserializacionJson() {
		JsonMapper json = JsonMapper.builder().build();

		assertEquals(new Email("ana@example.com"), json.readValue("{\"value\":\"ana@example.com\"}", Email.class));
		assertThrows(JacksonException.class, () -> json.readValue("{\"value\":\"ana@\"}", Email.class));
	}
}
