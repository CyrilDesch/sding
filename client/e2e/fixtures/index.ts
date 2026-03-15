import { test as base, type Page } from '@playwright/test'
import { type TestUser, createTestUser, configureLocalLlm, createChat } from './api'

interface E2EFixtures {
  authenticatedUser: TestUser & { page: Page }
  chat: TestUser & { page: Page; chatId: string }
}

export const test = base.extend<E2EFixtures>({
  authenticatedUser: async ({ request, page }, use) => {
    const user = await createTestUser(request)
    await configureLocalLlm(request, user.cookieHeader)

    const cookieParts = user.cookieHeader.split(';')[0]!.split('=')
    await page.context().addCookies([
      {
        name: cookieParts[0]!.trim(),
        value: cookieParts.slice(1).join('=').trim(),
        domain: 'localhost',
        path: '/',
      },
    ])

    await use({ ...user, page })
  },

  chat: async ({ request, page }, use) => {
    const user = await createTestUser(request)
    await configureLocalLlm(request, user.cookieHeader)

    const cookieParts = user.cookieHeader.split(';')[0]!.split('=')
    await page.context().addCookies([
      {
        name: cookieParts[0]!.trim(),
        value: cookieParts.slice(1).join('=').trim(),
        domain: 'localhost',
        path: '/',
      },
    ])

    const chatId = await createChat(request, user.cookieHeader)
    await use({ ...user, page, chatId })
  },
})

export { expect } from '@playwright/test'
