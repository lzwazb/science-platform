import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

/**
 * 路由表（开发档案第 9 章）。
 *
 * 阶段 1 只落地首页与 404 页面；其余页面随阶段 3~9 逐个实现后在此登记，
 * 不提前创建空壳页面冒充完成。
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

router.afterEach((to) => {
  const base = '科普知识共创与创意传播平台'
  document.title = to.meta?.title ? `${to.meta.title} - ${base}` : base
})

export default router
