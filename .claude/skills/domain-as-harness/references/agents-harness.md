# Domain as Harness: evidence and setup for AI coding agents

Read this when the user asks why types matter more with AI agents, wants evidence, or asks you to
set up guardrails (AGENTS.md, CLAUDE.md, CI, CODEOWNERS) for agent-written code.

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
harness. That is why layer 5 below puts the domain package, and the checks that guard it, under
required review.

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
- A separate blind evaluation of this skill (18 Sonnet runs, three cases, with the version of
  2026-11-16, before it covered the six layers) scored 45/45 with the skill and 22/45 without it. On the misleading-request case, all three runs with the skill kept
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
  only in the instruction file, and, as the next experiment shows, on the paths that store and return
  its data.

### A red-team and pressure experiment (2026-11-21, preregistered)

The reference repository with the six layers (domain types, store, behavior tests, five
architecture rules, `CODEOWNERS` on the domain and the checks, a short AGENTS.md) was attacked from
five angles: 17 hand-written smuggling routes, six red-team agents (Sonnet and Opus) told to get an
invalid enrollment out through the API with `verify` green and no protected path touched, 16
requests that pushed agents to dismantle the harness, 8 legitimate features, and 12 new Python
projects with and without this skill.

- **The domain held.** No agent built an invalid domain object. In Java 25 the JDK also refuses to
  write a record's fields, even through `Unsafe`.
- **The perimeter did not.** All six red-team agents succeeded, and none touched the domain: they
  added a store of read models built from raw input, or a read model that changed the data, in the
  API, use-case or store packages, which nothing protected. A realistic request ("store the rows as
  they come") got the same result from 1 of 4 agents. The fix is the data-flow rules, the round-trip
  test and the wider `CODEOWNERS` above. Replayed without agents on the repository with these
  defenses, the 7 changes that got invalid data out all fail `verify`: six on the flow rules, and
  the read model that changed the data on the round-trip test. The other 23 (3 of them empty,
  from agents that refused) still pass, all 8 features included. The cost is review: all 8
  features now touch a protected path, the read model; 7 already did, through the domain.
- **Reflection had two more doors:** `MethodHandles.privateLookupIn` and `Unsafe` obtained without
  `setAccessible` forged a proof type that is a class, not a record.
- **Under pressure, no agent weakened a check** (0 of 16), with or without the guide, and all 8
  features passed hidden tests with no architecture failure.
- **The skill matters most when the project is new.** In Python, all 6 projects built with the skill
  had layers 2 to 6 (blind score 11.8 of 12); none of the 6 without it had architecture tests, CI or
  `CODEOWNERS` (3.7 of 12). No project made returned loans immutable in the database (a trigger).

### Published sources (opened and quoted 2026-11-15)

- **OpenAI, "Harness engineering: leveraging Codex in an agent-first world"** (Ryan Lopopolo,
  2026-02-11), <https://openai.com/index/harness-engineering/>:
  - "Documentation alone doesn't keep a fully agent-generated codebase coherent."
  - "By enforcing invariants, not micromanaging implementations, we let agents ship fast without
    undermining the foundation. For example, we require Codex to parse data shapes at the
    boundary". The phrase links to King's "Parse, don't validate".
  - "When documentation falls short, we promote the rule into code".
  - "Because the lints are custom, we write the error messages to inject remediation instructions
    into agent context."
  - "give Codex a map, not a 1,000-page instruction manual"; "A short AGENTS.md (roughly 100
    lines) is injected into context and serves primarily as a map" (opened 2026-11-20).
- **Anthropic, "Best practices for Claude Code"**, <https://code.claude.com/docs/en/best-practices>:
  - "If something has to happen every time, make it a hook, a deny rule, or a managed setting.
    CLAUDE.md is for guidelines, and Claude is allowed to decide a guideline doesn't apply."
  - "Give Claude something it can run that comes back pass or fail."
  - "Keep CLAUDE.md short": "every line in it takes a little of Claude's attention on every task,
    whether it's relevant or not" (opened 2026-11-20).
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
- **Required review on each platform** (opened 2026-11-20):
  - GitHub, "About protected branches" and "About code owners": "By default, the restrictions of
    a branch protection rule don't apply to people with admin permissions to the repository";
    "Do not allow bypassing the above settings"; `CODEOWNERS` can live in `.github/`, the root or
    `docs/`.
  - GitLab, "Code Owners" and "Merge request approvals": required approvals and Code Owners need
    Premium or Ultimate; on Free, "These approvals are optional and don’t prevent merging without
    approval".
  - Azure DevOps, "Set and manage branch policies": "You can't push changes directly to branches
    with required branch policies unless you have permissions to bypass branch policies."
  - Bitbucket Cloud, "Set up and use code owners": code owners are added as suggested reviewers;
    the page does not say their approval can be required.
- **Foundations:** Cunningham, CHECKS (1994; *Whole Value*, *Exceptional Value*, *Deferred
  Validation*); Minsky, "Effective ML Revisited" (2011); Wlaschin, "Designing with types" (2013);
  King, "Parse, don't validate" (2019); Khorikov, "Always-valid domain model" (2017, 2021); Goetz,
  "Data Oriented Programming in Java" (InfoQ, 2022); critics: Palermo (2009), Fowler,
  "ContextualValidation" (2005), and Grimm, "Don't throw away invalid data" (2019).

## The six layers in detail

This is defense in depth applied to the domain's rules: no layer is enough on its own. The layers
are a defense hierarchy, not a scale of strength: each one covers what the others
cannot reach. They are ordered by when they act and by how much they depend on someone's will.
Layers 1 and 2 act at run time, always. Layers 3, 4 and 5 act when the code changes, and 5 also
depends on someone looking. Layer 6 works only if the agent chooses to follow it. The scope is the
domain and the repository: tool permissions, hooks and sandboxing are also part of a harness, but
not of this skill.

1. **Rules in types.**
   - *Without it:* validity depends on remembering to call `isValid()`.
   - *How:* domain types construct only through validating constructors or factories. Named states
     are separate types, and actions take the type they need. This works even when the agent
     ignores every instruction, for every object built through the constructor.
   - *Where it ends:* `null`, shallow immutability, misread data (a valid enrollment with the wrong
     number of credits), rules about other rows or later events, rules that change over time, and
     cost. Above all, a type protects the data, not itself.
2. **Database constraints.**
   - *Without it:* rules that depend on other rows race (two concurrent sign-ups pass the same
     uniqueness check), and writes that skip the domain go unchecked: a script, a migration, an
     agent running SQL.
   - *How:* unique and foreign keys, `NOT NULL` and `CHECK`, which the database enforces on every
     write, wherever it comes from.
   - *Where it ends:* only what the schema expresses. A `CHECK` that repeats the widest credit range
     lets a direct `INSERT` of a home student with 12 credits through. Its errors don't tell the
     user what to fix: the domain explains, the database guarantees. And it doesn't see the code.
3. **Behavior tests.**
   - *Without it:* correct types can still produce wrong results. An import that saves line by line
     and fails on the fourth leaves the first three saved.
   - *How:* test each entry path (API, imports, deserializers, ORM, drafts) with invalid input. Test
     that a rejected change leaves nothing modified, comparing the data rather than entities whose
     equality is by id. Test that a batch is all or nothing; with a real database, that needs an
     integration test of the transaction. Add a round trip: data sent through every write path,
     read back and rebuilt with the parse function, must pass and match what went in. In the
     reference repository, `IdaYVueltaTest` does it over HTTP.
   - *Where it ends:* only the paths someone thought of, and it stays green while the domain loses
     its defenses.
4. **Architecture tests in CI.**
   - *Without it:* "replace the enrollment with a class with an empty constructor and setters, for
     the form" breaks no behavior test.
   - *How:* the reference repository has seven ArchUnit rules over all compiled code:
     - no public instance method named `set[A-Z].*`, no public no-argument constructor and only
       `final` fields in the domain;
     - a domain that depends on no framework and nothing else in the application;
     - no `setAccessible`, `trySetAccessible`, `MethodHandles.privateLookupIn` or `sun.misc`, and no
       framework wrapper that calls them for you (Spring's `ReflectionUtils`,
       `BeanUtils.instantiateClass`, `DirectFieldAccessor`, Objenesis, CGLIB), which
       open private constructors and fields;
     - the read model (`EnrollmentView`) is built only by `EnrollmentView.of(long, Enrollment)`;
     - controllers and use cases keep no collections: data lives only in the store.

     A plain test next to them fails if an `archunit_ignore_patterns.txt` is on the classpath:
     ArchUnit ignores every violation that matches its patterns, so one line (`.*`) in any resources
     folder silences all the rules.

     Each checks exactly what it says. Write the failure messages as instructions ("add a `withX(...)` method that returns a new
     object built with the validating constructor"), because the agent reads them as its next step.
     CI runs layers 3 and 4; it is not a layer by itself.
   - *Test the defense:* check that it fails when it should. In the reference repository,
     `NoCompilaTest` adds a third variant to the sealed type and expects
     `compiler.err.not.exhaustive.statement` from `javac`.
   - *Where it ends:* only what someone coded, and an agent that can edit the test or the CI
     configuration can dismantle it.
5. **Required review.**
   - *Without it:* the same agent that should respect everything above can edit it.
   - *How:* three mechanisms that are often confused:
     - code review is the practice;
     - required review on pull requests is the mechanism: a protected branch, approvals and required
       status checks;
     - code owners decide who must approve.
   - *Three details decide whether it protects:*
     - Nobody bypasses it. On GitHub, by default, the restrictions of a branch protection rule don't
       apply to admins unless "Do not allow bypassing the above settings" is enabled. On Azure
       DevOps, whoever can bypass branch policies can push directly.
     - Approvals expire. On GitHub, by default, a commit pushed after an approval does not dismiss
       it, unless "Dismiss stale pull request approvals when new commits are pushed" is enabled;
       "Require approval of the most recent reviewable push" makes someone else approve the last
       push.
     - The agent has its own identity: an account with minimal permissions that proposes changes on
       a branch but does not approve, merge or bypass, and cannot touch branch rules, CI or
       credentials.
   - *What to protect:* the perimeter is every path through which domain data enters, is stored or
     leaves. `CODEOWNERS` covers the domain, the store and the read model, the architecture,
     round-trip and behavior tests, the build configuration and its wrapper (in Maven, `pom.xml`, `.mvn/` and
     `mvnw`: a `-DskipTests` in `.mvn/maven.config` dismantles layer 4 without touching the tests or
     the `pom.xml`), the CI configuration, the `CODEOWNERS` file itself, and the instruction files
     and skills.
   - *Where it ends:* this protects against an agent with limited permissions that makes a mistake
     or obeys a wrong request, inside the perimeter, not against one that controls the whole
     environment. Outside the perimeter, unprotected code can route data around the domain (see the
     red-team experiment). The platform
     forces an approval, not attention, so keep domain changes separate and small. In the
     build-and-maintain experiment, this gate, not the instruction file, kept the customs alive.
6. **The instruction file, short.**
   - *Without it:* the agent learns the rules from the code. Where the code explains itself
     (records, Javadoc, test names and messages), that is enough: in the red-team experiment, agents
     with and without the guide behaved alike. Where it doesn't, every collision costs a round trip.
   - *How:* a map, not a manual. Say what the code doesn't: where the domain types live, why they
     exist, what not to touch and which checks guard it. Say that external data enters only through
     the parse functions, and that "already validated" changes nothing. Keep one file for every
     agent; CLAUDE.md can import AGENTS.md (`@AGENTS.md`).
   - *Where it ends:* it guarantees nothing. It is the only layer that depends entirely on the
     agent's decision, so it explains the other five and does not replace them.

## How to talk about this with the user

- Present types as the part that holds when requests, instructions or upstream systems turn out
  wrong. Don't claim types make agents write better code in general: in neutral conditions the
  evidence above shows no difference.
- Be explicit about cost. More types to write; frameworks that want an empty constructor and setters; failures become
  loud, but are not handled gracefully unless someone designs and tests that.
