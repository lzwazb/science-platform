import request from './request'

/**
 * 公开分类列表（只含启用中的分类）。
 * 对应接口：GET /api/v1/categories
 * @param {{type?: 'ARTICLE'|'WORK'}} params
 */
export function listCategories(params) {
  return request.get('/categories', { params })
}

// ---------------- 后台 ----------------

/**
 * 后台分类列表（含已停用）。
 */
export function listAdminCategories(params) {
  return request.get('/admin/categories', { params })
}

/**
 * 创建分类。
 */
export function createCategory(payload) {
  return request.post('/admin/categories', payload)
}

/**
 * 编辑分类。
 */
export function updateCategory(id, payload) {
  return request.put(`/admin/categories/${id}`, payload)
}

/**
 * 删除分类（分类下有文章时后端会拒绝）。
 */
export function deleteCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}
