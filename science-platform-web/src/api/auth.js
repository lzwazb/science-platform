import request from './request'

/**
 * 公开注册普通用户。
 * 对应接口：POST /api/v1/auth/register
 */
export function register(payload) {
  return request.post('/auth/register', payload)
}

/**
 * 登录。
 * 对应接口：POST /api/v1/auth/login
 * @returns {Promise<{token: string, expiresIn: number, tokenType: string, user: object}>}
 */
export function login(payload) {
  return request.post('/auth/login', payload)
}

/**
 * 获取当前登录用户。
 * 对应接口：GET /api/v1/auth/me
 */
export function getMe() {
  return request.get('/auth/me')
}
