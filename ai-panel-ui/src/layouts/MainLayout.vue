<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <img src="@/assets/logo.svg" alt="Logo" v-if="!isCollapsed" />
        <span v-if="!isCollapsed">AI Panel</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapsed"
        router
        class="sidebar-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
            <Expand v-if="isCollapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">Home</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32">{{ userInitial }}</el-avatar>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/settings')">
                  <el-icon><Setting /></el-icon>Settings
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>Logout
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapsed = ref(false)

const allMenuItems = [
  { path: '/dashboard', title: 'Dashboard', icon: 'Odometer' },
  { path: '/api-keys', title: 'API Keys', icon: 'Key' },
  { path: '/providers', title: 'Providers', icon: 'Connection' },
  { path: '/models', title: 'Models', icon: 'Cpu' },
  { path: '/usage', title: 'Usage & Billing', icon: 'DataLine' },
  { path: '/subscription-requests', title: 'Subscription Requests', icon: 'Tickets' },
  { path: '/delivery/platforms', title: 'Platforms', icon: 'Shop', adminOnly: true },
  { path: '/delivery/skus', title: 'Product SKUs', icon: 'Goods', adminOnly: true },
  { path: '/delivery/orders', title: 'Orders', icon: 'List', adminOnly: true },
  { path: '/settings', title: 'Settings', icon: 'Setting' },
]

// Delivery pages call /api/v1/admin/**, which the backend restricts to role=ADMIN.
const menuItems = computed(() => allMenuItems.filter(m => !m.adminOnly || userStore.isAdmin))

onMounted(() => {
  // userInfo is not persisted; reload it so role-based menu items render after a refresh.
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {})
  }
})

const currentRoute = computed(() => route.path)
const currentTitle = computed(() => {
  const item = allMenuItems.find(m => route.path.startsWith(m.path))
  return item?.title || ''
})

const userInitial = computed(() => {
  return userStore.userInfo?.username?.charAt(0).toUpperCase() || 'U'
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('Are you sure you want to logout?', 'Confirm', {
      confirmButtonText: 'Logout',
      cancelButtonText: 'Cancel',
      type: 'warning',
    })
    await userStore.logout()
    router.push('/login')
  } catch {
    // Cancelled
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #1e293b;
  color: #fff;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    font-size: 20px;
    font-weight: 700;
    color: #fff;
    border-bottom: 1px solid #334155;

    img {
      width: 32px;
      height: 32px;
    }
  }

  .sidebar-menu {
    border-right: none;
    background-color: transparent;

    :deep(.el-menu-item) {
      color: #94a3b8;

      &:hover {
        background-color: #334155;
        color: #fff;
      }

      &.is-active {
        background-color: #3b82f6;
        color: #fff;
      }
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #64748b;

      &:hover {
        color: #3b82f6;
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 8px;

      &:hover {
        background-color: #f1f5f9;
      }

      .username {
        font-weight: 500;
        color: #1f2937;
      }
    }
  }
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
