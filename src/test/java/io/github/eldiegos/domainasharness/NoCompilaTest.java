package io.github.eldiegos.domainasharness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que se dice que no compila, no compila. Cada prueba compila un trozo de código contra las clases del
 * dominio ({@code target/classes}) y comprueba el error de {@code javac} por su código, que no depende del
 * idioma. Necesita un JDK, no basta un JRE.
 */
@DisplayName("Lo que no compila: el compilador hace cumplir las reglas")
class NoCompilaTest {

	private static final String PAQUETE = "io/github/eldiegos/domainasharness/";
	private static final Path DOMINIO = Path.of("src/main/java", PAQUETE, "dominio");

	@TempDir
	Path tmp;

	@Test
	@DisplayName("Control: el camino con control compila (StudentCard.issue con una matrícula confirmada)")
	void control() throws IOException {
		assertEquals(List.of(), errores(Map.of("prueba/ConControl.java", """
			package prueba;
			import io.github.eldiegos.domainasharness.dominio.*;
			class ConControl {
				StudentCard carne(ConfirmedEnrollment confirmada) { return StudentCard.issue(confirmada); }
			}
			""")));
	}

	@Test
	@DisplayName("El carné exige una matrícula confirmada: StudentCard.issue(enrollment) no compila")
	void carneSinControl() throws IOException {
		assertEquals(List.of("compiler.err.prob.found.req"), errores(Map.of("prueba/Atajo.java", """
			package prueba;
			import io.github.eldiegos.domainasharness.dominio.*;
			class Atajo {
				StudentCard carne(Enrollment enrollment) { return StudentCard.issue(enrollment); }
			}
			""")));
	}

	@Test
	@DisplayName("Solo la fábrica crea una matrícula confirmada: su constructor es privado")
	void confirmadaSoloPorFabrica() throws IOException {
		assertEquals(List.of("compiler.err.report.access"), errores(Map.of("prueba/Falsa.java", """
			package prueba;
			import io.github.eldiegos.domainasharness.dominio.*;
			class Falsa {
				ConfirmedEnrollment confirmar(Enrollment e) { return new ConfirmedEnrollment(1, e); }
			}
			""")));
	}

	@Test
	@DisplayName("Por eso no es un record: un record público no puede ocultar su constructor canónico")
	void recordNoOcultaConstructor() throws IOException {
		assertEquals(List.of("compiler.err.invalid.canonical.constructor.in.record"),
			errores(Map.of("prueba/Confirmada.java", """
				package prueba;
				public record Confirmada(long id) {
					private Confirmada { }
				}
				""")));
	}

	@Test
	@DisplayName("El código que evalúa las solicitudes recibe un Submitted: pasarle un Draft no compila")
	void borradorNoLlegaALaEvaluacion() throws IOException {
		assertEquals(List.of("compiler.err.prob.found.req"),
			errores(Map.of(PAQUETE + "dominio/admision/Evaluacion.java", """
				package io.github.eldiegos.domainasharness.dominio.admision;
				class Evaluacion {
					void evaluar(Submitted solicitud) { }
					void atajo(Draft borrador) { evaluar(borrador); }
				}
				""")));
	}

	@Test
	@DisplayName("Un switch sin default: si mañana hay una tercera variante, Enrollment no compila hasta tratarla")
	void terceraVariante() throws IOException {
		String origen = Files.readString(DOMINIO.resolve("StudentOrigin.java"))
			.replace("permits HomeStudent, MobilityStudent", "permits HomeStudent, MobilityStudent, VisitingStudent");
		List<String> errores = errores(Map.of(
			PAQUETE + "dominio/StudentOrigin.java", origen,
			PAQUETE + "dominio/VisitingStudent.java", """
				package io.github.eldiegos.domainasharness.dominio;
				public record VisitingStudent() implements StudentOrigin { }
				""",
			PAQUETE + "dominio/Enrollment.java", Files.readString(DOMINIO.resolve("Enrollment.java"))));

		assertTrue(errores.contains("compiler.err.not.exhaustive.statement"), errores::toString);
		assertTrue(errores.stream().allMatch(e -> e.startsWith("compiler.err.not.exhaustive")), errores::toString);
	}

	/** Compila los ficheros contra {@code target/classes} y devuelve los códigos de los errores, en orden. */
	private List<String> errores(Map<String, String> ficheros) throws IOException {
		JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
		assertNotNull(javac, "hace falta un JDK para compilar");
		List<Path> fuentes = new ArrayList<>();
		for (var f : ficheros.entrySet()) {
			Path p = tmp.resolve("src").resolve(f.getKey());
			Files.createDirectories(p.getParent());
			Files.writeString(p, f.getValue(), StandardCharsets.UTF_8);
			fuentes.add(p);
		}
		Path salida = Files.createDirectories(tmp.resolve("out"));
		DiagnosticCollector<JavaFileObject> diagnosticos = new DiagnosticCollector<>();
		try (StandardJavaFileManager fm = javac.getStandardFileManager(diagnosticos, null, StandardCharsets.UTF_8)) {
			javac.getTask(null, fm, diagnosticos,
				List.of("--release", "21", "-proc:none", "-cp", Path.of("target/classes").toString(),
					"-d", salida.toString()),
				null, fm.getJavaFileObjectsFromPaths(fuentes)).call();
		}
		return diagnosticos.getDiagnostics().stream()
			.filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
			.map(Diagnostic::getCode)
			.toList();
	}
}
