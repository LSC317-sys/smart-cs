import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 所有需要认证的页面路由（用户和管理员通用）
const commonRoutes = [
  {
    path: 'chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: { title: '智能问答', roles: ['USER', 'ADMIN'] }
  },
  {
    path: 'knowledge',
    name: 'Knowledge',
    component: () => import('@/views/Knowledge.vue'),
    meta: { title: '知识库管理', roles: ['USER', 'ADMIN'] }
  },
  {
    path: 'profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人信息', roles: ['USER', 'ADMIN'] }
  },
  {
    path: 'settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { title: '系统设置', roles: ['USER', 'ADMIN'] }
  }
]

// 仅管理员可访问的路由
const adminRoutes = [
  {
    path: 'admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { title: '系统管理', roles: ['ADMIN'] }
  }
]

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/admin-register',
    name: 'AdminRegister',
    component: () => import('@/views/AdminRegister.vue'),
    meta: { title: '管理员注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: (to) => {
      const userStore = useUserStore()
      const role = userStore.userInfo?.role
      if (role === 'ADMIN') return '/admin'
      return '/chat'
    },
    children: [
      ...commonRoutes,
      ...adminRoutes
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const title = to.meta?.title || 'SmartCS'
  document.title = title + ' - SmartCS'

  // 公开路由
  if (to.path === '/login' || to.path === '/admin-register') {
    next()
    return
  }

  // 需要登录的路由
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 角色检查
  const requiredRoles = to.meta?.roles || []
  const userRole = userStore.userInfo?.role
  if (requiredRoles.length > 0 && !requiredRoles.includes(userRole)) {
    next('/chat')
    return
  }

  next()
})

export default router