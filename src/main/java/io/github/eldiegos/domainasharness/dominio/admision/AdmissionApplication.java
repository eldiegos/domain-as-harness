package io.github.eldiegos.domainasharness.dominio.admision;

import io.github.eldiegos.domainasharness.dominio.Email;

import java.util.Objects;
import java.util.Optional;

/**
 * El depósito, en pequeño: la solicitud de admisión. El borrador guarda lo que el aspirante haya escrito,
 * días si hace falta; la solicitud enviada no puede existir incompleta. El código que evalúa las
 * solicitudes recibe un {@code Submitted}, y pasarle un {@code Draft} no compila.
 *
 * <p>La matrícula aplica la misma idea a lo grande con {@code EnrollmentDraft}, que además devuelve todos
 * los motivos del rechazo. Aquí {@code submit()} solo dice si la solicitud cumple las reglas.
 */
public sealed interface AdmissionApplication
		permits Draft, Submitted { }

// El borrador acepta lo que el aspirante haya escrito.
record Draft(String rawName, String rawEmail)
		implements AdmissionApplication {

	Optional<Submitted> submit() {
		if (rawName == null || rawName.isBlank()) {
			return Optional.empty();
		}
		return Email.parse(rawEmail)
			.map(e -> new Submitted(rawName, e));
	}
}

// La solicitud enviada no puede existir incompleta.
record Submitted(String name, Email email)
		implements AdmissionApplication {
	Submitted {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("falta el nombre");
		}
		name = name.strip(); // normaliza: es parte de la regla
		Objects.requireNonNull(email, "email");
	}
}
