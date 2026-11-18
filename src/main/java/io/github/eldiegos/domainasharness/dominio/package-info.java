/**
 * El dominio de la matrícula, escrito con la aduana del dominio: un objeto de dominio o es válido o
 * no existe.
 *
 * <ul>
 *   <li><b>Se despacha:</b> {@link io.github.eldiegos.domainasharness.dominio.EnrollmentDraft#submit()} devuelve una
 *       {@link io.github.eldiegos.domainasharness.dominio.Enrollment} válida, por la aduana del alumno propio o por la del
 *       alumno de movilidad.</li>
 *   <li><b>Espera en el depósito:</b> un {@link io.github.eldiegos.domainasharness.dominio.EnrollmentDraft} guarda lo que el
 *       alumno haya escrito, aunque esté incompleto.</li>
 *   <li><b>No entra:</b> {@code submit()} devuelve todos los motivos del rechazo.</li>
 *   <li><b>Control por destino:</b> el carné exige una {@link io.github.eldiegos.domainasharness.dominio.ConfirmedEnrollment},
 *       que solo existe si la matrícula está pagada.</li>
 * </ul>
 *
 * <p>Las reglas de negocio están en {@code docs/reglas-de-negocio.md}. Este paquete no depende de Spring
 * ni de ningún framework, y {@code ArquitecturaTest} rechaza cualquier cambio que dé a sus clases
 * setters, un constructor público vacío o campos mutables.
 */
package io.github.eldiegos.domainasharness.dominio;
