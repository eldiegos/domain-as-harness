package io.github.eldiegos.domainasharness;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import io.github.eldiegos.domainasharness.api.EnrollmentView;
import io.github.eldiegos.domainasharness.dominio.Enrollment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * La parte del harness que protege al propio dominio y los caminos que llegan a él. Se ejecuta en cada
 * {@code ./mvnw verify} y en la integración continua, y rechaza el cambio si una clase del paquete
 * {@code dominio} gana setters, un constructor público vacío, campos mutables o dependencias de un
 * framework; si alguien usa la reflexión, {@code Unsafe} o los envoltorios de Spring ({@code ReflectionUtils},
 * {@code BeanUtils.instantiateClass}, Objenesis) para abrir un constructor o un campo privados;
 * o si la API devuelve como matrícula algo que no salió de una {@link Enrollment} o guarda datos fuera
 * del almacén. También falla si un {@code archunit_ignore_patterns.txt} en el classpath silencia estas
 * reglas. Nadie desmonta la aduana sin que se note.
 *
 * <p>Los mensajes dicen qué usar en su lugar: un agente de IA lee ese error como su siguiente instrucción.
 */
@DisplayName("Arquitectura: nadie desmonta la aduana sin que se note")
class ArquitecturaTest {

	private static final String DOMINIO = "..dominio..";

	/**
	 * Lo que abre constructores y campos privados sin llamar a {@code setAccessible} desde el código propio:
	 * las clases internas del JDK y los envoltorios de Spring, que ya está en el classpath. Es la lista de lo
	 * conocido, no una garantía: otra biblioteca que haga lo mismo solo la vería la revisión.
	 */
	private static final DescribedPredicate<JavaClass> ABREN_LO_PRIVADO = resideInAnyPackage(
			"sun.misc..", "sun.reflect..", "jdk.internal..",
			"org.objenesis..", "org.springframework.objenesis..", "org.springframework.cglib..")
		.or(nameMatching("org\\.springframework\\.(util\\.ReflectionUtils"
			+ "|test\\.util\\.ReflectionTestUtils|beans\\.DirectFieldAccessor)"))
		.as("open private constructors or fields (JDK internals, Objenesis, CGLIB or Spring's"
			+ " ReflectionUtils, ReflectionTestUtils and DirectFieldAccessor)");

	/** Todo lo que se compila de src/main, en cualquier paquete. */
	private static final JavaClasses APLICACION = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
		.importClasspath();

	@Test
	@DisplayName("Ninguna clase del dominio tiene setters")
	void sinSetters() {
		ArchRule rule = noMethods()
			.that().areDeclaredInClassesThat().resideInAPackage(DOMINIO)
			.and().arePublic().and().areNotStatic()
			.should().haveNameMatching("set[A-Z].*")
			.because("un objeto de dominio no cambia después de "
				+ "construirse. Para cambiarlo, añade un método "
				+ "withX(...) que devuelva otro objeto creado con "
				+ "el constructor, que valida");
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
			.because("un campo que se puede reasignar deja "
				+ "que el objeto quede a medias. Usa records o "
				+ "campos final y crea un objeto nuevo para "
				+ "cada cambio");
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
	@DisplayName("Nadie usa la reflexión, Unsafe ni los envoltorios de Spring para abrir un constructor o un campo privados")
	void sinReflexion() {
		ArchRule rule = noClasses()
			.should().callMethodWhere(target(nameMatching("setAccessible|trySetAccessible|privateLookupIn"
				+ "|makeAccessible|accessibleConstructor|instantiateClass|forDirectFieldAccess")))
			.orShould().dependOnClassesThat(ABREN_LO_PRIVADO)
			.because("la reflexión, MethodHandles.privateLookupIn, Unsafe y los envoltorios de Spring"
				+ " (ReflectionUtils, BeanUtils.instantiateClass, DirectFieldAccessor, Objenesis) abren"
				+ " constructores y campos privados y saltan la aduana. Construye los objetos con su"
				+ " constructor, su fábrica o su función parse");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("La API solo devuelve como matrícula lo que sale de una Enrollment")
	void vistaDesdeElDominio() {
		DescribedPredicate<JavaMethod> desdeUnaEnrollment = describe("are EnrollmentView.of(long, Enrollment)",
			m -> m.getOwner().isEquivalentTo(EnrollmentView.class) && m.getName().equals("of")
				&& m.getRawParameterTypes().stream().anyMatch(t -> t.isEquivalentTo(Enrollment.class)));
		ArchRule rule = constructors()
			.that().areDeclaredIn(EnrollmentView.class)
			.should().onlyBeCalled().byMethodsThat(desdeUnaEnrollment)
			.because("lo que la API devuelve como matrícula tiene que haber pasado la aduana. Construye la vista"
				+ " con EnrollmentView.of a partir de una Enrollment; si los datos no la pasan, son un borrador"
				+ " (EnrollmentDraft) y se devuelven como tal");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("Solo el almacén guarda datos: la API y los casos de uso no tienen colecciones")
	void sinAlmacenesParalelos() {
		DescribedPredicate<JavaField> noSonUnValor = describe("are not instance fields of a record or an exception",
			f -> f.getModifiers().contains(JavaModifier.STATIC)
				|| !(f.getOwner().isRecord() || f.getOwner().isAssignableTo(Throwable.class)));
		DescribedPredicate<JavaField> sinColecciones = describe("not hold collections, maps or arrays",
			f -> f.getAllInvolvedRawTypes().stream().noneMatch(ArquitecturaTest::esUnaColeccion));
		ArchRule rule = fields()
			.that().areDeclaredInClassesThat().resideOutsideOfPackages(DOMINIO, "..almacen..", "..pagos..")
			.and(noSonUnValor)
			.should(ArchCondition.from(sinColecciones))
			.because("una colección en la API o en un caso de uso es un almacén paralelo: lo que guarda no pasa por"
				+ " la aduana y la API puede devolverlo. Guarda los datos en el paquete almacen, como tipos del"
				+ " dominio");
		rule.check(APLICACION);
	}

	@Test
	@DisplayName("Ninguna regla se silencia con un fichero de excepciones")
	void sinExcepcionesOcultas() {
		assertNull(ArquitecturaTest.class.getClassLoader().getResource("archunit_ignore_patterns.txt"),
			"archunit_ignore_patterns.txt hace que ArchUnit ignore las infracciones de estas reglas. Corrige el"
				+ " código que las incumple; si una regla está mal, cámbiala en esta clase, que revisan sus"
				+ " propietarios");
	}

	private static boolean esUnaColeccion(JavaClass c) {
		return c.isArray() || c.isAssignableTo(Collection.class) || c.isAssignableTo(Map.class);
	}
}
