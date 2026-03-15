import { test, expect } from '../fixtures'

test.describe('Auth: register and login', () => {
  test('registers and lands on home page', async ({ authenticatedUser: { page } }) => {
    await page.goto('/')
    // Authenticated users reach the app (not the login page)
    await expect(page).not.toHaveURL(/login/)
  })

  test('unauthenticated users are redirected to login', async ({ page }) => {
    await page.goto('/')
    // Without a cookie, the app shows the login page
    await expect(page.locator('h1')).toContainText(/Welcome back/i)
  })
})
