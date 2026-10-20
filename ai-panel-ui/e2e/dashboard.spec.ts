import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('Dashboard Page', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/dashboard')
  })

  test('should display dashboard header', async ({ page }) => {
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible()
  })

  test('should display stat cards', async ({ page }) => {
    // Check all 4 stat cards are visible (use .stat-label to avoid matching sidebar menu)
    await expect(page.locator('.stat-label:has-text("API Keys")')).toBeVisible()
    await expect(page.locator('.stat-label:has-text("Total Requests")')).toBeVisible()
    await expect(page.locator('.stat-label:has-text("Tokens Used")')).toBeVisible()
    await expect(page.locator('.stat-label:has-text("Balance")')).toBeVisible()
  })

  test('should display usage trend chart', async ({ page }) => {
    await expect(page.locator('text=Usage Trend')).toBeVisible()

    // Check chart period selector
    await expect(page.locator('text=7 Days')).toBeVisible()
    await expect(page.locator('text=30 Days')).toBeVisible()
  })

  test('should display model distribution chart', async ({ page }) => {
    await expect(page.locator('text=Model Distribution')).toBeVisible()
  })

  test('should display recent API calls table', async ({ page }) => {
    await expect(page.locator('text=Recent API Calls')).toBeVisible()

    // Check table headers
    await expect(page.locator('th:has-text("Time")')).toBeVisible()
    await expect(page.locator('th:has-text("Model")')).toBeVisible()
    await expect(page.locator('th:has-text("Tokens")')).toBeVisible()
    await expect(page.locator('th:has-text("Latency")')).toBeVisible()
    await expect(page.locator('th:has-text("Status")')).toBeVisible()
  })

  test('should have link to usage page', async ({ page }) => {
    const viewAllLink = page.locator('a:has-text("View All")')
    await expect(viewAllLink).toBeVisible()
    await expect(viewAllLink).toHaveAttribute('href', '/usage')
  })

  test('should toggle chart period', async ({ page }) => {
    // Click 30 Days button
    await page.click('text=30 Days')

    // Verify the button state changed
    const button30d = page.locator('.el-radio-button:has-text("30 Days")')
    await expect(button30d).toBeVisible()
  })
})
