package io.github.eldiegos.domainasharness.almacen;

import io.github.eldiegos.domainasharness.dominio.Enrollment;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Almacén en memoria de las matrículas dadas de alta.
 *
 * <p>Lo que los tipos no pueden expresar va aquí: «no hay dos matrículas con el mismo documento»
 * depende de las demás matrículas, no cabe en un constructor. Comprobarlo antes de guardar no basta,
 * porque dos altas simultáneas pueden pasar la comprobación a la vez. Por eso la comprobación y la
 * inserción son una sola operación ({@code synchronized}), como haría una restricción {@code UNIQUE}
 * en una base de datos.
 */
@Repository
public class EnrollmentStore {
	private final Map<Long, Enrollment> byId = new LinkedHashMap<>();
	private final Map<String, Long> idByDocument = new HashMap<>();
	private long nextId = 1;

	public synchronized long insert(Enrollment e) {
		return insertAll(List.of(e)).get(0);
	}

	/** Todo o nada: si algún documento ya existe, o se repite en el lote, no se guarda ninguno. */
	public synchronized List<Long> insertAll(List<Enrollment> enrollments) {
		Set<String> seen = new HashSet<>();
		for (Enrollment e : enrollments) {
			if (idByDocument.containsKey(e.document()) || !seen.add(e.document())) {
				throw new DuplicateDocumentException(e.document());
			}
		}
		List<Long> ids = new ArrayList<>();
		for (Enrollment e : enrollments) {
			long id = nextId++;
			byId.put(id, e);
			idByDocument.put(e.document(), id);
			ids.add(id);
		}
		return ids;
	}

	public synchronized boolean containsDocument(String document) {
		return idByDocument.containsKey(document);
	}

	public synchronized Optional<Enrollment> find(long id) {
		return Optional.ofNullable(byId.get(id));
	}

	/** Sustituye una matrícula por su versión nueva, que ya es válida. El documento no cambia. */
	public synchronized void replace(long id, Enrollment updated) {
		Enrollment current = byId.get(id);
		if (current == null) {
			throw new NoSuchElementException("matrícula " + id);
		}
		if (!current.document().equals(updated.document())) {
			throw new IllegalArgumentException("el documento de una matrícula no cambia");
		}
		byId.put(id, updated);
	}

	public synchronized Map<Long, Enrollment> all() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(byId));
	}
}
