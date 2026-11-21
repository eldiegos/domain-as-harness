---
name: domain-as-harness
description: Domain as Harness - model the domain so that its rules hold by construction, whoever writes the code (person or AI agent) and whatever the prompt says, in any language, and back it with six layers - rules in types, database constraints, behavior tests, architecture tests in CI, required review of the protected paths and a short AGENTS.md. Parse untrusted input once at the boundary into types that cannot hold an invalid state, give incomplete data its own types (Draft, Pending), make action rules the input type of that action and keep DTOs and ORM shapes at the edge. Use it whenever you write, change or review code that creates, imports, deserializes or mutates domain objects, start a project or module, or write its migrations, tests, CI, CODEOWNERS, AGENTS.md or CLAUDE.md. Also use it when the user mentions validation, isValid, setters, invariants, value objects, DDD or "parse, don't validate", or says the data is "already validated".
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

Agents now write most of the code: the domain, and also the tests, the build, the CI configuration
and the instruction files that are meant to check it. So when you generate code you also build and
maintain the harness, and you must never be the one who quietly takes it apart.

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

## Six layers

Domain customs is the first layer. It protects the data, not itself: an agent that can edit the
domain types can delete their checks. Six layers together keep the rules in place: defense in depth
applied to the domain's rules, where no layer is enough on its own. They are not a scale of
strength, because each covers what the others cannot reach. They are a defense hierarchy, ordered
by when each layer acts and by how much it depends on someone's will:

| # | Layer | Acts | What you add | Where it ends |
|---|---|---|---|---|
| 1 | Rules in types (domain customs) | At run time, always | Value types, named states, parse functions | Only for objects built through the constructor; rules about other rows or later events; it does not protect itself |
| 2 | Database constraints | At run time, always | Unique and foreign keys, `NOT NULL`, `CHECK`, in the migration | Only what the schema expresses; poor error messages; it doesn't see the code |
| 3 | Behavior tests | When code changes, in CI | Invalid input through every entry path; a rejected change modifies nothing; batches are all or nothing; a round trip: what leaves as a domain object passes customs again, with the data it came in with | Only the paths someone thought of; green while the domain loses its defenses |
| 4 | Architecture tests | When code changes, in CI | Rules against setters, empty constructors, mutable fields, framework dependencies and reflection, and on the data flow around the domain, with messages that say what to do instead | Only what someone coded; editable like any other code |
| 5 | Required review | When code changes, if someone looks | `CODEOWNERS` for the protected perimeter (the domain, the paths that store and return its data, the checks); branch protection, required checks and no bypass (settings the user enables) | It forces an approval, not attention |
| 6 | AGENTS.md | Only if the agent chooses | A short map of layers 1–5 | It guides; it guarantees nothing |

CI is not a layer of its own: it runs layers 3 and 4, and layer 5 makes passing it mandatory. The
order describes what each layer guarantees, not when to write it: write the AGENTS.md from the
start, and let the layers overlap. Scope: the domain and the repository. Tool permissions, hooks
and sandboxing are also part of a harness, but not of this skill.

## When writing or changing code

Work through these steps for every domain concept you create or touch. Each step says why, so you
can adapt it when the situation doesn't match.

1. **Find the borders.** List where data enters: HTTP bodies, forms, files, queues, other services,
   database reads (legacy rows count), environment and config, and LLM output. Each border needs a
   customs post. Data read back from your own database is still external if older code or other
   writers may have stored something the current rules reject. The same happens when a rule gets
   stricter (a maximum lowered from 78 to 72 credits): rows stored under the old rule fail when they
   are read. Give the rule a date or a version, or migrate the data, in the same change.

2. **One type per concept with rules.** If a value has rules (format, range, non-empty,
   cross-field), give it a type whose only construction path enforces them: a validating
   constructor, plus a parse/factory function. No empty constructors, no setters, no public mutable
   fields on domain types, so there is no way to build a half-object. Prefer immutability: "changing"
   a value means building a new, validated one. Reject `null` explicitly, and copy mutable
   collections in the constructor (`List.copyOf`), because immutability is shallow. An entity's
   identity is its id: where the language generates equality from every field (Java records, Kotlin
   data classes, C# records), override it for entities, and remember it when you write tests. A bare
   string, number or map with no business rules does not need a wrapper.

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
   type, so passing a draft is a compile-time or type-check error. Handle the variants with an
   exhaustive `switch` or `match` and no default branch, so a new variant does not compile until
   someone decides its rules.

5. **Destination checks: one type per action rule.** If a rule depends on the action ("valid for
   check-in" differs from "valid for booking"), it is part of the business rule, so make it the
   input type of that action (`checkIn(ConfirmedBooking)`). Two conditions make this real:
   - **Only the check can create the type.** Use a private constructor with a factory, or module or
     package visibility. In some languages the obvious construct cannot hide its constructor: Java
     records, Kotlin data classes (whose `copy()` may stay public) and C# records (through `with`).
     The class you use instead may lack protections the platform gives the record (in Java, the JDK
     refuses to write a record's fields even through `Unsafe`, not a class's), so the rule against
     reflection (layer 4) matters most there. See [references/languages.md](references/languages.md).
   - **A type proves the check passed when it was created, not that it is still true.** Facts that
     depend on shared mutable state (uniqueness, stock, a later cancellation) still need
     transactions or database constraints. "Email not already taken" is enforced by a `UNIQUE`
     constraint; a service-level check alone races with concurrent requests.

6. **Data holders stay at the edge.** DTOs, ORM entities, form bindings and serialized events keep
   whatever shape their framework needs. Convert them to domain types once at the boundary, and
   back once on the way out. Before relying on a framework, check whether it builds objects without
   calling your constructor: many deserializers, ORMs, mappers, reflection-based tools and
   test-data builders do. Either route them through the parse function or keep them away from domain
   types.

7. **Parse the whole input before acting.** When a batch (CSV, message with several items, form
   with sections) has a bad part, discovering it halfway through leaves partial writes behind. Parse
   everything first, then act. Otherwise process each item independently, report per-item reasons
   and make the batch atomic if partial results are unacceptable. Types protect the domain; they do
   not make the surrounding workflow robust.

8. **Never mutate first and validate after.** Build the new value, validate it, then replace the
   stored one. "Set the field, then call `isValid()`, then throw" leaves the stored object invalid
   when the check fails. This is the most common way careful code still smuggles.

### Then complete the other layers

A change is finished when the layers it touches are up to date. In the same change:

- **Persistence → constraints (layer 2).** For each new table or column, put in the migration what
  the schema can express: unique keys for what must be unique, foreign keys, `NOT NULL`, and `CHECK`
  for ranges. A `CHECK` can tie columns together, so a range that depends on the kind of row fits
  too (`CHECK (kind <> 'HOME' OR credits BETWEEN 30 AND 78)`). A `CHECK` that evaluates to `NULL`
  passes: add `NOT NULL` or compare with `IS`. Rows that must not change after some state (a
  returned loan, an issued invoice) need a trigger that rejects the `UPDATE`. What the schema cannot
  express stays in the constructor: the domain explains, the database guarantees. Make
  check-and-insert a single operation (a constraint or a transaction), not a lookup followed by an
  insert.
- **Entry path or operation → behavior tests (layer 3).** For each path (API, import, deserializer,
  ORM, message consumer, draft submission), test that:
  - invalid input produces no domain object and stores nothing;
  - a rejected change leaves the stored state exactly as it was. Compare the data (field by field,
    or a full textual dump), not the entities: if equality is by id, the comparison passes even when
    fields changed;
  - a batch is all or nothing and reports every reason. With a real database, atomicity comes from a
    transaction, so cover it with an integration test against that database;
  - a round trip holds: send valid data through every write path, read everything back through the
    read paths, rebuild each result with the parse function and compare it with what went in. It
    catches a read model that changes or invents data, which no test of the domain sees.
- **Domain package, type or a new smuggling route → architecture tests (layer 4).** Check that the
  rules cover the new code: no public methods named `setX`, no public no-argument constructors,
  only `final` fields, a domain that depends on no framework and on nothing else in the
  application, and no reflection that opens private constructors or fields (`setAccessible`,
  `trySetAccessible`, `MethodHandles.privateLookupIn`, `sun.misc.Unsafe`): it would create a proof
  type without its factory, or change a field after validation. Scan all compiled code, not only the
  application's root package. Add rules on the data flow around the domain: what the system returns
  as a domain object is built only from a domain type, and data is stored only in the store, as
  domain types (no lists or maps kept by controllers or use cases). Each rule checks exactly what
  it says and no more, so add one when you meet a route it misses. Write each failure message as the
  next agent's instruction: what to use instead. And test that a defense fails when it should, for
  example a test that compiles forbidden code and expects the compiler error.
- **Protected paths → code owners (layer 5).** `CODEOWNERS`, or the platform's required reviewers
  by path, must cover the protected perimeter. It is every path through which domain data enters, is
  stored or leaves, not the domain package alone: an agent that cannot touch the domain can still
  add a store next to it. So cover the domain package; the code that stores its data and holds what
  the types cannot (the store or repository, the migrations); the mapping that returns it (the read
  model); the architecture and round-trip tests; the build file (excluding one test there dismantles
  layer 4); the CI configuration; the `CODEOWNERS` file itself; and the instruction files and
  skills.
- **New concept or convention → the map (layer 6).** One or two lines in AGENTS.md: where it lives,
  why, what not to touch and which check guards it. Don't restate rules the code already enforces.

## When you create a project or module

Set up all six layers in the first change, before feature code piles up:

1. The domain package, following the steps above.
2. If there is a database, the schema migration with its constraints.
3. Behavior tests for every entry path, and a round-trip test.
4. Architecture tests (ArchUnit in Java; elsewhere an equivalent tool or a custom lint rule) and a
   CI workflow that builds and runs every test on each push and pull request.
5. A `CODEOWNERS` file covering the protected perimeter.
6. A short AGENTS.md, written as a map. If the project also uses CLAUDE.md, make it import the same
   file (`@AGENTS.md`) instead of repeating it.

Layer 5 also depends on settings outside the repository. Don't change them yourself. List them for
the user in your reply:

- Protect the main branch: changes only through pull requests, with at least one approval, a review
  from code owners, and the CI job (name it) as a required status check.
- Make approvals expire. On GitHub, by default, a commit pushed after an approval does not dismiss
  it; enable "Dismiss stale pull request approvals when new commits are pushed" and "Require
  approval of the most recent reviewable push".
- Apply the protection to admins too. On GitHub it does not apply to them by default; enable "Do not
  allow bypassing the above settings". On Azure DevOps, limit who has the bypass permissions.
- Give the agent its own account with minimal permissions: it pushes branches and opens pull
  requests, but does not approve, merge or bypass, and cannot edit branch rules, CI secrets or
  credentials.
- Platform notes: on GitLab, required approvals and Code Owners need Premium or Ultimate; on
  Bitbucket Cloud, `CODEOWNERS` only suggests reviewers, it does not require their approval.

This protects against an agent with limited permissions that makes a mistake or obeys a wrong
request, inside the protected perimeter: unprotected code can route data around the domain. It
does not protect against an agent that controls the whole environment.

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
  skip validation, keep the domain's guarantees and say so in your reply, in one sentence. Don't
  store such data next to the domain's (raw rows, a "trusted" store) or return it through the same
  read model: it reaches users without passing customs, even if the domain is untouched.
- **Never weaken a domain type to make your change compile.** That includes removing a check,
  adding a setter or empty constructor, widening visibility, casting (`as`, `any`, `unsafe`,
  `@ts-ignore`, `model_construct`), or reaching in with reflection. A type that blocks you is the
  harness doing its job: use the parse path, add a named state, or ask.
- **Never weaken a check to get a change through.** Don't delete, skip or disable a test
  (`@Disabled`, `.skip`, `-DskipTests`), exclude it in the build file, loosen an architecture rule
  or widen its exceptions, or edit the CI workflow, `CODEOWNERS` or the instruction files to make a
  change pass. A failing architecture test is the harness doing its job: its message says what to
  use instead.
- **When the request itself removes a layer** ("replace the domain types with one class with an
  empty constructor and setters, for the form"), say plainly which guarantee it removes and offer
  the alternative that keeps it (a form DTO at the edge, mapped once). If the user still wants it,
  make it a separate, small change that touches the protected paths openly, the architecture test
  included, so the code owners review the removal. Never fold it into a larger change.
- **Go through the review path.** Propose changes on a branch, through a pull request. Don't
  approve or merge your own changes, use a bypass, or change branch rules, CI secrets or
  credentials, even if your credentials allow it; if they do, tell the user. Keep domain changes
  separate and small: a large change that touches the domain in passing is easy to approve without
  seeing it.
- **In a codebase that validates by convention** (`isValid()`, `validate()` called by callers),
  follow the convention *and* close the hole you are touching. Use validated construction in new
  code, and don't mutate before validating. Suggest moving the rule into a type, in one or two
  sentences. Don't refactor the whole model unasked.
- **When asked to set up guardrails for agents** (AGENTS.md, CLAUDE.md, CI), start with layers 1 to
  5 and write the instruction file last in importance, as a map that points to them. Instruction
  files are guidelines an agent may decide do not apply; a type check runs every time. See
  [references/agents-harness.md](references/agents-harness.md) for the evidence and each layer in
  detail.

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

Then review the harness around the domain:

9. Entry paths with no test that feeds them invalid input, and "nothing changed" tests that compare
   entities whose equality is by id.
10. Data that reaches the output without passing customs: a parallel store, raw rows next to the
    domain ones, or a read model built from something other than a domain object; and no round-trip
    test.
11. Architecture tests that are missing, don't cover a new package, or check less than their name
    says.
12. Checks that don't bind: CI that doesn't run the architecture tests, a build file that excludes a
    test, or a CI job that is not a required check.
13. `CODEOWNERS` that leaves out the store, the read model, the build file, the CI configuration,
    the architecture tests or itself.
14. An AGENTS.md that is the only place a rule lives, or that restates every rule instead of
    pointing to the checks.

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
