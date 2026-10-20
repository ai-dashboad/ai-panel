import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('API Keys Page', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/api-keys')
    // Wait for page content to load
    await page.waitForSelector('.api-keys-page', { timeout: 10000 })
  })

  test('should display page header', async ({ page }) => {
    await expect(page.locator('h1:has-text("API Keys")')).toBeVisible()
  })

  test('should display create API key button', async ({ page }) => {
    await expect(page.locator('button:has-text("Create API Key")')).toBeVisible()
  })

  test('should display API keys table', async ({ page }) => {
    // Check table headers
    await expect(page.locator('th:has-text("Name")')).toBeVisible()
    await expect(page.locator('th:has-text("API Key")')).toBeVisible()
    await expect(page.locator('th:has-text("Status")')).toBeVisible()
    await expect(page.locator('th:has-text("Rate Limit")')).toBeVisible()
    await expect(page.locator('th:has-text("Last Used")')).toBeVisible()
    await expect(page.locator('th:has-text("Actions")')).toBeVisible()
  })

  test('should open create API key dialog', async ({ page }) => {
    await page.click('button:has-text("Create API Key")')

    // Check dialog opened
    await expect(page.locator('.el-dialog:has-text("Create API Key")')).toBeVisible()

    // Check form fields
    await expect(page.locator('label:has-text("Key Name")')).toBeVisible()
    await expect(page.locator('label:has-text("Description")')).toBeVisible()
    await expect(page.locator('label:has-text("Rate Limit")')).toBeVisible()
    await expect(page.locator('label:has-text("Expiration")')).toBeVisible()
  })

  test('should validate key name is required', async ({ page }) => {
    await page.click('button:has-text("Create API Key")')

    // Click Create without entering name
    await page.click('.el-dialog button:has-text("Create")')

    // Check validation error
    await expect(page.locator('text=Please enter a key name')).toBeVisible()
  })

  test('should close create dialog on cancel', async ({ page }) => {
    await page.click('button:has-text("Create API Key")')
    await expect(page.locator('.el-dialog')).toBeVisible()

    await page.click('.el-dialog button:has-text("Cancel")')
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })

  test('should fill create API key form', async ({ page }) => {
    await page.click('button:has-text("Create API Key")')

    // Fill form
    await page.fill('input[placeholder*="Production"]', 'My Test API Key')
    await page.fill('textarea', 'Test description for API key')

    // Rate limit input
    const rateLimitInput = page.locator('.el-input-number input')
    if (await rateLimitInput.isVisible()) {
      await rateLimitInput.fill('100')
    }
  })
})
