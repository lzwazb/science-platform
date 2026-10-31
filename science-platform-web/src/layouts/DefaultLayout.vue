<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/notification'
import {
  PRIMARY_NAV,
  ROLE_LABELS,
  buildPersonalItems,
  buildWorkspaceItems,
  primaryRoleLabel as pickRoleLabel,
} from '@/config/navigation'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 主导航（公共入口） */
const primaryNav = PRIMARY_NAV

/** 取最高权限角色用于顶栏展示 */
const primaryRoleLabel = computed(() =>
  ROLE_LABELS[pickRoleLabel(userStore.roles)] || '',
)

/** 角色工作台菜单：按权限动态拼装，无权限的不出现 */
const workspaceItems = computed(() =>
  buildWorkspaceItems(userStore.roles, userStore.isLoggedIn),
)

/** 个人菜单 */
const personalItems = computed(() =>
  buildPersonalItems(userStore.isLoggedIn, unreadCount.value),
)

/** 移动端抽屉 */
const drawerVisible = ref(false)

/** 未读消息数 */
const unreadCount = ref(0)

async function refreshUnread() {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  try {
    const data = await getUnreadCount()
    unreadCount.value = data.unreadCount || 0
  } catch {
    unreadCount.value = 0
  }
}

/** 路由变化时刷新未读数（读完消息返回就能看到数字减少） */
watch(() => route.fullPath, refreshUnread)

watch(drawerVisible, (open) => {
  // 抽屉打开时锁滚动，关闭时恢复
  document.body.style.overflow = open ? 'hidden' : ''
})

function go(path) {
  drawerVisible.value = false
  router.push(path)
}

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
  drawerVisible.value = false
  userStore.logout()
  unreadCount.value = 0
  router.push('/')
}

onMounted(refreshUnread)
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <!-- 品牌 -->
        <RouterLink to="/" class="brand">
          <span class="brand__mark">科</span>
          <span class="brand__text">
            <strong>科普知识共创平台</strong>
            <small>Knowledge Co-creation</small>
          </span>
        </RouterLink>

        <!-- 桌面端主导航 -->
        <nav class="app-nav">
          <RouterLink
            v-for="item in primaryNav"
            :key="item.to"
            :to="item.to"
            class="app-nav__item"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <!-- 右侧操作区 -->
        <div class="app-actions">
          <template v-if="userStore.isLoggedIn">
            <!-- 工作台（按角色出现） -->
            <el-dropdown v-if="workspaceItems.length" trigger="click" placement="bottom-end">
              <button type="button" class="app-actions__workspace">
                工作台
                <span class="app-actions__caret" />
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <template v-for="(item, index) in workspaceItems" :key="item.to">
                    <el-dropdown-item
                      v-if="item.group && (index === 0 || workspaceItems[index - 1].group !== item.group)"
                      disabled
                      class="app-menu__group"
                    >
                      {{ item.group }}
                    </el-dropdown-item>
                    <el-dropdown-item @click="go(item.to)">{{ item.label }}</el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <!-- 消息 -->
            <RouterLink to="/notifications" class="app-actions__icon" title="消息">
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                <span class="app-actions__bell">消息</span>
              </el-badge>
            </RouterLink>

            <!-- 用户菜单 -->
            <el-dropdown trigger="click" placement="bottom-end">
              <span class="app-user__trigger">
                <el-avatar :size="30" :src="userStore.profile?.avatar || undefined">
                  {{ userStore.profile?.nickname?.charAt(0) || '?' }}
                </el-avatar>
                <span class="app-user__name">{{ userStore.profile?.nickname }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled class="app-menu__group">
                    {{ primaryRoleLabel }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-for="item in personalItems"
                    :key="item.to"
                    @click="go(item.to)"
                  >
                    {{ item.label }}
                    <el-badge
                      v-if="item.badge"
                      :value="item.badge"
                      :max="99"
                      class="app-menu__badge"
                    />
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <RouterLink to="/login" class="app-actions__link">登录</RouterLink>
            <RouterLink to="/register" class="app-actions__link app-actions__link--primary">
              注册
            </RouterLink>
          </template>

          <!-- 移动端菜单按钮 -->
          <button
            type="button"
            class="app-actions__burger"
            aria-label="打开菜单"
            @click="drawerVisible = true"
          >
            <span />
            <span />
            <span />
          </button>
        </div>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>

    <footer class="app-footer">
      <p>本科毕业设计 · 基于 Spring Boot + Vue 的科普知识共创与创意传播平台的设计与实现</p>
    </footer>

    <!-- 移动端抽屉菜单：把桌面端的全部入口按分组列出来 -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      size="78%"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer">
        <div class="drawer__head">
          <span class="brand__mark brand__mark--sm">科</span>
          <div class="drawer__user">
            <strong>{{ userStore.profile?.nickname || '未登录' }}</strong>
            <small>{{ primaryRoleLabel }}</small>
          </div>
          <button type="button" class="drawer__close" aria-label="关闭" @click="drawerVisible = false">
            ×
          </button>
        </div>

        <nav class="drawer__group">
          <p class="drawer__label">浏览</p>
          <button
            v-for="item in primaryNav"
            :key="item.to"
            type="button"
            class="drawer__item"
            @click="go(item.to)"
          >
            {{ item.label }}
          </button>
        </nav>

        <nav v-if="workspaceItems.length" class="drawer__group">
          <p class="drawer__label">工作台</p>
          <button
            v-for="item in workspaceItems"
            :key="item.to"
            type="button"
            class="drawer__item"
            @click="go(item.to)"
          >
            {{ item.label }}
          </button>
        </nav>

        <nav v-if="personalItems.length" class="drawer__group">
          <p class="drawer__label">我的</p>
          <button
            v-for="item in personalItems"
            :key="item.to"
            type="button"
            class="drawer__item"
            @click="go(item.to)"
          >
            {{ item.label }}
            <span v-if="item.badge" class="drawer__badge">{{ item.badge }}</span>
          </button>
        </nav>

        <div class="drawer__foot">
          <button
            v-if="userStore.isLoggedIn"
            type="button"
            class="drawer__logout"
            @click="handleLogout"
          >
            退出登录
          </button>
          <template v-else>
            <button type="button" class="drawer__item" @click="go('/login')">登录</button>
            <button type="button" class="drawer__item drawer__item--primary" @click="go('/register')">
              注册
            </button>
          </template>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ---------- 顶栏 ---------- */

.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--sp-border);
}

.app-header__inner {
  max-width: var(--sp-container);
  margin: 0 auto;
  padding: 0 20px;
  height: var(--sp-header-height);
  display: flex;
  align-items: center;
  gap: var(--sp-gap-lg);
}

/* ---------- 品牌 ---------- */

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--sp-text-strong);
  flex-shrink: 0;
}

.brand__mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--sp-primary) 0%, var(--sp-primary-dark) 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.brand__mark--sm {
  width: 30px;
  height: 30px;
  font-size: 14px;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand__text strong {
  font-size: 15px;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.brand__text small {
  font-size: 10px;
  color: var(--sp-text-faint);
  letter-spacing: 0.3px;
}

/* ---------- 主导航：单行、不换行 ---------- */

.app-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.app-nav__item {
  padding: 7px 13px;
  border-radius: var(--sp-radius-sm);
  font-size: 14px;
  color: var(--sp-text);
  text-decoration: none;
  white-space: nowrap;
  transition:
    background var(--sp-transition),
    color var(--sp-transition);
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

/* ---------- 右侧操作区 ---------- */

.app-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-gap-xs);
  flex-shrink: 0;
}

.app-actions__workspace {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 13px;
  border: 1px solid var(--sp-border);
  border-radius: var(--sp-radius-sm);
  background: #fff;
  font-size: 13px;
  color: var(--sp-text);
  cursor: pointer;
  transition:
    border-color var(--sp-transition),
    color var(--sp-transition);
}

.app-actions__workspace:hover {
  border-color: var(--sp-primary-light);
  color: var(--sp-primary-dark);
}

.app-actions__caret {
  width: 0;
  height: 0;
  border-left: 3.5px solid transparent;
  border-right: 3.5px solid transparent;
  border-top: 4px solid currentColor;
  opacity: 0.6;
}

.app-actions__icon {
  text-decoration: none;
}

.app-actions__bell {
  display: inline-block;
  padding: 6px 10px;
  border-radius: var(--sp-radius-sm);
  font-size: 13px;
  color: var(--sp-text);
  transition: background var(--sp-transition);
}

.app-actions__bell:hover {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.app-user__trigger {
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  outline: none;
  padding: 3px 6px 3px 3px;
  border-radius: 20px;
  transition: background var(--sp-transition);
}

.app-user__trigger:hover {
  background: var(--sp-primary-soft);
}

.app-user__name {
  font-size: 13px;
  color: var(--sp-text-strong);
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-actions__link {
  padding: 6px 14px;
  border-radius: var(--sp-radius-sm);
  font-size: 13px;
  color: var(--sp-text);
  text-decoration: none;
  border: 1px solid var(--sp-border);
  transition: all var(--sp-transition);
}

.app-actions__link:hover {
  border-color: var(--sp-primary);
  color: var(--sp-primary-dark);
}

.app-actions__link--primary {
  background: var(--sp-primary);
  border-color: var(--sp-primary);
  color: #fff;
}

.app-actions__link--primary:hover {
  background: var(--sp-primary-dark);
  border-color: var(--sp-primary-dark);
  color: #fff;
}

/* 汉堡按钮：桌面端隐藏 */
.app-actions__burger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 1px solid var(--sp-border);
  border-radius: var(--sp-radius-sm);
  background: #fff;
  cursor: pointer;
}

.app-actions__burger span {
  display: block;
  width: 15px;
  height: 1.5px;
  margin: 0 auto;
  background: var(--sp-text);
  border-radius: 1px;
}

/* 下拉菜单内的小组标题 */
.app-menu__group {
  font-size: 11px !important;
  color: var(--sp-text-faint) !important;
  letter-spacing: 0.6px;
  cursor: default !important;
}

.app-menu__badge {
  margin-left: 6px;
}

/* ---------- 主体与页脚 ---------- */

.app-main {
  flex: 1;
  width: 100%;
}

.app-footer {
  border-top: 1px solid var(--sp-border);
  padding: 18px 20px 22px;
  text-align: center;
  font-size: 12px;
  color: var(--sp-text-faint);
  background: #fff;
}

.app-footer p {
  margin: 0;
}

/* ---------- 移动端抽屉 ---------- */

.drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 4px;
}

.drawer__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 14px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--sp-border);
}

.drawer__user {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}

.drawer__user strong {
  font-size: 14px;
  color: var(--sp-text-strong);
}

.drawer__user small {
  font-size: 11px;
  color: var(--sp-text-faint);
}

.drawer__close {
  margin-left: auto;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  color: var(--sp-text-muted);
  cursor: pointer;
}

.drawer__close:hover {
  background: var(--sp-primary-soft);
}

.drawer__group {
  padding: 6px 0;
}

.drawer__label {
  margin: 0 0 4px;
  padding-left: 2px;
  font-size: 11px;
  letter-spacing: 0.8px;
  color: var(--sp-text-faint);
}

.drawer__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 11px 10px;
  border: none;
  border-radius: var(--sp-radius-sm);
  background: transparent;
  font-size: 14px;
  color: var(--sp-text);
  text-align: left;
  cursor: pointer;
}

.drawer__item:hover,
.drawer__item:active {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.drawer__item--primary {
  justify-content: center;
  background: var(--sp-primary);
  color: #fff;
}

.drawer__item--primary:hover {
  background: var(--sp-primary-dark);
  color: #fff;
}

.drawer__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.drawer__foot {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--sp-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer__logout {
  width: 100%;
  padding: 11px;
  border: 1px solid var(--sp-border);
  border-radius: var(--sp-radius-sm);
  background: #fff;
  font-size: 14px;
  color: #e5534b;
  cursor: pointer;
}

/* ==========================================================================
   响应式：窄屏收起主导航，改用抽屉
   ========================================================================== */

@media (max-width: 1024px) {
  .app-nav__item {
    padding: 7px 10px;
    font-size: 13px;
  }

  .brand__text small {
    display: none;
  }
}

@media (max-width: 860px) {
  .app-nav {
    display: none;
  }

  .app-actions__workspace,
  .app-actions__icon,
  .app-user__name {
    display: none;
  }

  .app-actions__burger {
    display: flex;
  }

  .app-header__inner {
    gap: var(--sp-gap);
    padding: 0 16px;
  }
}

@media (max-width: 375px) {
  .app-header__inner {
    padding: 0 12px;
  }

  .brand__text strong {
    font-size: 14px;
  }
}
</style>
