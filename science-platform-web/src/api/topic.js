import request from './request'

/**
 * 公开专题列表（只含已发布专题，附带文章数）。
 * 对应接口：GET /api/v1/topics
 */
export function listTopics() {
  return request.get('/topics')
}

/**
 * 专题详情，含按顺序排列的文章。
 * 对应接口：GET /api/v1/topics/{id}
 */
export function getTopicDetail(id) {
  return request.get(`/topics/${id}`)
}

// ---------------- 后台 ----------------

/**
 * 后台专题列表（含草稿与已下架）。
 */
export function listAdminTopics() {
  return request.get('/admin/topics')
}

/**
 * 后台专题详情（含草稿文章）。
 */
export function getAdminTopic(id) {
  return request.get(`/admin/topics/${id}`)
}

/**
 * 创建专题。
 */
export function createTopic(payload) {
  return request.post('/admin/topics', payload)
}

/**
 * 编辑专题。
 */
export function updateTopic(id, payload) {
  return request.put(`/admin/topics/${id}`, payload)
}

/**
 * 删除专题。
 */
export function deleteTopic(id) {
  return request.delete(`/admin/topics/${id}`)
}

/**
 * 整体替换专题内的文章列表。
 */
export function updateTopicArticles(id, articleIds) {
  return request.put(`/admin/topics/${id}/articles`, { articleIds })
}
