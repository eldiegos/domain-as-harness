package io.github.eldiegos.domainasharness.dominio;

/** Nombre completo. El constructor normaliza: quitar los espacios de los extremos es parte de la regla. */
public record FullName(String value) {
	public static final int MAX = 100;

	public FullName {
		if (value == null || value.isBlank() || value.strip().length() > MAX) {
			throw new IllegalArgumentException("el nombre es obligatorio y tiene como mucho " + MAX + " caracteres");
		}
		value = value.strip();
	}
}
