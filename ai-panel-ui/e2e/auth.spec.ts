import { test, expect } from '@playwright/test'

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('should display login form correctly', async ({ page }) => {
    // Check page title and header
    await expect(page.locator('h1')).toContainText('AI Panel')
    await expect(page.locator('text=Sign in to your account')).toBeVisible()

    // Check form elements
    await expect(page.locator('input').first()).toBeVisible()
    await expect(page.locator('input[type="password"]')).toBeVisible()
    await expect(page.locator('button:has-text("Sign In")')).toBeVisible()

    // Check register link
    await expect(page.locator('text=Create one')).toBeVisible()
  })

  test('should show validation errors for empty form', async ({ page }) => {
    await page.click('button:has-text("Sign In")')

    // Wait for validation messages
    await expect(page.locator('.el-form-item__error').first()).toBeVisible()
  })

  test('should show validation error for short password', async ({ page }) => {
    await page.fill('input', 'testuser')
    await page.fill('input[type="password"]', '12345')
    await page.click('button:has-text("Sign In")')

    await expect(page.locator('text=Password must be at least 6 characters')).toBeVisible()
  })

  test('should navigate to register page', async ({ page }) => {
    await page.click('text=Create one')
    await expect(page).toHaveURL('/register')
  })

  test('should attempt login with credentials', async ({ page }) => {
    await page.fill('input', 'testuser@example.com')
    await page.fill('input[type="password"]', 'password123')
    await page.click('button:has-text("Sign In")')

    // Should show loading state or redirect
    await expect(page.locator('button:has-text("Sign In")')).toBeVisible()
  })
})

test.describe('Register Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/register')
  })

  test('should display register form correctly', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('AI Panel')
    await expect(page.locator('text=Create your account')).toBeVisible()

    // Check all form fields
    await expect(page.locator('label:has-text("Username")')).toBeVisible()
    await expect(page.locator('label:has-text("Email")')).toBeVisible()
    await expect(page.getByText('Password', { exact: true })).toBeVisible()
    await expect(page.locator('label:has-text("Confirm Password")')).toBeVisible()

    await expect(page.locator('button:has-text("Create Account")')).toBeVisible()
  })

  test('should show validation errors for empty form', async ({ page }) => {
    await page.click('button:has-text("Create Account")')

    // Wait for validation messages
    await page.waitForSelector('.el-form-item__error')
    const errors = await page.locator('.el-form-item__error').count()
    expect(errors).toBeGreaterThan(0)
  })

  test('should validate email format', async ({ page }) => {
    await page.locator('input').nth(1).fill('invalid-email')
    await page.locator('input').nth(1).blur()

    await expect(page.locator('text=Please enter a valid email')).toBeVisible()
  })

  test('should validate password match', async ({ page }) => {
    await page.locator('input[type="password"]').first().fill('password123')
    await page.locator('input[type="password"]').last().fill('different123')
    await page.locator('input[type="password"]').last().blur()

    await expect(page.locator('text=Passwords do not match')).toBeVisible()
  })

  test('should navigate to login page', async ({ page }) => {
    await page.click('text=Sign in')
    await expect(page).toHaveURL('/login')
  })
})
