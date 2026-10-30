import request from './request'

// ---------------- 公开 ----------------

/**
 * 活动列表（含时间状态）。
 */
export function listEvents() {
  return request.get('/events')
}

/**
 * 活动详情（含规则、优秀作品、我的投稿）。
 */
export function getEventDetail(id) {
  return request.get(`/events/${id}`)
}

/**
 * 活动优秀作品。
 */
export function listEventAwards(id) {
  return request.get(`/events/${id}/awards`)
}

// ---------------- 创作者 ----------------

/**
 * 提交作品参加活动。
 */
export function submitWorkToEvent(eventId, workId) {
  return request.post(`/events/${eventId}/submissions`, { workId })
}

/**
 * 我在某个活动中的投稿。
 */
export function listMySubmissionsOfEvent(eventId) {
  return request.get(`/events/${eventId}/submissions/mine`)
}

/**
 * 我的全部投稿（跨活动）。
 */
export function listMySubmissions() {
  return request.get('/events/submissions/mine')
}

// ---------------- 评审 ----------------

/**
 * 某活动的投稿列表（评审台）。
 */
export function listEventSubmissionsForReview(eventId) {
  return request.get(`/review/events/${eventId}/submissions`)
}

/**
 * 对投稿打分并写评语。
 */
export function reviewSubmission(submissionId, payload) {
  return request.post(`/review/submissions/${submissionId}/review`, payload)
}

// ---------------- 后台 ----------------

/**
 * 后台活动列表（含草稿）。
 */
export function listAdminEvents() {
  return request.get('/admin/events')
}

/**
 * 后台活动详情。
 */
export function getAdminEvent(id) {
  return request.get(`/admin/events/${id}`)
}

/**
 * 创建活动。
 */
export function createEvent(payload) {
  return request.post('/admin/events', payload)
}

/**
 * 编辑活动。
 */
export function updateEvent(id, payload) {
  return request.put(`/admin/events/${id}`, payload)
}

/**
 * 发布活动。
 */
export function publishEvent(id) {
  return request.post(`/admin/events/${id}/publish`)
}

/**
 * 结束活动。
 */
export function closeEvent(id) {
  return request.post(`/admin/events/${id}/close`)
}

/**
 * 删除活动。
 */
export function deleteEvent(id) {
  return request.delete(`/admin/events/${id}`)
}

/**
 * 确认优秀作品。
 * @param {number} id
 * @param {Array<{submissionId: number, awardLevel: string}>} awards
 */
export function confirmEventAwards(id, awards) {
  return request.post(`/admin/events/${id}/awards`, { awards })
}

/**
 * 撤销获奖标记。
 */
export function revokeAward(submissionId) {
  return request.post(`/admin/events/submissions/${submissionId}/revoke-award`)
}
