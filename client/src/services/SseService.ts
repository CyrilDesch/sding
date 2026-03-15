import { Chunk, Context, Effect, Layer, Option, Stream } from 'effect'
import { parseSseEvent } from '../protocol/parse'
import type { SseEvent } from '../protocol/types'
import { SSE_EVENT_TYPES } from '../protocol/types'
import { SseConnectionError, SseParseError } from './errors'

// ─── Service interface ────────────────────────────────────────────────────────

export interface SseServiceShape {
  readonly connect: (
    chatId: string,
    afterIndex?: number
  ) => Stream.Stream<SseEvent, SseConnectionError | SseParseError>
}

export class SseService extends Context.Tag('SseService')<SseService, SseServiceShape>() {}

// ─── Live implementation ─────────────────────────────────────────────────────

export const SseServiceLive = Layer.succeed(SseService, {
  connect: (
    chatId: string,
    afterIndex = 0
  ): Stream.Stream<SseEvent, SseConnectionError | SseParseError> =>
    Stream.async<SseEvent, SseConnectionError | SseParseError>((emit) => {
      const url = `/api/chat/${chatId}/stream?after=${afterIndex}`
      const es = new EventSource(url)

      SSE_EVENT_TYPES.forEach((evtType) => {
        es.addEventListener(evtType, (e: Event) => {
          const messageEvent = e as MessageEvent<string>
          try {
            const event = parseSseEvent(messageEvent.data)
            void emit(Effect.succeed(Chunk.of(event)))
          } catch {
            void emit(Effect.fail(Option.some(new SseParseError(messageEvent.data))))
          }
        })
      })

      es.onerror = () => {
        void emit(Effect.fail(Option.some(new SseConnectionError())))
      }

      return Effect.sync(() => es.close())
    }),
})
