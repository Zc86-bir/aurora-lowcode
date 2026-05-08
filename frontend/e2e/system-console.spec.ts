import { test, expect } from '@playwright/test'

const payload = {
  sub: '00000000-0000-0000-0000-000000000001',
  username: 'admin@aurora.dev',
  roles: 'ADMIN',
  exp: Math.floor(Date.now() / 1000) + 3600,
}

const encodedPayload = Buffer.from(JSON.stringify(payload)).toString('base64')
const token = `header.${encodedPayload}.signature`

test.describe('System Console smoke', () => {
  test('loads the authenticated console entry with system navigation visible', async ({ page }) => {
    await page.goto('/login')
    await page.evaluate((authToken: string) => {
      localStorage.setItem('auth_token', authToken)
      localStorage.setItem('tenant_id', '00000000-0000-0000-0000-000000000001')
    }, token)
    await page.goto('/workbench')
    await expect(page.locator('main').getByRole('heading', { name: 'Workbench' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'System Management' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Users' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Roles' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Tenant Management' })).toBeVisible()
  })
})
