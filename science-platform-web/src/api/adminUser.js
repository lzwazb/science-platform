import request from './request'

/**
 * 后台用户列表（仅超级管理员）。
 */
export function listAdminUsers(params) {
  return request.get('/admin/users', { params })
}

/**
 * 重新分配用户角色。
 */
export function updateUserRoles(id, roleCodes, reason) {
  return request.put(`/admin/users/${id}/roles`, { roleCodes, reason })
}

/**
 * 启用 / 禁用账号。
 */
export function updateUserStatus(id, status, reason) {
  return request.put(`/admin/users/${id}/status`, { status, reason })
}
