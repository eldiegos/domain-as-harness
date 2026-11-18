---
type: llm
focus: trace
weight: 1
---

review.md recommends making invalid customers unconstructible (for example unexported fields plus a constructor or parse function that returns an error), not only adding more Validate calls.
