import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, login as loginApi } from '@/api/auth'

const TOKEN_KEY = 'token'

/**
 * 安全读取令牌。
 *
 * 直接用 localStorage 在非浏览器环境（SSR、Node 脚本、单元测试）会抛 ReferenceError，
 * 这里做一次存在性判断，让 store 可以在无 DOM 的环境里初始化。
 */
function readStoredToken() {
  if (typeof localStorage === 'undefined') return ''
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 登录状态与当前用户信息。
 *
 * 令牌以 localStorage 为唯一来源，store 中的 ref 只是它的镜像，
 * 这样刷新页面后 request.js 的拦截器仍能读到令牌。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(readStoredToken())
  const profile = ref(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))

  /** 角色编码列表，未登录时为空数组 */
  const roles = computed(() => profile.value?.roles ?? [])

  /** 是否具备某个角色 */
  function hasRole(role) {
    return roles.value.includes(role)
  }

  /** 是否具备任一角色 */
  function hasAnyRole(roleList) {
    return roleList.some((role) => roles.value.includes(role))
  }

  /** 是否拥有管理类角色（管理员或超级管理员） */
  const isAdmin = computed(() => hasAnyRole(['ADMIN', 'SUPER_ADMIN']))

  /** 是否具备评审权限（评审员、管理员、超级管理员均可审核） */
  const canReview = computed(() => hasAnyRole(['REVIEWER', 'ADMIN', 'SUPER_ADMIN']))

  function setToken(value) {
    token.value = value || ''
    if (typeof localStorage === 'undefined') return
    if (value) {
      localStorage.setItem(TOKEN_KEY, value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  /**
   * 登录：成功后保存令牌与用户信息。
   */
  async function login(payload) {
    const data = await loginApi(payload)
    setToken(data.token)
    profile.value = data.user
    return data.user
  }

  /**
   * 拉取当前用户信息。
   * 刷新页面后用于恢复登录态；令牌失效时清空本地状态。
   */
  async function fetchMe(force = false) {
    if (!token.value) {
      profile.value = null
      return null
    }
    if (profile.value && !force) {
      return profile.value
    }
    loading.value = true
    try {
      profile.value = await getMe()
      return profile.value
    } catch (error) {
      // 401 已由 request.js 统一提示，这里只负责清理状态
      if (error?.response?.status === 401) {
        clearSession()
      }
      return null
    } finally {
      loading.value = false
    }
  }

  function clearSession() {
    setToken('')
    profile.value = null
  }

  function logout() {
    clearSession()
  }

  return {
    token,
    profile,
    loading,
    isLoggedIn,
    roles,
    isAdmin,
    canReview,
    hasRole,
    hasAnyRole,
    login,
    fetchMe,
    logout,
    clearSession,
  }
})
