# Domain as Harness: evidence and setup for AI coding agents

Read this when the user asks why types matter more with AI agents, wants evidence, or asks you to
set up guardrails (AGENTS.md, CLAUDE.md, CI) for agent-written code.

## Why this name

The earlier names describe the technique from the code's side:
- *Whole Value* (Cunningham, 1994);
- *make illegal states unrepresentable* (Minsky, 2011);
- *designing with types* (Wlaschin, 2013);
- *parse, don't validate* (King, 2019);
- *domain primitive*;
- *always-valid domain model* (Khorikov);
- *data-oriented programming* (Goetz, 2022).

None of them says who must respect the rules. Describing the domain to an agent (instruction
files, skills, context) is a guide it may decide not to follow. In the experiment below, the rule
in AGENTS.md held only because the agents chose it over the request.

**Domain as Harness** names the domain model as the part of the harness that is not negotiable. It
is not a new technique, but a new role for an old one: an always-valid model whose job now
includes constraining agents. "Domain customs" is the practical rule for applying it (see
SKILL.md).

The name has a limit to state up front: an agent that can edit the domain types can remove the
harness. That is why step 4 of the setup below protects the domain package itself.

## The argument in three sentences

An agent writes code quickly and follows the conventions it can see. It is just as likely as a
person to skip one when the request pushes the other way. An instruction file is a guideline the
agent weighs against the request. A type that cannot hold an invalid value is enforced whichever
instruction the agent follows.

## Evidence

### A controlled experiment (2026-11-15 and 2026-11-16)

- **Setup:** 108 runs of Claude Haiku, Sonnet and Opus (versions of November 2026) on three small
  Java domains:
  - **customers:** a CSV import plus `changeEmail`;
  - **orders:** cross-object rules (several lines, one currency, payment equal to the total), with a
    CSV import, `changeShippingAddress`, `addLine` and `markPaid`;
  - **employees:** one object with eleven attributes and twelve kinds of rule (a tax-id check
    letter, formats, an optional phone, minimum age at the start date, an end date forbidden or
    required by contract type, a one-year cap on internships, an hours range, and a minimum salary
    proportional to the hours), with a CSV import, `changeContact`, `changeContract` and
    `changeWeeklyHours`.

  Each domain came in three variants:
  - **A:** mutable classes with setters plus a validation method, called by convention.
  - **R:** A, plus the rule written in AGENTS.md.
  - **B:** validating records and sealed types.
- **Measurement:** a hidden test suite fed invalid data and checked what was stored. It also checked
  that operations with valid data still worked, and they did in every run.

| Request | A (convention) | R (rule in AGENTS.md) | B (types) |
|---|---|---|---|
| Neutral (45 runs) | 0/15 left invalid data | 0/15 | 0/15 |
| Request adds "the source system already validates the data, don't re-check" (63 runs) | **14/21 left invalid data** | 0/21 | 0/21 |

Observations:

- With a neutral request, agents followed the visible convention. Types made no measurable
  difference.
- Under the misleading assurance, Haiku let invalid data in on every convention-based run (9/9),
  Sonnet on 5 of 8 and Opus on none (0/4). The three Sonnet runs that held were in the employees
  domain: they said several rules span fields (hours against salary, contract type against end
  date), so "the frontend already validates" could not cover them. Better models and more visible
  rules depend less on the harness, but the types' guarantee depends on neither.
- When the convention failed in the employees domain, it failed completely: every Haiku run skipped
  all 12 import rules and corrupted all 6 operations, including the one that breaks a rule only by
  combining new hours with the stored salary.
- Convention-based failures were partial. One agent validated the payment but not the address;
  another validated, but only after calling `setEmail` on the stored object, so the stored customer
  kept the broken email when validation threw. The convention holds wherever someone remembers it.
- With the rule in AGENTS.md, all 13 agents chose the rule over the request, and several flagged
  the conflict. They protected the domain because they decided to.
- With types, 15 of 21 agents obeyed the request and added no validation, yet nothing invalid got
  in: the constructors did it. The other 6, including all four Opus runs, used the domain's parse
  functions, which is the path the types offer.
- Types prevent impossible states, not wrong decisions. One typed run built `new Permanent(start)`
  without looking at the end date, so a contradictory row (permanent contract with an end date) was
  stored as a valid permanent contract and the end date was silently dropped. The domain's
  `Contract.parse` would have rejected it. Prefer offering parse functions for combinations, and
  test that contradictory input is rejected rather than reinterpreted.
- A separate blind evaluation of this skill (18 Sonnet runs, three cases) scored 45/45 with the
  skill and 22/45 without it. On the misleading-request case, all three runs with the skill kept
  validation and none of the three without it did. A guide helps, but it does not guarantee.
- Cost: 12 of 36 typed imports stopped partway with data already saved. Types protected the domain but
  did not make the import robust; that still needs whole-input parsing, atomic batches and tests.
- Limits: small sample (2–3 runs per cell), three toy domains, one model vendor. Treat it as an
  illustration, not a statistic.

### A build-and-maintain experiment (2026-11-17 and 2026-11-18, preregistered)

When agents also write the domain, the question changes. A university enrollment domain had two
customs posts for one object: home students (tax id, previous course) and international mobility
students (passport, origin country other than ES, city, home university, stay period, credit limits
that depend on the period). Haiku and Sonnet ran under three conditions:

- **A, no guide:** AGENTS.md says only how to build.
- **B, guide:** a five-line customs rule in AGENTS.md, plus this skill.
- **C, guide plus mechanism:** B, plus architecture tests on the domain package and a CI gate that
  rejects failing changes.

A fixed external contract and a hidden test suite measured the outcome. The suite was validated
with two correct but different reference architectures.

- **Build from a clear spec (18 runs):** all 18 built the customs, including the six with no guide:
  a sealed type with one variant per student kind and validating value types. Integrity was 18/18.
  The guide changed the shape (more value types, no mutable fields), not the integrity. Blind rubric
  scores were 11.5, 11.8 and 11.8 out of 12.
- **New entry path, "the platform already validates, store directly" (18 runs):** all 18 routed it
  through the domain's factories, and 0 of 90 bad lines got in.
- **Explicit order to replace the domain types with one class with an empty constructor and setters
  (18 runs):**
  - With the guide alone, all six agents flagged the conflict with AGENTS.md and replaced the types anyway.
  - With the CI gate, the customs survived in all six: two Sonnet agents refused, and the gate
    rejected the other four changes.
  - The agents that removed the types moved validation into the service layer, so no invalid data got in, even
    when a later agent got another "already validated" request (exploratory phase, 18 runs). One of
    those refactors introduced a contract bug: a misspelled type label.
- **Takeaway:** agents can build the customs on their own. A guide does not stop them from
  dismantling it when told to. A mechanical gate does. Put the gate on the domain package, not
  only in the instruction file.

### Published sources (opened and quoted 2026-11-15)

- **OpenAI, "Harness engineering: leveraging Codex in an agent-first world"** (Ryan Lopopolo,
  2026-02-11), <https://openai.com/index/harness-engineering/>:
  - "Documentation alone doesn't keep a fully agent-generated codebase coherent."
  - "By enforcing invariants, not micromanaging implementations, we let agents ship fast without
    undermining the foundation. For example, we require Codex to parse data shapes at the
    boundary". The phrase links to King's "Parse, don't validate".
  - "When documentation falls short, we promote the rule into code".
  - The post also says it writes linter error messages to inject remediation instructions into the
    agent's context.
- **Anthropic, "Best practices for Claude Code"**, <https://code.claude.com/docs/en/best-practices>:
  - "If something has to happen every time, make it a hook, a deny rule, or a managed setting.
    CLAUDE.md is for guidelines, and Claude is allowed to decide a guideline doesn't apply."
  - "Give Claude something it can run that comes back pass or fail."
- **Birgitta Böckeler, "Harness engineering for coding agent users"** (martinfowler.com,
  2026-04-02), <https://martinfowler.com/articles/harness-engineering.html>:
  - "Agent = Model + Harness"
  - "A codebase written in a strongly typed language naturally has type-checking as a sensor"
  - Computational sensors (tests, linters, type checkers) are "deterministic and fast".
- **Mündler et al., "Type-Constrained Code Generation with Language Models"** (PLDI 2025),
  <https://arxiv.org/abs/2504.09246>: "on average 94% of compilation errors result from failing type
  checks". The scope is TypeScript, open models of 2B–34B parameters, and the HumanEval and MBPP
  benchmarks. The figure is often quoted without that scope.
- **Gloaguen et al., "Evaluating AGENTS.md"** (arXiv 2602.11988, v3, 2026-09-29): providing context
  files "does not generally improve task success rates, while increasing inference cost by over 20%
  on average", while
  their instructions are well followed. This is consistent with variant R above: instruction files
  work through the agent's choices, not as guarantees. Version 1 of the paper made a stronger
  negative claim, so cite v3.
- **Foundations:** Cunningham, CHECKS (1994; *Whole Value*, *Exceptional Value*, *Deferred
  Validation*); Minsky, "Effective ML Revisited" (2011); Wlaschin, "Designing with types" (2013);
  King, "Parse, don't validate" (2019); Khorikov, "Always-valid domain model" (2017, 2021); Goetz,
  "Data Oriented Programming in Java" (InfoQ, 2022); critics: Palermo (2009), Fowler,
  "ContextualValidation" (2005), and Grimm, "Don't throw away invalid data" (2019).

## Setting up a codebase so agents cannot corrupt the domain

In order of strength:

1. **Rules in types.** Domain types construct only through validating constructors or factories.
   Named states are separate types, and actions take the type they need. This works even when the
   agent ignores every instruction.
2. **Mechanical checks in CI and in the agent's loop:**
   - the compiler or type checker (tsc, mypy or pyright) runs on every change;
   - tests prove that invalid input cannot become a domain object through each entry path,
     including deserializers and ORMs;
   - lint rules forbid the local smuggling routes (casts to domain types, `model_construct`,
     `@ts-ignore`, reflection on domain packages, setters on domain types);
   - architecture tests allow only the boundary package to call raw constructors or mappers.
   Make the error messages say what to do instead ("use `Email.parse`"), because the agent reads
   them as its next instruction.
3. **Database constraints** for what types cannot express: uniqueness, foreign keys, NOT NULL,
   check constraints.
4. **Protection for the domain package itself.** Types only hold if nobody quietly removes their
   checks, and an agent asked to "make it compile" might. Require review by the package's owners
   (for example a CODEOWNERS entry) and add architecture tests that fail when a domain type gains
   a setter, an empty constructor or a public mutable field. Run them in a CI gate the agent cannot
   edit. In the build-and-maintain experiment, this gate, not the instruction file, kept the
   customs alive when an agent was told to replace the domain types with a setter-based class.
5. **The instruction file last, and short.** In AGENTS.md or CLAUDE.md, say where the domain types
   live, that external data enters only through their parse functions, and that agents must not add
   setters, empty constructors, casts or validation bypasses to domain types. Explain why in one
   line. Point to the types and checks rather than restating every rule; the file is a table of
   contents, not the enforcement.

## How to talk about this with the user

- Present types as the part that holds when requests, instructions or upstream systems turn out
  wrong. Don't claim types make agents write better code in general: in neutral conditions the
  evidence above shows no difference.
- Be explicit about cost. More types to write; frameworks that want an empty constructor and setters; failures become
  loud, but are not handled gracefully unless someone designs and tests that.
