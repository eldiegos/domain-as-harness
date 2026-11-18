# Matrícula del curso 2026-2027

Las reglas de negocio de la aplicación. Hay dos tipos de estudiante y los dos se matriculan por la
misma API. Todas las reglas son obligatorias: una matrícula que no las cumpla no puede quedar dada de
alta.

## Datos comunes

| Dato | Regla |
|---|---|
| Nombre completo | Obligatorio, como mucho 100 caracteres |
| Correo | Formato de correo (`algo@dominio.ext`) |
| Titulación | Uno de estos códigos: `GII`, `GADE`, `GDER`, `GPSI` |
| Documento | Único: no puede haber dos matrículas con el mismo documento |

## Alumno propio (`PROPIO`)

| Dato | Regla |
|---|---|
| Documento | NIF: 8 cifras y una letra de control. La letra es `TRWAGMYFPDXBNJZSQVHLCKE` en la posición del resto de dividir el número entre 23 |
| Créditos | Entre 30 y 78 ECTS |
| Curso previo | Obligatorio, entre 1 y 3: el último curso superado |
| País, ciudad y universidad de origen, periodo | No aplican. Una matrícula de alumno propio no los tiene |

## Alumno de movilidad internacional (`MOVILIDAD`)

| Dato | Regla |
|---|---|
| Documento | Pasaporte: de 6 a 9 caracteres, letras mayúsculas o cifras |
| País de origen | Código de dos letras mayúsculas, distinto de `ES` |
| Ciudad de origen | Obligatoria |
| Universidad de origen | Obligatoria, como mucho 150 caracteres |
| Periodo de estancia | `S1` (primer semestre), `S2` (segundo semestre) o `ANUAL` |
| Créditos | Entre 12 y 60 ECTS; en una estancia de un semestre (`S1` o `S2`), como mucho 30 |
| Curso previo | No aplica. Una matrícula de movilidad no lo tiene |

## Borradores

El formulario de matrícula se puede rellenar en varios días. Lo que el alumno haya escrito se guarda
como borrador, aunque esté incompleto o no cumpla las reglas. Un borrador no es una matrícula: solo
se da de alta cuando cumple todas las reglas, y si no las cumple se le dice al alumno todo lo que
falta.

## Carné universitario

Solo se emite para una matrícula pagada.

## API REST

Los cuerpos son JSON salvo la importación, que es CSV. Los errores siguen el RFC 9457
(`application/problem+json`): los datos que no cumplen las reglas devuelven **422** con todos los
motivos en `reasons`.

| Método y ruta | Qué hace | Respuestas |
|---|---|---|
| `POST /api/enrollments` | Da de alta una matrícula | 201 con la matrícula; 422; 409 si el documento ya existe |
| `GET /api/enrollments` | Lista las matrículas | 200 |
| `GET /api/enrollments/{id}` | Lee una matrícula | 200; 404 |
| `POST /api/enrollments/import` | Importa un CSV (`text/csv`). Todo o nada | 200 con `enrolled`; 422 con los motivos de cada línea |
| `PUT /api/enrollments/{id}/email` | Cambia el correo | 200; 422; 404 |
| `PUT /api/enrollments/{id}/degree` | Cambia la titulación | 200; 422; 404 |
| `PUT /api/enrollments/{id}/credits` | Cambia los créditos | 200; 422; 404 |
| `PUT /api/enrollments/{id}/previous-course` | Cambia el curso previo (solo alumnos propios) | 200; 422; 404 |
| `PUT /api/enrollments/{id}/origin` | Cambia país, ciudad y universidad de origen (solo movilidad) | 200; 422; 404 |
| `PUT /api/enrollments/{id}/stay` | Cambia periodo y créditos a la vez (solo movilidad) | 200; 422; 404 |
| `POST /api/enrollments/{id}/payment` | Registra el pago (simulado) | 204; 404 |
| `POST /api/enrollments/{id}/student-card` | Emite el carné | 201; 409 si no está pagada; 404 |
| `POST /api/drafts` | Guarda un borrador, completo o no | 201 |
| `PUT /api/drafts/{id}` | Actualiza un borrador | 200; 404 |
| `GET /api/drafts/{id}` | Lee un borrador | 200; 404 |
| `POST /api/drafts/{id}/submission` | Envía el borrador: si cumple las reglas, se da de alta y deja de ser borrador | 201 con la matrícula; 422 y el borrador sigue guardado |

Una matrícula o un borrador en JSON:

```json
{
  "type": "MOVILIDAD",
  "document": "X1234567",
  "fullName": "Lukas Weber",
  "email": "lukas@example.de",
  "degree": "GADE",
  "credits": 30,
  "originCountry": "DE",
  "originCity": "Berlín",
  "homeUniversity": "Freie Universität",
  "period": "S1"
}
```

Los campos que no aplican a un tipo de alumno se omiten o van a `null`. Cada línea del CSV sigue el
formato
`tipo;documento;nombre;email;titulacion;creditos;cursoPrevio;paisOrigen;ciudadOrigen;universidadOrigen;periodo`,
con vacíos los campos que no aplican.
