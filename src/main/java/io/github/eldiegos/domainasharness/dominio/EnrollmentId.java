package io.github.eldiegos.domainasharness.dominio;

/**
 * La identidad de una matrícula: sigue siendo la misma aunque cambien sus datos. La da el almacén
 * ({@code EnrollmentStore.nextId()}) antes de cruzar la frontera, como una secuencia en una base de
 * datos; un alta rechazada puede dejar un hueco en la numeración.
 */
public record EnrollmentId(long value) {
	public EnrollmentId {
		if (value < 1) {
			throw new IllegalArgumentException("el identificador de una matrícula es un número positivo");
		}
	}
}
