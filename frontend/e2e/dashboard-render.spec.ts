import { test, expect } from '@playwright/test'

test.describe('Dashboard Render', () => {
  test.beforeEach(async ({ page }) => {
    // Set auth token in localStorage
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'mock-jwt-token')
      localStorage.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')
    })
  })

  test('should render dashboard with stat cards', async ({ page }) => {
    await page.goto('/dashboard')

    // Check dashboard title
    await expect(page.locator('h2')).toContainText('Dashboard')

    // Check stat cards exist
    const statCards = page.locator('.stat-card')
    await expect(statCards).toHaveCount(4)
  })

  test('should have working navigation', async ({ page }) => {
    await page.goto('/dashboard')

    // Check nav links exist
    await expect(page.locator('.header-nav a')).toHaveCount(5)
  })

  test('should pass basic a11y checks', async ({ page }) => {
    await page.goto('/dashboard')

    // Check that the page has proper heading hierarchy
    const h2 = page.locator('h2')
    await expect(h2).toBeVisible()

    // Check that buttons have accessible text
    const buttons = page.locator('button')
    const count = await buttons.count()
    for (let i = 0; i < count; i++) {
      const text = await buttons.nth(i).textContent()
      expect(text?.trim()).toBeTruthy()
    }
  })
})
