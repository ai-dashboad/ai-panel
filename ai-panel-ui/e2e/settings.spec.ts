import { test, expect, setupAuthState } from './fixtures/auth'

test.describe('Settings Page', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuthState(page)
    await page.goto('/settings')
  })

  test('should display page header', async ({ page }) => {
    await expect(page.locator('h1:has-text("Settings")')).toBeVisible()
  })

  test('should display tabs', async ({ page }) => {
    await expect(page.locator('.el-tabs__item:has-text("Profile")')).toBeVisible()
    await expect(page.locator('.el-tabs__item:has-text("Security")')).toBeVisible()
    await expect(page.locator('.el-tabs__item:has-text("Plan")')).toBeVisible()
    await expect(page.locator('.el-tabs__item:has-text("Preferences")')).toBeVisible()
  })

  test.describe('Profile Tab', () => {
    test('should display profile form', async ({ page }) => {
      await expect(page.locator('text=Profile Information')).toBeVisible()
      await expect(page.locator('label:has-text("Username")')).toBeVisible()
      await expect(page.locator('label:has-text("Email")')).toBeVisible()
      await expect(page.locator('label:has-text("Nickname")')).toBeVisible()
    })

    test('should have disabled username field', async ({ page }) => {
      const usernameInput = page.locator('input').first()
      await expect(usernameInput).toBeDisabled()
    })

    test('should have save button', async ({ page }) => {
      await expect(page.locator('button:has-text("Save Changes")')).toBeVisible()
    })
  })

  test.describe('Security Tab', () => {
    test.beforeEach(async ({ page }) => {
      await page.locator('.el-tabs__item').filter({ hasText: 'Security' }).click()
      await page.waitForTimeout(300) // Wait for tab transition
    })

    test('should display password form', async ({ page }) => {
      await expect(page.locator('h3').filter({ hasText: 'Change Password' })).toBeVisible()
      await expect(page.getByText('Current Password', { exact: true })).toBeVisible()
      await expect(page.getByText('New Password', { exact: true })).toBeVisible()
      await expect(page.getByText('Confirm New Password', { exact: true })).toBeVisible()
    })

    test('should have update password button', async ({ page }) => {
      await expect(page.locator('button:has-text("Update Password")')).toBeVisible()
    })

    test('should validate password fields', async ({ page }) => {
      await page.click('button:has-text("Update Password")')
      await expect(page.locator('.el-form-item__error').first()).toBeVisible()
    })
  })

  test.describe('Plan Tab', () => {
    test.beforeEach(async ({ page }) => {
      await page.locator('.el-tabs__item').filter({ hasText: 'Plan' }).click()
      await page.waitForTimeout(300) // Wait for tab transition
    })

    test('should display current plan', async ({ page }) => {
      await expect(page.locator('h3').filter({ hasText: 'Current Plan' })).toBeVisible()
      await expect(page.locator('.plan-card')).toBeVisible()
    })

    test('should display plan details', async ({ page }) => {
      // Plan details are shown with span text
      await expect(page.locator('.plan-item span').filter({ hasText: 'Monthly Quota' })).toBeVisible()
      await expect(page.locator('.plan-item span').filter({ hasText: 'Rate Limit' })).toBeVisible()
      await expect(page.locator('.plan-item span').filter({ hasText: 'API Keys' })).toBeVisible()
    })

    test('should display usage progress', async ({ page }) => {
      await expect(page.locator('.el-progress')).toBeVisible()
    })

    test('should display upgrade options', async ({ page }) => {
      await expect(page.locator('h3').filter({ hasText: 'Upgrade Plan' })).toBeVisible()

      // Check plan cards - use upgrade-name class
      await expect(page.locator('.upgrade-name').filter({ hasText: 'Free' })).toBeVisible()
      await expect(page.locator('.upgrade-name').filter({ hasText: 'Basic' })).toBeVisible()
      await expect(page.locator('.upgrade-name').filter({ hasText: 'Pro' })).toBeVisible()
      await expect(page.locator('.upgrade-name').filter({ hasText: 'Enterprise' })).toBeVisible()
    })

    test('should display plan prices', async ({ page }) => {
      await expect(page.locator('.upgrade-price').first()).toBeVisible()
    })
  })

  test.describe('Preferences Tab', () => {
    test.beforeEach(async ({ page }) => {
      await page.locator('.el-tabs__item').filter({ hasText: 'Preferences' }).click()
      await page.waitForTimeout(300) // Wait for tab transition
    })

    test('should display preferences form', async ({ page }) => {
      await expect(page.locator('h3').filter({ hasText: 'Display Preferences' })).toBeVisible()
      await expect(page.locator('label').filter({ hasText: 'Language' })).toBeVisible()
      await expect(page.locator('label').filter({ hasText: 'Theme' })).toBeVisible()
    })

    test('should have language options', async ({ page }) => {
      // Click the first select (Language)
      await page.locator('.el-select').first().click()
      await page.waitForTimeout(200) // Wait for dropdown animation
      // Use getByRole for dropdown items
      await expect(page.getByRole('option', { name: 'English' })).toBeVisible()
      await expect(page.getByRole('option', { name: '中文' })).toBeVisible()
      // Close dropdown by clicking elsewhere
      await page.keyboard.press('Escape')
    })

    test('should have theme options', async ({ page }) => {
      // Click the second select (Theme)
      const themeSelect = page.locator('.el-select').nth(1)
      await themeSelect.click()
      await page.waitForTimeout(200) // Wait for dropdown animation
      // Use getByRole for dropdown items
      await expect(page.getByRole('option', { name: 'Light' })).toBeVisible()
      await expect(page.getByRole('option', { name: 'Dark' })).toBeVisible()
      await expect(page.getByRole('option', { name: 'System' })).toBeVisible()
    })
  })
})
