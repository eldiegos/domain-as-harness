package io.github.eldiegos.domainasharness.almacen;

import io.github.eldiegos.domainasharness.dominio.EnrollmentDraft;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** El depósito: guarda los borradores tal como llegan, completos o no. */
@Repository
public class DraftStore {
	private final Map<Long, EnrollmentDraft> drafts = new LinkedHashMap<>();
	private long nextId = 1;

	public synchronized long save(EnrollmentDraft draft) {
		long id = nextId++;
		drafts.put(id, draft);
		return id;
	}

	public synchronized void update(long id, EnrollmentDraft draft) {
		if (!drafts.containsKey(id)) {
			throw new NoSuchElementException("no existe el borrador " + id);
		}
		drafts.put(id, draft);
	}

	public synchronized Optional<EnrollmentDraft> find(long id) {
		return Optional.ofNullable(drafts.get(id));
	}

	public synchronized void remove(long id) {
		drafts.remove(id);
	}
}
