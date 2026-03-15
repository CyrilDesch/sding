import { Context, Effect, Layer, SubscriptionRef } from 'effect'
import { type Page, parsePage, toHash } from '../protocol/types'

// ─── Service interface ────────────────────────────────────────────────────────

export interface RouterServiceShape {
  readonly currentPage: SubscriptionRef.SubscriptionRef<Page>
  readonly navigate: (page: Page) => Effect.Effect<void>
}

export class RouterService extends Context.Tag('RouterService')<
  RouterService,
  RouterServiceShape
>() {}

// ─── Live implementation ─────────────────────────────────────────────────────

export const RouterServiceLive = Layer.effect(
  RouterService,
  Effect.gen(function* () {
    const initialPage = parsePage(window.location.hash)
    const currentPage = yield* SubscriptionRef.make<Page>(initialPage)

    // Sync to browser hash changes (e.g., back button, AuthService redirects).
    yield* Effect.sync(() => {
      window.addEventListener('hashchange', () => {
        Effect.runFork(SubscriptionRef.set(currentPage, parsePage(window.location.hash)))
      })
    })

    return {
      currentPage,
      navigate: (page: Page) =>
        Effect.sync(() => {
          window.location.hash = toHash(page)
          // hashchange fires asynchronously; update the ref immediately for
          // same-tick consumers.
          Effect.runFork(SubscriptionRef.set(currentPage, page))
        }),
    }
  })
)
