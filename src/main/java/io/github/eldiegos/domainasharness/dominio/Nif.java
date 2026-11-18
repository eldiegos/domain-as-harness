package io.github.eldiegos.domainasharness.dominio;

import java.util.regex.Pattern;

/** Documento de un alumno propio: 8 cifras y la letra de control. */
public record Nif(String value) {
	private static final Pattern FORMAT = Pattern.compile("\\d{8}[A-Z]");
	private static final String LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

	public Nif {
		if (value == null || !FORMAT.matcher(value).matches()
				|| value.charAt(8) != LETTERS.charAt(Integer.parseInt(value.substring(0, 8)) % 23)) {
			throw new IllegalArgumentException("NIF no válido: 8 cifras y la letra de control");
		}
	}
}
