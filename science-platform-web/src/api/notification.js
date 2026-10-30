import request from './request'

/**
 * 我的消息分页。
 */
export function listNotifications(params) {
  return request.get('/notifications', { params })
}

/**
 * 未读消息数。
 */
export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

/**
 * 标记单条消息已读。
 */
export function markNotificationRead(id) {
  return request.put(`/notifications/${id}/read`)
}

/**
 * 全部标记已读。
 */
export function markAllNotificationsRead() {
  return request.put('/notifications/read-all')
}

/**
 * 个人中心概况统计。
 */
export function getMyOverview() {
  return request.get('/users/me/overview')
}
