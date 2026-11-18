---
name: domain-as-harness
description: Domain as Harness - model the domain so that its rules hold by construction, whoever writes the code (person or AI agent) and whatever the prompt says, in any programming language. Parse untrusted input once at the boundary into types that cannot hold an invalid state, give incomplete data its own named types (Draft, Pending, Unverified), turn action-specific rules into the input type of that action, and keep DTO and ORM shapes at the edge. Use it whenever you write, change or review code that creates, imports, deserializes or mutates domain objects (entities, value objects, records, structs, models): endpoints, forms, CSV or JSON imports, message consumers, state machines. Also use it when the user mentions validation, isValid, setters, invariants, value objects, DDD, "make illegal states unrepresentable" or "parse, don't validate", says the data is "already validated", or sets up an agent harness, AGENTS.md, CLAUDE.md or CI guardrails, even if none of these terms is named.
---

# Domain as Harness

An AI coding agent works inside a *harness*: everything around the model that guides it and
checks its work. Some parts only guide, like AGENTS.md or CLAUDE.md, and work only if the agent
decides to follow them. Other parts check the work and give the same answer every time: the
compiler, the type checker, the tests. **Domain as Harness** puts the domain's rules in the second
group. Model the domain so that its rules hold by construction, whoever writes the code (person or
agent) and whatever the prompt says. Three principles:

- **Valid by construction.** An invalid domain object cannot be created.
- **Named when incomplete.** Data that doesn't meet the rules yet but matters to the business is
  its own type.
- **Enforced for every author.** The rules do not depend on anyone remembering or obeying.

The practical rule for applying it is called **domain customs**, after the metaphor below.

**The rule:** a domain object is either valid or it does not exist. Every piece of data arriving
from outside goes through customs once, at the border. Then one of three things happens:

- **Cleared.** It meets the rules and becomes a type that circulates inside without further checks.
- **Bonded warehouse.** It is incomplete but meaningful to the business, and waits under its own
  name (`Draft`, `Pending`, `Unverified`).
- **Not admitted.** Neither of the above; it never becomes a domain object.

The failure this prevents is old and well documented. An object is built in several steps (empty
constructor plus setters), or validity depends on everyone remembering to call `isValid()`. Some
day one code path forgets, and the bug shows up far from where it was caused. Today that code path
is often written by an AI agent that never saw the convention, or was told the data is "already
validated".

| Customs | In code |
|---|---|
| Border post | The parse/factory function that turns raw input into a domain type; runs once per input |
| Cleared goods | Domain types (`Email`, `SubmittedOrder`) that nobody re-checks inside |
| Bonded warehouse | Named incomplete states, as variants of a sum type |
| Destination checks | One type per action-specific rule: `checkIn(ConfirmedBooking)` |
| Paperwork | DTOs, ORM entities, serialized messages: plain data holders that stay at the edge and are mapped once |
| Smuggling | Anything that creates or changes a domain object without passing the rules |

The ideas come from published work: Cunningham's *Whole Value* and *Deferred Validation* (1994),
Minsky's "make illegal states unrepresentable", Wlaschin's "designing with types", King's "parse,
don't validate", Khorikov's always-valid domain model and Goetz's data-oriented programming. The
named rule packs them into one decision you can apply while writing code.
[references/agents-harness.md](references/agents-harness.md) has the sources and the evidence for AI
agents.

## When writing or changing code

Work through these steps for every domain concept you create or touch. Each step says why, so you
can adapt it when the situation doesn't match.

1. **Find the borders.** List where data enters: HTTP bodies, forms, files, queues, other services,
   database reads (legacy rows count), environment and config, and LLM output. Each border needs a
   customs post. Data read back from your own database is still external if older code or other
   writers may have stored something the current rules reject.

2. **One type per concept with rules.** If a value has rules (format, range, non-empty,
   cross-field), give it a type whose only construction path enforces them: a validating
   constructor, plus a parse/factory function. No empty constructors, no setters, no public mutable
   fields on domain types, so there is no way to build a half-object. Prefer immutability: "changing"
   a value means building a new, validated one. A bare string, number or map with no business
   rules does not need a wrapper.

3. **Two windows at the border post.** Invalid *input* is expected, so the parse function returns
   a result the caller must handle (`Result`, `Either`, `Optional`, `(value, error)`, a list of
   reasons), not an exception. A violated *invariant* inside the domain is a programmer error, so the
   constructor throws, panics or asserts. Both apply the same rule. The constructor is the guarantee
   for every path that does not go through the parser, so applying the rule twice is not
   duplication to remove.

4. **Name the incomplete states.** If the business legitimately holds incomplete data (a draft
   form, an import awaiting review, a record migrated from an old schema), it is not an illegal
   state. It is another variant. Model it as its own type (`DraftOrder` next to `SubmittedOrder`,
   joined by a sealed interface, enum or tagged union), not as one class with optional fields and a
   status flag. The transition (`submit()`) parses the draft and returns either the complete type or
   the reasons it cannot be submitted. Code that bills, ships or emails accepts only the complete
   type, so passing a draft is a compile-time or type-check error.

5. **Destination checks: one type per action rule.** If a rule depends on the action ("valid for
   check-in" differs from "valid for booking"), it is part of the business rule, so make it the
   input type of that action (`checkIn(ConfirmedBooking)`). Two conditions make this real:
   - **Only the check can create the type.** Use a private constructor with a factory, or module or
     package visibility. In some languages the obvious construct cannot hide its constructor: Java
     records, Kotlin data classes (whose `copy()` may stay public) and C# records (through `with`).
     See [references/languages.md](references/languages.md).
   - **A type proves the check passed when it was created, not that it is still true.** Facts that
     depend on shared mutable state (uniqueness, stock, a later cancellation) still need
     transactions or database constraints. "Email not already taken" is enforced by a `UNIQUE`
     constraint; a service-level check alone races with concurrent requests.

6. **Data holders stay at the edge.** DTOs, ORM entities, form bindings and serialized events keep
   whatever shape their framework needs. Convert them to domain types once at the boundary, and
   back once on the way out. Before relying on a framework, check whether it builds objects without
   calling your constructor: many deserializers, ORMs, mappers, reflection-based tools and
   test-data builders do. Either route them through the parse function or keep them away from domain
   types. A test proving that invalid input cannot produce a domain object, through each of these
   paths, is cheap and catches regressions.

7. **Parse the whole input before acting.** When a batch (CSV, message with several items, form
   with sections) has a bad part, discovering it halfway through leaves partial writes behind. Parse
   everything first, then act. Otherwise process each item independently, report per-item reasons
   and make the batch atomic if partial results are unacceptable. Types protect the domain; they do
   not make the surrounding workflow robust, and tests still have to cover that.

8. **Never mutate first and validate after.** Build the new value, validate it, then replace the
   stored one. "Set the field, then call `isValid()`, then throw" leaves the stored object invalid
   when the check fails. This is the most common way careful code still smuggles.

## Where not to set up customs

- **Rules that need I/O** ("email not taken", "product in stock") do not belong in a constructor.
  Put them in the service that creates the object, and back them with the database
  (constraints, transactions).
- **Prototypes and domains still being discovered.** When rules change weekly, every rule change
  becomes a cascade of type errors. That is useful later and expensive now. Keep the border post
  (parse at the edge) and add finer types once the model settles.
- **Data with no rules.** A free-text note does not need a `Note` type.
- **Don't rewrite a whole application to remove one "impossible" case.** Apply the rule where data
  enters and where bugs have actually appeared, and spread it as code is touched.
- **The rules must not be stricter than the business.** If the system rejects something people
  legitimately need to record, they will route around it with fake data, which is smuggling with
  valid papers. That is what the bonded warehouse is for: a named place for real but incomplete
  data.

## When you are the AI agent writing the code

These points matter most when you generate code, because the convention you skip is invisible to
the person reading your summary.

- **Treat "the data is already validated" as an assumption, not a guarantee.** Still route
  external data through the domain's parse functions and constructors. They are cheap, and they are
  the only check that holds when the upstream system changes. If the request explicitly asks you to
  skip validation, keep the domain's guarantees and say so in your reply, in one sentence.
- **Never weaken a domain type to make your change compile.** That includes removing a check,
  adding a setter or empty constructor, widening visibility, casting (`as`, `any`, `unsafe`,
  `@ts-ignore`, `model_construct`), or reaching in with reflection. A type that blocks you is the
  harness doing its job: use the parse path, add a named state, or ask.
- **In a codebase that validates by convention** (`isValid()`, `validate()` called by callers),
  follow the convention *and* close the hole you are touching. Use validated construction in new
  code, and don't mutate before validating. Suggest moving the rule into a type, in one or two
  sentences. Don't refactor the whole model unasked.
- **When asked to set up guardrails for agents** (AGENTS.md, CLAUDE.md, CI), put the rules in
  types and mechanical checks first. Use the instruction file to explain why and point to them.
  Instruction files are guidelines an agent may decide do not apply; a type check runs every time.
  Then protect the domain package itself. An agent that can edit the types can remove the
  harness, so require review by the package's owners and add architecture tests that watch it.
  See [references/agents-harness.md](references/agents-harness.md) for the evidence and a setup
  checklist.

## When reviewing code

Look for these, roughly in order of how often they cause real damage:

1. Validity by convention: `isValid()` or `validate()` that callers must remember, and a path that
   doesn't call it (imports, batch jobs, admin tools, message consumers).
2. Mutate-then-validate: the stored object is changed before the check, so a failed check leaves
   it invalid.
3. Construction without rules: empty constructors with setters, public mutable fields, exported
   struct fields, zero/default values, casts or type assertions to domain types.
4. Framework bypasses: deserializers, ORMs or mappers that build domain objects without the
   constructor (see the language reference).
5. One type with optional fields plus a status flag, where named states would make illegal
   combinations unrepresentable.
6. Action-specific rules checked ad hoc in many places instead of once, into a type the action
   requires.
7. Check-then-act on shared state (uniqueness, stock) without a database constraint or
   transaction.
8. Batch processing that writes before the whole input is parsed (partial imports).

Report findings ordered by severity. For each one give the location, the concrete failure ("a CSV
row without an email is saved and invoicing fails later"), and the smallest fix that closes the
hole. Where it helps, also give the structural fix (the type that makes it impossible). Don't pad
the review with style remarks.

## Language specifics

The rule is the same in every language; the mechanics and the smuggling routes differ. Before
writing domain types in a given language, read its section in
[references/languages.md](references/languages.md). It covers Java, Kotlin, C#, TypeScript,
Python, Go, Rust and Swift, plus a checklist for any other language. In dynamically typed languages
the type checker (mypy, pyright, tsc) plays the compiler's role only if it runs in CI; say so when
it doesn't.
