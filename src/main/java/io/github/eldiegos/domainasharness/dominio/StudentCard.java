package io.github.eldiegos.domainasharness.dominio;

import java.util.Objects;

/** El carné universitario: solo se emite para una matrícula confirmada. */
public record StudentCard(EnrollmentId enrollmentId, String document, FullName holder, Degree degree) {
	public StudentCard {
		Objects.requireNonNull(enrollmentId, "enrollmentId");
		Objects.requireNonNull(document, "document");
		Objects.requireNonNull(holder, "holder");
		Objects.requireNonNull(degree, "degree");
	}

	/** El destino «emitir el carné» exige su tipo: pasarle una {@link Enrollment} sin pagar no compila. */
	public static StudentCard issue(ConfirmedEnrollment confirmed) {
		Enrollment e = confirmed.enrollment();
		return new StudentCard(confirmed.id(), e.document(), e.fullName(), e.degree());
	}
}
