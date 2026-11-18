package io.github.eldiegos.domainasharness.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Acumula los motivos de rechazo para devolverlos todos a la vez, no solo el primero. */
final class Reasons {
	private final List<String> reasons = new ArrayList<>();

	/** Construye el valor; si su constructor lo rechaza, anota el motivo y devuelve null. */
	<T> T attempt(Supplier<T> build) {
		try {
			return build.get();
		} catch (IllegalArgumentException e) {
			reasons.add(e.getMessage());
			return null;
		}
	}

	void add(String reason) {
		reasons.add(reason);
	}

	boolean isEmpty() {
		return reasons.isEmpty();
	}

	List<String> list() {
		return List.copyOf(reasons);
	}

	static boolean blank(String s) {
		return s == null || s.isBlank();
	}
}
