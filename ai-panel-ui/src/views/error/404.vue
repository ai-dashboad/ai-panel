<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>Page not found</p>
      <el-button type="primary" @click="router.push('/')">
        Go to Dashboard
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .content {
    text-align: center;
    color: #fff;

    h1 {
      font-size: 120px;
      font-weight: 700;
      margin: 0;
      text-shadow: 4px 4px 0 rgba(0, 0, 0, 0.2);
    }

    p {
      font-size: 24px;
      margin: 16px 0 32px;
      opacity: 0.9;
    }
  }
}
</style>
