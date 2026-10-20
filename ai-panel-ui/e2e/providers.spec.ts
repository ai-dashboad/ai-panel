import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('Providers Page', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/providers')
  })

  test('should display page header', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Provider Configuration')
  })

  test('should display Reload Pool button', async ({ page }) => {
    const reloadBtn = page.getByRole('button', { name: /Reload Pool/i })
    await expect(reloadBtn).toBeVisible()
  })

  test('should display Add API Key button', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /Add API Key/i })
    await expect(addBtn).toBeVisible()
  })

  test('should display provider cards', async ({ page }) => {
    const providerCards = page.locator('.provider-card')
    await expect(providerCards).toHaveCount(4) // OpenAI, Anthropic, DeepSeek, Moonshot
  })

  test('should display provider names', async ({ page }) => {
    await expect(page.locator('.provider-card h3').first()).toBeVisible()
  })

  test('should display provider stats', async ({ page }) => {
    const stats = page.locator('.provider-stats .stat')
    await expect(stats.first()).toBeVisible()
  })

  test('should display API Keys section', async ({ page }) => {
    await expect(page.locator('h2')).toContainText('API Keys')
  })

  test('should display API keys table', async ({ page }) => {
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('should click Manage Keys button', async ({ page }) => {
    const manageKeysBtn = page.locator('.provider-card').first().getByRole('button', { name: /Manage Keys/i })
    await expect(manageKeysBtn).toBeVisible()
    await manageKeysBtn.click()
    // Should filter keys by provider
    await expect(page.locator('.filter-tag')).toBeVisible()
  })

  test('should click Edit provider button', async ({ page }) => {
    const editBtn = page.locator('.provider-card').first().getByRole('button', { name: /Edit/i })
    await expect(editBtn).toBeVisible()
    await editBtn.click()
    // Should open edit dialog
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('Edit Provider')
  })

  test('should toggle provider enabled switch', async ({ page }) => {
    const switchEl = page.locator('.provider-card').first().locator('.el-switch')
    await expect(switchEl).toBeVisible()
    // The switch should be clickable
    await switchEl.click()
  })

  test('should open Add API Key dialog', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /Add API Key/i })
    await addBtn.click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('Add API Key')
  })

  test('should display Add API Key form fields', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /Add API Key/i })
    await addBtn.click()

    // Check form fields - use dialog-specific locators to avoid ambiguity
    const dialog = page.locator('.el-dialog:has-text("Add API Key")')
    await expect(dialog.locator('.el-form-item:has-text("Provider")')).toBeVisible()
    await expect(dialog.locator('.el-form-item:has-text("Key Name")')).toBeVisible()
    await expect(dialog.locator('.el-form-item:has-text("API Key")')).toBeVisible()
    await expect(dialog.locator('.el-form-item:has-text("Weight")')).toBeVisible()
  })

  test('should select provider in Add API Key dialog', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /Add API Key/i })
    await addBtn.click()

    // Click provider select
    const providerSelect = page.locator('.el-select').first()
    await providerSelect.click()

    // Should show provider options
    await expect(page.locator('.el-select-dropdown__item').first()).toBeVisible()
  })

  test('should close Add API Key dialog on cancel', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /Add API Key/i })
    await addBtn.click()

    const cancelBtn = page.locator('.el-dialog').getByRole('button', { name: 'Cancel' })
    await cancelBtn.click()

    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })

  test('should validate required fields in Add API Key form', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /Add API Key/i })
    await addBtn.click()

    // Try to submit without filling required fields
    const dialog = page.locator('.el-dialog:has-text("Add API Key")')
    const submitBtn = dialog.locator('.el-dialog__footer').getByRole('button', { name: /^Add$/i })
    await submitBtn.click()

    // Should show validation errors (use first() since multiple errors appear)
    await expect(dialog.locator('.el-form-item__error').first()).toBeVisible()
  })

  test('should clear provider filter', async ({ page }) => {
    // First click Manage Keys to add filter
    const manageKeysBtn = page.locator('.provider-card').first().getByRole('button', { name: /Manage Keys/i })
    await manageKeysBtn.click()

    // Filter tag should be visible
    const filterTag = page.locator('.filter-tag')
    await expect(filterTag).toBeVisible()

    // Click close icon to clear filter
    const closeIcon = filterTag.locator('.close-icon')
    await closeIcon.click()

    // Filter tag should be hidden
    await expect(filterTag).not.toBeVisible()
  })

  test('should display provider models', async ({ page }) => {
    const modelTags = page.locator('.provider-card').first().locator('.provider-models .el-tag')
    await expect(modelTags.first()).toBeVisible()
  })

  test('should click Reload Pool button', async ({ page }) => {
    const reloadBtn = page.getByRole('button', { name: /Reload Pool/i })
    await reloadBtn.click()
    // Should show loading state or success message
  })
})

test.describe('Providers Page - Edit Provider Dialog', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/providers')
    // Open edit dialog
    const editBtn = page.locator('.provider-card').first().getByRole('button', { name: /Edit/i })
    await editBtn.click()
  })

  test('should display provider name field', async ({ page }) => {
    await expect(page.getByLabel('Name')).toBeVisible()
  })

  test('should display disabled type field', async ({ page }) => {
    const typeInput = page.getByLabel('Type')
    await expect(typeInput).toBeVisible()
    await expect(typeInput).toBeDisabled()
  })

  test('should display base URL field', async ({ page }) => {
    await expect(page.getByLabel('Base URL')).toBeVisible()
  })

  test('should display priority field', async ({ page }) => {
    await expect(page.getByLabel('Priority')).toBeVisible()
  })

  test('should display timeout field', async ({ page }) => {
    await expect(page.getByLabel('Timeout (s)')).toBeVisible()
  })

  test('should display retries field', async ({ page }) => {
    await expect(page.getByLabel('Retries')).toBeVisible()
  })

  test('should close dialog on cancel', async ({ page }) => {
    const cancelBtn = page.locator('.el-dialog').getByRole('button', { name: 'Cancel' })
    await cancelBtn.click()
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })

  test('should have Save button', async ({ page }) => {
    const saveBtn = page.locator('.el-dialog').getByRole('button', { name: 'Save' })
    await expect(saveBtn).toBeVisible()
  })
})
