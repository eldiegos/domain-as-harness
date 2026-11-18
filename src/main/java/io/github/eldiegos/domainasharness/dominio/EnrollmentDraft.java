package io.github.eldiegos.domainasharness.dominio;

/**
 * El depósito: una matrícula tal como la escribió el alumno, completa o no. Se puede guardar durante
 * días; lo que no puede es llegar al código que trabaja con matrículas, porque ese código recibe una
 * {@link Enrollment}.
 *
 * <p>{@link #submit()} es el puesto fronterizo de las dos aduanas: según el tipo de alumno, pasa por
 * la del alumno propio o por la del alumno de movilidad, y devuelve la matrícula o todos los motivos
 * del rechazo. Todas las entradas (API, CSV, formulario) llegan aquí: el dato se comprueba una vez.
 */
public record EnrollmentDraft(String type, String document, String fullName, String email, String degree,
		String credits, String previousCourse, String originCountry, String originCity,
		String homeUniversity, String period) {

	public static final String HOME = "PROPIO";
	public static final String MOBILITY = "MOVILIDAD";

	public static EnrollmentDraft home(String nif, String fullName, String email, String degree,
			int credits, int previousCourse) {
		return new EnrollmentDraft(HOME, nif, fullName, email, degree, String.valueOf(credits),
			String.valueOf(previousCourse), null, null, null, null);
	}

	public static EnrollmentDraft mobility(String passport, String fullName, String email, String degree,
			int credits, String originCountry, String originCity,
					String homeUniversity, String period) {
		return new EnrollmentDraft(MOBILITY, passport, fullName, email, degree, String.valueOf(credits),
			null, originCountry, originCity, homeUniversity, period);
	}

	/**
	 * Una línea del CSV de docs/reglas-de-negocio.md:
	 * {@code tipo;documento;nombre;email;titulacion;creditos;cursoPrevio;paisOrigen;ciudadOrigen;universidadOrigen;periodo}.
	 */
	public static EnrollmentDraft fromCsvLine(String line) {
		String[] f = line.split(";", -1);
		if (f.length != 11) {
			throw new IllegalArgumentException("se esperaban 11 campos y hay " + f.length);
		}
		return new EnrollmentDraft(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10]);
	}

	public Result<Enrollment> submit() {
		Reasons r = new Reasons();
		FullName name = r.attempt(() -> new FullName(fullName));
		Email mail = r.attempt(() -> Email.parse(email)
			.orElseThrow(() -> new IllegalArgumentException("correo no válido")));
		Degree deg = r.attempt(() -> Degree.parse(degree));
		Integer ects = r.attempt(() -> number(credits, "faltan los créditos", "los créditos"));
		StudentOrigin origin = switch (Reasons.blank(type) ? "" : type.strip()) {
			case HOME -> homeStudent(r);
			case MOBILITY -> mobilityStudent(r);
			default -> {
				r.add("tipo de alumno desconocido: usa " + HOME + " o " + MOBILITY);
				yield null;
			}
		};
		if (!r.isEmpty()) {
			return Result.rejected(r.list());
		}
		Enrollment enrollment = r.attempt(() -> new Enrollment(origin, name, mail, deg, ects));
		return enrollment != null ? Result.accepted(enrollment) : Result.rejected(r.list());
	}

	/** Primera aduana. Lo que no aplica a un alumno propio debe venir vacío. */
	private HomeStudent homeStudent(Reasons r) {
		if (!Reasons.blank(originCountry) || !Reasons.blank(originCity)
				|| !Reasons.blank(homeUniversity) || !Reasons.blank(period)) {
			r.add("un alumno propio no tiene país, ciudad ni universidad de origen ni periodo de estancia");
		}
		Nif nif = r.attempt(() -> new Nif(document));
		PreviousCourse course = r.attempt(() -> new PreviousCourse(
			number(previousCourse, "falta el curso previo, obligatorio para un alumno propio", "el curso previo")));
		return nif != null && course != null ? new HomeStudent(nif, course) : null;
	}

	/** Segunda aduana. Lo que no aplica a un alumno de movilidad debe venir vacío. */
	private MobilityStudent mobilityStudent(Reasons r) {
		if (!Reasons.blank(previousCourse)) {
			r.add("un alumno de movilidad no tiene curso previo");
		}
		Passport passport = r.attempt(() -> new Passport(document));
		CountryCode country = r.attempt(() -> new CountryCode(originCountry));
		City city = r.attempt(() -> new City(originCity));
		University university = r.attempt(() -> new University(homeUniversity));
		Period stay = r.attempt(() -> Period.parse(period));
		if (passport == null || country == null || city == null || university == null || stay == null) {
			return null;
		}
		return r.attempt(() -> new MobilityStudent(passport, country, city, university, stay));
	}

	private static int number(String raw, String missing, String field) {
		if (Reasons.blank(raw)) {
			throw new IllegalArgumentException(missing);
		}
		try {
			return Integer.parseInt(raw.strip());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(field + ": «" + raw + "» no es un número entero");
		}
	}
}
