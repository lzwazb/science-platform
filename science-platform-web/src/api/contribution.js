import request from './request'

// ---------------- 提交与个人 ----------------

/**
 * 提交知识补充 / 纠错 / 修改建议。
 * 对应接口：POST /api/v1/contributions
 */
export function submitContribution(payload) {
  return request.post('/contributions', payload)
}

/**
 * 我的贡献记录。
 */
export function listMyContributions(params) {
  return request.get('/contributions/mine', { params })
}

/**
 * 查看本人的某条贡献。
 */
export function getMyContribution(id) {
  return request.get(`/contributions/${id}`)
}

/**
 * 撤回仍在待审核状态的建议。
 */
export function withdrawContribution(id) {
  return request.post(`/contributions/${id}/withdraw`)
}

// ---------------- 审核工作台 ----------------

/**
 * 待审核建议列表，默认只返回 PENDING。
 */
export function listReviewContributions(params) {
  return request.get('/review/contributions', { params })
}

/**
 * 审核详情（含文章当前正文，便于对照）。
 */
export function getReviewContribution(id) {
  return request.get(`/review/contributions/${id}`)
}

/**
 * 通过：更新正式文章并生成历史版本。
 * @param {number} id
 * @param {{comment?: string, finalContent?: string}} payload
 */
export function approveContribution(id, payload) {
  return request.post(`/review/contributions/${id}/approve`, payload)
}

/**
 * 驳回（必须填写意见）。
 */
export function rejectContribution(id, comment) {
  return request.post(`/review/contributions/${id}/reject`, { comment })
}

// ---------------- 版本历史 ----------------

/**
 * 文章历史版本列表。
 */
export function listArticleVersions(articleId) {
  return request.get(`/articles/${articleId}/versions`)
}

/**
 * 指定版本的内容快照。
 */
export function getArticleVersion(articleId, versionNo) {
  return request.get(`/articles/${articleId}/versions/${versionNo}`)
}
