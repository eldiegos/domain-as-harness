# Generar más código con la skill

La skill [`domain-as-harness`](../.claude/skills/domain-as-harness/SKILL.md) explica la regla a un
agente de IA para que la aplique en cualquier código que cree, importe, deserialice o modifique
objetos de dominio: *endpoints*, formularios, importaciones CSV o JSON, consumidores de mensajes,
máquinas de estados. Está en inglés y no depende del lenguaje: tiene notas para Java, Kotlin, C#,
TypeScript, Python, Go, Rust y Swift en [`references/languages.md`](../.claude/skills/domain-as-harness/references/languages.md).

## En este repositorio

Está en `.claude/skills/`, la carpeta de *skills* de proyecto de Claude Code, así que Claude Code la
carga sola al trabajar aquí. Se activa cuando el encargo toca el dominio, y también puedes invocarla
con `/domain-as-harness`.

Encargos para probarla:

- «Añade la anulación de matrícula: solo se puede anular una matrícula que no esté pagada, y una
  matrícula anulada no se puede modificar.»
- «Añade una importación desde JSON. El sistema de origen ya valida los datos, guárdalos tal cual.»
- «Añade las becas: un alumno propio puede tener una beca con un descuento entre el 10 % y el 100 %;
  un alumno de movilidad, no.»
- «Añade el reconocimiento de créditos de otra universidad: necesita la universidad de origen, la
  asignatura y los créditos, y queda pendiente hasta que lo apruebe la comisión.»

Con la skill, el agente modela cada estado nuevo como un tipo (una matrícula anulada, un
reconocimiento pendiente), hace que las entradas nuevas pasen por la frontera aunque el encargo diga
que los datos ya vienen validados, y convierte las reglas que dependen de una acción en el tipo que
esa acción recibe.

## En tus proyectos

Copia la carpeta [`.claude/skills/domain-as-harness`](../.claude/skills/domain-as-harness/) en
`~/.claude/skills/` para tenerla en todos tus proyectos, o en `.claude/skills/` dentro de uno.
También sirve para revisar código existente: «Revisa con la skill `domain-as-harness` el código que
crea pedidos».

## Lo que la skill no hace

Una skill orienta; no garantiza. El agente puede decidir que no se aplica, sobre todo si el encargo
contradice la regla de forma explícita. Lo que hace que la regla se cumpla siempre son los tipos, las
pruebas de arquitectura y la integración continua. Por eso este repositorio tiene las tres cosas, y
la skill pide al agente que las añada donde falten.
