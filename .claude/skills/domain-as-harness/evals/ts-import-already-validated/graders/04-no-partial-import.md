---
type: llm
focus: trace
weight: 1
---

A malformed line (for example one with no ';' separator) cannot make importCsv throw after earlier rows were saved: either the whole input is parsed before anything is saved, or each line is handled independently and problems are reported.
