package io.github.eldiegos.domainasharness.dominio;

/** Periodo de estancia de un alumno de movilidad. */
public enum Period {
	S1, S2, ANUAL;

	public int maxCredits() {
		return this == ANUAL ? 60 : 30;
	}

	public static Period parse(String code) {
		for (Period p : values()) {
			if (p.name().equals(code)) return p;
		}
		throw new IllegalArgumentException("periodo desconocido: usa S1, S2 o ANUAL");
	}
}
