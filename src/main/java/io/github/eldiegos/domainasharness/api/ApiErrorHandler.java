package io.github.eldiegos.domainasharness.api;

import io.github.eldiegos.domainasharness.almacen.DuplicateDocumentException;
import io.github.eldiegos.domainasharness.aplicacion.RejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Traduce los rechazos de la aduana a respuestas HTTP (RFC 9457). Los datos que no pasan la frontera
 * dan 422 con todos los motivos en {@code reasons}.
 */
@RestControllerAdvice
public class ApiErrorHandler {
	private static final HttpStatusCode UNPROCESSABLE = HttpStatusCode.valueOf(422);

	@ExceptionHandler(RejectedException.class)
	ProblemDetail rejected(RejectedException e) {
		return withReasons(e.reasons());
	}

	@ExceptionHandler(DuplicateDocumentException.class)
	ProblemDetail duplicate(DuplicateDocumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	/** El constructor de un tipo del dominio rechazó un cambio. */
	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail invalid(IllegalArgumentException e) {
		return withReasons(List.of(e.getMessage()));
	}

	/** La operación exige un estado que la matrícula no tiene, como el carné sin pago. */
	@ExceptionHandler(IllegalStateException.class)
	ProblemDetail conflict(IllegalStateException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(NoSuchElementException.class)
	ProblemDetail notFound(NoSuchElementException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	private static ProblemDetail withReasons(List<String> reasons) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(UNPROCESSABLE, "Los datos no pasan la aduana del dominio");
		problem.setProperty("reasons", reasons);
		return problem;
	}
}
