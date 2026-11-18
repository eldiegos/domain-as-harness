package io.github.eldiegos.domainasharness.dominio;

import java.util.regex.Pattern;

/** Código de país de dos letras mayúsculas. */
public record CountryCode(String value) {
	private static final Pattern FORMAT = Pattern.compile("[A-Z]{2}");

	public CountryCode {
		if (value == null || !FORMAT.matcher(value).matches()) {
			throw new IllegalArgumentException("el país de origen es un código de dos letras mayúsculas");
		}
	}
}
