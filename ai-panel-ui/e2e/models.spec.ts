import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('Models Ops Page', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/models')
  })

  test('should display models ops layout', async ({ page }) => {
    await expect(page.getByTestId('models-ops')).toBeVisible()
    await expect(page.locator('h1:has-text("Models")')).toBeVisible()
  })

  test('should display search input', async ({ page }) => {
    await expect(page.getByTestId('model-search')).toBeVisible()
  })

  test('should display sidebar filters', async ({ page }) => {
    await expect(page.locator('.ops-sidebar h3:has-text("Groups")')).toBeVisible()
    await expect(page.locator('.ops-sidebar h3:has-text("Providers")')).toBeVisible()
    await expect(page.locator('.ops-sidebar h3:has-text("Pricing type")')).toBeVisible()
  })

  test('should display model cards', async ({ page }) => {
    const cards = page.getByTestId('model-card')
    await expect(cards.first()).toBeVisible()
    await expect(page.getByTestId('result-count')).toBeVisible()
  })

  test('should display dynamic pricing on cards', async ({ page }) => {
    await expect(page.locator('.model-card .pricing').first()).toBeVisible()
    await expect(page.locator('text=Input').first()).toBeVisible()
    await expect(page.locator('text=Output').first()).toBeVisible()
  })

  test('should search models', async ({ page }) => {
    await expect(page.getByTestId('model-card').first()).toBeVisible()
    await page.locator('input[placeholder*="Search models"]').fill('claude')
    await page.waitForTimeout(200)
    await expect(page.locator('.model-card .model-id').first()).toContainText(/claude/i)
  })

  test('should filter by provider from sidebar', async ({ page }) => {
    await expect(page.getByTestId('model-card').first()).toBeVisible()
    const openaiBtn = page.locator('.ops-sidebar .filter-item', { hasText: 'OpenAI' }).first()
    if (await openaiBtn.isVisible()) {
      await openaiBtn.click()
      await page.waitForTimeout(200)
      await expect(page.getByTestId('model-card').first()).toBeVisible()
    }
  })

  test('should filter by group with multiplier badge', async ({ page }) => {
    await expect(page.getByTestId('model-card').first()).toBeVisible()
    const groupBtn = page.locator('.ops-sidebar .filter-item em').first()
    await expect(groupBtn).toBeVisible()
    await groupBtn.click()
    await page.waitForTimeout(200)
    await expect(page.getByTestId('model-card').first()).toBeVisible()
  })

  test('should copy model ID', async ({ page }) => {
    await expect(page.getByTestId('model-card').first()).toBeVisible()
    await page.context().grantPermissions(['clipboard-read', 'clipboard-write'])
    await page.locator('.model-card .icon-btn').first().click()
    await expect(page.locator('.el-message')).toContainText(/copied/i)
  })

  test('should toggle price unit', async ({ page }) => {
    await expect(page.getByTestId('model-card').first()).toBeVisible()
    await page.locator('.el-radio-button', { hasText: '/1K' }).click()
    await expect(page.locator('.model-card .price-row').first()).toContainText('1K')
  })
})
