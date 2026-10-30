import request from './request'

// ---------------- 公开 ----------------

/**
 * 分页查询已发布作品。
 * 对应接口：GET /api/v1/works
 */
export function listWorks(params) {
  return request.get('/works', { params })
}

/**
 * 作品详情。
 */
export function getWorkDetail(id) {
  return request.get(`/works/${id}`)
}

// ---------------- 创作者中心 ----------------

/**
 * 我的作品（含草稿与被驳回的）。
 */
export function listMyWorks(params) {
  return request.get('/creator/works', { params })
}

/**
 * 我的作品详情，允许草稿状态。
 */
export function getMyWork(id) {
  return request.get(`/creator/works/${id}`)
}

/**
 * 创建作品（保存为草稿）。
 */
export function createWork(payload) {
  return request.post('/creator/works', payload)
}

/**
 * 编辑作品。已发布作品编辑后会回到待审核。
 */
export function updateWork(id, payload) {
  return request.put(`/creator/works/${id}`, payload)
}

/**
 * 提交审核。
 */
export function submitWork(id) {
  return request.post(`/creator/works/${id}/submit`)
}

/**
 * 删除作品。
 */
export function deleteWork(id) {
  return request.delete(`/creator/works/${id}`)
}

/**
 * 更新作品关联的文章。
 */
export function updateWorkArticles(id, articleIds) {
  return request.put(`/creator/works/${id}/articles`, { articleIds })
}

// ---------------- 审核 ----------------

/**
 * 审核工作台列表，默认只返回待审核。
 */
export function listReviewWorks(params) {
  return request.get('/review/works', { params })
}

/**
 * 审核详情。
 */
export function getReviewWork(id) {
  return request.get(`/review/works/${id}`)
}

/**
 * 审核通过。
 */
export function approveWork(id, comment) {
  return request.post(`/review/works/${id}/approve`, { comment })
}

/**
 * 审核驳回（必须填写意见）。
 */
export function rejectWork(id, comment) {
  return request.post(`/review/works/${id}/reject`, { comment })
}

/**
 * 下架已发布作品。
 */
export function offlineWork(id, comment) {
  return request.post(`/review/works/${id}/offline`, { comment })
}
