import request from './request'

// ---------------- 点赞 ----------------

/** 点赞（幂等） */
export function like(targetType, targetId) {
  return request.post('/interactions/likes', { targetType, targetId })
}

/** 取消点赞（幂等） */
export function unlike(targetType, targetId) {
  return request.delete('/interactions/likes', { data: { targetType, targetId } })
}

// ---------------- 收藏 ----------------

/** 收藏（幂等） */
export function favorite(targetType, targetId) {
  return request.post('/interactions/favorites', { targetType, targetId })
}

/** 取消收藏（幂等） */
export function unfavorite(targetType, targetId) {
  return request.delete('/interactions/favorites', { data: { targetType, targetId } })
}

/**
 * 查询某目标的互动状态与计数（游客可访问）。
 */
export function getInteractionStatus(targetType, targetId) {
  return request.get('/interactions/status', { params: { targetType, targetId } })
}

/**
 * 我的收藏。
 */
export function listMyFavorites(params) {
  return request.get('/interactions/favorites/mine', { params })
}

// ---------------- 评论 ----------------

/**
 * 按目标查询评论。
 */
export function listComments(params) {
  return request.get('/comments', { params })
}

/**
 * 发表评论或回复。
 */
export function createComment(payload) {
  return request.post('/comments', payload)
}

/**
 * 删除评论（本人或管理员）。
 */
export function deleteComment(id) {
  return request.delete(`/comments/${id}`)
}

// ---------------- 内容反馈 ----------------

/**
 * 提交内容反馈。
 */
export function submitFeedback(payload) {
  return request.post('/feedback', payload)
}

/**
 * 我的反馈记录。
 */
export function listMyFeedback(params) {
  return request.get('/feedback/mine', { params })
}

// ---------------- 后台反馈 ----------------

/**
 * 后台反馈列表。
 */
export function listAdminFeedback(params) {
  return request.get('/admin/feedback', { params })
}

/**
 * 处理反馈。
 */
export function handleFeedback(id, payload) {
  return request.post(`/admin/feedback/${id}/handle`, payload)
}
