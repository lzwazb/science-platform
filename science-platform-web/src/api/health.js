import request from './request'

/**
 * 后端健康检查。
 * 对应接口：GET /api/v1/health
 * @returns {Promise<{status: string, application: string, project: string, stage: string, serverTime: string}>}
 */
export function getHealth() {
  return request.get('/health')
}
