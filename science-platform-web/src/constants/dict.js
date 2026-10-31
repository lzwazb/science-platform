/**
 * 全站状态字典
 * --------------------------------------------------------------------------
 * 同一个状态在不同页面必须显示同样的文字和同样的颜色。
 * 之前每个页面各写一份 statusMeta，已经出现了分歧
 * （「已下架」在文章页是 warning、在作品页是 info），
 * 所以统一收到这里，页面只引用、不再自己定义。
 *
 * 颜色语义约定：
 *   success  —— 正常、通过、公开
 *   warning  —— 需要关注（待审核、待处理、已下架）
 *   danger   —— 失败、被拒、异常（已驳回、已禁用）
 *   primary  —— 进行中（处理中、已评审）
 *   info     —— 中性、归档（草稿、撤回、已取消）
 */

/** 内容类状态（文章 / 作品 / 贡献 / 活动 / 课题等） */
export const CONTENT_STATUS = {
  DRAFT: { text: '草稿', type: 'info' },
  PENDING: { text: '待审核', type: 'warning' },
  PUBLISHED: { text: '已发布', type: 'success' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  WITHDRAWN: { text: '已撤回', type: 'info' },
  OFFLINE: { text: '已下架', type: 'warning' },
  CLOSED: { text: '已结束', type: 'warning' },
  CANCELLED: { text: '已取消', type: 'info' },
  VISIBLE: { text: '可见', type: 'success' },
  HIDDEN: { text: '已隐藏', type: 'warning' },
  DELETED: { text: '已删除', type: 'info' },
}

/** 活动投稿状态 */
export const SUBMISSION_STATUS = {
  SUBMITTED: { text: '待评审', type: 'info' },
  UNDER_REVIEW: { text: '评审中', type: 'primary' },
  REVIEWED: { text: '已评审', type: 'primary' },
  SELECTED: { text: '已获奖', type: 'danger' },
  NOT_SELECTED: { text: '未获奖', type: 'info' },
  WITHDRAWN: { text: '已撤回', type: 'info' },
}

/** 内容反馈状态（与内容审核区分：PENDING 在这里叫「待处理」） */
export const FEEDBACK_STATUS = {
  PENDING: { text: '待处理', type: 'warning' },
  PROCESSING: { text: '处理中', type: 'primary' },
  RESOLVED: { text: '已处理', type: 'success' },
  REJECTED: { text: '已驳回', type: 'info' },
}

/** 账号状态 */
export const ACCOUNT_STATUS = {
  ACTIVE: { text: '正常', type: 'success' },
  DISABLED: { text: '已禁用', type: 'danger' },
}

/** 活动时间状态（按当前时间实时计算，不落库） */
export const TIME_STATUS = {
  ONGOING: { text: '进行中', type: 'success' },
  NOT_STARTED: { text: '未开始', type: 'info' },
  ENDED: { text: '已结束', type: 'warning' },
  UNPUBLISHED: { text: '未发布', type: 'info' },
}

/**
 * 合并后的内容状态字典。
 * 页面直接用它即可——查不到的 key 传进来会走 {@link statusOf} 的兜底。
 */
export const STATUS_META = {
  ...CONTENT_STATUS,
  ...SUBMISSION_STATUS,
  ...ACCOUNT_STATUS,
}

/** 知识贡献类型 */
export const CONTRIBUTION_TYPE_LABELS = {
  SUPPLEMENT: '补充内容',
  CORRECTION: '纠正错误',
  MODIFICATION: '修改建议',
  REWRITE: '重写段落',
}

/** 内容反馈类型 */
export const FEEDBACK_TYPE_LABELS = {
  WRONG_CONTENT: '内容错误',
  COPYRIGHT: '版权问题',
  SPAM: '垃圾信息',
  SUGGESTION: '改进建议',
  OTHER: '其他',
}

/** 活动获奖等级 */
export const AWARD_LEVELS = {
  FIRST: { text: '一等奖', type: 'danger' },
  SECOND: { text: '二等奖', type: 'warning' },
  THIRD: { text: '三等奖', type: 'primary' },
  EXCELLENT: { text: '优秀奖', type: 'success' },
}

/** 内容目标类型 */
export const TARGET_TYPE_LABELS = {
  ARTICLE: '文章',
  WORK: '作品',
}

/**
 * 安全取状态元信息：未知状态原样显示，不会渲染成空白。
 *
 * @param {string} status 状态编码
 * @param {Record<string, {text: string, type: string}>} [dict] 字典，默认内容状态
 * @returns {{text: string, type: string}}
 */
export function statusOf(status, dict = STATUS_META) {
  return dict[status] || { text: status || '—', type: 'info' }
}
