# Domain as Harness

Una aplicación Spring Boot de matrícula universitaria cuyas reglas se cumplen por construcción,
escriba el código quien lo escriba (una persona o un agente de IA) y diga lo que diga el encargo. Y
la *skill* para que tus agentes escriban código nuevo con la misma regla.

> **English.** A runnable Spring Boot example of *Domain as Harness*: a university enrollment service
> whose domain rules hold by construction, whoever writes the code and whatever the prompt says. It
> ships the `domain-as-harness` agent skill so your AI agents can write new code the same way.

**La aduana del dominio:** un objeto de dominio o es válido o no existe. Todo dato que llega de fuera
pasa una vez por la frontera, y allí **se despacha**, **espera en el depósito con su nombre** o **no
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
  aplicacion/EnrollmentServiceTest   lo inválido no entra por ningún camino
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
