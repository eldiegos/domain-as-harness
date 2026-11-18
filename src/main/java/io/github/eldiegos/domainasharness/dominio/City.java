package io.github.eldiegos.domainasharness.dominio;

public record City(String value) {
	public City {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("la ciudad de origen es obligatoria");
		}
		value = value.strip();
	}
}
