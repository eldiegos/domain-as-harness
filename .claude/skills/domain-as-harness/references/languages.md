# Domain as Harness by language

The rule is the same everywhere. What changes is how you build a type that cannot be invalid,
how you model named states, how you restrict who can construct a type, and which **smuggling
routes** let objects in without passing the rules. Read only the section you need. Version-specific
details are marked; check them against the version the project uses.

Every language section answers the same five questions. For a language not listed, answer them
before writing domain types:

1. **Value type:** what is the immutable type whose only construction path runs the rules?
2. **Border post:** how does a parse function return "value or reasons" without exceptions?
3. **Named states:** what is the closed sum type, and does the compiler (or type checker) report a
   missing case?
4. **Who can construct:** how do you make a type constructible only by its factory (private
   constructor, module visibility, unexported fields)?
5. **Smuggling routes:** what creates or mutates instances without the constructor? Typical
   culprits are deserializers, ORMs, reflection, copy/with helpers, default or zero values, and
   casts.

Contents: [Java](#java) · [Kotlin](#kotlin) · [C#](#c) · [TypeScript](#typescript) ·
[Python](#python) · [Go](#go) · [Rust](#rust) · [Swift](#swift)

---

## Java

Requires 16+ for records, 17+ for sealed types and 21+ for pattern-matching `switch`.

```java
public record Email(String value) {
    private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    public Email {                                  // invariant: throws
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("invalid email");
    }
    public static Optional<Email> parse(String raw) { // border: returns a result
        if (raw == null) return Optional.empty();
        String s = raw.strip();
        return FORMAT.matcher(s).matches() ? Optional.of(new Email(s)) : Optional.empty();
    }
}
```

- **Named states:** `sealed interface Order permits DraftOrder, SubmittedOrder {}` with record
  variants. A `switch` expression without `default` fails to compile when a variant is missing.
  Code compiled against an older hierarchy throws `MatchException` at run time if it meets a new
  variant, so recompile the consumers.
- **Identity and collections:** a record's `equals` and `hashCode` compare every component. For an
  entity, override them to compare the id, and in tests compare the data (the fields or
  `toString()`), not `equals`. Copy lists in the compact constructor with `List.copyOf`, which also
  rejects `null` elements; otherwise the record is immutable only on the surface.
- **Architecture tests:** ArchUnit, run by the build in CI (see layer 4 in SKILL.md).
- **Who can construct:** a public record's canonical constructor must be public (JLS §8.10.4), so a
  record cannot restrict who creates it. For proof types (`ConfirmedBooking`), use a `final class`
  with a private constructor and a static factory, or a package-private type.
- **Smuggling routes:**
  - JavaBeans (no-arg constructor plus setters).
  - Java serialization of ordinary classes skips constructors. Records are deserialized through the
    canonical constructor.
  - Reflection on final fields: `Field.set` refuses record fields; ordinary classes allow it, and
    JDK 26+ only warns. `sun.misc.Unsafe` cannot write a record's fields either, but it can write a
    class's; on the class path, `MethodHandles.privateLookupIn` opens a class's private constructor
    without `setAccessible`. Framework wrappers do the same without your code naming any of them:
    in Spring, `ReflectionUtils.makeAccessible`, `BeanUtils.instantiateClass`, `DirectFieldAccessor`,
    Objenesis and CGLIB. Ban all of them in the architecture tests, over all compiled code.
  - Lombok `@Data` / `@Setter` on domain types.
  - Builders that allow `build()` with missing fields.
- **Persistence:** JPA entities need a public or protected no-arg constructor and cannot be records.
  Either keep the entity at the boundary and convert, or give the entity a protected no-arg
  constructor for the provider only, plus a validating public constructor and no setters. Jakarta
  Persistence 3.2 accepts records as `@Embeddable`, so `Email` can live inside an entity. Jackson
  deserializes records through the canonical constructor; for classes with setters it bypasses your
  rules.

## Kotlin

```kotlin
@JvmInline
value class Email private constructor(val value: String) {
    init { require(FORMAT.matches(value)) { "invalid email" } }
    companion object {
        private val FORMAT = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
        fun parse(raw: String?): Email? =
            raw?.trim()?.takeIf(FORMAT::matches)?.let(::Email)
    }
}
```

- **Named states:** `sealed interface Order` with `data class` or `object` variants. `when` used
  as an expression must be exhaustive.
- **Who can construct:** private constructor plus a factory in the `companion object`. A data
  class's generated `copy()` has historically stayed public even with a private constructor. Kotlin
  2.0.20 added `@ConsistentCopyVisibility` to opt in to matching visibility, and later releases make
  it the default. Keep invariants in `init`, which `copy()` runs. For proof types where authority
  matters, avoid `data class` or opt in.
- **Smuggling routes:** `var` properties, `lateinit`, Java interop setters, and reflection-based
  mappers. Check whether your serializer (kotlinx.serialization, Jackson) runs `init`, with a test
  that decodes invalid JSON.

## C#

```csharp
public sealed record Email
{
    public string Value { get; }                     // get-only: `with` cannot set it
    private Email(string value) => Value = value;
    public static Email? Parse(string? raw) =>
        raw is not null && Format.IsMatch(raw.Trim()) ? new Email(raw.Trim()) : null;
    private static readonly Regex Format = new(@"^[^@\s]+@[^@\s]+\.[^@\s]+$");
}
```

- **The `with` trap:** a `with` expression calls the record's copy constructor, then sets the
  listed properties, without running your constructor. Positional records
  (`record Email(string Value)`) generate `init` properties, so `email with { Value = "bad" }`
  compiles and skips validation. Use get-only properties, or validate inside the `init` accessor.
  Positional `record struct` generates read-write properties.
- **Structs:** `default(T)` and arrays of structs skip constructors. Prefer reference types for
  value objects, or make the default value explicitly invalid and check it.
- **Named states:** an abstract record with a private constructor and nested `sealed record`
  variants. The compiler does not know the hierarchy is closed, so a `switch` expression needs a
  `_ => throw ...` arm. Keep that arm, and cover the variants with tests.
- **Persistence and serialization:** EF Core can bind constructor parameters and use private setters
  or backing fields, and value converters or owned types can map value objects. System.Text.Json
  calls a parameterized constructor when one is used for deserialization (`[JsonConstructor]`), but
  sets public setters directly. Test that invalid JSON does not produce a domain object.

## TypeScript

TypeScript types are structural and erased at run time, so the border post must exist at run time
too.

```ts
declare const brand: unique symbol;
export type Email = string & { readonly [brand]: "Email" };

const FORMAT = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
export function parseEmail(raw: unknown): Email | EmailError {
  if (typeof raw !== "string") return { kind: "not-a-string" };
  const s = raw.trim();
  return FORMAT.test(s) ? (s as Email) : { kind: "bad-format", input: s };
}
export type EmailError = { kind: "not-a-string" } | { kind: "bad-format"; input: string };
```

- The only `as Email` lives in this module. Anywhere else, a type assertion to a domain type is
  smuggling. Forbid it with a lint rule or review. A class with a `private constructor` and
  `static parse` is the alternative when you want an `instanceof` check at run time.
- **Border post for structures:** use a schema library (Zod, Valibot and similar) with
  `parse`/`safeParse` at the edge, then map to domain types. Treat `JSON.parse`, `fetch().json()`
  and `req.body` as `unknown`, not `any`.
- **Named states:** discriminated unions (`{ status: "draft"; ... } | { status: "submitted"; ... }`).
  Make a `switch` exhaustive with `const unreachable: never = state` in the default branch, and
  enable `strict` in `tsconfig`.
- **Smuggling routes:**
  - `as`, `any`, the non-null `!`, `@ts-ignore` / `@ts-expect-error`.
  - Building domain objects by spreading DTOs.
  - Test fixtures cast to domain types.
  - Mutating objects that are only `readonly` at compile time (`Object.freeze` if run-time
    immutability matters).
- `tsc --noEmit` in CI is part of the harness. Without it the types are only suggestions.

## Python

Python checks nothing until run time, so you get two layers: run-time validation in constructors,
and a static type checker (mypy, pyright) in CI that plays the compiler's role. Without the type
checker in CI, named states and destination types are documentation, not customs. Say so when a
project lacks it.

```python
from dataclasses import dataclass
import re

_FORMAT = re.compile(r"[^@\s]+@[^@\s]+\.[^@\s]+")

@dataclass(frozen=True, slots=True)
class Email:
    value: str
    def __post_init__(self) -> None:                 # invariant: raises
        if not _FORMAT.fullmatch(self.value):
            raise ValueError(f"invalid email: {self.value!r}")

    @classmethod
    def parse(cls, raw: object) -> "Email | EmailError":   # border: returns a result
        if not isinstance(raw, str) or not _FORMAT.fullmatch(raw.strip()):
            return EmailError(raw)
        return cls(raw.strip())
```

- **Named states:** a union of frozen dataclasses (`Order = DraftOrder | SubmittedOrder`) with
  `match` and `typing.assert_never` in the fallback, checked by mypy or pyright. Functions such as
  `pay(order: SubmittedOrder)` make passing a draft a type error.
- **`typing.NewType` is not a border post:** it does no run-time check and anyone can call
  `Email("x")`. Pair it with a parse function, or use a class with validation.
- **Pydantic** works well at the border (`Model.model_validate(data)`, `frozen=True`). Note that
  `model_construct()` skips validation, so it is smuggling for domain types.
- **Smuggling routes:**
  - `object.__setattr__` on frozen dataclasses.
  - Mutable fields (`list`, `dict`) inside "frozen" objects; use tuples or `frozenset`.
  - ORM models (SQLAlchemy, Django) used as domain objects, since they are mutable and assigned
    field by field.
  - `dataclasses.replace` runs `__init__` and `__post_init__`, so it is safe.

## Go

```go
package email

type Email struct{ value string } // unexported field: no Email{value: "x"} outside the package

func Parse(raw string) (Email, error) {
	s := strings.TrimSpace(raw)
	if !format.MatchString(s) {
		return Email{}, fmt.Errorf("invalid email %q", raw)
	}
	return Email{value: s}, nil
}
func (e Email) String() string { return e.value }
func (e Email) IsZero() bool   { return e.value == "" }
```

- **Zero values:** other packages can still write `email.Email{}`, which gives an empty value. Treat
  the zero value as "absent": check `IsZero()` where it matters, or design the type so its zero
  value is meaningful. Exported struct fields (`Customer{Email: "x"}`) mean there is no customs at
  all.
- **Named states:** an interface with an unexported marker method (`isOrderState()`) closes the set
  to the package. Go does not check `switch` exhaustiveness, so add a linter to CI (for example
  `go-sumtype` for sealed interfaces, or `exhaustive` for enum-like constants) and a `default` that
  panics.
- **Serialization:** `encoding/json` sets exported fields directly and ignores unexported ones.
  Implement `UnmarshalJSON` on the domain type and call `Parse` from it, or decode into a DTO and
  convert.
- **Smuggling routes:** exported fields, zero values, `reflect` and `unsafe`, and decoding JSON or
  database rows straight into domain structs.

## Rust

```rust
pub mod email {
    #[derive(Debug, Clone, PartialEq, Eq)]
    pub struct Email(String); // private field outside this module

    impl TryFrom<String> for Email {
        type Error = EmailError;
        fn try_from(raw: String) -> Result<Self, Self::Error> {
            let s = raw.trim();
            if is_valid(s) { Ok(Email(s.to_owned())) } else { Err(EmailError::BadFormat(raw)) }
        }
    }
    impl Email { pub fn as_str(&self) -> &str { &self.0 } }
    // EmailError and is_valid omitted
}
```

- **Named states:** `enum Order { Draft(DraftOrder), Submitted(SubmittedOrder) }`; `match` is
  exhaustive. Functions take the variant's struct (`fn pay(order: SubmittedOrder)`).
- **Serde bypass:** `#[derive(Deserialize)]` on a domain type builds it field by field without your
  constructor. Use `#[serde(try_from = "String")]` (and `into` for `Serialize`) so deserialization
  goes through `TryFrom`.
- **Smuggling routes:** `pub` fields, `#[derive(Default)]` producing invalid values, `unsafe` and
  `transmute`, and test helpers that construct through private access.

## Swift

```swift
struct Email: Hashable {
    let value: String
    init?(_ raw: String) {                     // border: failable init (or throwing init)
        let s = raw.trimmingCharacters(in: .whitespaces)
        guard s.range(of: #"^[^@\s]+@[^@\s]+\.[^@\s]+$"#, options: .regularExpression) != nil
        else { return nil }
        value = s
    }
}
```

- Define the validating `init` inside the type's main declaration. That suppresses the synthesized
  memberwise initializer, which would otherwise accept any string.
- **Named states:** `enum` with associated values; `switch` is exhaustive.
- **Codable bypass:** the synthesized `init(from:)` assigns stored properties without calling your
  initializer. Implement `init(from decoder:)` that decodes the raw value and runs the same check,
  throwing `DecodingError.dataCorrupted` on failure.
- **Smuggling routes:** `var` properties, memberwise initializers, and `Codable` synthesis.
