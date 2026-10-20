import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types/user'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const language = ref<string>('en')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role?.toUpperCase() === 'ADMIN')

  async function login(data: LoginRequest) {
    const res = await userApi.login(data)
    token.value = res.data.accessToken
    userInfo.value = res.data.user
    return res
  }

  async function register(data: RegisterRequest) {
    const res = await userApi.register(data)
    return res
  }

  async function logout() {
    token.value = ''
    userInfo.value = null
  }

  async function fetchUserInfo() {
    if (!token.value) return
    const res = await userApi.getUserInfo()
    userInfo.value = res.data
  }

  function setLanguage(lang: string) {
    language.value = lang
  }

  return {
    token,
    userInfo,
    language,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout,
    fetchUserInfo,
    setLanguage,
  }
}, {
  persist: {
    paths: ['token', 'language'],
  },
})
