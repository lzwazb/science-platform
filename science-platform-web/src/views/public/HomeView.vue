<script setup>
import { computed, onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'
import { getSiteInfo } from '@/api/system'

/**
 * 首页：展示平台基础信息、后端连通状态与功能模块导航。
 *
 * 平台名称、简介、横幅与公告都来自后台「系统设置」，由超级管理员维护；
 * 后端读取失败时用内置默认值兜底，保证首页永远能打开。
 */
const loading = ref(true)
const connected = ref(false)
const health = ref(null)
const errorMessage = ref('')
const checkedAt = ref('')

const siteInfo = ref(null)

/** 后台未配置或接口异常时的兜底文案 */
const FALLBACK_SITE = {
  name: '科普知识共创与创意传播平台',
  subtitle: 'Knowledge Co-creation & Creative Communication',
  description:
    '让科普知识在阅读、共创、创作与传播之间流动起来：用户既阅读经过核对的科普文章，也能提交补充与纠错建议，创作与文章相关的创意作品，并参与主题活动。',
  banner: '发现不准确？提交纠错，让知识更靠谱',
  announcements: [],
}

const site = computed(() => siteInfo.value || FALLBACK_SITE)

/**
 * 七个核心模块（开发档案 1.3 / 14.2 节），均已实现并接入真实接口。
 */
const modules = [
  { name: '科普知识库与专题探索', detail: '分类筛选、文章详情、关键词搜索与专题浏览', link: '/knowledge' },
  { name: '创作者中心与作品管理', detail: '作品发布、编辑与知识文章关联', link: '/works' },
  { name: '知识共建与纠错', detail: '提交补充 / 纠错建议、人工审核、历史版本追溯', link: '/knowledge' },
  { name: '社区互动', detail: '评论、点赞、收藏与内容反馈', link: '/works' },
  { name: '主题创作活动', detail: '活动发布、作品投稿、人工评审与优秀作品展示', link: '/events' },
  { name: '消息通知与个人中心', detail: '个人资料、我的作品 / 贡献 / 收藏、站内消息', link: '/profile' },
  { name: '后台管理与系统设置', detail: '分类、文章、作品、活动、反馈与角色权限管理', link: '/admin' },
]

async function checkBackend() {
  loading.value = true
  errorMessage.value = ''
  try {
    health.value = await getHealth()
    connected.value = true
  } catch (error) {
    connected.value = false
    health.value = null
    errorMessage.value =
      error?.response?.status === undefined
        ? '无法连接后端服务，请确认 Spring Boot 已在 8080 端口启动'
        : `后端返回异常状态：${error.response.status}`
  } finally {
    loading.value = false
    checkedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }
}

async function fetchSiteInfo() {
  try {
    siteInfo.value = await getSiteInfo()
  } catch {
    // 静默兜底：首页不应因为配置接口失败而打不开
    siteInfo.value = null
  }
}

onMounted(() => {
  checkBackend()
  fetchSiteInfo()
})
</script>

<template>
  <div class="home">
    <section class="hero">
      <p class="hero__eyebrow">本科毕业设计 · {{ site.subtitle }}</p>
      <h1 class="hero__title">{{ site.name }}</h1>
      <p class="hero__subtitle">{{ site.description }}</p>
      <p v-if="site.banner" class="hero__banner">{{ site.banner }}</p>
      <p class="hero__stack">
        Spring Boot 3.5 · Java 21 · Maven · MyBatis-Plus · MySQL 8 · Vue 3 · Vite · Element Plus · Axios
      </p>
    </section>

    <section v-if="site.announcements?.length" class="announcements">
      <div class="announcements__head">
        <h2>系统公告</h2>
        <span class="announcements__count">{{ site.announcements.length }} 条</span>
      </div>
      <ul class="announcements__list">
        <li v-for="item in site.announcements" :key="item.id" class="announcement">
          <div class="announcement__head">
            <strong>{{ item.title }}</strong>
            <span class="announcement__time">{{ (item.publishedAt || '').slice(0, 10) }}</span>
          </div>
          <p class="announcement__content">{{ item.content }}</p>
        </li>
      </ul>
    </section>

    <section class="status-card">
      <div class="status-card__head">
        <h2>前后端连通性</h2>
        <el-button size="small" :loading="loading" @click="checkBackend">重新检测</el-button>
      </div>

      <el-alert
        v-if="!loading && connected"
        type="success"
        :closable="false"
        show-icon
        title="后端连接正常"
        :description="`接口 GET /api/v1/health 调用成功，服务端时间 ${health?.serverTime || '-'}`"
      />
      <el-alert
        v-else-if="!loading && !connected"
        type="error"
        :closable="false"
        show-icon
        title="后端未连接"
        :description="errorMessage"
      />
      <div v-else class="status-card__loading">正在检测后端连接……</div>

      <el-descriptions
        v-if="connected && health"
        class="status-card__detail"
        :column="2"
        border
        size="small"
      >
        <el-descriptions-item label="服务状态">{{ health.status }}</el-descriptions-item>
        <el-descriptions-item label="应用名称">{{ health.application }}</el-descriptions-item>
        <el-descriptions-item label="当前阶段">{{ health.stage }}</el-descriptions-item>
        <el-descriptions-item label="检测时间">{{ checkedAt }}</el-descriptions-item>
        <el-descriptions-item label="数据库">
          <el-tag :type="health.db?.status === 'UP' ? 'success' : 'danger'" size="small">
            {{ health.db?.status === 'UP' ? '已连接' : '异常' }}
          </el-tag>
          <span class="status-card__db-name">{{ health.db?.database }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已发布文章">
          {{ health.db?.publishedArticleCount }} 篇
        </el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="modules">
      <div class="modules__head">
        <h2>核心功能模块</h2>
        <p>以下七个模块均已实现并接入真实后端接口，点击卡片进入对应页面。</p>
      </div>

      <div class="modules__grid">
        <RouterLink v-for="item in modules" :key="item.name" :to="item.link" class="module-card">
          <div class="module-card__top">
            <h3>{{ item.name }}</h3>
            <el-tag size="small" type="success" effect="plain">已开放</el-tag>
          </div>
          <p class="module-card__detail">{{ item.detail }}</p>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.hero {
  padding: 40px 36px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 82% 18%, rgba(64, 158, 255, 0.18), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 62%);
  border: 1px solid var(--sp-border);
}

.hero__eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 1.6px;
  color: var(--sp-primary-dark);
  font-weight: 600;
}

.hero__title {
  margin: 0 0 14px;
  font-size: 32px;
  line-height: 1.3;
  color: var(--sp-text-strong);
  letter-spacing: 0.5px;
}

.hero__subtitle {
  margin: 0 0 16px;
  max-width: 780px;
  font-size: 14px;
  line-height: 1.85;
  color: var(--sp-text);
}

.hero__banner {
  margin: 0 0 16px;
  display: inline-block;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(64, 158, 255, 0.12);
  font-size: 13px;
  color: var(--sp-primary-dark);
}

.hero__stack {
  margin: 0;
  font-size: 12px;
  color: var(--sp-text-muted);
  letter-spacing: 0.2px;
}

.announcements {
  margin-top: 24px;
  padding: 20px 24px 8px;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
}

.announcements__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.announcements__head h2 {
  margin: 0;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.announcements__count {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.announcements__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.announcement {
  padding: 12px 0;
  border-top: 1px dashed var(--sp-border);
}

.announcement:first-child {
  border-top: none;
  padding-top: 0;
}

.announcement__head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.announcement__head strong {
  font-size: 14px;
  color: var(--sp-text-strong);
}

.announcement__time {
  margin-left: auto;
  font-size: 11px;
  color: var(--sp-text-faint);
}

.announcement__content {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
  white-space: pre-wrap;
}

.status-card {
  margin-top: 24px;
  padding: 22px 24px;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
}

.status-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.status-card__head h2 {
  margin: 0;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.status-card__loading {
  font-size: 13px;
  color: var(--sp-text-muted);
}

.status-card__detail {
  margin-top: 16px;
}

.status-card__db-name {
  margin-left: 8px;
  font-size: 12px;
  color: var(--sp-text-muted);
}

.modules {
  margin-top: 32px;
}

.modules__head h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--sp-text-strong);
}

.modules__head p {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.modules__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.module-card {
  display: block;
  padding: 16px 18px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.module-card:hover {
  border-color: rgba(64, 158, 255, 0.45);
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

.module-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.module-card__top h3 {
  margin: 0;
  font-size: 15px;
  color: var(--sp-text-strong);
}

.module-card__detail {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}
</style>
