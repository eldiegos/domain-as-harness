package io.github.eldiegos.domainasharness.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Puesto fronterizo: un {@code Email} inválido no puede existir.
 *
 * <ul>
 *   <li>{@link #parse(String)} es para lo que llega de fuera, que puede venir mal: devuelve un
 *       {@code Optional} que quien llama tiene que tratar.</li>
 *   <li>El constructor es para el resto del código: un correo inválido ahí es un error de
 *       programación, así que lanza una excepción.</li>
 * </ul>
 */
public record Email(String value) {

	// Simplificado: no es la gramática de la RFC 5322.
	private static final Pattern FORMAT =
		Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	public Email {
		Objects.requireNonNull(value, "value");
		if (!FORMAT.matcher(value).matches()) {
			throw new IllegalArgumentException("correo no válido");
		}
	}

	// La frontera: lo que escribe el usuario puede venir mal.
	// El constructor repite la regla: es la invariante.
	public static Optional<Email> parse(String raw) {
		if (raw == null) return Optional.empty();
		String s = raw.strip();
		return FORMAT.matcher(s).matches()
			? Optional.of(new Email(s))
			: Optional.empty();
	}
}
