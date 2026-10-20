import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('Usage & Billing Page', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/usage')
  })

  test('should display page header', async ({ page }) => {
    await expect(page.locator('h1:has-text("Usage & Billing")')).toBeVisible()
  })

  test('should display date range picker', async ({ page }) => {
    await expect(page.locator('.el-date-editor')).toBeVisible()
  })

  test('should display summary cards', async ({ page }) => {
    await expect(page.locator('text=Total Requests')).toBeVisible()
    await expect(page.locator('text=Total Tokens')).toBeVisible()
    await expect(page.locator('text=Total Cost')).toBeVisible()
  })

  test('should display summary values', async ({ page }) => {
    const summaryCards = page.locator('.summary-card')
    await expect(summaryCards).toHaveCount(3)

    // Check that values are displayed
    const summaryValues = page.locator('.summary-value')
    await expect(summaryValues.first()).toBeVisible()
  })

  test('should display daily usage chart', async ({ page }) => {
    await expect(page.locator('text=Daily Usage')).toBeVisible()
    await expect(page.locator('.chart-container')).toBeVisible()
  })

  test('should display usage records table', async ({ page }) => {
    await expect(page.locator('text=Usage Records')).toBeVisible()

    // Check table headers
    await expect(page.locator('th:has-text("Date")')).toBeVisible()
    await expect(page.locator('th:has-text("Model")')).toBeVisible()
    await expect(page.locator('th:has-text("Requests")')).toBeVisible()
    await expect(page.locator('th:has-text("Input Tokens")')).toBeVisible()
    await expect(page.locator('th:has-text("Output Tokens")')).toBeVisible()
    await expect(page.locator('th:has-text("Cost")')).toBeVisible()
  })

  test('should display pagination', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toBeVisible()
  })

  test('should open date picker', async ({ page }) => {
    await page.click('.el-date-editor')
    await expect(page.locator('.el-picker-panel')).toBeVisible()
  })

  test('should display date shortcuts', async ({ page }) => {
    await page.click('.el-date-editor')

    // Check shortcuts
    await expect(page.locator('text=Last 7 days')).toBeVisible()
    await expect(page.locator('text=Last 30 days')).toBeVisible()
    await expect(page.locator('text=This month')).toBeVisible()
  })

  test('should select date shortcut', async ({ page }) => {
    await page.click('.el-date-editor')
    await page.click('text=Last 30 days')

    // Date picker should close after selection
    await page.waitForTimeout(500)
  })

  test('should have working pagination', async ({ page }) => {
    const pagination = page.locator('.el-pagination')

    // Check page size selector
    await expect(pagination.locator('.el-select')).toBeVisible()

    // Check page numbers or navigation
    const pager = pagination.locator('.el-pager, .btn-prev, .btn-next')
    await expect(pager.first()).toBeVisible()
  })
})
