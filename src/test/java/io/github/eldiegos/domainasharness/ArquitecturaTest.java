package io.github.eldiegos.domainasharness;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * La parte del harness que protege al propio dominio. Se ejecuta en cada {@code ./mvnw verify} y en la
 * integración continua, y rechaza el cambio si una clase del paquete {@code dominio} gana setters, un
 * constructor público vacío, campos mutables o dependencias de un framework, o si alguien usa la
 * reflexión para saltarse un constructor. Nadie desmonta la aduana sin que se note.
 *
 * <p>Los mensajes dicen qué usar en su lugar: un agente de IA lee ese error como su siguiente instrucción.
 */
@DisplayName("Arquitectura: nadie desmonta la aduana sin que se note")
class ArquitecturaTest {

	private static final String DOMINIO = "..dominio..";

	private static final JavaClasses APLICACION = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("io.github.eldiegos.domainasharness");

	@Test
	@DisplayName("Ninguna clase del dominio tiene setters")
	void sinSetters() {
		ArchRule rule = noMethods()
			.that().areDeclaredInClassesThat().resideInAPackage(DOMINIO)
			.and().arePublic().and().areNotStatic()
			.should().haveNameMatching("set[A-Z].*")
			.because("un objeto de dominio no cambia después de construirse. Para cambiarlo, añade un método "
				+ "withX(...) que devuelva otro objeto creado con el constructor, que valida");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("Ninguna clase del dominio tiene un constructor público vacío")
	void sinConstructorVacio() {
		ArchRule rule = noConstructors()
			.that().areDeclaredInClassesThat().resideInAPackage(DOMINIO)
			.and().haveRawParameterTypes(new Class<?>[0])
			.should().bePublic()
			.because("un objeto de dominio no puede existir sin sus datos. Créalo con su constructor completo o "
				+ "con su función parse. Si un formulario o un framework necesita un DTO, ponlo en el paquete api "
				+ "y conviértelo una vez, como EnrollmentRequest.toDraft()");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("Todos los campos de instancia del dominio son final")
	void camposFinales() {
		ArchRule rule = fields()
			.that().areDeclaredInClassesThat().resideInAPackage(DOMINIO)
			.and().areNotStatic()
			.should().beFinal()
			.because("un campo que se puede reasignar deja que el objeto quede a medias. Usa records o campos "
				+ "final y crea un objeto nuevo para cada cambio");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("El dominio no depende de frameworks ni de las demás capas")
	void dominioAislado() {
		ArchRule rule = noClasses()
			.that().resideInAPackage(DOMINIO)
			.should().dependOnClassesThat()
			.resideInAnyPackage("org.springframework..", "tools.jackson..", "com.fasterxml..", "jakarta..",
				"..api..", "..aplicacion..", "..almacen..", "..pagos..")
			.because("el dominio no conoce la frontera ni la infraestructura: son ellas las que lo conocen a él");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("Nadie usa la reflexión para saltarse un constructor")
	void sinReflexion() {
		ArchRule rule = noClasses()
			.that().resideInAPackage("io.github.eldiegos.domainasharness..")
			.should().callMethodWhere(target(name("setAccessible")))
			.because("la reflexión salta la aduana. Construye los objetos con su constructor o su función parse");
		rule.check(APLICACION);
	}
}
