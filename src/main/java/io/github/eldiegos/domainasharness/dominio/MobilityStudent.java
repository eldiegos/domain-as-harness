package io.github.eldiegos.domainasharness.dominio;

import java.util.Objects;

/** Segunda aduana: el alumno de movilidad internacional. */
public record MobilityStudent(Passport passport, CountryCode country, City city, University university,
		Period period) implements StudentOrigin {
	public MobilityStudent {
		Objects.requireNonNull(passport, "passport");
		Objects.requireNonNull(country, "country");
		Objects.requireNonNull(city, "city");
		Objects.requireNonNull(university, "university");
		Objects.requireNonNull(period, "period");
		if (country.value().equals("ES")) {
			throw new IllegalArgumentException("un alumno de movilidad internacional no puede venir de ES");
		}
	}
}
