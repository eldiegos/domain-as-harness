package io.github.eldiegos.domainasharness.dominio;

public record University(String value) {
	public static final int MAX = 150;

	public University {
		if (value == null || value.isBlank() || value.strip().length() > MAX) {
			throw new IllegalArgumentException(
				"la universidad de origen es obligatoria y tiene como mucho " + MAX + " caracteres");
		}
		value = value.strip();
	}
}
