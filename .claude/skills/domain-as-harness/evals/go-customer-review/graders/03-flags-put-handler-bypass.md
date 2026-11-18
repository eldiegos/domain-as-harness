---
type: llm
focus: trace
weight: 1
---

review.md points out that the PUT handler decodes the request body into Customer and saves it with repo.Update without any validation.
