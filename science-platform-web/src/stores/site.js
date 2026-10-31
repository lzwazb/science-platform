import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getSiteInfo } from '@/api/system'

/**
 * 平台信息 store
 * --------------------------------------------------------------------------
 * 后台「系统设置」里维护的平台名称、简介、横幅、公告，在这里统一获取一次，
 * 顶栏品牌、页脚、首页 hero、浏览器标题都读同一份数据。
 *
 * 接口失败时回落到内置默认值，保证前台永远有内容可显示，不会白屏。
 */

/** 兜底文案：后台未配置或接口异常时使用 */
const DEFAULTS = {
  name: '科普知识共创与创意传播平台',
  subtitle: 'Knowledge Co-creation & Creative Communication',
  description:
    '让科普知识在阅读、共创、创作与传播之间流动起来：用户既阅读经过核对的科普文章，也能提交补充与纠错建议，创作与文章相关的创意作品，并参与主题活动。',
  banner: '发现不准确？提交纠错，让知识更靠谱',
  contact: '',
}

export const useSiteStore = defineStore('site', () => {
  /** 接口返回的原始数据，null 表示尚未加载或加载失败 */
  const info = ref(null)

  /** 是否已尝试过加载（避免重复请求） */
  const loaded = ref(false)

  const name = computed(() => info.value?.name || DEFAULTS.name)
  const subtitle = computed(() => info.value?.subtitle || DEFAULTS.subtitle)
  const description = computed(() => info.value?.description || DEFAULTS.description)
  const banner = computed(() => info.value?.banner || DEFAULTS.banner)
  const contact = computed(() => info.value?.contact || DEFAULTS.contact)
  const announcements = computed(() => info.value?.announcements || [])

  /**
   * 拉取平台信息。
   *
   * @param {boolean} force 为 true 时忽略已加载标记重新拉取（后台改名保存后调用）
   */
  async function fetchSiteInfo(force = false) {
    if (loaded.value && !force) return
    try {
      info.value = await getSiteInfo()
    } catch {
      info.value = null
    } finally {
      loaded.value = true
    }
  }

  return {
    info,
    loaded,
    name,
    subtitle,
    description,
    banner,
    contact,
    announcements,
    fetchSiteInfo,
  }
})
