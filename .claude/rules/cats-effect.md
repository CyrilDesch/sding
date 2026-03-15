---
paths:
  - "**/*.scala"
---
# Cats Effect Best Practices

## Tagless Final Architecture

Structure business code in three layers:

- **Algebra**: `trait X[F[_]]` with business capabilities, no implementation detail, minimal constraints on methods.
- **Interpreter**: provides concrete behavior, holds `Ref`, `Resource`, etc.
- **Program**: composes algebras into business workflows, describes logic only.

```scala
// ✅ Algebra - pure DSL, no constraints on methods
trait ShoppingCart[F[_]]:
  def add(userId: UserId, item: Item): F[Unit]
  def get(userId: UserId): F[CartTotal]

// ✅ Interpreter - constraints live here
final class LiveShoppingCart[F[_]: MonadThrow: Logger] private (
    ref: Ref[F, Map[UserId, List[Item]]]
) extends ShoppingCart[F]:
  def add(userId: UserId, item: Item): F[Unit] = ref.update(...)
  def get(userId: UserId): F[CartTotal]        = ref.get.map(...)

object LiveShoppingCart:
  def make[F[_]: Sync: Logger]: Resource[F, ShoppingCart[F]] =
    Resource.eval(Ref.of[F, Map[UserId, List[Item]]](Map.empty).map(new LiveShoppingCart(_)))
```

### Business algebras are explicit parameters, not implicits

Only lawful typeclasses and small capability traits (`Console`, `Clock`, `Logger`) belong in implicit scope. Business algebras must be explicit:

```scala
// ✅ Correct - algebras are explicit
class CheckoutProgram[F[_]: MonadThrow](
    cart: ShoppingCart[F],
    orders: Orders[F],
    payment: Payment[F]
):
  def checkout(userId: UserId, card: Card): F[OrderId] = ...

// ❌ Wrong - business algebras as implicits
def checkout[F[_]](userId: UserId, card: Card)(using
    cart: ShoppingCart[F],
    orders: Orders[F],
    payment: Payment[F]
): F[OrderId] = ...
```

### Keep algebras free of unnecessary constraints

```scala
// ✅ Correct - no constraints on algebra
trait Orders[F[_]]:
  def create(userId: UserId, items: List[Item]): F[OrderId]

// ❌ Wrong - algebra leaks implementation details
trait Orders[F[_]: Async]:
  def create(userId: UserId, items: List[Item]): F[OrderId]
```

## Resource Management

**ALWAYS** use `Resource` for lifecycle management. Never manually manage open/close.

| Pattern | Use when |
|---------|----------|
| `Resource.make(acquire)(release)` | Need cleanup on finalization |
| `Resource.eval(fa)` | No cleanup needed, just lift F[A] |
| `Resource.onFinalize(cleanup)` | Add cleanup without acquisition |

## For-Comprehension Style

```scala
// ✅ Correct - aligned arrows
for
  config  <- loadConfig
  client  <- createClient(config)
  result  <- client.fetch(url)
  _       <- Logger[F].info(s"Fetched: $result")
yield result
```

Pattern match directly in `<-` — no intermediate binding needed.

## Background Fibers with Supervisor

**ALWAYS** use `Supervisor` for background work, never raw `.start`.

## Ref and State

Use `Ref.modify` for atomic read-modify-write. Never: `get` then `set`.

| Scenario | Use |
|---|---|
| Purely sequential, single-fiber | `cats.data.State[S, A]` or recursive function |
| Shared mutable state across fibers | `Ref[F, S]` |
| Shared state with stream notifications | `SignallingRef[F, S]` |

## Error Handling

- Use `F[A]` + `MonadThrow` by default. Only use `F[Either[E, A]]` when callers **must** branch.
- Custom domain errors extend `NoStackTrace`.
- `handleErrorWith` for recovery — never swallow all errors.
- `ValidatedNel` for multi-field user input validation at the boundary.

## Parallel Execution

- `parTraverse` / `parTraverse_` for bounded parallelism.
- `parTupled` for independent effects.

## Blocking Work

**NEVER** call blocking JVM operations on the compute thread pool:

```scala
// ✅ Correct
Sync[F].blocking(Files.readString(path))
// ❌ Wrong
Sync[F].delay(Files.readString(path))
```

## Cancellation Safety

Use `uncancelable` with `poll` — never wrap everything in `uncancelable`.

## Anti-patterns

| Anti-pattern | Correct approach |
|--------------|------------------|
| `.unsafeRunSync()` in production | Use `IOApp` |
| Manual `try/finally` | `Resource.make` |
| `var` for state | `Ref` or `SignallingRef` |
| `.start` without supervision | `Supervisor` |
| `Thread.sleep` | `Temporal[F].sleep` |
| `println` for logging | `Logger[F].info` |
| `Seq` in public interfaces | `List`, `Vector`, `Chain`, or `fs2.Stream` |
| `OptionT`/`EitherT` in public APIs | `F[Option[A]]` / `F[Either[E, A]]` |
| Blocking call on compute pool | `Sync[F].blocking(...)` |
| `uncancelable` without `poll` | `uncancelable { poll => ... poll(interruptible) ... }` |
