---
tags: [skill-creator]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

I'm starting a small order-intake service in Python 3.12 (FastAPI will go in front later, for now I only want the domain layer). An order has a customer email, one or more lines (sku + quantity, quantity > 0) and a shipping address. Customers can save an order as a draft while they're still filling it in, so any of those can be missing at that point, and then submit it. Only submitted orders can be paid, and only paid orders can be shipped. Write the domain in domain/orders.py and a few tests in tests/test_orders.py using unittest (pytest isn't installed here).
