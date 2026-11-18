package io.github.eldiegos.domainasharness.aplicacion;

import java.util.List;

/** Datos que no pasan la frontera, con todos los motivos. */
public class RejectedException extends IllegalArgumentException {
	private final List<String> reasons;

	public RejectedException(List<String> reasons) {
		super(String.join("; ", reasons));
		this.reasons = List.copyOf(reasons);
	}

	public List<String> reasons() {
		return reasons;
	}
}
