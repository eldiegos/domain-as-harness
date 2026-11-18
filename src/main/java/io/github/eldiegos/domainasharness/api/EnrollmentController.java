package io.github.eldiegos.domainasharness.api;

import io.github.eldiegos.domainasharness.aplicacion.EnrollmentService;
import io.github.eldiegos.domainasharness.dominio.StudentCard;
import io.github.eldiegos.domainasharness.pagos.PaymentsRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * La frontera HTTP de las matrículas. No valida nada: convierte el JSON en tipos del dominio a
 * través del servicio, y los tipos del dominio en DTO de salida.
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
	private final EnrollmentService service;
	private final PaymentsRegistry payments;

	public EnrollmentController(EnrollmentService service, PaymentsRegistry payments) {
		this.service = service;
		this.payments = payments;
	}

	public record EmailChange(String email) { }

	public record DegreeChange(String degree) { }

	public record CreditsChange(int credits) { }

	public record PreviousCourseChange(int previousCourse) { }

	public record OriginChange(String originCountry, String originCity, String homeUniversity) { }

	public record StayChange(String period, int credits) { }

	public record ImportResult(int enrolled) { }

	public record StudentCardView(long enrollmentId, String document, String holder, String degree) {
		static StudentCardView of(StudentCard c) {
			return new StudentCardView(c.enrollmentId(), c.document(), c.holder().value(), c.degree().name());
		}
	}

	@PostMapping
	public ResponseEntity<EnrollmentView> enroll(@RequestBody EnrollmentRequest request) {
		long id = service.enroll(request.toDraft());
		return ResponseEntity.created(URI.create("/api/enrollments/" + id)).body(view(id));
	}

	@GetMapping
	public List<EnrollmentView> findAll() {
		return service.findAll().entrySet().stream().map(e -> EnrollmentView.of(e.getKey(), e.getValue())).toList();
	}

	@GetMapping("/{id}")
	public EnrollmentView find(@PathVariable long id) {
		return view(id);
	}

	/** Importación CSV con el formato de docs/reglas-de-negocio.md. Todo o nada. */
	@PostMapping(path = "/import", consumes = {"text/csv", MediaType.TEXT_PLAIN_VALUE})
	public ImportResult importCsv(@RequestBody String csv) {
		return new ImportResult(service.importCsv(csv));
	}

	@PutMapping("/{id}/email")
	public EnrollmentView changeEmail(@PathVariable long id, @RequestBody EmailChange change) {
		return EnrollmentView.of(id, service.changeEmail(id, change.email()));
	}

	@PutMapping("/{id}/degree")
	public EnrollmentView changeDegree(@PathVariable long id, @RequestBody DegreeChange change) {
		return EnrollmentView.of(id, service.changeDegree(id, change.degree()));
	}

	@PutMapping("/{id}/credits")
	public EnrollmentView changeCredits(@PathVariable long id, @RequestBody CreditsChange change) {
		return EnrollmentView.of(id, service.changeCredits(id, change.credits()));
	}

	@PutMapping("/{id}/previous-course")
	public EnrollmentView changePreviousCourse(@PathVariable long id, @RequestBody PreviousCourseChange change) {
		return EnrollmentView.of(id, service.changePreviousCourse(id, change.previousCourse()));
	}

	@PutMapping("/{id}/origin")
	public EnrollmentView changeOrigin(@PathVariable long id, @RequestBody OriginChange change) {
		return EnrollmentView.of(id, service.changeOrigin(id, change.originCountry(), change.originCity(),
			change.homeUniversity()));
	}

	@PutMapping("/{id}/stay")
	public EnrollmentView changeStay(@PathVariable long id, @RequestBody StayChange change) {
		return EnrollmentView.of(id, service.changeStay(id, change.period(), change.credits()));
	}

	/** Simula el pago de la matrícula. En producción lo notificaría la pasarela de pago. */
	@PostMapping("/{id}/payment")
	public ResponseEntity<Void> pay(@PathVariable long id) {
		service.find(id);
		payments.markPaid(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/student-card")
	public ResponseEntity<StudentCardView> issueStudentCard(@PathVariable long id) {
		return ResponseEntity.status(201).body(StudentCardView.of(service.issueStudentCard(id)));
	}

	private EnrollmentView view(long id) {
		return EnrollmentView.of(id, service.find(id));
	}
}
