package io.github.eldiegos.domainasharness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de matrícula universitaria escrito con la aduana del dominio: un objeto de dominio o es
 * válido o no existe.
 *
 * <ul>
 *   <li>{@code dominio}: la aduana. Tipos que validan al construirse, sin dependencias de Spring.</li>
 *   <li>{@code aplicacion}: los casos de uso. Convierten la entrada en tipos del dominio y guardan.</li>
 *   <li>{@code api}: la frontera HTTP. DTO que se convierten una vez en tipos del dominio.</li>
 *   <li>{@code almacen} y {@code pagos}: infraestructura en memoria.</li>
 * </ul>
 */
@SpringBootApplication
public class DomainAsHarnessApplication {

	public static void main(String[] args) {
		SpringApplication.run(DomainAsHarnessApplication.class, args);
	}
}
