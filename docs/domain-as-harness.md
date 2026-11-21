# Domain as Harness en esta aplicación

Cuando un agente de IA escribe código, todo lo que lo rodea y comprueba su trabajo es su *harness*.
Unas piezas lo orientan, como un `AGENTS.md`, y funcionan si el agente decide hacerles caso. Otras dan
siempre el mismo resultado: el compilador y las pruebas. **Domain as Harness** pone las reglas del
dominio en el segundo grupo: el dominio se modela para que sus reglas se cumplan por construcción,
escriba el código quien lo escriba y diga lo que diga el encargo.

La regla práctica es **la aduana del dominio**: un objeto de dominio o es válido o no existe. Todo dato
que llega de fuera pasa una vez por la frontera, y allí le ocurre una de tres cosas:

- **Se despacha.** Cumple las reglas y se convierte en un tipo que circula por dentro sin más
  controles.
- **Espera en el depósito.** Está incompleto pero tiene sentido para el negocio, y se guarda aparte,
  como borrador: un tipo propio que el código de negocio no acepta.
- **No entra.** Ni una cosa ni la otra.

## Los casos

La aplicación es una matrícula universitaria con dos tipos de alumno, el propio y el de movilidad
internacional, con datos y reglas distintos ([reglas de negocio](reglas-de-negocio.md)). Las rutas
son relativas a `src/main/java/io/github/eldiegos/domainasharness/`.

| Caso | Qué significa | Dónde está |
|---|---|---|
| **Puesto fronterizo** | Cada dato es un tipo que valida al construirse. `parse` es para lo que llega de fuera; el constructor, para el código de dentro. | `dominio/Email`, `Nif`, `Passport`, `FullName`… |
| **Dos aduanas, un objeto** | Cada tipo de alumno es una variante de un tipo sellado: lo que no le aplica no se puede escribir. | `dominio/StudentOrigin`, `HomeStudent`, `MobilityStudent` |
| **Reglas que cruzan campos** | Los créditos dependen del tipo de alumno y del periodo. No hay *setters*: cada cambio crea otro objeto, que pasa otra vez por el constructor. | `dominio/Enrollment` |
| **Una entidad con identidad** | La matrícula se reconoce por su `EnrollmentId`, no por sus datos: un cambio crea otro objeto con el mismo identificador, y los dos son la misma matrícula. El identificador lo da el almacén (`nextId()`) antes de cruzar la frontera, como una secuencia. | `dominio/Enrollment`, `EnrollmentId`, `almacen/EnrollmentStore` |
| **El depósito** | Lo incompleto se guarda como borrador, un tipo propio; solo se da de alta cuando cumple las reglas. | `dominio/EnrollmentDraft`, `almacen/DraftStore` |
| **El depósito, en pequeño** | La solicitud de admisión: un `Draft` que guarda lo que haya y un `Submitted` que no puede estar incompleto. El código que evalúa las solicitudes recibe un `Submitted`. | `dominio/admision/AdmissionApplication` |
| **Todos los motivos a la vez** | La frontera devuelve un resultado con todos los motivos del rechazo, no solo el primero. | `dominio/Result` |
| **Control por destino** | El carné exige una matrícula confirmada, y solo la fábrica que comprueba el pago puede crearla. Es una clase con constructor privado, porque un *record* público no puede ocultar su constructor. | `dominio/ConfirmedEnrollment`, `StudentCard` |
| **DTO en la frontera** | El JSON llega a un DTO de texto que no promete nada y se convierte una vez en un tipo del dominio. El dominio no sabe nada de Spring ni de Jackson. | `api/EnrollmentRequest`, `api/EnrollmentView` |
| **Una sola frontera** | La API, el CSV y los borradores entran por el mismo sitio. La importación es todo o nada. | `aplicacion/EnrollmentService` |
| **Lo que los tipos no pueden expresar** | «Un documento, una matrícula» depende de las demás matrículas: lo garantiza el almacén, como lo haría una restricción `UNIQUE` en una base de datos. | `almacen/EnrollmentStore` |

Para verlos funcionar, arranca la aplicación con `./mvnw spring-boot:run` y ejecuta en orden las
peticiones de [`casos.http`](casos.http).

## El harness alrededor del dominio

1. **Las reglas, en tipos:** el paquete `dominio`.
2. **Lo que los tipos no expresan, en el almacén:** el documento único. Con una base de datos,
   serían sus restricciones (`UNIQUE`, claves ajenas, `CHECK`).
3. **Pruebas de que lo inválido no entra por ningún camino:** altas, importación, cambios y
   borradores, en `EnrollmentServiceTest`, y la frontera HTTP en `EnrollmentApiTest`. Lo que el
   compilador impide, como emitir el carné sin una matrícula confirmada, en `NoCompilaTest`.
4. **Reglas automáticas contra los atajos:** `ArquitecturaTest` rechaza el cambio si una clase del
   dominio gana *setters*, un constructor público vacío, campos mutables o una dependencia de un
   *framework*, o si alguien usa la reflexión para saltarse un constructor. Sus mensajes dicen qué
   usar en su lugar: un agente lee ese error como su siguiente instrucción.
5. **Protección del propio dominio:** la integración continua (`.github/workflows/ci.yml`) ejecuta
   todo con cada cambio, y `.github/CODEOWNERS` pide la revisión de los propietarios para el dominio,
   las pruebas de arquitectura, el `pom.xml`, la integración continua, el `AGENTS.md` y la *skill*.
   Para que sea obligatorio, activa en GitHub la protección de la rama `main`, con las comprobaciones
   obligatorias y sin excepciones para los administradores.
6. **El `AGENTS.md`, al final y corto:** dónde están los tipos, por qué existen y qué no se toca.

## Prueba a romperlo

Añade un `setEmail` a `Enrollment`, un constructor vacío o una anotación de Spring a cualquier clase
del dominio, y ejecuta `./mvnw verify`. `ArquitecturaTest` falla y dice qué hacer en su lugar.

## Lo que la aduana no hace

- **No sustituye a la base de datos.** Lo que depende de otros datos, como el documento único, se
  garantiza en el almacén.
- **No hace robusto el código que la rodea.** Por eso la importación parsea el fichero entero antes de
  guardar.
- **No impide decisiones equivocadas.** Si alguien construye un tipo a mano en vez de usar su `parse`,
  puede perder un dato sin que el objeto sea inválido. Los tipos impiden estados imposibles, no
  decisiones equivocadas.
- **El tipo prueba que el control se pasó, no que siga siendo cierto.** Si se devuelve un pago después
  de confirmar, la confirmación sigue existiendo: eso se protege con transacciones.
