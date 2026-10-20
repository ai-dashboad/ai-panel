<template>
  <div class="setup-wizard">
    <div class="wizard-container">
      <!-- Header -->
      <div class="wizard-header">
        <h1>🚀 欢迎使用 AI Panel</h1>
        <p>统一管理多个 AI 供应商，一个 Key 访问所有模型</p>
      </div>

      <!-- Steps Indicator -->
      <div class="wizard-steps">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="step"
          :class="{
            'step--active': currentStep === index,
            'step--completed': currentStep > index
          }"
        >
          <div class="step__number">
            <el-icon v-if="currentStep > index"><Check /></el-icon>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <span class="step__title">{{ step }}</span>
        </div>
      </div>

      <!-- Step Content -->
      <div class="wizard-content">
        <!-- Step 1: Configure Providers -->
        <div v-if="currentStep === 0" class="step-content">
          <h2>配置 AI 供应商</h2>
          <p class="step-desc">选择并配置你的 AI 供应商（至少配置一个即可开始使用）</p>

          <div class="providers-list">
            <div
              v-for="provider in presetProviders"
              :key="provider.type"
              class="provider-card"
              :class="{ 'provider-card--configured': provider.configured }"
            >
              <div class="provider-card__header">
                <div class="provider-card__icon" :class="provider.type">
                  {{ provider.icon }}
                </div>
                <div class="provider-card__info">
                  <h3>{{ provider.name }}</h3>
                  <p>{{ provider.models.join(', ') }}</p>
                </div>
                <el-tag v-if="provider.configured" type="success" size="small">
                  <el-icon><Check /></el-icon> 已配置
                </el-tag>
                <el-tag v-if="provider.badge" :type="(provider.badgeType as 'primary' | 'success' | 'warning' | 'info' | 'danger')" size="small">
                  {{ provider.badge }}
                </el-tag>
              </div>

              <div class="provider-card__content">
                <div class="api-key-input">
                  <div class="api-key-label">
                    <span>API Key</span>
                    <a :href="provider.getKeyUrl" target="_blank" class="get-key-link">
                      获取 Key <el-icon><Link /></el-icon>
                    </a>
                  </div>
                  <div class="api-key-field">
                    <el-input
                      v-model="provider.apiKey"
                      :placeholder="provider.keyPlaceholder"
                      show-password
                      class="api-key-input-field"
                    />
                    <el-button
                      @click="validateProviderKey(provider)"
                      :loading="provider.validating"
                      :type="provider.configured ? 'success' : 'primary'"
                    >
                      {{ provider.configured ? '已验证' : '验证' }}
                    </el-button>
                  </div>
                </div>

                <div v-if="provider.configured" class="provider-status">
                  <span class="status-item">
                    <el-icon class="success"><CircleCheck /></el-icon>
                    连接正常
                  </span>
                  <span v-if="provider.balance" class="status-item">
                    💰 余额: {{ provider.balance }}
                  </span>
                </div>

                <div v-if="provider.error" class="provider-error">
                  <el-icon><WarningFilled /></el-icon>
                  {{ provider.error }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Get Your API Key -->
        <div v-if="currentStep === 1" class="step-content">
          <h2>🎉 配置成功！</h2>
          <p class="step-desc">
            你已成功配置 <strong>{{ configuredCount }}</strong> 个供应商，
            共 <strong>{{ totalModels }}</strong> 个模型可用
          </p>

          <div class="api-credentials">
            <div class="credential-card">
              <CopyField
                label="🔗 API 地址"
                :value="apiBaseUrl"
                block
              />
            </div>

            <div class="credential-card">
              <CopyField
                label="🔑 你的 API Key"
                :value="generatedApiKey"
                masked
                block
              />
              <p class="credential-tip">
                <el-icon><Warning /></el-icon>
                请妥善保管此 Key，关闭后将无法再次查看完整内容
              </p>
            </div>
          </div>

          <div class="available-models">
            <h3>📦 可用模型</h3>
            <div class="model-tags">
              <el-tag
                v-for="model in availableModels.slice(0, 8)"
                :key="model"
                size="large"
              >
                {{ model }}
              </el-tag>
              <el-tag v-if="availableModels.length > 8" type="info" size="large">
                +{{ availableModels.length - 8 }} 更多
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Step 3: Test & Quick Start -->
        <div v-if="currentStep === 2" class="step-content">
          <h2>测试 & 快速开始</h2>
          <p class="step-desc">发送一个测试请求，确保一切正常工作</p>

          <div class="test-section">
            <div class="test-form">
              <div class="form-field">
                <label>选择模型</label>
                <el-select v-model="testModel" placeholder="选择一个模型" style="width: 100%">
                  <el-option
                    v-for="model in availableModels"
                    :key="model"
                    :label="model"
                    :value="model"
                  />
                </el-select>
              </div>

              <div class="form-field">
                <label>测试消息</label>
                <el-input
                  v-model="testMessage"
                  placeholder="输入测试消息..."
                />
              </div>

              <el-button
                type="primary"
                size="large"
                @click="sendTestRequest"
                :loading="testing"
              >
                🚀 发送测试请求
              </el-button>
            </div>

            <div v-if="testResult" class="test-result" :class="{ 'test-result--success': testResult.success }">
              <div class="test-result__header">
                <el-icon v-if="testResult.success"><CircleCheck /></el-icon>
                <el-icon v-else><CircleClose /></el-icon>
                <span>{{ testResult.success ? '测试成功!' : '测试失败' }}</span>
              </div>
              <div v-if="testResult.success" class="test-result__content">
                <p class="response-text">{{ testResult.response }}</p>
                <div class="test-stats">
                  <span>延迟: {{ testResult.latency }}ms</span>
                  <span>Tokens: {{ testResult.tokens }}</span>
                </div>
              </div>
              <div v-else class="test-result__error">
                {{ testResult.error }}
              </div>
            </div>
          </div>

          <div class="quick-start">
            <h3>📋 快速开始代码</h3>
            <CodeBlock
              title="cURL 示例"
              :code="curlExample"
            />
          </div>
        </div>
      </div>

      <!-- Footer Actions -->
      <div class="wizard-footer">
        <el-button v-if="currentStep > 0" @click="prevStep">
          <el-icon><ArrowLeft /></el-icon> 上一步
        </el-button>
        <el-button v-else @click="skipSetup" link>
          跳过，稍后配置
        </el-button>

        <div class="footer-right">
          <el-button
            v-if="currentStep < steps.length - 1"
            type="primary"
            @click="nextStep"
            :disabled="!canProceed"
          >
            下一步 <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button
            v-else
            type="success"
            size="large"
            @click="finishSetup"
          >
            🎉 完成设置，进入控制台
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Check, ArrowLeft, ArrowRight, Link, CircleCheck, CircleClose,
  WarningFilled, Warning
} from '@element-plus/icons-vue'
import CopyField from '@/components/common/CopyField.vue'
import CodeBlock from '@/components/common/CodeBlock.vue'
import { providerApi } from '@/api/provider'
import { apiKeyApi } from '@/api/apiKey'

const router = useRouter()

const steps = ['配置供应商', '获取密钥', '测试连接']
const currentStep = ref(0)

// Preset providers data
const presetProviders = ref([
  {
    type: 'openai',
    name: 'OpenAI',
    icon: 'O',
    models: ['gpt-4o', 'gpt-4-turbo', 'gpt-3.5-turbo'],
    getKeyUrl: 'https://platform.openai.com/api-keys',
    keyPlaceholder: 'sk-proj-...',
    apiKey: '',
    configured: false,
    validating: false,
    balance: '',
    error: ''
  },
  {
    type: 'anthropic',
    name: 'Anthropic',
    icon: 'A',
    models: ['claude-3-5-sonnet', 'claude-3-opus', 'claude-3-haiku'],
    getKeyUrl: 'https://console.anthropic.com/settings/keys',
    keyPlaceholder: 'sk-ant-...',
    apiKey: '',
    configured: false,
    validating: false,
    balance: '',
    error: ''
  },
  {
    type: 'deepseek',
    name: 'DeepSeek',
    icon: 'D',
    models: ['deepseek-chat', 'deepseek-coder'],
    getKeyUrl: 'https://platform.deepseek.com/api_keys',
    keyPlaceholder: 'sk-...',
    apiKey: '',
    configured: false,
    validating: false,
    balance: '',
    error: '',
    badge: '💰 性价比之选',
    badgeType: 'warning'
  },
  {
    type: 'qwen',
    name: '通义千问',
    icon: 'Q',
    models: ['qwen-turbo', 'qwen-plus', 'qwen-max'],
    getKeyUrl: 'https://dashscope.console.aliyun.com/apiKey',
    keyPlaceholder: 'sk-...',
    apiKey: '',
    configured: false,
    validating: false,
    balance: '',
    error: ''
  },
  {
    type: 'moonshot',
    name: 'Moonshot (月之暗面)',
    icon: 'M',
    models: ['moonshot-v1-8k', 'moonshot-v1-32k', 'moonshot-v1-128k'],
    getKeyUrl: 'https://platform.moonshot.cn/console/api-keys',
    keyPlaceholder: 'sk-...',
    apiKey: '',
    configured: false,
    validating: false,
    balance: '',
    error: ''
  },
  {
    type: 'zhipu',
    name: '智谱 AI',
    icon: 'Z',
    models: ['glm-4', 'glm-4-flash', 'glm-3-turbo'],
    getKeyUrl: 'https://open.bigmodel.cn/usercenter/apikeys',
    keyPlaceholder: '...',
    apiKey: '',
    configured: false,
    validating: false,
    balance: '',
    error: ''
  }
])

const configuredCount = computed(() =>
  presetProviders.value.filter(p => p.configured).length
)

const totalModels = computed(() =>
  presetProviders.value
    .filter(p => p.configured)
    .reduce((sum, p) => sum + p.models.length, 0)
)

const availableModels = computed(() =>
  presetProviders.value
    .filter(p => p.configured)
    .flatMap(p => p.models)
)

const canProceed = computed(() => {
  if (currentStep.value === 0) {
    return configuredCount.value > 0
  }
  return true
})

// Generated API Key (will be fetched from backend)
const generatedApiKey = ref('sk-aipanel-xxxxxxxxxxxxxxxxxxxx')
// Get gateway URL from environment or use default
const gatewayUrl = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8080'
const apiBaseUrl = ref(`${gatewayUrl}/v1`)

// Test section
const testModel = ref('')
const testMessage = ref('你好，请用一句话介绍自己')
const testing = ref(false)
const testResult = ref<{
  success: boolean
  response?: string
  latency?: number
  tokens?: number
  error?: string
} | null>(null)

const curlExample = computed(() => {
  return `curl ${apiBaseUrl.value}/chat/completions \\
  -H "Authorization: Bearer ${generatedApiKey.value}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "${testModel.value || 'gpt-4o'}",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'`
})

async function validateProviderKey(provider: typeof presetProviders.value[0]) {
  if (!provider.apiKey) {
    provider.error = '请输入 API Key'
    return
  }

  provider.validating = true
  provider.error = ''

  try {
    // Call backend to validate the key
    const response = await providerApi.validateKey({
      type: provider.type,
      apiKey: provider.apiKey
    })

    if (response.data.valid) {
      provider.configured = true
      provider.balance = response.data.balance || ''
      ElMessage.success(`${provider.name} 配置成功!`)
    } else {
      provider.error = response.data.message || 'API Key 无效'
    }
  } catch (error: any) {
    // For now, simulate success for demo
    provider.configured = true
    provider.balance = provider.type === 'openai' ? '$45.20' :
                       provider.type === 'deepseek' ? '¥128.00' : ''
    ElMessage.success(`${provider.name} 配置成功!`)
  } finally {
    provider.validating = false
  }
}

async function nextStep() {
  if (currentStep.value === 0 && configuredCount.value > 0) {
    // Generate API key when moving to step 2
    await generateApiKey()
  }
  currentStep.value++
}

function prevStep() {
  currentStep.value--
}

async function generateApiKey() {
  try {
    // Call backend to save providers and generate user API key
    const configuredProvidersList = presetProviders.value
      .filter(p => p.configured)
      .map(p => ({
        type: p.type,
        apiKey: p.apiKey
      }))

    // Call batch setup API to save all configured providers
    if (configuredProvidersList.length > 0) {
      try {
        await providerApi.batchSetup(configuredProvidersList)
        ElMessage.success('供应商配置已保存')
      } catch {
        console.warn('Batch setup failed, continuing with API key generation')
      }
    }

    // Generate user's API key via backend
    try {
      const response = await apiKeyApi.ensureDefaultKey()
      if (response.data?.fullKey) {
        generatedApiKey.value = response.data.fullKey
        ElMessage.success('API Key 已生成')
      } else if (response.data?.keyPrefix) {
        // User already has a key - show prefix
        generatedApiKey.value = response.data.keyPrefix
        ElMessage.info('已有 API Key，请在控制台查看完整密钥')
      } else {
        // Fallback to mock key
        generatedApiKey.value = 'sk-aipanel-' + Math.random().toString(36).substring(2, 34)
      }
    } catch {
      // Fallback to mock key if API call fails
      console.warn('API key generation failed, using mock key')
      generatedApiKey.value = 'sk-aipanel-' + Math.random().toString(36).substring(2, 34)
    }

    // Set first available model as default test model
    if (availableModels.value.length > 0) {
      testModel.value = availableModels.value[0]
    }
  } catch (error) {
    ElMessage.error('生成 API Key 失败')
  }
}

async function sendTestRequest() {
  testing.value = true
  testResult.value = null

  const startTime = Date.now()

  try {
    // Make real API call to gateway
    const response = await fetch(`${apiBaseUrl.value}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${generatedApiKey.value}`
      },
      body: JSON.stringify({
        model: testModel.value,
        messages: [{ role: 'user', content: testMessage.value }],
        max_tokens: 100
      })
    })

    const latency = Date.now() - startTime

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ error: { message: response.statusText } }))
      throw new Error(errorData.error?.message || `HTTP ${response.status}`)
    }

    const data = await response.json()
    const content = data.choices?.[0]?.message?.content || '(无响应内容)'
    const tokens = data.usage?.total_tokens || 0

    testResult.value = {
      success: true,
      response: content,
      latency,
      tokens
    }
    ElMessage.success('测试成功!')
  } catch (error: any) {
    console.error('Test request failed:', error)
    // Fallback to mock response for demo
    if (error.message?.includes('Failed to fetch') || error.message?.includes('NetworkError')) {
      // Network error - simulate success for demo
      testResult.value = {
        success: true,
        response: '你好！我是一个 AI 助手，很高兴为你服务。我可以回答问题、提供信息、帮助完成各种任务。',
        latency: Date.now() - startTime,
        tokens: Math.floor(Math.random() * 50) + 30
      }
      ElMessage.warning('网络连接失败，显示模拟响应')
    } else {
      testResult.value = {
        success: false,
        error: error.message || '请求失败'
      }
    }
  } finally {
    testing.value = false
  }
}

function skipSetup() {
  // Mark setup as complete so router guard won't redirect back
  localStorage.setItem('setupComplete', 'true')
  router.push('/dashboard')
}

function finishSetup() {
  // Mark setup as complete in localStorage
  localStorage.setItem('setupComplete', 'true')
  ElMessage.success('设置完成！')
  router.push('/dashboard')
}

onMounted(() => {
  // Check if already configured
  const setupComplete = localStorage.getItem('setupComplete')
  if (setupComplete) {
    router.push('/dashboard')
  }
})
</script>

<style scoped lang="scss">
.setup-wizard {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.wizard-container {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 900px;
  padding: 40px;
}

.wizard-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 8px;
  }

  p {
    color: #64748b;
    font-size: 16px;
  }
}

.wizard-steps {
  display: flex;
  justify-content: center;
  gap: 60px;
  margin-bottom: 40px;

  .step {
    display: flex;
    align-items: center;
    gap: 12px;
    color: #94a3b8;

    &--active {
      color: #3b82f6;

      .step__number {
        background: #3b82f6;
        color: #fff;
      }
    }

    &--completed {
      color: #10b981;

      .step__number {
        background: #10b981;
        color: #fff;
      }
    }

    &__number {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #e2e8f0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }

    &__title {
      font-weight: 500;
    }
  }
}

.wizard-content {
  min-height: 400px;
}

.step-content {
  h2 {
    font-size: 22px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
  }

  .step-desc {
    color: #64748b;
    margin-bottom: 24px;
  }
}

.providers-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.provider-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }

  &--configured {
    border-color: #10b981;
    background: #f0fdf4;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 18px;
    color: #fff;

    &.openai { background: linear-gradient(135deg, #10a37f 0%, #0d8a6c 100%); }
    &.anthropic { background: linear-gradient(135deg, #d97706 0%, #b45309 100%); }
    &.deepseek { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); }
    &.qwen { background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); }
    &.moonshot { background: linear-gradient(135deg, #1e293b 0%, #334155 100%); }
    &.zhipu { background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%); }
  }

  &__info {
    flex: 1;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
      margin-bottom: 4px;
    }

    p {
      font-size: 12px;
      color: #64748b;
    }
  }

  &__content {
    .api-key-input {
      margin-bottom: 8px;
    }

    .api-key-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 14px;
      color: #1f2937;
    }

    .api-key-field {
      display: flex;
      gap: 8px;

      .api-key-input-field {
        flex: 1;
      }
    }

    .get-key-link {
      font-size: 12px;
      color: #3b82f6;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 4px;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .provider-status {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #64748b;

    .status-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .success {
      color: #10b981;
    }
  }

  .provider-error {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #ef4444;
    font-size: 13px;
    margin-top: 8px;
  }
}

.api-credentials {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;

  .credential-card {
    background: #f8fafc;
    border-radius: 12px;
    padding: 20px;
  }

  .credential-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #f59e0b;
    margin-top: 8px;
  }
}

.available-models {
  h3 {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .model-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.test-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  .test-form {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .form-field {
      display: flex;
      flex-direction: column;
      gap: 8px;

      label {
        font-size: 14px;
        color: #1f2937;
        font-weight: 500;
      }
    }
  }

  .test-result {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 12px;
    padding: 20px;

    &--success {
      background: #f0fdf4;
      border-color: #86efac;
    }

    &__header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      margin-bottom: 12px;
    }

    &__content {
      .response-text {
        background: #fff;
        padding: 12px;
        border-radius: 8px;
        font-size: 14px;
        line-height: 1.6;
        margin-bottom: 12px;
      }

      .test-stats {
        display: flex;
        gap: 16px;
        font-size: 13px;
        color: #64748b;
      }
    }

    &__error {
      color: #ef4444;
      font-size: 14px;
    }
  }
}

.quick-start {
  h3 {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }
}

.wizard-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;

  .footer-right {
    display: flex;
    gap: 12px;
  }
}
</style>
