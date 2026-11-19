package io.github.eldiegos.domainasharness.dominio.admision;

import io.github.eldiegos.domainasharness.dominio.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("El depósito, en pequeño: la solicitud de admisión")
class AdmissionApplicationTest {

	@Test
	@DisplayName("Una solicitud completa se envía, con el nombre normalizado")
	void completaSeEnvia() {
		assertEquals(Optional.of(new Submitted("Ana", new Email("ana@example.com"))),
			new Draft("  Ana  ", " ana@example.com ").submit());
	}

	@Test
	@DisplayName("Sin nombre o con un correo inválido, sigue siendo un borrador")
	void incompletaSigueSiendoBorrador() {
		assertEquals(Optional.empty(), new Draft(" ", "ana@example.com").submit());
		assertEquals(Optional.empty(), new Draft(null, "ana@example.com").submit());
		assertEquals(Optional.empty(), new Draft("Ana", "ana@").submit());
		assertEquals(Optional.empty(), new Draft("Ana", null).submit());
	}

	@Test
	@DisplayName("El borrador guarda lo que el aspirante escribió, aunque esté mal o vacío")
	void borradorTalCual() {
		Draft borrador = new Draft(null, "ana@");
		assertNull(borrador.rawName());
		assertEquals("ana@", borrador.rawEmail());
	}

	@Test
	@DisplayName("Una solicitud enviada no puede existir incompleta")
	void enviadaNuncaIncompleta() {
		assertThrows(IllegalArgumentException.class, () -> new Submitted(" ", new Email("ana@example.com")));
		assertThrows(NullPointerException.class, () -> new Submitted("Ana", null));
	}
}
