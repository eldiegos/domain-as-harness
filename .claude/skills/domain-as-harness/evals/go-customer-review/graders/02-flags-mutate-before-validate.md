---
type: llm
focus: trace
weight: 1
---

review.md points out that ChangeEmail modifies the customer returned by FindByID before validating it, and that because the repository caches and returns shared pointers, an invalid email stays visible after validation fails.
