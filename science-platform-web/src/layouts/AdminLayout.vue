<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

/**
 * 后台布局：左侧菜单 + 右侧内容。
 *
 * 菜单项按角色过滤只是体验优化，真正的权限边界在后端接口。
 */
const menuItems = computed(() => {
  const items = [
    { path: '/admin/articles', label: '文章管理', role: ['ADMIN', 'SUPER_ADMIN'] },
    { path: '/admin/categories', label: '分类管理', role: ['ADMIN', 'SUPER_ADMIN'] },
    { path: '/admin/topics', label: '专题管理', role: ['ADMIN', 'SUPER_ADMIN'] },
    { path: '/admin/events', label: '活动管理', role: ['ADMIN', 'SUPER_ADMIN'] },
    { path: '/admin/feedback', label: '内容反馈', role: ['ADMIN', 'SUPER_ADMIN'] },
  ]
  return items.filter((item) => userStore.hasAnyRole(item.role))
})

const activePath = computed(() => route.path)
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="admin-side__head">
        <span class="admin-side__title">内容管理</span>
        <el-tag size="small" effect="plain" type="warning">后台</el-tag>
      </div>

      <nav class="admin-side__nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="admin-side__item"
          :class="{ 'admin-side__item--active': activePath === item.path }"
        >
          {{ item.label }}
        </RouterLink>
        <span
          v-for="(label, index) in ['作品管理', '用户与角色']"
          :key="`pending-${index}`"
          class="admin-side__item admin-side__item--disabled"
          title="后续阶段开放"
        >
          {{ label }}
        </span>
      </nav>

      <p class="admin-side__note">
        菜单按角色显示，越权请求仍会被后端拒绝。
      </p>
    </aside>

    <section class="admin-main">
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 208px 1fr;
  gap: 20px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 48px;
  align-items: start;
}

.admin-side {
  position: sticky;
  top: 88px;
  padding: 18px 14px;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
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
  font-size: 15px;
  font-weight: 600;
  color: var(--sp-text-strong);
}

.admin-side__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-side__item {
  display: block;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--sp-text);
  text-decoration: none;
  transition: background 0.16s ease, color 0.16s ease;
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

.admin-side__item--disabled {
  color: var(--sp-text-faint);
  cursor: not-allowed;
}

.admin-side__item--disabled:hover {
  background: transparent;
  color: var(--sp-text-faint);
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

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-side {
    position: static;
  }

  .admin-side__nav {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
