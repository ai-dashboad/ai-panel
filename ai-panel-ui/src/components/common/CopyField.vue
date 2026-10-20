<template>
  <div class="copy-field" :class="{ 'copy-field--block': block }">
    <label v-if="label" class="copy-field__label">{{ label }}</label>
    <div class="copy-field__container">
      <div class="copy-field__input-wrapper">
        <input
          ref="inputRef"
          :type="masked && !revealed ? 'password' : 'text'"
          :value="displayValue"
          readonly
          class="copy-field__input"
          :class="{ 'copy-field__input--mono': mono }"
        />
        <div class="copy-field__actions">
          <el-tooltip v-if="masked" :content="revealed ? '隐藏' : '显示'" placement="top">
            <el-button link class="copy-field__btn" @click="revealed = !revealed">
              <el-icon><View v-if="!revealed" /><Hide v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="copied ? '已复制!' : '复制'" placement="top">
            <el-button link class="copy-field__btn" @click="handleCopy">
              <el-icon :class="{ 'copy-success': copied }">
                <Check v-if="copied" /><CopyDocument v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, View, Hide, Check } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  value: string
  label?: string
  masked?: boolean
  mono?: boolean
  block?: boolean
  successMessage?: string
}>(), {
  masked: false,
  mono: true,
  block: false,
  successMessage: '已复制到剪贴板'
})

const inputRef = ref<HTMLInputElement>()
const revealed = ref(false)
const copied = ref(false)

const displayValue = computed(() => {
  if (!props.masked || revealed.value) {
    return props.value
  }
  // Show first 8 and last 4 characters
  if (props.value.length > 16) {
    return props.value.slice(0, 8) + '••••••••' + props.value.slice(-4)
  }
  return '••••••••••••'
})

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.value)
    copied.value = true
    ElMessage.success(props.successMessage)
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    // Fallback for older browsers
    if (inputRef.value) {
      inputRef.value.select()
      document.execCommand('copy')
      copied.value = true
      ElMessage.success(props.successMessage)
      setTimeout(() => {
        copied.value = false
      }, 2000)
    }
  }
}
</script>

<style scoped lang="scss">
.copy-field {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;

  &--block {
    display: flex;
    width: 100%;
  }

  &__label {
    font-size: 13px;
    color: #606266;
    font-weight: 500;
  }

  &__container {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__input-wrapper {
    display: flex;
    align-items: center;
    background: #f5f7fa;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    padding: 0 8px 0 12px;
    transition: border-color 0.2s;
    flex: 1;

    &:hover {
      border-color: #c0c4cc;
    }
  }

  &__input {
    flex: 1;
    border: none;
    background: transparent;
    padding: 8px 0;
    font-size: 14px;
    color: #303133;
    outline: none;
    min-width: 200px;

    &--mono {
      font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
    }
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  &__btn {
    padding: 4px;
    color: #909399;

    &:hover {
      color: #409eff;
    }

    .copy-success {
      color: #67c23a;
    }
  }
}
</style>
