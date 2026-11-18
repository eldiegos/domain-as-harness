package io.github.eldiegos.domainasharness.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * Control por destino: una matrícula confirmada solo existe si pasó el control del pago. Las
 * operaciones que exigen una matrícula pagada, como emitir el carné, reciben este tipo y no
 * necesitan preguntar nada.
 *
 * <p>No es un record a propósito. Un record público no puede tener un constructor canónico privado
 * (JLS §8.10.4), así que cualquiera que viera el tipo podría escribir
 * {@code new ConfirmedEnrollment(...)} sin pasar el control. Por eso es una clase con constructor
 * privado y una fábrica: {@link #confirm} es la única puerta.
 *
 * <p>El tipo prueba que el control se pasó, no que siga siendo cierto: si después se devuelve el
 * pago, este objeto sigue existiendo. Eso se protege con transacciones, no con tipos.
 */
public final class ConfirmedEnrollment {
	private final long id;
	private final Enrollment enrollment;

	private ConfirmedEnrollment(long id, Enrollment enrollment) {
		this.id = id;
		this.enrollment = enrollment;
	}

	public static Optional<ConfirmedEnrollment> confirm(long id, Enrollment enrollment, Payments payments) {
		Objects.requireNonNull(enrollment, "enrollment");
		Objects.requireNonNull(payments, "payments");
		return payments.isPaid(id)
			? Optional.of(new ConfirmedEnrollment(id, enrollment))
			: Optional.empty();
	}

	public long id() { return id; }

	public Enrollment enrollment() { return enrollment; }

	@Override public boolean equals(Object o) {
		return o instanceof ConfirmedEnrollment c && c.id == id && c.enrollment.equals(enrollment);
	}

	@Override public int hashCode() { return Objects.hash(id, enrollment); }

	@Override public String toString() { return "ConfirmedEnrollment[id=" + id + ", " + enrollment + "]"; }
}
