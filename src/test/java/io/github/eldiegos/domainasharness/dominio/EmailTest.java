package io.github.eldiegos.domainasharness.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
}
