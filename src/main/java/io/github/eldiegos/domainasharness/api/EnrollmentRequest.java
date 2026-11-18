package io.github.eldiegos.domainasharness.api;

import io.github.eldiegos.domainasharness.dominio.EnrollmentDraft;

/**
 * El DTO de entrada: el formulario de matrícula tal como llega en JSON. Todos los campos son texto y
 * pueden venir vacíos o mal; no es un tipo del dominio y no promete nada.
 *
 * <p>Se convierte una vez en un {@link EnrollmentDraft}, que es el que pasa por la aduana.
 */
public record EnrollmentRequest(String type, String document, String fullName, String email, String degree,
		String credits, String previousCourse, String originCountry, String originCity,
		String homeUniversity, String period) {

	EnrollmentDraft toDraft() {
		return new EnrollmentDraft(type, document, fullName, email, degree, credits, previousCourse,
			originCountry, originCity, homeUniversity, period);
	}

	static EnrollmentRequest of(EnrollmentDraft d) {
		return new EnrollmentRequest(d.type(), d.document(), d.fullName(), d.email(), d.degree(), d.credits(),
			d.previousCourse(), d.originCountry(), d.originCity(), d.homeUniversity(), d.period());
	}
}
