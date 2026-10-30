<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const roleLabels = {
  USER: '普通用户',
  CREATOR: '创作者',
  REVIEWER: '评审员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

/** 取最高权限角色用于顶栏展示 */
const primaryRoleLabel = computed(() => {
  const order = ['SUPER_ADMIN', 'ADMIN', 'REVIEWER', 'CREATOR', 'USER']
  const matched = order.find((role) => userStore.roles.includes(role))
  return matched ? roleLabels[matched] : ''
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  userStore.logout()
  router.push('/')
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <RouterLink to="/" class="brand">
          <span class="brand__mark">科</span>
          <span class="brand__text">
            <strong>科普知识共创与创意传播平台</strong>
            <small>Knowledge Co-creation &amp; Creative Communication</small>
          </span>
        </RouterLink>

        <nav class="app-nav">
          <RouterLink to="/" class="app-nav__item">首页</RouterLink>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 4 开放">知识库</span>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 5 开放">创意作品</span>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 8 开放">主题活动</span>
        </nav>

        <div class="app-user">
          <template v-if="userStore.isLoggedIn && userStore.profile">
            <el-dropdown trigger="click">
              <span class="app-user__trigger">
                <el-avatar :size="28" :src="userStore.profile.avatar || undefined">
                  {{ userStore.profile.nickname?.charAt(0) || '?' }}
                </el-avatar>
                <span class="app-user__name">{{ userStore.profile.nickname }}</span>
                <el-tag v-if="primaryRoleLabel" size="small" effect="plain" class="app-user__role">
                  {{ primaryRoleLabel }}
                </el-tag>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <RouterLink to="/login" class="app-user__link">登录</RouterLink>
            <RouterLink to="/register" class="app-user__link app-user__link--primary">注册</RouterLink>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>

    <footer class="app-footer">
      <p>本科毕业设计 · 基于 Spring Boot + Vue 的科普知识共创与创意传播平台的设计与实现</p>
      <p class="app-footer__meta">阶段 3：登录与角色权限</p>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--sp-border);
}

.app-header__inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--sp-text-strong);
  flex-shrink: 0;
}

.brand__mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--sp-primary) 0%, var(--sp-primary-dark) 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand__text strong {
  font-size: 16px;
  letter-spacing: 0.3px;
}

.brand__text small {
  font-size: 11px;
  color: var(--sp-text-muted);
  letter-spacing: 0.4px;
}

.app-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.app-nav__item {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--sp-text);
  text-decoration: none;
  transition: background 0.18s ease, color 0.18s ease;
}

.app-nav__item:hover {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.app-nav__item.router-link-exact-active {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-weight: 600;
}

.app-nav__item--disabled {
  color: var(--sp-text-faint);
  cursor: not-allowed;
}

.app-nav__item--disabled:hover {
  background: transparent;
  color: var(--sp-text-faint);
}

.app-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.app-user__trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.app-user__name {
  font-size: 14px;
  color: var(--sp-text-strong);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-user__role {
  display: none;
}

.app-user__link {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--sp-text);
  text-decoration: none;
  border: 1px solid var(--sp-border);
  transition: all 0.18s ease;
}

.app-user__link:hover {
  border-color: var(--sp-primary);
  color: var(--sp-primary-dark);
}

.app-user__link--primary {
  background: var(--sp-primary);
  border-color: var(--sp-primary);
  color: #fff;
}

.app-user__link--primary:hover {
  background: var(--sp-primary-dark);
  border-color: var(--sp-primary-dark);
  color: #fff;
}

.app-main {
  flex: 1;
  width: 100%;
}

.app-footer {
  border-top: 1px solid var(--sp-border);
  padding: 18px 24px 24px;
  text-align: center;
  font-size: 12px;
  color: var(--sp-text-muted);
  background: #fff;
}

.app-footer p {
  margin: 2px 0;
}

.app-footer__meta {
  color: var(--sp-text-faint);
}

@media (min-width: 1080px) {
  .app-user__role {
    display: inline-flex;
  }
}

@media (max-width: 980px) {
  .app-header__inner {
    height: auto;
    padding: 12px 16px;
    flex-wrap: wrap;
    gap: 10px;
  }

  .app-nav {
    order: 3;
    width: 100%;
    flex-wrap: wrap;
  }

  .brand__text small {
    display: none;
  }
}
</style>
