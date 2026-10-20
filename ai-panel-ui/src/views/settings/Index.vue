<template>
  <div class="settings-page">
    <div class="page-header">
      <h1>Settings</h1>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="Profile" name="profile">
        <div class="settings-section">
          <h3>Profile Information</h3>
          <el-form :model="profileForm" label-position="top" style="max-width: 500px">
            <el-form-item label="Username">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="Email">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="Nickname">
              <el-input v-model="profileForm.displayName" placeholder="Display name" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">Save Changes</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Security" name="security">
        <div class="settings-section">
          <h3>Change Password</h3>
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            style="max-width: 500px"
          >
            <el-form-item label="Current Password" prop="currentPassword">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item label="New Password" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item label="Confirm New Password" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword">Update Password</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Plan" name="plan">
        <div class="settings-section">
          <h3>Current Plan</h3>
          <div class="plan-card">
            <div class="plan-header">
              <span class="plan-name">{{ currentPlan.name }}</span>
              <el-tag :type="currentPlan.type === 'FREE' ? 'info' : 'success'">
                {{ currentPlan.type }}
              </el-tag>
            </div>
            <div class="plan-details">
              <div class="plan-item">
                <span>Monthly Quota</span>
                <strong>{{ formatNumber(currentPlan.monthlyQuota) }} tokens</strong>
              </div>
              <div class="plan-item">
                <span>Rate Limit</span>
                <strong>{{ currentPlan.rateLimitPerMinute }} req/min</strong>
              </div>
              <div class="plan-item">
                <span>API Keys</span>
                <strong>{{ currentPlan.maxApiKeys }} keys</strong>
              </div>
            </div>
            <el-progress
              :percentage="usagePercent"
              :stroke-width="10"
              :status="usagePercent > 90 ? 'exception' : ''"
            />
            <div class="usage-text">
              {{ formatNumber(currentPlan.usedQuota) }} / {{ formatNumber(currentPlan.monthlyQuota) }} tokens used
            </div>
          </div>

          <h3 style="margin-top: 32px">Upgrade Plan</h3>
          <el-row :gutter="20">
            <el-col v-for="plan in plans" :key="plan.type" :xs="24" :sm="12" :lg="6">
              <div
                class="upgrade-card"
                :class="{ current: plan.type === currentPlan.type }"
              >
                <div class="upgrade-name">{{ plan.name }}</div>
                <div class="upgrade-price">
                  ${{ plan.price }}<span>/month</span>
                </div>
                <ul class="upgrade-features">
                  <li>{{ formatNumber(plan.monthlyQuota) }} tokens/month</li>
                  <li>{{ plan.rateLimitPerMinute }} req/min</li>
                  <li>{{ plan.maxApiKeys }} API keys</li>
                </ul>
                <el-button
                  :type="plan.type === currentPlan.type ? 'info' : 'primary'"
                  :disabled="plan.type === currentPlan.type"
                >
                  {{ plan.type === currentPlan.type ? 'Current Plan' : 'Upgrade' }}
                </el-button>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Preferences" name="preferences">
        <div class="settings-section">
          <h3>Display Preferences</h3>
          <el-form label-position="left" label-width="150px" style="max-width: 400px">
            <el-form-item label="Language">
              <el-select v-model="preferences.language">
                <el-option label="English" value="en" />
                <el-option label="中文" value="zh-cn" />
              </el-select>
            </el-form-item>
            <el-form-item label="Theme">
              <el-select v-model="preferences.theme">
                <el-option label="Light" value="light" />
                <el-option label="Dark" value="dark" />
                <el-option label="System" value="system" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/modules/user'
import { userApi } from '@/api/user'

const userStore = useUserStore()
const activeTab = ref('profile')
const passwordFormRef = ref<FormInstance>()

const profileForm = reactive({
  username: userStore.userInfo?.username || 'demo',
  email: userStore.userInfo?.email || 'demo@example.com',
  displayName: userStore.userInfo?.displayName || '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules: FormRules = {
  currentPassword: [{ required: true, message: 'Please enter current password', trigger: 'blur' }],
  newPassword: [
    { required: true, message: 'Please enter new password', trigger: 'blur' },
    { min: 8, message: 'Password must be at least 8 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('Passwords do not match'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const currentPlan = reactive({
  type: 'PRO',
  name: 'Pro',
  monthlyQuota: 5000000,
  usedQuota: 1256000,
  rateLimitPerMinute: 120,
  maxApiKeys: 10,
})

const usagePercent = computed(() => {
  return Math.round((currentPlan.usedQuota / currentPlan.monthlyQuota) * 100)
})

const plans = [
  { type: 'FREE', name: 'Free', price: 0, monthlyQuota: 100000, rateLimitPerMinute: 20, maxApiKeys: 2 },
  { type: 'BASIC', name: 'Basic', price: 9, monthlyQuota: 1000000, rateLimitPerMinute: 60, maxApiKeys: 5 },
  { type: 'PRO', name: 'Pro', price: 29, monthlyQuota: 5000000, rateLimitPerMinute: 120, maxApiKeys: 10 },
  { type: 'ENTERPRISE', name: 'Enterprise', price: 99, monthlyQuota: 50000000, rateLimitPerMinute: 500, maxApiKeys: 50 },
]

const preferences = reactive({
  language: userStore.language,
  theme: 'light',
})

function formatNumber(num: number): string {
  if (num >= 1000000) return (num / 1000000).toFixed(0) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(0) + 'K'
  return num.toString()
}

function saveProfile() {
  ElMessage.success('Profile updated successfully')
}

async function changePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await userApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('Password changed successfully')
  } catch {
    // request interceptor already surfaces the API error message
  }
}
</script>

<style scoped lang="scss">
.settings-page {
  .settings-section {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 20px;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
      margin-bottom: 20px;
    }
  }

  .plan-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    padding: 24px;
    color: #fff;
    max-width: 400px;

    .plan-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .plan-name {
        font-size: 24px;
        font-weight: 700;
      }
    }

    .plan-details {
      display: flex;
      gap: 24px;
      margin-bottom: 20px;

      .plan-item {
        span {
          display: block;
          font-size: 12px;
          opacity: 0.8;
        }

        strong {
          font-size: 14px;
        }
      }
    }

    .usage-text {
      font-size: 12px;
      margin-top: 8px;
      opacity: 0.9;
    }
  }

  .upgrade-card {
    background: #fff;
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    padding: 24px;
    text-align: center;
    margin-bottom: 20px;
    transition: border-color 0.2s;

    &:hover {
      border-color: #3b82f6;
    }

    &.current {
      border-color: #10b981;
      background: #f0fdf4;
    }

    .upgrade-name {
      font-size: 18px;
      font-weight: 600;
      color: #1f2937;
    }

    .upgrade-price {
      font-size: 32px;
      font-weight: 700;
      color: #3b82f6;
      margin: 12px 0;

      span {
        font-size: 14px;
        font-weight: 400;
        color: #64748b;
      }
    }

    .upgrade-features {
      list-style: none;
      padding: 0;
      margin: 0 0 20px 0;
      font-size: 14px;
      color: #64748b;

      li {
        padding: 4px 0;
      }
    }
  }
}
</style>
