package io.github.eldiegos.domainasharness.dominio;

import java.util.regex.Pattern;

/** Documento de un alumno de movilidad. */
public record Passport(String value) {
	private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{6,9}");

	public Passport {
		if (value == null || !FORMAT.matcher(value).matches()) {
			throw new IllegalArgumentException("pasaporte no válido: de 6 a 9 letras mayúsculas o cifras");
		}
	}
}
