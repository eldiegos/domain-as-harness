package io.github.eldiegos.domainasharness.dominio;

/** El último curso superado por un alumno propio. */
public record PreviousCourse(int value) {
	public PreviousCourse {
		if (value < 1 || value > 3) {
			throw new IllegalArgumentException("el curso previo va de 1 a 3");
		}
	}
}
