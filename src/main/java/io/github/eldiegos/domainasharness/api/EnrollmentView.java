package io.github.eldiegos.domainasharness.api;

import io.github.eldiegos.domainasharness.dominio.Enrollment;
import io.github.eldiegos.domainasharness.dominio.EnrollmentDraft;
import io.github.eldiegos.domainasharness.dominio.HomeStudent;
import io.github.eldiegos.domainasharness.dominio.MobilityStudent;

/** Modelo de lectura de una matrícula. Los campos que no aplican al tipo de alumno van a null. */
public record EnrollmentView(long id, String type, String document, String fullName, String email,
		String degree, int credits, Integer previousCourse, String originCountry,
		String originCity, String homeUniversity, String period) {

	/** De vuelta a la frontera: el tipo del dominio se convierte en el DTO de salida. */
	static EnrollmentView of(long id, Enrollment e) {
		return switch (e.origin()) {
			case HomeStudent h -> new EnrollmentView(id, EnrollmentDraft.HOME, h.nif().value(),
				e.fullName().value(), e.email().value(), e.degree().name(), e.credits(),
				h.previousCourse().value(), null, null, null, null);
			case MobilityStudent m -> new EnrollmentView(id, EnrollmentDraft.MOBILITY, m.passport().value(),
				e.fullName().value(), e.email().value(), e.degree().name(), e.credits(),
				null, m.country().value(), m.city().value(), m.university().value(), m.period().name());
		};
	}
}
