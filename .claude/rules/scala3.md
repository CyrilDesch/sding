---
paths:
  - "**/*.scala"
---
# Scala 3 Best Practices

## Domain Modeling

### Prefer strong types over primitives

**NEVER** use raw `String`, `Int`, or `Boolean` where a domain concept is involved. Wrap them in newtypes.

### Newtypes for zero-cost wrappers

Use `opaque type` (Scala 3 native). Avoid `case class` wrappers (they allocate and expose `.copy`).

```scala
// ✅ Correct - opaque type, zero allocation
opaque type UserId = Long
object UserId:
  inline def apply(v: Long): UserId = v
  extension (id: UserId) inline def value: Long = id

// ❌ Wrong - case class wrapper allocates and leaks .copy
case class UserId(value: Long)
```

### Eliminate boolean blindness

Replace boolean parameters with small ADTs:

```scala
// ❌ Wrong
def setActive(user: User, flag: Boolean): User

// ✅ Correct
enum AccountStatus:
  case Active, Suspended
```

### Smart constructors for external input

Return `Either` or `ValidatedNel` from constructors that accept untrusted input:

```scala
opaque type Email = String
object Email:
  def from(s: String): Either[String, Email] =
    if s.contains("@") then Right(s) else Left(s"Invalid email: $s")
```

## Impure Calls and Effects

**NEVER** call impure functions directly. Always wrap in `Sync.delay` or `Sync.blocking`:

```scala
// ✅ Correct
val getHostname: IO[String] = IO(InetAddress.getLocalHost.getHostName)

// ❌ Wrong
val hostname = InetAddress.getLocalHost.getHostName
```

## No `var` in Pure Modules

Use `foldLeft`, `foldRight`, or `boundary.break` instead.

## Pattern Matching in For-Comprehensions

Pattern match directly in `<-` bindings — no intermediate binding:

```scala
// ✅ Correct
for
    (start, end, result) <- fetchData().withStartEnd
yield process(result)

// ❌ Wrong
for
    response             <- fetchData().withStartEnd
    (start, end, result) = response
yield process(result)
```

## Type Inference

Prefer explicit types for public APIs, allow inference for local vals.

## Avoid Anonymous Tuples

**NEVER** use anonymous tuples `(A, B, C)`. Use named tuples (Scala 3.5+) or case classes:

```scala
// ❌ Wrong
def getStatus: (String, Style) = ("✓", greenStyle)

// ✅ Correct (named tuple)
def getStatus: (symbol: String, style: Style) = (symbol = "✓", style = greenStyle)

// ✅ Correct (case class for reusable types)
case class Status(symbol: String, style: Style)
```

## Typeclasses

Use `given`/`using` — never `implicit def`/`implicit val`.

## Collections

**NEVER** use `Seq` in public interfaces:

| Context | Use |
|---------|-----|
| Indexed, fast random access | `Vector` |
| Cats / http4s accumulation | `Chain` |
| Streaming / unbounded | `fs2.Stream` |
| Ordered, eager list | `List` |

Avoid `Array` except at Java API boundaries.

## JSON Return Types

On public helpers returning JSON, use `io.circe.Json` not `String`.

## Uninitialized Variables

Use `scala.compiletime.uninitialized` — `= _` is not valid in Scala 3.

## Compilation Verification

**ALWAYS verify compilation after Scala changes using Metals MCP tools.**

- After editing a single file → `compile-file`
- After changes spanning multiple files → `compile-module`
- Fix compilation errors before proceeding.

## Scala-CLI Scripts

```scala
//> using scala 3.8.1
//> using jvm 21
```
