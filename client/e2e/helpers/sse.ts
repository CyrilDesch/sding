import type { Page } from '@playwright/test'
import { API_URL } from '../fixtures/api'

export interface SseEvent {
  type: string
  [key: string]: unknown
}

export async function waitForEventType(
  page: Page,
  chatId: string,
  eventType: string,
  timeoutMs = 120_000
): Promise<SseEvent> {
  return page.evaluate(
    ({ url, eventType: evtType, timeoutMs: tms }) => {
      return new Promise<SseEvent>((resolve, reject) => {
        const es = new EventSource(url)
        const timer = setTimeout(() => {
          es.close()
          reject(new Error(`Timeout waiting for event: ${evtType}`))
        }, tms)

        es.addEventListener(evtType, (e: Event) => {
          clearTimeout(timer)
          es.close()
          resolve(JSON.parse((e as MessageEvent<string>).data) as SseEvent)
        })

        es.onerror = () => {
          clearTimeout(timer)
          es.close()
          reject(new Error('SSE connection error'))
        }
      })
    },
    { url: `${API_URL}/api/chat/${chatId}/stream`, eventType, timeoutMs }
  ) as Promise<SseEvent>
}
