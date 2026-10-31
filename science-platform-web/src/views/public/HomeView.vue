<script setup>
import { computed, onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'
import { getSiteInfo, getSiteStats } from '@/api/system'
import { listArticles } from '@/api/article'
import { listWorks } from '@/api/work'
import { listEvents } from '@/api/event'
import { TIME_STATUS } from '@/constants/dict'

/**
 * 首页：平台身份 + 数据概览 + 知识共建流程 + 最新内容 + 模块入口。
 *
 * 平台名称、简介、横幅与公告由超级管理员在「系统设置」维护；
 * 内容区读取真实的后台数据，不是静态文案。
 */

// ---------------- 平台信息 ----------------

const siteInfo = ref(null)
const stats = ref(null)

/** 后台未配置或接口异常时的兜底文案，保证首页永远能打开 */
const FALLBACK_SITE = {
  name: '科普知识共创与创意传播平台',
  subtitle: 'Knowledge Co-creation & Creative Communication',
  description:
    '让科普知识在阅读、共创、创作与传播之间流动起来：用户既阅读经过核对的科普文章，也能提交补充与纠错建议，创作与文章相关的创意作品，并参与主题活动。',
  banner: '发现不准确？提交纠错，让知识更靠谱',
  announcements: [],
}

const site = computed(() => siteInfo.value || FALLBACK_SITE)

// ---------------- 内容区 ----------------

const articles = ref([])
const works = ref([])
const events = ref([])
const contentLoading = ref(true)

// ---------------- 公告折叠 ----------------

const expandedAnnouncement = ref(null)

function toggleAnnouncement(id) {
  expandedAnnouncement.value = expandedAnnouncement.value === id ? null : id
}

// ---------------- 后端状态（折叠在页脚，仅作连通性佐证）----------------

const health = ref(null)
const healthError = ref('')

// ---------------- 数据加载 ----------------

async function loadSite() {
  try {
    siteInfo.value = await getSiteInfo()
  } catch {
    siteInfo.value = null
  }
}

async function loadStats() {
  try {
    stats.value = await getSiteStats()
  } catch {
    stats.value = null
  }
}

async function loadContent() {
  contentLoading.value = true
  // 三个列表互不依赖，并发请求；任一失败不影响其他区块
  const [articleResult, workResult, eventResult] = await Promise.allSettled([
    listArticles({ page: 1, size: 6 }),
    listWorks({ page: 1, size: 4 }),
    listEvents(),
  ])

  articles.value = articleResult.status === 'fulfilled' ? articleResult.value.records || [] : []
  works.value = workResult.status === 'fulfilled' ? workResult.value.records || [] : []
  events.value = eventResult.status === 'fulfilled' ? eventResult.value || [] : []

  contentLoading.value = false
}

async function loadHealth() {
  try {
    health.value = await getHealth()
    healthError.value = ''
  } catch {
    health.value = null
    healthError.value = '后端服务未连接'
  }
}

// ---------------- 展示辅助 ----------------

/** 首页统计的五项数字 */
const statItems = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { key: 'article', value: s.articleCount, label: '科普文章' },
    { key: 'work', value: s.workCount, label: '创意作品' },
    { key: 'event', value: s.eventCount, label: '主题活动' },
    { key: 'contribution', value: s.adoptedContributionCount, label: '已采纳建议' },
    { key: 'creator', value: s.creatorCount, label: '创作者' },
  ]
})

/** 知识共建的四个环节——这是本项目区别于普通内容站的地方 */
const workflow = [
  { step: '01', title: '阅读', detail: '浏览经过人工核对的科普文章，按分类与专题探索。' },
  { step: '02', title: '共建', detail: '发现错误或不完整？提交补充与纠错建议，必须填写理由与参考来源。' },
  { step: '03', title: '审核', detail: '评审员对照原文与建议逐条处理，通过时必须确定最终采用的正文。' },
  { step: '04', title: '留痕', detail: '正文更新、版本号递增，旧内容存为历史版本，建议人收到站内通知。' },
]

/** 正在接受投稿的活动 */
const ongoingEvents = computed(() =>
  events.value.filter((event) => event.acceptingSubmission).slice(0, 1),
)

/** 其余活动（含已结束）用于展示活动历史 */
const otherEvents = computed(() =>
  events.value.filter((event) => !event.acceptingSubmission).slice(0, 2),
)

const timeMeta = TIME_STATUS

const modules = [
  { name: '科普知识库', detail: '分类、专题与关键词检索', link: '/knowledge' },
  { name: '创意作品', detail: '与文章关联的插图与图文创作', link: '/works' },
  { name: '主题活动', detail: '投稿、评审与优秀作品公示', link: '/events' },
  { name: '个人中心', detail: '我的贡献、收藏与站内消息', link: '/profile' },
]

function formatDate(value) {
  return (value || '').slice(0, 10)
}

onMounted(() => {
  loadSite()
  loadStats()
  loadContent()
  loadHealth()
})
</script>

<template>
  <div class="home">
    <!-- 一、平台身份 -->
    <section class="hero">
      <div class="hero__main">
        <p class="hero__eyebrow">{{ site.subtitle }}</p>
        <h1 class="hero__title">{{ site.name }}</h1>
        <p class="hero__desc">{{ site.description }}</p>
        <p v-if="site.banner" class="hero__banner">{{ site.banner }}</p>
        <div class="hero__actions">
          <el-button type="primary" size="large" @click="$router.push('/knowledge')">
            浏览科普文章
          </el-button>
          <el-button size="large" @click="$router.push('/events')">
            参与主题活动
          </el-button>
        </div>
      </div>

      <aside v-if="statItems.length" class="hero__stats">
        <div v-for="item in statItems" :key="item.key" class="stat">
          <span class="stat__value">{{ item.value }}</span>
          <span class="stat__label">{{ item.label }}</span>
        </div>
      </aside>
    </section>

    <!-- 二、最新科普文章 -->
    <section class="section">
      <div class="section-head section-head--row">
        <div>
          <h2>最新科普文章</h2>
          <p>按发布时间排序，均经过人工核对。</p>
        </div>
        <RouterLink to="/knowledge" class="section-link">全部文章 →</RouterLink>
      </div>

      <div v-loading="contentLoading" class="article-grid">
        <RouterLink
          v-for="article in articles"
          :key="article.id"
          :to="`/knowledge/${article.id}`"
          class="article-card"
        >
          <div class="article-card__cover">
            <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" loading="lazy" />
            <div v-else class="cover-fallback">{{ article.title.charAt(0) }}</div>
            <span v-if="article.versionNo > 1" class="article-card__version">
              第 {{ article.versionNo }} 版
            </span>
          </div>
          <div class="article-card__body">
            <div class="article-card__meta">
              <span class="article-card__category">{{ article.categoryName }}</span>
              <span>{{ article.viewCount }} 次阅读</span>
            </div>
            <h3>{{ article.title }}</h3>
            <p>{{ article.summary }}</p>
          </div>
        </RouterLink>

        <el-empty
          v-if="!contentLoading && articles.length === 0"
          description="暂无已发布文章"
          :image-size="80"
          class="grid-empty"
        />
      </div>
    </section>

    <!-- 三、最新创意作品 -->
    <section class="section">
      <div class="section-head section-head--row">
        <div>
          <h2>最新创意作品</h2>
          <p>创作者围绕具体文章创作的插图与图文内容。</p>
        </div>
        <RouterLink to="/works" class="section-link">全部作品 →</RouterLink>
      </div>

      <div v-loading="contentLoading" class="work-grid">
        <RouterLink
          v-for="work in works"
          :key="work.id"
          :to="`/works/${work.id}`"
          class="work-card"
        >
          <div class="work-card__cover">
            <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" loading="lazy" />
            <div v-else class="cover-fallback">{{ work.title.charAt(0) }}</div>
          </div>
          <div class="work-card__body">
            <h3>{{ work.title }}</h3>
            <div class="work-card__meta">
              <span>{{ work.creatorName }}</span>
              <span>关联 {{ work.articleCount }} 篇文章</span>
            </div>
          </div>
        </RouterLink>

        <el-empty
          v-if="!contentLoading && works.length === 0"
          description="暂无已发布作品"
          :image-size="80"
          class="grid-empty"
        />
      </div>
    </section>

    <!-- 四、主题活动 -->
    <section v-if="ongoingEvents.length || otherEvents.length" class="section">
      <div class="section-head section-head--row">
        <div>
          <h2>主题活动</h2>
          <p>活动期间投稿，评审员人工评分，结束后由管理员确认优秀作品。</p>
        </div>
        <RouterLink to="/events" class="section-link">全部活动 →</RouterLink>
      </div>

      <div class="event-list">
        <RouterLink
          v-for="event in [...ongoingEvents, ...otherEvents]"
          :key="event.id"
          :to="`/events/${event.id}`"
          class="event-card"
        >
          <div class="event-card__main">
            <div class="event-card__tags">
              <el-tag :type="timeMeta[event.timeStatus]?.type || 'info'" size="small" effect="dark">
                {{ timeMeta[event.timeStatus]?.text || event.timeStatus }}
              </el-tag>
              <span class="event-card__range">
                {{ formatDate(event.startAt) }} ~ {{ formatDate(event.endAt) }}
              </span>
            </div>
            <h3>{{ event.title }}</h3>
            <p>{{ event.description }}</p>
          </div>
          <div class="event-card__side">
            <span class="event-card__count">{{ event.submissionCount }}</span>
            <span class="event-card__count-label">件投稿</span>
          </div>
        </RouterLink>
      </div>
    </section>

    <!-- 五、知识共建流程：本项目的核心特色 -->
    <section class="workflow">
      <div class="section-head">
        <h2>知识怎么被「共建」</h2>
        <p>
          这里不只是读文章。读者提交的每一条建议都会经过人工审核，
          被采纳的内容进入正式正文，旧版本完整留存——所以「这处改动是谁提的、什么时候改的」始终可查。
        </p>
      </div>

      <ol class="workflow__steps">
        <li v-for="item in workflow" :key="item.step" class="workflow-step">
          <span class="workflow-step__no">{{ item.step }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.detail }}</p>
        </li>
      </ol>

      <p class="workflow__note">
        已采纳读者建议 <strong>{{ stats?.adoptedContributionCount ?? '—' }}</strong> 条
        <template v-if="stats?.adoptedContributionCount">
          ，每一条都能在对应文章的「版本历史」里追溯到来源建议编号。
        </template>
      </p>
    </section>


    <!-- 六、系统公告 -->
    <section v-if="site.announcements?.length" class="announcements">
      <div class="announcements__head">
        <span class="announcements__icon">公告</span>
        <span class="announcements__hint">共 {{ site.announcements.length }} 条，点击展开全文</span>
      </div>
      <ul class="announcements__list">
        <li
          v-for="item in site.announcements"
          :key="item.id"
          class="announcement"
          :class="{ 'announcement--open': expandedAnnouncement === item.id }"
          @click="toggleAnnouncement(item.id)"
        >
          <div class="announcement__head">
            <strong>{{ item.title }}</strong>
            <span class="announcement__time">{{ formatDate(item.publishedAt) }}</span>
          </div>
          <p v-if="expandedAnnouncement === item.id" class="announcement__content">
            {{ item.content }}
          </p>
        </li>
      </ul>
    </section>



    <!-- 七、模块入口 -->
    <section class="modules">
      <div class="modules__grid">
        <RouterLink v-for="item in modules" :key="item.name" :to="item.link" class="module-card">
          <h3>{{ item.name }}</h3>
          <p>{{ item.detail }}</p>
        </RouterLink>
      </div>
    </section>

    <!-- 八、系统状态：折叠成一行，仅用于佐证前后端连通 -->
    <footer class="status-bar">
      <template v-if="health">
        <span class="status-dot status-dot--ok" />
        <span>服务正常</span>
        <span class="status-sep">·</span>
        <span>数据库 {{ health.db?.database }}</span>
        <span class="status-sep">·</span>
        <span>已发布 {{ health.db?.publishedArticleCount }} 篇</span>
        <span class="status-sep">·</span>
        <span>{{ health.serverTime }}</span>
      </template>
      <template v-else>
        <span class="status-dot status-dot--down" />
        <span>{{ healthError || '正在检测服务状态……' }}</span>
      </template>
      <span v-if="site.contact" class="status-contact">联系：{{ site.contact }}</span>
    </footer>
  </div>
</template>

<style scoped>
.home {
  max-width: var(--sp-container);
  margin: 0 auto;
  padding: 28px 24px 40px;
}

/* ---------- Hero ---------- */

.hero {
  display: flex;
  gap: 36px;
  padding: 40px 40px 36px;
  border-radius: 18px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 88% 14%, var(--sp-primary-a16), transparent 48%),
    linear-gradient(135deg, #f5faff 0%, #ffffff 66%);
}

.hero__main {
  flex: 1;
  min-width: 0;
}

.hero__eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 1.4px;
  color: var(--sp-primary-dark);
  font-weight: 600;
}

.hero__title {
  margin: 0 0 14px;
  font-size: 32px;
  line-height: 1.3;
  color: var(--sp-text-strong);
  letter-spacing: 0.4px;
}

.hero__desc {
  margin: 0 0 14px;
  max-width: 640px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--sp-text);
}

.hero__banner {
  display: inline-block;
  margin: 0 0 18px;
  padding: 6px 14px;
  border-radius: 20px;
  background: var(--sp-primary-a12);
  font-size: 13px;
  color: var(--sp-primary-dark);
}

.hero__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero__stats {
  flex-shrink: 0;
  width: 200px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 0 6px 28px;
  border-left: 1px solid var(--sp-border);
}

.stat {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 8px 0;
}

.stat__value {
  font-size: 26px;
  font-weight: 600;
  color: var(--sp-primary-dark);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat__label {
  font-size: 12px;
  color: var(--sp-text-muted);
}

/* ---------- 公告 ---------- */

.announcements {
  margin-top: 20px;
  padding: 14px 20px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
}

.announcements__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.announcements__icon {
  padding: 1px 9px;
  border-radius: 4px;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-size: 11px;
  font-weight: 600;
}

.announcements__hint {
  font-size: 11px;
  color: var(--sp-text-faint);
}

.announcements__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.announcement {
  padding: 9px 0;
  border-top: 1px dashed var(--sp-border);
  cursor: pointer;
}

.announcement:first-child {
  border-top: none;
}

.announcement__head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.announcement__head strong {
  font-size: 13px;
  color: var(--sp-text-strong);
  font-weight: 600;
}

.announcement--open .announcement__head strong {
  color: var(--sp-primary-dark);
}

.announcement__time {
  margin-left: auto;
  font-size: 11px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.announcement__content {
  margin: 8px 0 2px;
  font-size: 13px;
  line-height: 1.85;
  color: var(--sp-text-muted);
  white-space: pre-wrap;
}

/* ---------- 通用区块 ---------- */

.section {
  margin-top: 40px;
}

.section-head {
  margin-bottom: 18px;
}

.section-head--row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.section-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--sp-text-strong);
}

.section-head p {
  margin: 0;
  max-width: 760px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.section-link {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.section-link:hover {
  text-decoration: underline;
}

/* ---------- 共建流程 ---------- */

.workflow {
  margin-top: 40px;
  padding: 30px 32px 26px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background: linear-gradient(160deg, #fbfdff 0%, #ffffff 60%);
}

.workflow__steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.workflow-step {
  position: relative;
  padding: 18px 18px 16px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
}

.workflow-step__no {
  display: inline-block;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--sp-primary);
}

.workflow-step h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: var(--sp-text-strong);
}

.workflow-step p {
  margin: 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.workflow__note {
  margin: 18px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.workflow__note strong {
  color: var(--sp-primary-dark);
  font-size: 14px;
}

/* ---------- 文章卡片 ---------- */

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  min-height: 60px;
}

.article-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.article-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 8px 22px var(--sp-primary-a10);
  transform: translateY(-2px);
}

.article-card__cover {
  position: relative;
  aspect-ratio: 16 / 7;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.article-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.article-card__version {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 9px;
  border-radius: 10px;
  background: var(--sp-primary-a92);
  color: #fff;
  font-size: 11px;
}

.article-card__body {
  padding: 14px 16px 16px;
}

.article-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 11px;
  color: var(--sp-text-faint);
}

.article-card__category {
  padding: 1px 8px;
  border-radius: 4px;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.article-card__body h3 {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.5;
  color: var(--sp-text-strong);
}

.article-card__body p {
  margin: 0;
  font-size: 12px;
  line-height: 1.75;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.grid-empty {
  grid-column: 1 / -1;
}

/* ---------- 作品卡片 ---------- */

.work-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  min-height: 60px;
}

.work-card {
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.work-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 8px 20px var(--sp-primary-a10);
}

.work-card__cover {
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.work-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.work-card__body {
  padding: 12px 14px 14px;
}

.work-card__body h3 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--sp-text-strong);
}

.work-card__meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--sp-text-faint);
}

/* ---------- 活动 ---------- */

.event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-card {
  display: flex;
  align-items: stretch;
  gap: 20px;
  padding: 18px 22px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.event-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 8px 20px var(--sp-primary-a10);
}

.event-card__main {
  flex: 1;
  min-width: 0;
}

.event-card__tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.event-card__range {
  font-size: 11px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.event-card__main h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--sp-text-strong);
}

.event-card__main p {
  margin: 0;
  font-size: 12px;
  line-height: 1.75;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-card__side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-left: 20px;
  border-left: 1px solid var(--sp-border);
}

.event-card__count {
  font-size: 24px;
  font-weight: 600;
  color: var(--sp-primary-dark);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.event-card__count-label {
  margin-top: 4px;
  font-size: 11px;
  color: var(--sp-text-faint);
}

/* ---------- 模块入口 ---------- */

.modules {
  margin-top: 40px;
}

.modules__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.module-card {
  padding: 16px 18px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.module-card:hover {
  border-color: var(--sp-primary-a45);
  box-shadow: 0 6px 16px var(--sp-primary-a10);
}

.module-card h3 {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--sp-text-strong);
}

.module-card p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

/* ---------- 状态条 ---------- */

.status-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 36px;
  padding-top: 16px;
  border-top: 1px solid var(--sp-border);
  font-size: 11px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-dot--ok {
  background: #67c23a;
}

.status-dot--down {
  background: #f56c6c;
}

.status-sep {
  color: var(--sp-border);
}

.status-contact {
  margin-left: auto;
}

@media (max-width: 860px) {
  .hero {
    flex-direction: column;
    padding: 28px 24px;
  }

  .hero__stats {
    width: auto;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 18px;
    padding: 16px 0 0;
    border-left: none;
    border-top: 1px solid var(--sp-border);
  }

  .stat {
    flex-direction: column;
    gap: 4px;
  }

  .section-head--row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .event-card {
    flex-direction: column;
    gap: 12px;
  }

  .event-card__side {
    flex-direction: row;
    gap: 6px;
    padding-left: 0;
    padding-top: 12px;
    border-left: none;
    border-top: 1px solid var(--sp-border);
  }
}
</style>

