import { test as base, Page } from '@playwright/test'

// Mock user data
export const mockUser = {
  username: 'testuser',
  email: 'testuser@example.com',
  password: 'password123',
  token: 'mock-jwt-token-for-testing'
}

// Mock API responses
const mockApiResponses: Record<string, any> = {
  '/api/v1/providers': {
    code: 200,
    message: 'Success',
    data: [
      {
        id: 1,
        name: 'OpenAI',
        type: 'openai',
        baseUrl: 'https://api.openai.com',
        enabled: true,
        priority: 100,
        timeoutSeconds: 60,
        retryCount: 3,
        description: 'OpenAI GPT models',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00',
        keyCount: 2,
        activeKeyCount: 2,
        models: ['gpt-4o', 'gpt-4-turbo', 'gpt-3.5-turbo']
      },
      {
        id: 2,
        name: 'Anthropic',
        type: 'anthropic',
        baseUrl: 'https://api.anthropic.com',
        enabled: true,
        priority: 100,
        timeoutSeconds: 60,
        retryCount: 3,
        description: 'Anthropic Claude models',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00',
        keyCount: 1,
        activeKeyCount: 1,
        models: ['claude-3-opus', 'claude-3-sonnet']
      },
      {
        id: 3,
        name: 'DeepSeek',
        type: 'deepseek',
        baseUrl: 'https://api.deepseek.com',
        enabled: true,
        priority: 200,
        timeoutSeconds: 60,
        retryCount: 3,
        description: 'DeepSeek models',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00',
        keyCount: 0,
        activeKeyCount: 0,
        models: ['deepseek-chat', 'deepseek-coder']
      },
      {
        id: 4,
        name: 'Moonshot',
        type: 'moonshot',
        baseUrl: 'https://api.moonshot.cn',
        enabled: false,
        priority: 200,
        timeoutSeconds: 60,
        retryCount: 3,
        description: 'Moonshot AI models',
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00',
        keyCount: 0,
        activeKeyCount: 0,
        models: []
      }
    ]
  },
  '/api/v1/providers/keys': {
    code: 200,
    message: 'Success',
    data: [
      {
        id: 1,
        providerId: 1,
        providerName: 'OpenAI',
        providerType: 'openai',
        keyName: 'Production Key',
        keyPrefix: 'sk-pr****ef12',
        enabled: true,
        weight: 100,
        rateLimitPerMinute: 60,
        dailyQuota: 1000000,
        usedQuotaToday: 50000,
        totalRequests: 1500,
        totalTokens: 250000,
        lastUsedAt: '2024-01-15T10:30:00',
        lastErrorAt: null,
        lastErrorMessage: null,
        consecutiveErrors: 0,
        status: 'active',
        expiresAt: null,
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-15T10:30:00'
      },
      {
        id: 2,
        providerId: 1,
        providerName: 'OpenAI',
        providerType: 'openai',
        keyName: 'Backup Key',
        keyPrefix: 'sk-ba****cd34',
        enabled: true,
        weight: 50,
        rateLimitPerMinute: 30,
        dailyQuota: 500000,
        usedQuotaToday: 10000,
        totalRequests: 200,
        totalTokens: 30000,
        lastUsedAt: '2024-01-14T08:00:00',
        lastErrorAt: null,
        lastErrorMessage: null,
        consecutiveErrors: 0,
        status: 'active',
        expiresAt: null,
        createdAt: '2024-01-05T00:00:00',
        updatedAt: '2024-01-14T08:00:00'
      }
    ]
  },
  '/api/v1/providers/status': {
    code: 200,
    message: 'Success',
    data: {
      openai: {
        keys: [
          { id: 1, name: 'Production Key', weight: 100, status: 'active', dailyQuota: 1000000, usedQuotaToday: 50000, consecutiveErrors: 0 },
          { id: 2, name: 'Backup Key', weight: 50, status: 'active', dailyQuota: 500000, usedQuotaToday: 10000, consecutiveErrors: 0 }
        ],
        totalKeys: 2,
        activeKeys: 2
      }
    }
  },
  '/api/v1/keys': {
    code: 0,
    message: 'success',
    data: {
      records: [
        {
          id: '1',
          name: 'Production API Key',
          keyPrefix: 'sk-prod-****1234',
          status: 1,
          rateLimitPerMinute: 60,
          lastUsedAt: '2024-01-15T10:30:00Z',
          createdAt: '2024-01-01T00:00:00Z'
        },
        {
          id: '2',
          name: 'Development API Key',
          keyPrefix: 'sk-dev-****5678',
          status: 1,
          rateLimitPerMinute: 100,
          lastUsedAt: null,
          createdAt: '2024-01-10T00:00:00Z'
        }
      ],
      total: 2
    }
  },
  '/api/v1/models': {
    code: 0,
    message: 'success',
    data: [
      {
        id: 'gpt-4o',
        displayName: 'GPT-4o',
        provider: 'openai',
        modelType: 'CHAT',
        contextWindow: 128000,
        maxOutputTokens: 16384,
        inputPrice: 0.0025,
        outputPrice: 0.01,
        groupCode: 'openai-main',
        groupName: 'OpenAI-Main',
        priceMultiplier: 1.2,
        sellInputPricePerMillion: 3.0,
        sellOutputPricePerMillion: 12.0,
        labels: ['chat', 'openai'],
        pricingType: 'usage',
        description: 'OpenAI flagship multimodal',
      },
      {
        id: 'claude-sonnet-4',
        displayName: 'Claude Sonnet 4',
        provider: 'anthropic',
        modelType: 'CHAT',
        contextWindow: 200000,
        maxOutputTokens: 64000,
        inputPrice: 0.003,
        outputPrice: 0.015,
        groupCode: 'claude-official',
        groupName: 'Claude-Official-Main',
        priceMultiplier: 1.5,
        sellInputPricePerMillion: 4.5,
        sellOutputPricePerMillion: 22.5,
        labels: ['chat', 'anthropic'],
        pricingType: 'usage',
        description: 'Claude Sonnet 4',
      },
      {
        id: 'deepseek-chat',
        displayName: 'DeepSeek Chat',
        provider: 'deepseek',
        modelType: 'CHAT',
        contextWindow: 64000,
        maxOutputTokens: 8192,
        inputPrice: 0.00014,
        outputPrice: 0.00028,
        groupCode: 'domestic-agg',
        groupName: 'Domestic-Aggregation',
        priceMultiplier: 0.3,
        sellInputPricePerMillion: 0.042,
        sellOutputPricePerMillion: 0.084,
        labels: ['chat', 'deepseek'],
        pricingType: 'usage',
      },
    ],
  },
  '/api/v1/model-groups': {
    code: 0,
    message: 'success',
    data: [
      { code: 'claude-official', name: 'Claude-Official-Main', priceMultiplier: 1.5, modelCount: 1 },
      { code: 'openai-main', name: 'OpenAI-Main', priceMultiplier: 1.2, modelCount: 1 },
      { code: 'domestic-agg', name: 'Domestic-Aggregation', priceMultiplier: 0.3, modelCount: 1 },
    ],
  },
  '/api/v1/stats/models/health': {
    code: 0,
    message: 'success',
    data: {
      'gpt-4o': {
        modelId: 'gpt-4o',
        successRate: 98.5,
        avgLatencySeconds: 1.2,
        throughputTokensPerSecond: 72.0,
        requestCount: 120,
        recentStatuses: [1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1],
      },
      'claude-sonnet-4': {
        modelId: 'claude-sonnet-4',
        successRate: 100,
        avgLatencySeconds: 2.1,
        throughputTokensPerSecond: 65.3,
        requestCount: 80,
        recentStatuses: [1, 1, 1, 1, 1, 1, 1, 1],
      },
    },
  },
  '/api/v1/user/profile': {
    code: 0,
    message: 'success',
    data: {
      id: 1,
      username: 'testuser',
      email: 'testuser@example.com',
      nickname: 'Test User',
      planType: 'free',
      monthlyQuota: 100000,
      usedQuota: 25000,
      rateLimitPerMinute: 60,
      maxApiKeys: 5
    }
  },
  '/api/v1/usage/summary': {
    code: 0,
    message: 'success',
    data: {
      totalRequests: 1250,
      totalTokens: 125000,
      totalCost: 12.50,
      dailyUsage: []
    }
  },
  '/api/v1/admin/platforms': {
    code: 0,
    message: 'success',
    data: [
      {
        id: 1,
        platform: 'taobao',
        name: 'Taobao Store 1',
        appKey: 'app***',
        webhookUrl: 'http://localhost:8080/api/v1/webhook/taobao',
        deliveryTemplate: 'Your API Key: ${apiKey}\nExpires: ${expiresAt}',
        autoCreateUser: true,
        autoDelivery: true,
        enabled: true,
        config: null,
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00'
      },
      {
        id: 2,
        platform: 'xianyu',
        name: 'Xianyu Store',
        appKey: 'xy_***',
        webhookUrl: 'http://localhost:8080/api/v1/webhook/xianyu',
        deliveryTemplate: null,
        autoCreateUser: true,
        autoDelivery: false,
        enabled: false,
        config: null,
        createdAt: '2024-01-05T00:00:00',
        updatedAt: '2024-01-05T00:00:00'
      }
    ]
  },
  '/api/v1/admin/skus': {
    code: 0,
    message: 'success',
    data: [
      {
        id: 1,
        platform: null,
        sku: 'API-PRO-30D',
        name: 'API Pro 30 Days',
        description: '1M tokens, 30 days validity',
        quotaType: 'tokens',
        quotaTokens: 1000000,
        quotaRequests: null,
        validityDays: 30,
        allowedModels: null,
        rateLimit: 60,
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
        updatedAt: '2024-01-01T00:00:00'
      },
      {
        id: 2,
        platform: 'taobao',
        sku: 'API-BASIC-7D',
        name: 'API Basic 7 Days',
        description: '100K tokens, 7 days validity',
        quotaType: 'tokens',
        quotaTokens: 100000,
        quotaRequests: null,
        validityDays: 7,
        allowedModels: 'gpt-3.5-turbo,deepseek-chat',
        rateLimit: 30,
        enabled: true,
        createdAt: '2024-01-02T00:00:00',
        updatedAt: '2024-01-02T00:00:00'
      }
    ]
  },
  '/api/v1/admin/orders': {
    code: 0,
    message: 'success',
    data: {
      total: 3,
      pageNum: 1,
      pageSize: 20,
      totalPages: 1,
      hasNext: false,
      hasPrevious: false,
      list: [
        {
          id: 1,
          platform: 'taobao',
          externalOrderId: 'TB202401150001',
          externalItemId: 'ITEM001',
          sku: 'API-PRO-30D',
          buyerNick: 'Buyer1',
          buyerPhone: '138****8001',
          buyerEmail: 'buyer1@example.com',
          buyerMessage: null,
          quantity: 1,
          amount: 99.00,
          userId: 10,
          apiKeyId: 100,
          status: 'delivered',
          deliveryMethod: 'email',
          deliveryContent: 'Your API Key: sk-xxx...',
          deliveryTime: '2024-01-15T10:30:00',
          failReason: null,
          retryCount: 0,
          createdAt: '2024-01-15T10:00:00',
          updatedAt: '2024-01-15T10:30:00'
        },
        {
          id: 2,
          platform: 'taobao',
          externalOrderId: 'TB202401150002',
          externalItemId: 'ITEM002',
          sku: 'API-BASIC-7D',
          buyerNick: 'Buyer2',
          buyerPhone: '139****8002',
          buyerEmail: null,
          buyerMessage: 'Please send quickly',
          quantity: 1,
          amount: 29.00,
          userId: null,
          apiKeyId: null,
          status: 'pending',
          deliveryMethod: null,
          deliveryContent: null,
          deliveryTime: null,
          failReason: null,
          retryCount: 0,
          createdAt: '2024-01-15T11:00:00',
          updatedAt: '2024-01-15T11:00:00'
        },
        {
          id: 3,
          platform: 'xianyu',
          externalOrderId: 'XY202401150001',
          externalItemId: null,
          sku: 'API-PRO-30D',
          buyerNick: 'Buyer3',
          buyerPhone: null,
          buyerEmail: 'buyer3@example.com',
          buyerMessage: null,
          quantity: 1,
          amount: 89.00,
          userId: 11,
          apiKeyId: null,
          status: 'failed',
          deliveryMethod: 'email',
          deliveryContent: null,
          deliveryTime: null,
          failReason: 'Email sending failed',
          retryCount: 2,
          createdAt: '2024-01-15T12:00:00',
          updatedAt: '2024-01-15T12:30:00'
        }
      ]
    }
  }
}

// Helper to set up API route mocking
export async function setupApiMocks(page: Page) {
  // Intercept API calls and return mock data
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    const pathname = url.pathname

    // Find matching mock response
    const mockResponse = mockApiResponses[pathname]

    if (mockResponse) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockResponse)
      })
    } else {
      // Return a generic success response for unhandled endpoints
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'success',
          data: {}
        })
      })
    }
  })
}

// Helper to set up authenticated state
export async function setupAuthState(page: Page) {
  // Set up API mocks first
  await setupApiMocks(page)

  // Set up mock authentication in localStorage before navigating
  // Pinia persist plugin uses the store name as key
  await page.addInitScript(() => {
    // Set token for authentication check
    localStorage.setItem('user', JSON.stringify({
      token: 'mock-jwt-token-for-testing',
      language: 'en'
    }))
    // Skip first-time Setup Wizard for e2e authenticated pages
    localStorage.setItem('setupComplete', 'true')
    // Mock userInfo in sessionStorage (for components that need it)
    sessionStorage.setItem('userInfo', JSON.stringify({
      id: 1,
      username: 'testuser',
      email: 'testuser@example.com',
      displayName: 'Test User'
    }))
  })

  // Navigate to a page first to initialize the script
  await page.goto('/')
  // Wait for the app to initialize
  await page.waitForLoadState('networkidle')
}

// Create authenticated test fixture
export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page }, use) => {
    await setupAuthState(page)
    await use(page)
  }
})

export { expect } from '@playwright/test'
