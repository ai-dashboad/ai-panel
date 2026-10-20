import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
  })

  test('should redirect to dashboard from root', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL('/dashboard')
  })

  test('should navigate between pages', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    // Navigate to API Keys
    await page.locator('.el-menu-item').filter({ hasText: 'API Keys' }).click()
    await expect(page).toHaveURL('/api-keys')
    await expect(page.locator('h1').filter({ hasText: 'API Keys' })).toBeVisible()

    // Navigate to Providers
    await page.locator('.el-menu-item').filter({ hasText: 'Providers' }).click()
    await expect(page).toHaveURL('/providers')
    await expect(page.locator('h1').filter({ hasText: 'Provider Configuration' })).toBeVisible()

    // Navigate to Models
    await page.locator('.el-menu-item').filter({ hasText: 'Models' }).click()
    await expect(page).toHaveURL('/models')
    await expect(page.locator('h1').filter({ hasText: 'Available Models' })).toBeVisible()

    // Navigate to Usage (menu shows "Usage & Billing")
    await page.locator('.el-menu-item').filter({ hasText: 'Usage' }).click()
    await expect(page).toHaveURL('/usage')
    await expect(page.locator('h1').filter({ hasText: 'Usage' })).toBeVisible()

    // Navigate to Settings
    await page.locator('.el-menu-item').filter({ hasText: 'Settings' }).click()
    await expect(page).toHaveURL('/settings')
    await expect(page.locator('h1').filter({ hasText: 'Settings' })).toBeVisible()

    // Navigate back to Dashboard
    await page.locator('.el-menu-item').filter({ hasText: 'Dashboard' }).click()
    await expect(page).toHaveURL('/dashboard')
  })

  test('should display sidebar menu items', async ({ page }) => {
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    await expect(page.locator('.el-menu-item').filter({ hasText: 'Dashboard' })).toBeVisible()
    await expect(page.locator('.el-menu-item').filter({ hasText: 'API Keys' })).toBeVisible()
    await expect(page.locator('.el-menu-item').filter({ hasText: 'Providers' })).toBeVisible()
    await expect(page.locator('.el-menu-item').filter({ hasText: 'Models' })).toBeVisible()
    await expect(page.locator('.el-menu-item').filter({ hasText: 'Usage' })).toBeVisible()
    await expect(page.locator('.el-menu-item').filter({ hasText: 'Settings' })).toBeVisible()
  })

  test('should highlight active menu item', async ({ page }) => {
    await page.goto('/api-keys')
    await page.waitForLoadState('networkidle')

    // The active menu item should have a distinct style
    const activeItem = page.locator('.el-menu-item.is-active')
    await expect(activeItem).toBeVisible()
  })
})

test.describe('404 Page', () => {
  test('should display 404 for unknown routes', async ({ page }) => {
    // Set up auth state first so we're not redirected to login
    await setupAuthState(page)
    await page.goto('/unknown-page-xyz')
    await expect(page.locator('h1').filter({ hasText: '404' })).toBeVisible()
  })
})

test.describe('Auth Guards', () => {
  test('should redirect to login when not authenticated', async ({ page }) => {
    // Visit protected route without auth
    await page.goto('/dashboard')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('should redirect to dashboard when authenticated user visits login', async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/login')

    // Should redirect to dashboard
    await expect(page).toHaveURL('/dashboard')
  })
})
