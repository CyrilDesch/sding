import { test, expect } from '../fixtures'
import { waitForEventType } from '../helpers/sse'

test.describe('Workflow: create chat', () => {
  test('creates a chat and receives workflow_plan event', async ({ chat: { page, chatId } }) => {
    const planEvent = await waitForEventType(page, chatId, 'workflow_plan', 15_000)
    expect(planEvent).toHaveProperty('steps')
    expect(Array.isArray((planEvent as { steps: unknown[] }).steps)).toBe(true)
  })
})

test.describe('Workflow: first InputRequest', () => {
  test('workflow emits input_request for human requirements', async ({
    chat: { page, chatId },
  }) => {
    const inputEvent = await waitForEventType(page, chatId, 'input_request', 30_000)
    expect(inputEvent).toHaveProperty('prompt')
    expect((inputEvent as { options?: unknown }).options).toEqual(['B2B', 'B2C', 'C2C', 'C2B'])
  })
})
