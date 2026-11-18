package io.github.eldiegos.domainasharness.dominio;

/** Titulaciones (ficticias) en las que se puede matricular un alumno. */
public enum Degree {
	GII, GADE, GDER, GPSI;

	public static Degree parse(String code) {
		for (Degree d : values()) {
			if (d.name().equals(code)) return d;
		}
		throw new IllegalArgumentException("titulación desconocida: usa GII, GADE, GDER o GPSI");
	}
}
