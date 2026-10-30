import request from './request'

/**
 * 分页查询已发布文章。
 * 对应接口：GET /api/v1/articles
 * @param {{page?: number, size?: number, categoryId?: number, keyword?: string}} params
 */
export function listArticles(params) {
  return request.get('/articles', { params })
}

/**
 * 关键词搜索。
 * 对应接口：GET /api/v1/articles/search
 */
export function searchArticles(params) {
  return request.get('/articles/search', { params })
}

/**
 * 文章详情。
 * 对应接口：GET /api/v1/articles/{id}
 */
export function getArticleDetail(id) {
  return request.get(`/articles/${id}`)
}

// ---------------- 后台 ----------------

/**
 * 后台文章分页（含草稿与已下架）。
 * 对应接口：GET /api/v1/admin/articles
 */
export function listAdminArticles(params) {
  return request.get('/admin/articles', { params })
}

/**
 * 后台查看文章详情（允许草稿）。
 */
export function getAdminArticle(id) {
  return request.get(`/admin/articles/${id}`)
}

/**
 * 创建文章（保存为草稿）。
 */
export function createArticle(payload) {
  return request.post('/admin/articles', payload)
}

/**
 * 编辑文章。
 */
export function updateArticle(id, payload) {
  return request.put(`/admin/articles/${id}`, payload)
}

/**
 * 发布文章。
 */
export function publishArticle(id) {
  return request.post(`/admin/articles/${id}/publish`)
}

/**
 * 下架文章。
 */
export function offlineArticle(id) {
  return request.post(`/admin/articles/${id}/offline`)
}

/**
 * 删除文章（逻辑删除）。
 */
export function deleteArticle(id) {
  return request.delete(`/admin/articles/${id}`)
}
