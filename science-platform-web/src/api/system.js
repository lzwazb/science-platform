import request from './request'

// ---------------- 公开 ----------------

/**
 * 首页平台信息：名称、简介、横幅 + 最近公告。
 */
export function getSiteInfo() {
  return request.get('/site/info')
}

/**
 * 已发布公告列表。
 */
export function listAnnouncements(limit) {
  return request.get('/announcements', { params: limit ? { limit } : undefined })
}

// ---------------- 后台（仅超级管理员）----------------

/**
 * 读取平台基础信息。
 */
export function getSiteSettings() {
  return request.get('/admin/settings')
}

/**
 * 保存平台基础信息。
 */
export function updateSiteSettings(payload) {
  return request.put('/admin/settings', payload)
}

/**
 * 公告列表（含草稿与已下架）。
 */
export function listAdminAnnouncements(status) {
  return request.get('/admin/announcements', { params: status ? { status } : undefined })
}

/**
 * 新增公告。
 */
export function createAnnouncement(payload) {
  return request.post('/admin/announcements', payload)
}

/**
 * 编辑公告。
 */
export function updateAnnouncement(id, payload) {
  return request.put(`/admin/announcements/${id}`, payload)
}

/**
 * 发布公告。
 */
export function publishAnnouncement(id) {
  return request.post(`/admin/announcements/${id}/publish`)
}

/**
 * 下架公告。
 */
export function offlineAnnouncement(id) {
  return request.post(`/admin/announcements/${id}/offline`)
}

/**
 * 删除公告。
 */
export function deleteAnnouncement(id) {
  return request.delete(`/admin/announcements/${id}`)
}
