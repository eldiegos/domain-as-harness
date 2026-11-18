---
type: llm
focus: trace
weight: 1
---

review.md points out that the EmailExists-then-Insert check races under concurrency and recommends a UNIQUE constraint, or an equivalent atomic guarantee, in the database.
