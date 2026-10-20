<template>
  <div class="code-block">
    <div class="code-block__header">
      <span class="code-block__title">{{ title }}</span>
      <el-button size="small" @click="handleCopy" :type="copied ? 'success' : 'default'">
        <el-icon><Check v-if="copied" /><CopyDocument v-else /></el-icon>
        {{ copied ? '已复制' : '复制代码' }}
      </el-button>
    </div>
    <pre class="code-block__content"><code>{{ code }}</code></pre>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Check } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  code: string
  title?: string
  language?: string
}>(), {
  title: '示例代码',
  language: 'bash'
})

const copied = ref(false)

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.code)
    copied.value = true
    ElMessage.success('代码已复制到剪贴板')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped lang="scss">
.code-block {
  background: #1e293b;
  border-radius: 8px;
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #334155;
    border-bottom: 1px solid #475569;
  }

  &__title {
    font-size: 12px;
    color: #94a3b8;
    font-weight: 500;
  }

  &__content {
    margin: 0;
    padding: 16px;
    overflow-x: auto;

    code {
      font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
      font-size: 13px;
      line-height: 1.6;
      color: #e2e8f0;
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}
</style>
