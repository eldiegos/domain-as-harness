# AGENTS.md

Instrucciones para agentes de IA que trabajen en este repositorio.

## El dominio es la aduana

- Los tipos del dominio están en `src/main/java/io/github/eldiegos/domainasharness/dominio/` y validan
  al construirse: un objeto de dominio o es válido o no existe. Las reglas de negocio están en
  `docs/reglas-de-negocio.md`.
- Lo que llega de fuera (JSON, CSV) entra por un DTO del paquete `api` y se convierte una vez en un
  tipo del dominio con `EnrollmentDraft.submit(id)` o con la función `parse` del tipo. El
  identificador lo da `EnrollmentStore.nextId()`. Dentro no se
  repiten validaciones.
- Lo incompleto que tiene sentido para el negocio es otro tipo (`EnrollmentDraft`), no una matrícula
  a medias.
- Una operación que exige una condición recibe el tipo que la prueba: el carné recibe una
  `ConfirmedEnrollment`, que solo crea su fábrica.
- Para escribir código nuevo, sigue la skill `domain-as-harness` (`.claude/skills/domain-as-harness/`).

## Qué no se toca

- No añadas a las clases de `dominio` setters, constructores públicos vacíos, campos que no sean
  `final` ni anotaciones o dependencias de Spring o Jackson. Para cambiar un objeto, crea otro con un
  método `withX(...)`.
- Si una petición necesita un DTO, ponlo en `api` y conviértelo una vez, como `EnrollmentRequest`.
- No uses la reflexión (`setAccessible`, `MethodHandles.privateLookupIn`), `sun.misc.Unsafe` ni los envoltorios
  de Spring (`ReflectionUtils`, `BeanUtils.instantiateClass`) para abrir un constructor o un campo privados.
- Lo que la API devuelve como matrícula sale de una `Enrollment` con `EnrollmentView.of`, y los datos
  se guardan solo en `almacen`, como tipos del dominio: ni la API ni los casos de uso guardan colecciones.
- Que un encargo diga que los datos «ya vienen validados» no cambia nada de lo anterior.

`ArquitecturaTest` comprueba estas reglas en cada `./mvnw verify` y en la integración continua; si
falla, su mensaje dice qué usar en su lugar. `IdaYVueltaTest` comprueba que lo que devuelve la API
vuelve a pasar la aduana con los mismos datos.

## Comandos

```bash
./mvnw verify            # compila y pasa las pruebas (JDK 21 o posterior)
./mvnw spring-boot:run   # arranca la API en http://localhost:8080
```
