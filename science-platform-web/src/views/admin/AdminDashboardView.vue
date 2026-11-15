<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAdminArticles } from '@/api/article'
import { listReviewContributions } from '@/api/contribution'
import { listEvents } from '@/api/event'
import { listAdminFeedback } from '@/api/interaction'
import { listReviewWorks } from '@/api/work'

const loading = ref(false)

const stats = reactive({
  contributions: 0,
  works: 0,
  feedback: 0,
  articles: 0,
  ongoingEvents: 0,
})

/** 卡片：数字 + 说明 + 去处理；整张卡可点，点击直达对应页面 */
const cards = [
  {
    key: 'contributions',
    label: '待审知识贡献',
    hint: '通过后会更新文章并生成历史版本',
    path: '/review/contributions',
    tone: 'primary',
  },
  {
    key: 'works',
    label: '待审作品',
    hint: '通过后才会出现在作品广场',
    path: '/review/works',
    tone: 'warning',
  },
  {
    key: 'feedback',
    label: '待处理反馈',
    hint: '用户对文章、作品或评论的举报与建议',
    path: '/admin/feedback',
    tone: 'danger',
  },
  {
    key: 'articles',
    label: '已发布文章',
    hint: '知识库当前对外可见的文章数',
    path: '/admin/articles',
    tone: 'success',
  },
  {
    key: 'ongoingEvents',
    label: '进行中的活动',
    hint: '按起止时间判断，草稿不计入',
    path: '/admin/events',
    tone: 'info',
  },
]

async function loadCounts() {
  loading.value = true
  try {
    const [contributionPage, workPage, feedbackPage, articlePage, events] = await Promise.all([
      listReviewContributions({ page: 1, size: 1, status: 'PENDING' }),
      listReviewWorks({ page: 1, size: 1, status: 'PENDING' }),
      listAdminFeedback({ page: 1, size: 1, status: 'PENDING' }),
      listAdminArticles({ page: 1, size: 1, status: 'PUBLISHED' }),
      listEvents(),
    ])
    stats.contributions = contributionPage.total || 0
    stats.works = workPage.total || 0
    stats.feedback = feedbackPage.total || 0
    stats.articles = articlePage.total || 0
    stats.ongoingEvents = (events || []).filter((event) => event.timeStatus === 'ONGOING').length
  } catch {
    // 单个统计失败不影响页面，错误已由请求层提示
  } finally {
    loading.value = false
  }
}

onMounted(loadCounts)
</script>

<template>
  <div class="admin-page">
    <header class="sp-page-head">
      <div class="sp-page-head__text">
        <h1>概览</h1>
        <p>待处理事项与内容状态汇总。数字是「此刻」的快照，处理完点刷新即可更新。</p>
      </div>
      <el-button size="small" :loading="loading" @click="loadCounts">刷新</el-button>
    </header>

    <div class="dash-grid" v-loading="loading">
      <RouterLink
        v-for="card in cards"
        :key="card.key"
        :to="card.path"
        class="dash-card"
        :class="`dash-card--${card.tone}`"
      >
        <span class="dash-card__label">{{ card.label }}</span>
        <strong class="dash-card__value">{{ stats[card.key] }}</strong>
        <span class="dash-card__hint">{{ card.hint }}</span>
        <span class="dash-card__go">去处理 →</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.dash-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--sp-gap);
}

.dash-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px;
  background: var(--sp-surface);
  border: 1px solid var(--sp-border);
  border-left: 3px solid var(--sp-primary);
  border-radius: var(--sp-radius-lg);
  color: inherit;
  text-decoration: none;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.dash-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(15, 40, 80, 0.08);
}

.dash-card--warning { border-left-color: #d18f28; }
.dash-card--danger { border-left-color: #e0645c; }
.dash-card--success { border-left-color: #15a081; }
.dash-card--info { border-left-color: #5b6ee1; }

.dash-card__label {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.dash-card__value {
  font-size: 30px;
  line-height: 1.1;
  color: var(--sp-text-strong);
  font-variant-numeric: tabular-nums;
}

.dash-card__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
  line-height: 1.6;
}

.dash-card__go {
  margin-top: auto;
  font-size: 12px;
  color: var(--sp-primary);
}
</style>
