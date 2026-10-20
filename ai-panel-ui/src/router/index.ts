import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/setup',
    name: 'Setup',
    component: () => import('@/views/setup/SetupWizard.vue'),
    meta: { requiresAuth: true, isSetup: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: 'Dashboard', icon: 'Odometer' },
      },
      {
        path: 'api-keys',
        name: 'ApiKeys',
        component: () => import('@/views/api-keys/Index.vue'),
        meta: { title: 'API Keys', icon: 'Key' },
      },
      {
        path: 'providers',
        name: 'Providers',
        component: () => import('@/views/providers/Index.vue'),
        meta: { title: 'Providers', icon: 'Connection' },
      },
      {
        path: 'models',
        name: 'Models',
        component: () => import('@/views/models/Index.vue'),
        meta: { title: 'Models', icon: 'Cpu' },
      },
      {
        path: 'usage',
        name: 'Usage',
        component: () => import('@/views/usage/Index.vue'),
        meta: { title: 'Usage & Billing', icon: 'DataLine' },
      },
      {
        path: 'subscription-requests',
        name: 'SubscriptionRequests',
        component: () => import('@/views/subscription-requests/Index.vue'),
        meta: { title: 'Subscription Requests', icon: 'Tickets' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/Index.vue'),
        meta: { title: 'Settings', icon: 'Setting' },
      },
      {
        path: 'delivery/platforms',
        name: 'DeliveryPlatforms',
        component: () => import('@/views/delivery/Platforms.vue'),
        meta: { title: 'Platforms', icon: 'Shop' },
      },
      {
        path: 'delivery/skus',
        name: 'DeliverySkus',
        component: () => import('@/views/delivery/Skus.vue'),
        meta: { title: 'Product SKUs', icon: 'Goods' },
      },
      {
        path: 'delivery/orders',
        name: 'DeliveryOrders',
        component: () => import('@/views/delivery/Orders.vue'),
        meta: { title: 'Orders', icon: 'List' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false
  const isSetupPage = to.meta.isSetup === true

  if (requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    // Check if setup is complete
    const setupComplete = localStorage.getItem('setupComplete')
    if (!setupComplete) {
      next({ name: 'Setup' })
    } else {
      next({ name: 'Dashboard' })
    }
  } else if (userStore.isLoggedIn && !isSetupPage && to.name !== 'Login' && to.name !== 'Register') {
    // Check if first-time user needs setup
    const setupComplete = localStorage.getItem('setupComplete')
    if (!setupComplete && to.name !== 'Setup') {
      next({ name: 'Setup' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
