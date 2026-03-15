import { type SseEvent, SSE_EVENT_TYPES } from './types'

/**
 * Parse a raw SSE data string into a typed SseEvent.
 * Throws if the JSON is invalid or the event type is unknown.
 */
export function parseSseEvent(data: string): SseEvent {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const json = JSON.parse(data) as Record<string, any>

  if (typeof json.type !== 'string') {
    throw new Error(`SSE event missing "type" field: ${data}`)
  }

  if (!SSE_EVENT_TYPES.includes(json.type as (typeof SSE_EVENT_TYPES)[number])) {
    throw new Error(`Unknown SSE event type: ${json.type}`)
  }

  // The JSON shape from the Scala server matches our TypeScript types exactly.
  // source_node is null when absent (not missing/undefined).
  return json as SseEvent
}
