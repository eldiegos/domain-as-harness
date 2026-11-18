package io.github.eldiegos.domainasharness.dominio;

/** Lo que el dominio necesita saber del sistema de pagos. Es externo: ningún constructor puede comprobarlo. */
public interface Payments {
	boolean isPaid(long enrollmentId);
}
