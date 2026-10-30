import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useUserStore } from '@/stores/user'

/**
 * 路由表（开发档案第 9 章）。
 *
 * meta 约定：
 *   requiresAuth  需要登录，未登录跳转 /login 并带回跳地址
 *   guestOnly     仅未登录可访问（登录、注册），已登录时回首页
 *   roles         需要的角色，任一命中即可
 *
 * 注意：路由守卫只改善体验，真实授权在后端完成。
 */
const routes = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/public/HomeView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '登录', guestOnly: true },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { title: '注册', guestOnly: true },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 刷新页面后 store 里只有令牌、没有用户信息，先补齐
  if (userStore.token && !userStore.profile) {
    await userStore.fetchMe()
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && userStore.isLoggedIn) {
    return { path: '/' }
  }

  if (Array.isArray(to.meta.roles) && to.meta.roles.length > 0) {
    if (!userStore.hasAnyRole(to.meta.roles)) {
      ElMessage.error('没有权限访问该页面')
      return { path: '/' }
    }
  }

  return true
})

router.afterEach((to) => {
  const base = '科普知识共创与创意传播平台'
  document.title = to.meta?.title ? `${to.meta.title} - ${base}` : base
})

export default router
