/**
 * 后端时间字段是 'YYYY-MM-DD HH:mm:ss' 字符串（LocalDateTime 序列化结果）。
 *
 * 不要直接 `new Date('2026-12-31 23:59:59')`：这种形式在 Safari 与部分移动端
 * 是解析失败的（Invalid Date）。这里手工拆分后按本地时区构造，全站行为一致。
 */

export function parseDateTime(value) {
  if (!value) return null
  const [datePart, timePart = '00:00:00'] = String(value).split(' ')
  const [y, m, d] = datePart.split('-').map(Number)
  const [hh = 0, mm = 0, ss = 0] = timePart.split(':').map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d, hh, mm, ss)
}

/** 2026-12-31 23:59:59 → 2026.12.31 */
export function formatDate(value) {
  const date = parseDateTime(value)
  if (!date) return ''
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(
    date.getDate(),
  ).padStart(2, '0')}`
}

/** 2026-12-31 23:59:59 → 2026.12.31 23:59 */
export function formatDateTime(value) {
  const date = parseDateTime(value)
  if (!date) return ''
  return `${formatDate(value)} ${String(date.getHours()).padStart(2, '0')}:${String(
    date.getMinutes(),
  ).padStart(2, '0')}`
}

/**
 * 距离某个时间还有多少天，用于活动倒计时。
 * 返回 { ended, days, text }：已过期为 ended=true。
 */
export function remainingTo(value) {
  const target = parseDateTime(value)
  if (!target) return { ended: false, days: null, text: '' }
  const diffMs = target.getTime() - Date.now()
  if (diffMs <= 0) return { ended: true, days: 0, text: '已结束' }
  const days = Math.ceil(diffMs / 86400000)
  if (days <= 1) return { ended: false, days: 0, text: '今天截止' }
  return { ended: false, days, text: `还剩 ${days} 天` }
}
