---
type: llm
focus: trace
weight: 1
---

The pay operation accepts only the submitted-order type and the ship operation only the paid-order type, according to their type annotations, so a static type checker would reject passing a draft.
