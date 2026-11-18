---
type: llm
focus: trace
weight: 1
---

importCsv never saves a customer whose name is empty or whose email is missing or malformed; such rows are skipped, rejected or reported instead.
