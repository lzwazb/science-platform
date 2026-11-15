import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUnreadCount } from '@/api/notification'

/**
 * 站内消息的未读数。
 *
 * <p>顶栏红点与「站内消息」页共用这一份状态：在消息页标记已读后红点要立刻变化，
 * 只靠路由变化时刷新是不够的（页面内操作不会触发路由跳转）。</p>
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  /** 从服务端重新取一次未读数 */
  async function refresh() {
    try {
      const data = await getUnreadCount()
      unreadCount.value = data.unreadCount || 0
    } catch {
      unreadCount.value = 0
    }
  }

  /** 读掉一条：本地先减一，避免为一个红点再发一次请求 */
  function consumeOne() {
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  function reset() {
    unreadCount.value = 0
  }

  return { unreadCount, refresh, consumeOne, reset }
})
