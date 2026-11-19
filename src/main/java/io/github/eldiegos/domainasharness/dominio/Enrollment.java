package io.github.eldiegos.domainasharness.dominio;

import java.util.Objects;

/**
 * Una matrícula dada de alta. Siempre cumple las reglas de docs/reglas-de-negocio.md: no hay forma de
 * construir una que no lo haga.
 *
 * <p>Las reglas de cada campo están en su propio tipo; aquí solo queda la que cruza campos: los
 * créditos según el tipo de alumno y el periodo. No hay setters: cada cambio crea otra matrícula,
 * que pasa otra vez por el constructor.
 */
public record Enrollment(StudentOrigin origin,
		FullName fullName, Email email, Degree degree,
		int credits) {

	public Enrollment {
		Objects.requireNonNull(origin, "origin");
		Objects.requireNonNull(fullName, "fullName");
		Objects.requireNonNull(email, "email");
		Objects.requireNonNull(degree, "degree");
		switch (origin) {
			case HomeStudent h -> {
				if (credits < 30 || credits > 78) {
					throw new IllegalArgumentException(
						"un alumno propio se matricula"
						+ " de 30 a 78 ECTS");
				}
			}
			case MobilityStudent m -> {
				int max = m.period().maxCredits(); // 30 o 60
				if (credits < 12 || credits > max) {
					throw new IllegalArgumentException(
						"un alumno de movilidad con periodo "
						+ m.period() + " se matricula de 12 a "
						+ max + " ECTS");
				}
			}
		}
	}

	/** El documento que identifica al alumno: el NIF o el pasaporte. */
	public String document() {
		return switch (origin) {
			case HomeStudent h -> h.nif().value();
			case MobilityStudent m -> m.passport().value();
		};
	}

	public Enrollment withEmail(Email newEmail) {
		return new Enrollment(origin, fullName, newEmail, degree, credits);
	}

	public Enrollment withDegree(Degree newDegree) {
		return new Enrollment(origin, fullName, email, newDegree, credits);
	}

	public Enrollment withCredits(int newCredits) {
		return new Enrollment(origin, fullName, email, degree, newCredits);
	}

	/** Solo para alumnos propios. */
	public Enrollment withPreviousCourse(PreviousCourse course) {
		return switch (origin) {
			case HomeStudent h -> new Enrollment(new HomeStudent(h.nif(), course), fullName, email, degree, credits);
			case MobilityStudent m -> throw new IllegalArgumentException("un alumno de movilidad no tiene curso previo");
		};
	}

	/** Solo para alumnos de movilidad. */
	public Enrollment withOrigin(CountryCode country, City city, University university) {
		return switch (origin) {
			case HomeStudent h -> throw new IllegalArgumentException("un alumno propio no tiene origen");
			case MobilityStudent m -> new Enrollment(
				new MobilityStudent(m.passport(), country, city, university, m.period()),
				fullName, email, degree, credits);
		};
	}

	/** Solo para alumnos de movilidad: el periodo y los créditos cambian a la vez, porque la regla los cruza. */
	public Enrollment withStay(Period period, int newCredits) {
		return switch (origin) {
			case HomeStudent h -> throw new IllegalArgumentException("un alumno propio no tiene periodo de estancia");
			case MobilityStudent m -> new Enrollment(
				new MobilityStudent(m.passport(), m.country(), m.city(), m.university(), period),
				fullName, email, degree, newCredits);
		};
	}
}
