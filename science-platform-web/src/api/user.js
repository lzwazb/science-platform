import request from './request'

/**
 * 查看本人资料。
 * 对应接口：GET /api/v1/users/me
 */
export function getMyProfile() {
  return request.get('/users/me')
}

/**
 * 更新本人资料。
 * 对应接口：PUT /api/v1/users/me
 */
export function updateMyProfile(payload) {
  return request.put('/users/me', payload)
}
