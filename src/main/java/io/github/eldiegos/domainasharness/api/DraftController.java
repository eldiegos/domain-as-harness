package io.github.eldiegos.domainasharness.api;

import io.github.eldiegos.domainasharness.aplicacion.EnrollmentService;
import io.github.eldiegos.domainasharness.aplicacion.RejectedException;
import io.github.eldiegos.domainasharness.dominio.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * El depósito por HTTP: el formulario se guarda como borrador, completo o no, y se envía cuando el
 * alumno quiera. Solo se da de alta si pasa la aduana.
 */
@RestController
@RequestMapping("/api/drafts")
public class DraftController {
	private final EnrollmentService service;

	public DraftController(EnrollmentService service) {
		this.service = service;
	}

	public record DraftView(long id, EnrollmentRequest draft) { }

	@PostMapping
	public ResponseEntity<DraftView> save(@RequestBody EnrollmentRequest request) {
		long id = service.saveDraft(request.toDraft());
		return ResponseEntity.created(URI.create("/api/drafts/" + id)).body(view(id));
	}

	@PutMapping("/{id}")
	public DraftView update(@PathVariable long id, @RequestBody EnrollmentRequest request) {
		service.updateDraft(id, request.toDraft());
		return view(id);
	}

	@GetMapping("/{id}")
	public DraftView find(@PathVariable long id) {
		return view(id);
	}

	/** Si el borrador pasa la frontera, devuelve la matrícula creada; si no, 422 con todos los motivos. */
	@PostMapping("/{id}/submission")
	public ResponseEntity<EnrollmentView> submit(@PathVariable long id) {
		return switch (service.submitDraft(id)) {
			case Result.Accepted<Long> a -> ResponseEntity.created(URI.create("/api/enrollments/" + a.value()))
				.body(EnrollmentView.of(a.value(), service.find(a.value())));
			case Result.Rejected<Long> r -> throw new RejectedException(r.reasons());
		};
	}

	private DraftView view(long id) {
		return new DraftView(id, EnrollmentRequest.of(service.findDraft(id)));
	}
}
