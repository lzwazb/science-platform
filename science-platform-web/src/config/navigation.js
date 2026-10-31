/**
 * 导航配置
 * --------------------------------------------------------------------------
 * 导航项的「谁能看到」逻辑集中在这里，不散落在布局组件的模板里。
 *
 * 好处有三个：
 * 1. 布局组件只负责渲染，逻辑可单独验证（不用起浏览器）
 * 2. 桌面端顶栏和移动端抽屉共用同一份定义，不会出现两处不一致
 * 3. 调整菜单分组时只改这一个文件
 *
 * 注意：这里的角色判断只是「菜单可见性」，真正的权限边界在后端接口。
 */

/** 主导航：高频公共入口，所有人都能看到 */
export const PRIMARY_NAV = [
  { to: '/', label: '首页' },
  { to: '/knowledge', label: '知识库' },
  { to: '/topics', label: '专题' },
  { to: '/works', label: '创意作品' },
  { to: '/events', label: '主题活动' },
]

/** 角色编码 */
export const ROLES = {
  USER: 'USER',
  CREATOR: 'CREATOR',
  REVIEWER: 'REVIEWER',
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
}

function hasAnyRole(roles, list) {
  return list.some((role) => roles.includes(role))
}

/**
 * 角色工作台菜单。
 *
 * 创作与投稿已对所有登录用户开放，所以「创作者中心」只要求登录；
 * 审核与后台仍然按角色出现。
 *
 * @param {string[]} roles 当前用户角色
 * @param {boolean} isLoggedIn 是否已登录
 * @returns {Array<{to: string, label: string, group?: string}>}
 */
export function buildWorkspaceItems(roles, isLoggedIn) {
  if (!isLoggedIn) return []

  const items = [{ to: '/creator/works', label: '创作者中心' }]

  if (hasAnyRole(roles, [ROLES.REVIEWER, ROLES.ADMIN, ROLES.SUPER_ADMIN])) {
    items.push(
      { to: '/review/contributions', label: '知识贡献审核', group: '审核工作台' },
      { to: '/review/works', label: '作品审核', group: '审核工作台' },
      { to: '/review/events', label: '活动审核', group: '审核工作台' },
    )
  }

  if (hasAnyRole(roles, [ROLES.ADMIN, ROLES.SUPER_ADMIN])) {
    items.push({ to: '/admin', label: '后台管理' })
  }

  return items
}

/**
 * 个人菜单（收进用户下拉，不再占用主导航）。
 *
 * @param {boolean} isLoggedIn 是否已登录
 * @param {number} unreadCount 未读消息数
 */
export function buildPersonalItems(isLoggedIn, unreadCount) {
  if (!isLoggedIn) return []
  return [
    { to: '/profile', label: '个人中心' },
    { to: '/contributions/mine', label: '我的贡献' },
    { to: '/favorites', label: '我的收藏' },
    { to: '/notifications', label: '消息', badge: unreadCount },
  ]
}

/** 取最高权限角色用于顶栏展示 */
export function primaryRoleLabel(roles) {
  const order = [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.REVIEWER, ROLES.CREATOR, ROLES.USER]
  const matched = order.find((role) => roles.includes(role))
  return matched || ''
}

/** 角色中文名 */
export const ROLE_LABELS = {
  USER: '普通用户',
  CREATOR: '创作者',
  REVIEWER: '评审员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}
