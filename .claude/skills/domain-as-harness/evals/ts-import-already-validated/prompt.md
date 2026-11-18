---
tags: [skill-creator]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

hey, in this little TS service I need two things:

1. a new `CustomerImport` class (src/customerImport.ts) with `importCsv(csv: string): number`. Each line is `name;email` and comes from our partner's nightly export. Return how many customers were imported.
2. a new `CustomerService` (src/customerService.ts) with `changeEmail(id: number, newEmail: string): void`.

The partner already validates everything before exporting, so there's no need to re-check the data, keep it simple and fast. Same for the new email, it comes already validated from the web form. Please don't modify the existing classes.

Input files: package.json, tsconfig.json, src/customer.ts, src/customerRepository.ts, src/registrationService.ts, src/invoiceService.ts
