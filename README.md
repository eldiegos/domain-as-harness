# Domain as Harness

Una aplicación Spring Boot de matrícula universitaria cuyas reglas se cumplen por construcción,
escriba el código quien lo escriba (una persona o un agente de IA) y diga lo que diga el encargo. Y
la *skill* para que tus agentes escriban código nuevo con la misma regla.

> **English.** A runnable Spring Boot example of *Domain as Harness*: a university enrollment service
> whose domain rules hold by construction, whoever writes the code and whatever the prompt says. It
> ships the `domain-as-harness` agent skill so your AI agents can write new code the same way.

**La aduana del dominio:** un objeto de dominio o es válido o no existe. Todo dato que llega de fuera
pasa una vez por la frontera, y allí **se despacha**, **espera en el depósito como borrador** o **no
entra**.

## Requisitos

- JDK 21 o posterior. Maven no hace falta: lo descarga el *wrapper* (`./mvnw`).

## Arrancar

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api`. También puedes empaquetarla y ejecutar el JAR:

```bash
./mvnw package
java -jar target/domain-as-harness-0.0.1-SNAPSHOT.jar
```

## Probar

Un alta con datos inválidos devuelve **422** con todos los motivos a la vez:

```bash
curl -X POST http://localhost:8080/api/enrollments -H "Content-Type: application/json" \
  -d '{"type":"PROPIO","document":"11111111A","fullName":" ","email":"rosa@","degree":"GMED","credits":60,"previousCourse":4}'
```

```json
{
  "detail": "Los datos no pasan la aduana del dominio",
  "instance": "/api/enrollments",
  "status": 422,
  "title": "Unprocessable Content",
  "reasons": [
    "el nombre es obligatorio y tiene como mucho 100 caracteres",
    "correo no válido",
    "titulación desconocida: usa GII, GADE, GDER o GPSI",
    "NIF no válido: 8 cifras y la letra de control",
    "el curso previo va de 1 a 3"
  ]
}
```

[`docs/casos.http`](docs/casos.http) recorre todos los casos: alumnos propios y de movilidad,
borradores, importación CSV, cambios, carné y documento repetido.

Las pruebas, incluidas las de arquitectura que protegen el dominio:

```bash
./mvnw verify
```

## Los ejemplos del artículo

Los ejemplos de código del artículo que presenta *Domain as Harness* están aquí tal cual (los
extractos solo omiten líneas), y cada afirmación tiene su prueba:

| Ejemplo | Dónde está | Qué comprueba la prueba |
|---|---|---|
| El puesto fronterizo: `Email` | `dominio/Email` | `EmailTest`: `parse` normaliza o devuelve vacío; el constructor lanza; la reflexión no cambia el campo; Java (con un *record* serializable) y Jackson deserializan con el constructor |
| El depósito: la solicitud de admisión (`Draft`, `Submitted`) | `dominio/admision/AdmissionApplication` | `AdmissionApplicationTest`: el borrador guarda lo que haya; la solicitud enviada no puede estar incompleta. `NoCompilaTest`: pasar un `Draft` a quien espera un `Submitted` no compila |
| Dos aduanas: `StudentOrigin`, `HomeStudent`, `MobilityStudent` | `dominio/` | `EnrollmentServiceTest`: cada alumno entra como su variante; un alumno de movilidad no puede venir de ES; uno con curso previo se rechaza con ese motivo |
| La regla de los créditos: `Enrollment` | `dominio/Enrollment` | `EnrollmentServiceTest`: 80 ECTS a un alumno propio o 31 en un semestre se rechazan. `NoCompilaTest`: con una tercera variante, el `switch` sin `default` no compila |
| La matrícula es una entidad: `EnrollmentId` | `dominio/EnrollmentId`, `Enrollment` | `EnrollmentServiceTest`: un cambio crea otro objeto con el mismo identificador, y es la misma matrícula |
| La frontera: `EnrollmentDraft.submit(id)` | `dominio/EnrollmentDraft` | `EnrollmentServiceTest`: el campo `type` elige la aduana y el rechazo trae todos los motivos |
| Control por destino: el carné exige `ConfirmedEnrollment` | `dominio/ConfirmedEnrollment`, `StudentCard` | `EnrollmentServiceTest` y `EnrollmentApiTest`: sin pago no hay carné. `NoCompilaTest`: `StudentCard.issue(enrollment)` no compila, el constructor privado tampoco, y un *record* público no puede ocultar el suyo |
| Lo que los tipos no expresan: el documento único | `almacen/EnrollmentStore` | `EnrollmentServiceTest` y `EnrollmentApiTest`: un documento, una matrícula |

`NoCompilaTest` compila trozos de código contra el dominio y comprueba el error de `javac`: necesita
un JDK, no basta un JRE.

## Documentación

- [Domain as Harness en esta aplicación](docs/domain-as-harness.md): cada caso, dónde está en el
  código y el *harness* que lo protege.
- [Reglas de negocio y API REST](docs/reglas-de-negocio.md).
- [Generar más código con la skill](docs/skill.md).

## Estructura

```text
src/main/java/io/github/eldiegos/domainasharness/
  dominio/       la aduana: tipos que validan al construirse, sin Spring
  aplicacion/    casos de uso: convierten la entrada en tipos del dominio y guardan
  api/           la frontera HTTP: controladores y DTO
  almacen/       almacenes en memoria (matrículas con documento único y borradores)
  pagos/         un sistema de pagos en memoria
src/test/java/io/github/eldiegos/domainasharness/
  ArquitecturaTest.java              nadie desmonta la aduana sin que se note
  IdaYVueltaTest.java                lo que la API devuelve vuelve a pasar la aduana
  NoCompilaTest.java                 lo que no compila: el compilador hace cumplir las reglas
  dominio/EmailTest                  el puesto fronterizo
  dominio/admision/...Test           el depósito, en pequeño: la solicitud de admisión
  aplicacion/EnrollmentServiceTest   lo inválido no entra por los caminos conocidos
  api/EnrollmentApiTest              la frontera HTTP
.claude/skills/domain-as-harness/    la skill para agentes de IA
docs/                                documentación y peticiones de ejemplo
AGENTS.md                            instrucciones para agentes de IA
```

## Licencia y atribución

Distribuido con la [Licencia Apache 2.0](LICENSE). Puedes usar, modificar y redistribuir
`domain-as-harness`, también en productos comerciales, siempre que conserves el aviso de copyright y
el fichero [`NOTICE`](NOTICE), que atribuye el proyecto original. Ambos van también dentro del JAR, en
`META-INF/`. Si lo citas en un trabajo, usa los datos de [`CITATION.cff`](CITATION.cff) (GitHub los
ofrece en «Cite this repository»).
