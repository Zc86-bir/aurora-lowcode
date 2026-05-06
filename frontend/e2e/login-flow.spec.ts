import { test, expect } from '@playwright/test'

test.describe('Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Mock the login API
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'mock-jwt-token-for-e2e',
          tokenType: 'Bearer',
          expiresIn: '3600',
          tenantId: '00000000-0000-0000-0000-000000000001',
          tenantCode: 'default',
          userId: '00000000-0000-0000-0000-000000000001',
          username: 'admin@aurora.dev',
          roles: ['ADMIN'],
        }),
      })
    })
  })

  test('should redirect to login when not authenticated', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })

  test('should login and redirect to dashboard', async ({ page }) => {
    await page.goto('/login')

    // Fill in login form
    await page.fill('input[type="text"]', 'admin@aurora.dev')
    await page.fill('input[type="password"]', 'admin123')

    // Submit
    await page.click('button[type="submit"]')

    // Should redirect to dashboard
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('should show error on invalid credentials', async ({ page }) => {
    // Override mock to return 401
    await page.route('**/api/v1/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid credentials' }),
      })
    })

    await page.goto('/login')
    await page.fill('input[type="text"]', 'wrong@example.com')
    await page.fill('input[type="password"]', 'wrong')
    await page.click('button[type="submit"]')

    // Should show error
    await expect(page.locator('.error')).toBeVisible()
  })
})
