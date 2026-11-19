package io.github.eldiegos.domainasharness.dominio;

import java.util.Objects;

/** Primera aduana: el alumno propio. */
public record HomeStudent(
		Nif nif, PreviousCourse previousCourse)
		implements StudentOrigin {

	public HomeStudent {
		Objects.requireNonNull(nif, "nif");
		Objects.requireNonNull(previousCourse, "previousCourse");
	}
}
