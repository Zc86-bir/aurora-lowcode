import { test, expect } from '@playwright/test'

const payload = {
  sub: '00000000-0000-0000-0000-000000000001',
  username: 'admin@aurora.dev',
  roles: 'ADMIN',
  exp: Math.floor(Date.now() / 1000) + 3600,
}

const encodedPayload = Buffer.from(JSON.stringify(payload)).toString('base64')
const token = `header.${encodedPayload}.signature`

test.describe('Online Low-Code smoke', () => {
  test('loads the authenticated online hub entry with key links visible', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate((authToken: string) => {
      localStorage.setItem('auth_token', authToken)
      localStorage.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')
    }, token)

    await page.goto('/online')

    await expect(page.locator('main').getByRole('heading', { name: 'Online Low-Code' })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Online Forms' })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Online Reports' })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Online Code Generator' })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Online Dashboards' })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Online Naming Rules' })).toBeVisible()
    await expect(page.locator('main').getByRole('link', { name: 'Online Validation Rules' })).toBeVisible()

    await page.locator('main').getByRole('link', { name: 'Online Code Generator' }).click()
    await expect(page.locator('main').getByRole('heading', { name: 'Code Generator' })).toBeVisible()

    await page.goto('/online')
    await page.locator('main').getByRole('link', { name: 'Online Dashboards' }).click()
    await expect(page.locator('main').getByRole('heading', { name: 'Online Dashboards' })).toBeVisible()

    await page.goto('/online')
    await page.locator('main').getByRole('link', { name: 'Online Naming Rules' }).click()
    await expect(page.locator('main').getByRole('heading', { name: 'Online Naming Rules' })).toBeVisible()

    await page.goto('/online')
    await page.locator('main').getByRole('link', { name: 'Online Validation Rules' }).click()
    await expect(page.locator('main').getByRole('heading', { name: 'Online Validation Rules' })).toBeVisible()
  })
})
