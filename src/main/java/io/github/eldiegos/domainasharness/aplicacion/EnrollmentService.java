package io.github.eldiegos.domainasharness.aplicacion;

import io.github.eldiegos.domainasharness.almacen.DraftStore;
import io.github.eldiegos.domainasharness.almacen.EnrollmentStore;
import io.github.eldiegos.domainasharness.dominio.City;
import io.github.eldiegos.domainasharness.dominio.ConfirmedEnrollment;
import io.github.eldiegos.domainasharness.dominio.CountryCode;
import io.github.eldiegos.domainasharness.dominio.Degree;
import io.github.eldiegos.domainasharness.dominio.Email;
import io.github.eldiegos.domainasharness.dominio.Enrollment;
import io.github.eldiegos.domainasharness.dominio.EnrollmentDraft;
import io.github.eldiegos.domainasharness.dominio.Payments;
import io.github.eldiegos.domainasharness.dominio.Period;
import io.github.eldiegos.domainasharness.dominio.PreviousCourse;
import io.github.eldiegos.domainasharness.dominio.Result;
import io.github.eldiegos.domainasharness.dominio.StudentCard;
import io.github.eldiegos.domainasharness.dominio.University;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Los casos de uso de la matrícula. No valida nada por su cuenta: convierte la entrada en tipos del
 * dominio, que validan al construirse, y guarda el resultado. Cada cambio construye la matrícula
 * nueva antes de tocar el almacén, así que un cambio inválido no deja nada a medias.
 */
@Service
public class EnrollmentService {
	private final EnrollmentStore enrollments;
	private final DraftStore drafts;
	private final Payments payments;

	public EnrollmentService(EnrollmentStore enrollments, DraftStore drafts, Payments payments) {
		this.enrollments = enrollments;
		this.drafts = drafts;
		this.payments = payments;
	}

	/** Da de alta una matrícula si pasa la frontera; si no, lanza {@link RejectedException} con todos los motivos. */
	public long enroll(EnrollmentDraft draft) {
		return enrollments.insert(admit(draft));
	}

	/** Parsea el fichero entero antes de guardar: si una línea no pasa la frontera, no se guarda ninguna. */
	public int importCsv(String csv) {
		List<Enrollment> accepted = new ArrayList<>();
		List<String> reasons = new ArrayList<>();
		Set<String> documents = new HashSet<>();
		String[] lines = csv.strip().split("\\R");
		for (int i = 0; i < lines.length; i++) {
			String prefix = "línea " + (i + 1) + ": ";
			Result<Enrollment> result;
			try {
				result = EnrollmentDraft.fromCsvLine(lines[i]).submit();
			} catch (IllegalArgumentException e) {
				reasons.add(prefix + e.getMessage());
				continue;
			}
			switch (result) {
				case Result.Accepted<Enrollment> a -> {
					String document = a.value().document();
					if (enrollments.containsDocument(document) || !documents.add(document)) {
						reasons.add(prefix + "ya hay una matrícula con el documento " + document);
					} else {
						accepted.add(a.value());
					}
				}
				case Result.Rejected<Enrollment> r -> r.reasons().forEach(m -> reasons.add(prefix + m));
			}
		}
		if (!reasons.isEmpty()) {
			throw new RejectedException(reasons);
		}
		return enrollments.insertAll(accepted).size();
	}

	public Enrollment find(long id) {
		return enrollments.find(id).orElseThrow(() -> new NoSuchElementException("no existe la matrícula " + id));
	}

	public Map<Long, Enrollment> findAll() {
		return enrollments.all();
	}

	public Enrollment changeEmail(long id, String email) {
		Email parsed = Email.parse(email).orElseThrow(() -> new RejectedException(List.of("correo no válido")));
		return change(id, e -> e.withEmail(parsed));
	}

	public Enrollment changeDegree(long id, String degree) {
		return change(id, e -> e.withDegree(Degree.parse(degree)));
	}

	public Enrollment changeCredits(long id, int credits) {
		return change(id, e -> e.withCredits(credits));
	}

	/** Solo para alumnos propios. */
	public Enrollment changePreviousCourse(long id, int previousCourse) {
		return change(id, e -> e.withPreviousCourse(new PreviousCourse(previousCourse)));
	}

	/** Solo para alumnos de movilidad. */
	public Enrollment changeOrigin(long id, String originCountry, String originCity, String homeUniversity) {
		return change(id, e -> e.withOrigin(new CountryCode(originCountry), new City(originCity),
			new University(homeUniversity)));
	}

	/** Solo para alumnos de movilidad: periodo y créditos a la vez, porque la regla los cruza. */
	public Enrollment changeStay(long id, String period, int credits) {
		return change(id, e -> e.withStay(Period.parse(period), credits));
	}

	// El depósito: borradores que todavía no son matrículas.

	/** Guarda lo que haya escrito el alumno, aunque esté incompleto. No valida nada. */
	public long saveDraft(EnrollmentDraft draft) {
		return drafts.save(draft);
	}

	public void updateDraft(long draftId, EnrollmentDraft draft) {
		drafts.update(draftId, draft);
	}

	public EnrollmentDraft findDraft(long draftId) {
		return drafts.find(draftId).orElseThrow(() -> new NoSuchElementException("no existe el borrador " + draftId));
	}

	/**
	 * Intenta despachar el borrador. Si pasa la frontera, se da de alta y deja el depósito; si no, se
	 * queda en el depósito y se devuelven los motivos.
	 */
	public synchronized Result<Long> submitDraft(long draftId) {
		return switch (findDraft(draftId).submit()) {
			case Result.Accepted<Enrollment> a -> {
				long id = enrollments.insert(a.value());
				drafts.remove(draftId);
				yield Result.accepted(id);
			}
			case Result.Rejected<Enrollment> r -> Result.rejected(r.reasons());
		};
	}

	// Control por destino.

	/** El carné exige una matrícula confirmada, y solo el pago la confirma. */
	public StudentCard issueStudentCard(long id) {
		return ConfirmedEnrollment.confirm(id, find(id), payments)
			.map(StudentCard::issue)
			.orElseThrow(() -> new IllegalStateException(
				"la matrícula " + id + " no está pagada: el carné exige una matrícula confirmada"));
	}

	private static Enrollment admit(EnrollmentDraft draft) {
		return switch (draft.submit()) {
			case Result.Accepted<Enrollment> a -> a.value();
			case Result.Rejected<Enrollment> r -> throw new RejectedException(r.reasons());
		};
	}

	/** Construye la matrícula nueva antes de tocar el almacén: si falla, la guardada no cambia. */
	private synchronized Enrollment change(long id, UnaryOperator<Enrollment> update) {
		Enrollment updated = update.apply(find(id));
		enrollments.replace(id, updated);
		return updated;
	}
}
