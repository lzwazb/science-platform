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
 *   roles         需要的角色，任一命中即可；父级设置会被子级继承
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
        path: 'knowledge',
        name: 'knowledge',
        component: () => import('@/views/knowledge/KnowledgeListView.vue'),
        meta: { title: '知识库' },
      },
      {
        path: 'knowledge/:id',
        name: 'article-detail',
        component: () => import('@/views/knowledge/ArticleDetailView.vue'),
        meta: { title: '文章详情' },
      },
      {
        path: 'topics',
        name: 'topics',
        component: () => import('@/views/knowledge/TopicListView.vue'),
        meta: { title: '专题' },
      },
      {
        path: 'topics/:id',
        name: 'topic-detail',
        component: () => import('@/views/knowledge/TopicDetailView.vue'),
        meta: { title: '专题详情' },
      },
      {
        path: 'works',
        name: 'works',
        component: () => import('@/views/work/WorkListView.vue'),
        meta: { title: '创意作品' },
      },
      {
        path: 'works/:id',
        name: 'work-detail',
        component: () => import('@/views/work/WorkDetailView.vue'),
        meta: { title: '作品详情' },
      },
      {
        path: 'creator/works',
        name: 'creator-works',
        component: () => import('@/views/creator/CreatorWorksView.vue'),
        meta: { title: '创作者中心', requiresAuth: true, roles: ['CREATOR', 'ADMIN', 'SUPER_ADMIN'] },
      },
      {
        path: 'review/works',
        name: 'review-works',
        component: () => import('@/views/review/ReviewWorksView.vue'),
        meta: { title: '作品审核', requiresAuth: true, roles: ['REVIEWER', 'ADMIN', 'SUPER_ADMIN'] },
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
      {
        path: 'admin',
        component: () => import('@/layouts/AdminLayout.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'] },
        children: [
          {
            path: '',
            redirect: '/admin/articles',
          },
          {
            path: 'articles',
            name: 'admin-articles',
            component: () => import('@/views/admin/AdminArticleView.vue'),
            meta: { title: '文章管理' },
          },
          {
            path: 'categories',
            name: 'admin-categories',
            component: () => import('@/views/admin/AdminCategoryView.vue'),
            meta: { title: '分类管理' },
          },
          {
            path: 'topics',
            name: 'admin-topics',
            component: () => import('@/views/admin/AdminTopicView.vue'),
            meta: { title: '专题管理' },
          },
        ],
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
