<script setup>
import { computed, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

/** 管理员用 /admin 前缀，评审员用 /workbench 前缀，菜单路径随之切换 */
const isBackendStaff = computed(() => userStore.hasAnyRole(['ADMIN', 'SUPER_ADMIN']))
const workbenchBase = computed(() => (isBackendStaff.value ? '/admin' : '/workbench'))

/** 面包屑「工作台」回首页：管理员回概览，评审员回贡献审核 */
const workbenchHome = computed(() =>
  isBackendStaff.value ? '/admin/dashboard' : '/workbench/reviews/contributions',
)

/**
 * 后台布局：左侧分组菜单 + 右侧内容。
 *
 * 菜单按业务域分组，避免十几项平铺造成扫读负担；
 * 菜单项按角色过滤只是体验优化，真正的权限边界在后端接口。
 */
const menuGroups = computed(() => {
  const groups = [
    {
      label: '总览',
      items: [
        { path: '/admin/dashboard', label: '概览', role: ['ADMIN', 'SUPER_ADMIN'] },
      ],
    },
    {
      label: '内容',
      items: [
        { path: '/admin/articles', label: '文章管理', role: ['ADMIN', 'SUPER_ADMIN'] },
        { path: '/admin/categories', label: '分类管理', role: ['ADMIN', 'SUPER_ADMIN'] },
        { path: '/admin/topics', label: '专题管理', role: ['ADMIN', 'SUPER_ADMIN'] },
      ],
    },
    {
      label: '审核',
      items: [
        { path: `${workbenchBase.value}/reviews/contributions`, label: '知识贡献审核', role: ['REVIEWER', 'ADMIN', 'SUPER_ADMIN'] },
        { path: `${workbenchBase.value}/reviews/works`, label: '作品审核', role: ['REVIEWER', 'ADMIN', 'SUPER_ADMIN'] },
        { path: `${workbenchBase.value}/reviews/events`, label: '活动审核', role: ['REVIEWER', 'ADMIN', 'SUPER_ADMIN'] },
      ],
    },
    {
      label: '运营',
      items: [
        { path: '/admin/works', label: '作品管理', role: ['ADMIN', 'SUPER_ADMIN'] },
        { path: '/admin/events', label: '活动管理', role: ['ADMIN', 'SUPER_ADMIN'] },
        { path: '/admin/feedback', label: '内容反馈', role: ['ADMIN', 'SUPER_ADMIN'] },
        { path: '/admin/comments', label: '评论管理', role: ['ADMIN', 'SUPER_ADMIN'] },
      ],
    },
    {
      label: '系统',
      items: [
        { path: '/admin/users', label: '用户与角色', role: ['SUPER_ADMIN'] },
        { path: '/admin/settings', label: '系统设置', role: ['SUPER_ADMIN'] },
      ],
    },
  ]

  // 空组直接丢弃，避免出现只有标题没有菜单项的分组
  return groups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => userStore.hasAnyRole(item.role)),
    }))
    .filter((group) => group.items.length > 0)
})

const activePath = computed(() => route.path)

/** 窄屏下的菜单折叠 */
const sideOpen = ref(false)

watch(
  () => route.path,
  () => {
    sideOpen.value = false
  },
)
</script>

<template>
  <div class="admin-shell">
    <!-- 窄屏切换按钮 -->
    <button
      type="button"
      class="admin-toggle"
      :aria-expanded="sideOpen"
      @click="sideOpen = !sideOpen"
    >
      <span class="admin-toggle__bars"><i /><i /><i /></span>
      工作台菜单
    </button>

    <aside class="admin-side" :class="{ 'admin-side--open': sideOpen }">
      <div class="admin-side__head">
        <span class="admin-side__title">工作台</span>
        <el-tag size="small" effect="plain" type="warning">{{ userStore.isSuperAdmin ? '超管' : isBackendStaff ? '管理' : '评审' }}</el-tag>
      </div>

      <nav class="admin-side__nav">
        <div v-for="group in menuGroups" :key="group.label" class="admin-side__group">
          <p class="admin-side__label">{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="admin-side__item"
            :class="{ 'admin-side__item--active': activePath === item.path }"
          >
            {{ item.label }}
          </RouterLink>
        </div>
      </nav>

      <p class="admin-side__note">
        菜单按角色显示，越权请求仍会被后端拒绝。
      </p>
    </aside>

    <section class="admin-main">
      <!-- 面包屑：窄屏侧栏收起后，这里仍能说明「当前在哪个页面」 -->
      <el-breadcrumb separator="/" class="admin-crumb">
        <el-breadcrumb-item :to="{ path: workbenchHome }">工作台</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta?.title">{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 196px 1fr;
  gap: var(--sp-gap-lg);
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
  align-items: start;
}

/* 窄屏切换按钮：桌面端隐藏 */
.admin-toggle {
  display: none;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin-bottom: var(--sp-gap-sm);
  padding: 10px 14px;
  border: 1px solid var(--sp-border);
  border-radius: var(--sp-radius-sm);
  background: #fff;
  font-size: 13px;
  color: var(--sp-text);
  cursor: pointer;
}

.admin-toggle__bars {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.admin-toggle__bars i {
  display: block;
  width: 13px;
  height: 1.5px;
  background: var(--sp-text);
  border-radius: 1px;
}

/* ---------- 侧边栏 ---------- */

.admin-side {
  position: sticky;
  top: calc(var(--sp-header-height) + 16px);
  padding: 16px 12px;
  border: 1px solid var(--sp-border);
  border-radius: var(--sp-radius);
  background: var(--sp-surface);
  box-shadow: var(--sp-shadow-sm);
}

.admin-side__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 6px 12px;
  border-bottom: 1px solid var(--sp-border);
  margin-bottom: 10px;
}

.admin-side__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--sp-text-strong);
}

.admin-side__nav {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.admin-side__label {
  margin: 0 0 4px;
  padding-left: 12px;
  font-size: 11px;
  letter-spacing: 0.8px;
  color: var(--sp-text-faint);
}

.admin-side__item {
  display: block;
  padding: 8px 12px;
  border-radius: var(--sp-radius-sm);
  font-size: 13px;
  color: var(--sp-text);
  text-decoration: none;
  transition:
    background var(--sp-transition),
    color var(--sp-transition);
}

.admin-side__item:hover {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.admin-side__item--active {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-weight: 600;
}

.admin-side__note {
  margin: 16px 6px 0;
  padding-top: 12px;
  border-top: 1px dashed var(--sp-border);
  font-size: 11px;
  line-height: 1.7;
  color: var(--sp-text-faint);
}

.admin-main {
  min-width: 0;
}

.admin-crumb {
  margin-bottom: var(--sp-gap);
  font-size: 12px;
}

/* ==========================================================================
   响应式
   ========================================================================== */

@media (max-width: 1024px) {
  .admin-shell {
    grid-template-columns: 176px 1fr;
    gap: var(--sp-gap);
    padding: var(--sp-gap) 18px var(--sp-gap-xl);
  }
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
    padding: var(--sp-gap) 16px var(--sp-gap-lg);
  }

  .admin-toggle {
    display: flex;
  }

  /* 窄屏收起为折叠面板，避免菜单占满首屏 */
  .admin-side {
    display: none;
    position: static;
  }

  .admin-side--open {
    display: block;
  }

  .admin-side__nav {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 10px;
  }

  .admin-side__group {
    flex: 1 1 140px;
  }
}
</style>
