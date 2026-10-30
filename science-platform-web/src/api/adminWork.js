import request from './request'

/**
 * 后台作品列表（全部状态，含草稿与已下架）。
 */
export function listAdminWorks(params) {
  return request.get('/admin/works', { params })
}

/**
 * 后台作品详情（任意状态）。
 */
export function getAdminWork(id) {
  return request.get(`/admin/works/${id}`)
}

/**
 * 下架违规作品。
 */
export function offlineWork(id, comment) {
  return request.post(`/admin/works/${id}/offline`, { comment })
}
