import { test, expect } from '@playwright/test'

test.describe('AI platform smoke', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('Username').fill('admin@aurora.dev')
    await page.getByPlaceholder('Password').fill('admin123')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await page.waitForURL('**/dashboard')
  })

  test('loads the ai platform hub and key subpages', async ({ page }) => {
    await page.goto('/ai')

    await expect(page).toHaveURL(/\/ai$/)
    await expect(page.locator('main').getByRole('heading', { name: 'AI Platform' })).toBeVisible()
    await expect(page.locator('main').getByText('Model Management')).toBeVisible()
    await expect(page.locator('main').getByText('AI Assistant')).toBeVisible()

    await page.goto('/ai/models')
    await expect(page).toHaveURL(/\/ai\/models$/)
    await expect(page.locator('main').getByRole('heading', { name: 'AI Model Configuration' })).toBeVisible()
    await expect(page.locator('main').getByRole('button', { name: /Add Model/ })).toBeVisible()

    await page.goto('/ai/assistant')
    await expect(page).toHaveURL(/\/ai\/assistant$/)
    await expect(page.locator('main').getByRole('heading', { name: 'AI Assistant' })).toBeVisible()
    await expect(page.locator('main').getByText('AI Copilot')).toBeVisible()

    await page.goto('/ai/generation')
    await expect(page).toHaveURL(/\/ai\/generation$/)
    await expect(page.locator('main').getByRole('heading', { name: 'AI Generation' })).toBeVisible()
    await expect(page.locator('main').getByText('Route AI-powered app generation through the AI platform', { exact: false })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Open capability' })).toBeVisible()

    await page.goto('/ai/knowledge')
    await expect(page).toHaveURL(/\/ai\/knowledge$/)
    await expect(page.locator('main h2')).toHaveText('Knowledge Q&A')
    await expect(page.locator('main').getByText('Status')).toBeVisible()
    await expect(page.locator('main').getByText('Next milestone')).toBeVisible()
    await expect(page.locator('main').getByText('Define document-source, retrieval, and answer-evaluation workflows.')).toBeVisible()
  })
})
