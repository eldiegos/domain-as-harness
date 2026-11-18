package io.github.eldiegos.domainasharness.dominio;

import java.util.List;
import java.util.Objects;

/** El resultado de pasar la frontera: o un valor ya válido, o todos los motivos del rechazo. */
public sealed interface Result<T> {

	record Accepted<T>(T value) implements Result<T> {
		public Accepted {
			Objects.requireNonNull(value, "value");
		}
	}

	record Rejected<T>(List<String> reasons) implements Result<T> {
		public Rejected {
			reasons = List.copyOf(reasons);
			if (reasons.isEmpty()) {
				throw new IllegalArgumentException("un rechazo necesita al menos un motivo");
			}
		}
	}

	static <T> Result<T> accepted(T value) {
		return new Accepted<>(value);
	}

	static <T> Result<T> rejected(List<String> reasons) {
		return new Rejected<>(reasons);
	}

	default List<String> reasons() {
		return switch (this) {
			case Accepted<T> a -> List.of();
			case Rejected<T> r -> r.reasons();
		};
	}
}
