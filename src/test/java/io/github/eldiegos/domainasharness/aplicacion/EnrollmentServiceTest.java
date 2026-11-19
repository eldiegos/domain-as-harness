package io.github.eldiegos.domainasharness.aplicacion;

import io.github.eldiegos.domainasharness.almacen.DraftStore;
import io.github.eldiegos.domainasharness.almacen.DuplicateDocumentException;
import io.github.eldiegos.domainasharness.almacen.EnrollmentStore;
import io.github.eldiegos.domainasharness.dominio.Enrollment;
import io.github.eldiegos.domainasharness.dominio.EnrollmentDraft;
import io.github.eldiegos.domainasharness.dominio.EnrollmentId;
import io.github.eldiegos.domainasharness.dominio.HomeStudent;
import io.github.eldiegos.domainasharness.dominio.MobilityStudent;
import io.github.eldiegos.domainasharness.dominio.Result;
import io.github.eldiegos.domainasharness.pagos.PaymentsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Lo inválido no entra por ningún camino: altas, importación, cambios y borradores. */
@DisplayName("Casos de uso: lo inválido no entra por ningún camino")
class EnrollmentServiceTest {

	/** Un identificador cualquiera para comprobar la frontera sin pasar por el almacén. */
	private static final EnrollmentId ID = new EnrollmentId(99);

	private PaymentsRegistry payments;
	private EnrollmentService service;
	private long home;
	private long mobility;

	@BeforeEach
	void setUp() {
		payments = new PaymentsRegistry();
		service = new EnrollmentService(new EnrollmentStore(), new DraftStore(), payments);
		home = service.enroll(EnrollmentDraft.home("12345678Z", "Ana Perez", "ana@example.com", "GII", 60, 2));
		mobility = service.enroll(EnrollmentDraft.mobility("X1234567", "Lukas Weber", "lukas@example.de", "GADE", 30,
			"DE", "Berlin", "Freie Universitat", "S1"));
	}

	@Test
	@DisplayName("Dos aduanas, un objeto: cada tipo de alumno acaba en una Enrollment con su variante")
	void dosAduanasUnObjeto() {
		assertInstanceOf(HomeStudent.class, service.find(home).origin());
		assertInstanceOf(MobilityStudent.class, service.find(mobility).origin());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {
		"PROPIO;11111111A;Eva Ruiz;eva@example.com;GII;60;2;;;;",
		"PROPIO;22222222J; ;juan@example.com;GII;60;2;;;;",
		"PROPIO;33333333P;Rosa Diaz;rosa@;GII;60;2;;;;",
		"PROPIO;44444444A;Pepe Gil;pepe@example.com;GMED;60;2;;;;",
		"PROPIO;55555555K;Lola Sanz;lola@example.com;GII;20;2;;;;",
		"PROPIO;99999999R;Ivan Rey;ivan@example.com;GII;60;4;;;;",
		"PROPIO;10000001S;Nora Gil;nora@example.com;GII;60;;;;;",
		"PROPIO;20000002F;Raul Paz;raul@example.com;GII;60;2;FR;Paris;Sorbonne;S1",
		"MOVILIDAD;x12;Anna Rossi;anna@example.it;GADE;30;;IT;Roma;Sapienza;S1",
		"MOVILIDAD;M2000001;Pablo Ruiz;pablo@example.com;GADE;30;;ES;Madrid;Complutense;S1",
		"MOVILIDAD;M2000003;Jean Martin;jean@example.fr;GADE;30;;FR;;Sorbonne;S1",
		"MOVILIDAD;M2000005;Emma Petit;emma@example.fr;GADE;30;;FR;Paris;Sorbonne;VERANO",
		"MOVILIDAD;M2000007;Lea Moreau;lea@example.fr;GADE;40;;FR;Paris;Sorbonne;S1",
		"MOVILIDAD;M2000009;Giulia Bianchi;giulia@example.it;GADE;30;2;IT;Roma;Sapienza;S1",
		"VISITANTE;M2000010;Ole Hansen;ole@example.dk;GADE;30;;DK;Aarhus;Aarhus University;S1"
	})
	@DisplayName("Ninguna línea inválida pasa la frontera")
	void ningunaLineaInvalidaPasa(String line) {
		assertInstanceOf(Result.Rejected.class, EnrollmentDraft.fromCsvLine(line).submit(ID));
	}

	@Test
	@DisplayName("El rechazo trae todos los motivos a la vez")
	void todosLosMotivos() {
		var e = assertThrows(RejectedException.class,
			() -> service.enroll(EnrollmentDraft.home("11111111A", " ", "rosa@", "GMED", 60, 4)));

		assertEquals(List.of(
			"el nombre es obligatorio y tiene como mucho 100 caracteres",
			"correo no válido",
			"titulación desconocida: usa GII, GADE, GDER o GPSI",
			"NIF no válido: 8 cifras y la letra de control",
			"el curso previo va de 1 a 3"), e.reasons());
		assertEquals(2, service.findAll().size());
	}

	@Test
	@DisplayName("Un alumno de movilidad con curso previo no pasa: el dato no se descarta en silencio, es un motivo")
	void movilidadConCursoPrevio() {
		var result = EnrollmentDraft.fromCsvLine(
			"MOVILIDAD;M2000009;Giulia Bianchi;giulia@example.it;GADE;30;2;IT;Roma;Sapienza;S1").submit(ID);

		assertEquals(List.of("un alumno de movilidad no tiene curso previo"), result.reasons());
	}

	@Test
	@DisplayName("Las reglas de los créditos y del origen: 80 ECTS propio, 31 en un semestre y movilidad desde ES")
	void reglasDeLaMatricula() {
		assertEquals(List.of("un alumno propio se matricula de 30 a 78 ECTS"),
			EnrollmentDraft.home("87654321X", "Luis Gomez", "luis@example.com", "GII", 80, 2).submit(ID).reasons());
		assertEquals(List.of("un alumno de movilidad con periodo S1 se matricula de 12 a 30 ECTS"),
			EnrollmentDraft.mobility("FR112233", "Marie Dubois", "marie@example.fr", "GDER", 31,
				"FR", "Lyon", "Universite Lyon 2", "S1").submit(ID).reasons());
		assertEquals(List.of("un alumno de movilidad internacional no puede venir de ES"),
			EnrollmentDraft.mobility("M2000001", "Pablo Ruiz", "pablo@example.com", "GADE", 30,
				"ES", "Madrid", "Complutense", "S1").submit(ID).reasons());
	}

	@Test
	@DisplayName("Los cambios inválidos fallan y la matrícula guardada no cambia")
	void cambiosInvalidos() {
		Map<Long, Enrollment> before = service.findAll();

		assertAll(
			fails(() -> service.changeEmail(home, "maria@")),
			fails(() -> service.changeDegree(home, "GMED")),
			fails(() -> service.changeCredits(home, 90)),
			fails(() -> service.changeCredits(mobility, 40)),
			fails(() -> service.changePreviousCourse(home, 5)),
			fails(() -> service.changePreviousCourse(mobility, 2)),
			fails(() -> service.changeOrigin(mobility, "ES", "Madrid", "Complutense")),
			fails(() -> service.changeOrigin(home, "FR", "Paris", "Sorbonne")),
			fails(() -> service.changeStay(mobility, "S1", 40)));

		assertEquals(datos(before), datos(service.findAll()));
	}

	@Test
	@DisplayName("La importación es todo o nada y dice qué falla en cada línea")
	void importacionTodoONada() {
		var e = assertThrows(RejectedException.class, () -> service.importCsv("""
			PROPIO;87654321X;Luis Gomez;luis@example.com;GDER;72;3;;;;
			PROPIO;mal formado
			PROPIO;12345678Z;Ana Perez;ana2@example.com;GII;60;2;;;;
			"""));

		assertEquals(List.of("línea 2: se esperaban 11 campos y hay 2",
			"línea 3: ya hay una matrícula con el documento 12345678Z"), e.reasons());
		assertEquals(2, service.findAll().size());
	}

	@Test
	@DisplayName("Un documento, una matrícula: el almacén rechaza el duplicado")
	void documentoUnico() {
		assertThrows(DuplicateDocumentException.class,
			() -> service.enroll(EnrollmentDraft.home("12345678Z", "Ana Perez", "ana2@example.com", "GII", 60, 2)));
	}

	@Test
	@DisplayName("Lo incompleto espera en el depósito y se despacha cuando cumple las reglas")
	void deposito() {
		long draft = service.saveDraft(new EnrollmentDraft("MOVILIDAD", null, "Marie Dubois", null, null, null, null,
			null, null, null, null));

		assertInstanceOf(Result.Rejected.class, service.submitDraft(draft));
		assertEquals("Marie Dubois", service.findDraft(draft).fullName());

		service.updateDraft(draft, EnrollmentDraft.mobility("FR998877", "Marie Dubois", "marie@example.fr", "GDER", 60,
			"FR", "Lyon", "Universite Lyon 2", "ANUAL"));

		assertInstanceOf(Result.Accepted.class, service.submitDraft(draft));
		assertEquals(3, service.findAll().size());
	}

	@Test
	@DisplayName("El carné exige una matrícula confirmada: sin pago no hay carné")
	void carneSoloConPago() {
		assertThrows(IllegalStateException.class, () -> service.issueStudentCard(home));

		payments.markPaid(home);

		assertEquals("12345678Z", service.issueStudentCard(home).document());
	}

	@Test
	@DisplayName("Una entidad: cada cambio crea otro objeto, pero es la misma matrícula")
	void mismaMatricula() {
		Enrollment antes = service.find(home);
		Enrollment despues = service.changeCredits(home, 72);

		assertEquals(antes, despues);
		assertEquals(antes.id(), despues.id());
		assertEquals(60, antes.credits());
		assertEquals(72, despues.credits());
	}

	/** Los datos de cada matrícula, no solo su identidad: para ver que nada ha cambiado. */
	private static List<String> datos(Map<Long, Enrollment> enrollments) {
		return enrollments.values().stream().map(Enrollment::toString).toList();
	}

	private static Executable fails(Executable change) {
		return () -> assertThrows(IllegalArgumentException.class, change);
	}
}
