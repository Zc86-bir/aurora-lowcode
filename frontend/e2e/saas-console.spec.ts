import { test, expect } from '@playwright/test'

test.describe('SaaS Console', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'mock-jwt-token-for-e2e')
      localStorage.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')
    })
    // Mock metadata API for all categories
    await page.route('**/api/v1/metadata*', async (route) => {
      const url = route.request().url()
      const category = url.includes('category=form') ? 'form'
        : url.includes('category=report') ? 'report'
        : url.includes('category=workflow') ? 'workflow'
        : 'form'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: '1', name: `Sample ${category}`, type: category.toUpperCase(), version: 1, status: 'PUBLISHED', createdAt: new Date().toISOString() },
        ]),
      })
    })
    await page.goto('/dashboard')
  })

  test('should navigate to all pages without crashing', async ({ page }) => {
    const navLinks = page.locator('.header-nav a')
    const count = await navLinks.count()
    for (let i = 0; i < count; i++) {
      const text = await navLinks.nth(i).textContent()
      await navLinks.nth(i).click()
      await page.waitForTimeout(300)
      // Verify no crash — page should have content
      await expect(page.locator('.layout-main')).toBeVisible()
    }
  })

  test('should display Copilot trigger button', async ({ page }) => {
    await page.goto('/dashboard')
    const copilotBtn = page.locator('.copilot-trigger')
    await expect(copilotBtn).toBeVisible()
  })

  test('should open Copilot panel on click', async ({ page }) => {
    await page.goto('/dashboard')
    await page.locator('.copilot-trigger').click()
    await expect(page.locator('.copilot-panel')).toBeVisible()
    await expect(page.locator('.copilot-input-area input')).toBeVisible()
  })

  test('should show form list on Forms page', async ({ page }) => {
    await page.goto('/forms')
    await page.waitForTimeout(500)
    // Should either show data table or empty state
    const hasTable = await page.locator('.data-table').count()
    const hasEmpty = await page.locator('.empty-state').count()
    expect(hasTable + hasEmpty).toBeGreaterThan(0)
  })

  test('should show report list on Reports page', async ({ page }) => {
    await page.goto('/reports')
    await page.waitForTimeout(500)
    const hasTable = await page.locator('.data-table').count()
    const hasEmpty = await page.locator('.empty-state').count()
    expect(hasTable + hasEmpty).toBeGreaterThan(0)
  })

  test('should show workflow list on Workflows page', async ({ page }) => {
    await page.goto('/workflows')
    await page.waitForTimeout(500)
    const hasTable = await page.locator('.data-table').count()
    const hasEmpty = await page.locator('.empty-state').count()
    expect(hasTable + hasEmpty).toBeGreaterThan(0)
  })

  test('should render Settings with all tabs', async ({ page }) => {
    await page.goto('/settings')
    await expect(page.locator('.settings-tabs')).toBeVisible()
    const tabs = page.locator('.tab-btn')
    await expect(tabs).toHaveCount(3)
  })

  test('should switch Settings tabs', async ({ page }) => {
    await page.goto('/settings')
    const tabs = page.locator('.tab-btn')
    for (let i = 0; i < 3; i++) {
      await tabs.nth(i).click()
      await page.waitForTimeout(200)
      await expect(tabs.nth(i)).toHaveClass(/active/)
    }
  })
})
