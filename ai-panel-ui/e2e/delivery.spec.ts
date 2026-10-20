import { test, expect } from './fixtures/auth'

test.describe('Delivery Platforms Page', () => {
  test.beforeEach(async ({ authenticatedPage: page }) => {
    await page.goto('/delivery/platforms')
    await page.waitForLoadState('networkidle')
  })

  test('should display platforms page header', async ({ authenticatedPage: page }) => {
    await expect(page.locator('h1')).toContainText('Platform Configuration')
  })

  test('should display Add Platform button', async ({ authenticatedPage: page }) => {
    await expect(page.getByRole('button', { name: /Add Platform/i })).toBeVisible()
  })

  test('should display platforms table', async ({ authenticatedPage: page }) => {
    await expect(page.locator('table')).toBeVisible()
  })

  test('should display platform data in table', async ({ authenticatedPage: page }) => {
    // Check for taobao platform
    await expect(page.getByText('taobao')).toBeVisible()
    await expect(page.getByText('Taobao Store 1')).toBeVisible()
    // Check for xianyu platform
    await expect(page.getByText('xianyu')).toBeVisible()
    await expect(page.getByText('Xianyu Store')).toBeVisible()
  })

  test('should display webhook URL for platforms', async ({ authenticatedPage: page }) => {
    await expect(page.getByText(/\/api\/v1\/webhook\/taobao/)).toBeVisible()
  })

  test('should display auto create user status', async ({ authenticatedPage: page }) => {
    const yesTagsLocator = page.locator('td:has-text("Yes")').first()
    await expect(yesTagsLocator).toBeVisible()
  })

  test('should display enabled toggle switches', async ({ authenticatedPage: page }) => {
    const switches = page.locator('.el-switch')
    await expect(switches.first()).toBeVisible()
  })

  test('should open add platform dialog when clicking Add Platform', async ({ authenticatedPage: page }) => {
    await page.getByRole('button', { name: /Add Platform/i }).click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('Add Platform')
  })

  test('should display form fields in add dialog', async ({ authenticatedPage: page }) => {
    await page.getByRole('button', { name: /Add Platform/i }).click()
    await expect(page.getByLabel('Platform ID')).toBeVisible()
    await expect(page.getByLabel('Name')).toBeVisible()
    await expect(page.getByLabel('App Key')).toBeVisible()
    await expect(page.getByLabel('Delivery Template')).toBeVisible()
  })

  test('should close dialog when clicking Cancel', async ({ authenticatedPage: page }) => {
    await page.getByRole('button', { name: /Add Platform/i }).click()
    await page.getByRole('button', { name: 'Cancel' }).click()
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })
})

test.describe('Product SKUs Page', () => {
  test.beforeEach(async ({ authenticatedPage: page }) => {
    await page.goto('/delivery/skus')
    await page.waitForLoadState('networkidle')
  })

  test('should display SKUs page header', async ({ authenticatedPage: page }) => {
    await expect(page.locator('h1')).toContainText('Product SKUs')
  })

  test('should display Add SKU button', async ({ authenticatedPage: page }) => {
    await expect(page.getByRole('button', { name: /Add SKU/i })).toBeVisible()
  })

  test('should display platform filter dropdown', async ({ authenticatedPage: page }) => {
    await expect(page.getByPlaceholder('Filter by platform')).toBeVisible()
  })

  test('should display SKUs table', async ({ authenticatedPage: page }) => {
    await expect(page.locator('table')).toBeVisible()
  })

  test('should display SKU data in table', async ({ authenticatedPage: page }) => {
    // Check for SKU codes
    await expect(page.getByText('API-PRO-30D')).toBeVisible()
    await expect(page.getByText('API-BASIC-7D')).toBeVisible()
    // Check for SKU names
    await expect(page.getByText('API Pro 30 Days')).toBeVisible()
    await expect(page.getByText('API Basic 7 Days')).toBeVisible()
  })

  test('should display quota information', async ({ authenticatedPage: page }) => {
    await expect(page.getByText(/1\.0M tokens/)).toBeVisible()
    await expect(page.getByText(/100\.0K tokens/)).toBeVisible()
  })

  test('should display validity days', async ({ authenticatedPage: page }) => {
    await expect(page.getByText('30 days')).toBeVisible()
    await expect(page.getByText('7 days')).toBeVisible()
  })

  test('should display platform tags', async ({ authenticatedPage: page }) => {
    await expect(page.getByRole('cell').filter({ hasText: 'All' }).first()).toBeVisible()
    await expect(page.getByRole('cell').filter({ hasText: 'taobao' }).first()).toBeVisible()
  })

  test('should open add SKU dialog when clicking Add SKU', async ({ authenticatedPage: page }) => {
    await page.getByRole('button', { name: /Add SKU/i }).click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog__title')).toContainText('Add SKU')
  })

  test('should display form fields in add dialog', async ({ authenticatedPage: page }) => {
    await page.getByRole('button', { name: /Add SKU/i }).click()
    await expect(page.getByLabel('SKU Code')).toBeVisible()
    await expect(page.getByLabel('Name')).toBeVisible()
    await expect(page.getByLabel('Platform')).toBeVisible()
    await expect(page.getByLabel('Validity (Days)')).toBeVisible()
  })

  test('should display quota type radio buttons', async ({ authenticatedPage: page }) => {
    await page.getByRole('button', { name: /Add SKU/i }).click()
    await expect(page.getByLabel('Tokens')).toBeVisible()
    await expect(page.getByLabel('Requests')).toBeVisible()
  })
})

test.describe('External Orders Page', () => {
  test.beforeEach(async ({ authenticatedPage: page }) => {
    await page.goto('/delivery/orders')
    await page.waitForLoadState('networkidle')
  })

  test('should display orders page header', async ({ authenticatedPage: page }) => {
    await expect(page.locator('h1')).toContainText('External Orders')
  })

  test('should display Retry Failed button', async ({ authenticatedPage: page }) => {
    await expect(page.getByRole('button', { name: /Retry Failed/i })).toBeVisible()
  })

  test('should display filter dropdowns', async ({ authenticatedPage: page }) => {
    await expect(page.getByPlaceholder('Platform')).toBeVisible()
    await expect(page.getByPlaceholder('Status')).toBeVisible()
  })

  test('should display search input', async ({ authenticatedPage: page }) => {
    await expect(page.getByPlaceholder('Search order ID, buyer...')).toBeVisible()
  })

  test('should display orders table', async ({ authenticatedPage: page }) => {
    await expect(page.locator('table')).toBeVisible()
  })

  test('should display order data in table', async ({ authenticatedPage: page }) => {
    // Check for order IDs
    await expect(page.getByText('TB202401150001')).toBeVisible()
    await expect(page.getByText('TB202401150002')).toBeVisible()
    await expect(page.getByText('XY202401150001')).toBeVisible()
  })

  test('should display buyer names', async ({ authenticatedPage: page }) => {
    await expect(page.getByText('Buyer1')).toBeVisible()
    await expect(page.getByText('Buyer2')).toBeVisible()
    await expect(page.getByText('Buyer3')).toBeVisible()
  })

  test('should display order status tags', async ({ authenticatedPage: page }) => {
    await expect(page.getByRole('cell').filter({ hasText: 'delivered' }).first()).toBeVisible()
    await expect(page.getByRole('cell').filter({ hasText: 'pending' }).first()).toBeVisible()
    await expect(page.getByRole('cell').filter({ hasText: 'failed' }).first()).toBeVisible()
  })

  test('should display retry count for failed orders', async ({ authenticatedPage: page }) => {
    // The failed order has retryCount = 2
    const failedRow = page.locator('tr').filter({ hasText: 'XY202401150001' })
    await expect(failedRow.getByText('2')).toBeVisible()
  })

  test('should display retry button for failed orders', async ({ authenticatedPage: page }) => {
    const failedRow = page.locator('tr').filter({ hasText: 'failed' })
    await expect(failedRow.getByRole('button', { name: /Retry/i })).toBeVisible()
  })

  test('should display view detail button for orders', async ({ authenticatedPage: page }) => {
    const viewButtons = page.locator('button').filter({ has: page.locator('.el-icon') }).first()
    await expect(viewButtons).toBeVisible()
  })

  test('should display pagination controls', async ({ authenticatedPage: page }) => {
    await expect(page.locator('.el-pagination')).toBeVisible()
  })
})

test.describe('Delivery Navigation', () => {
  test('should navigate to Platforms page from menu', async ({ authenticatedPage: page }) => {
    await page.click('.sidebar-menu >> text=Platforms')
    await expect(page).toHaveURL(/\/delivery\/platforms/)
    await expect(page.locator('h1')).toContainText('Platform Configuration')
  })

  test('should navigate to Product SKUs page from menu', async ({ authenticatedPage: page }) => {
    await page.click('.sidebar-menu >> text=Product SKUs')
    await expect(page).toHaveURL(/\/delivery\/skus/)
    await expect(page.locator('h1')).toContainText('Product SKUs')
  })

  test('should navigate to Orders page from menu', async ({ authenticatedPage: page }) => {
    await page.click('.sidebar-menu >> text=Orders')
    await expect(page).toHaveURL(/\/delivery\/orders/)
    await expect(page.locator('h1')).toContainText('External Orders')
  })
})
