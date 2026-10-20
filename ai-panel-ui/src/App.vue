<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useUserStore } from '@/stores/modules/user'

const userStore = useUserStore()
const locale = computed(() => userStore.language === 'zh-cn' ? zhCn : en)
</script>

<style>
#app {
  height: 100vh;
}
</style>
