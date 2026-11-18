package io.github.eldiegos.domainasharness.dominio;

/**
 * Dos aduanas para el mismo objeto. Cada tipo de alumno es su propia variante: un alumno propio no
 * tiene país de origen y uno de movilidad no tiene curso previo, porque no hay dónde escribirlos.
 * Un {@code switch} sobre este tipo sin {@code default} deja de compilar si mañana se añade otra
 * variante y alguien olvida tratarla.
 */
public sealed interface StudentOrigin permits HomeStudent, MobilityStudent {
}
