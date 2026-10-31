<script setup>
import { onMounted, ref } from 'vue'
import { listEvents } from '@/api/event'

const loading = ref(false)
const loadError = ref('')
const events = ref([])

const timeMeta = {
  ONGOING: { text: '进行中', type: 'success' },
  NOT_STARTED: { text: '未开始', type: 'info' },
  ENDED: { text: '已结束', type: 'warning' },
  UNPUBLISHED: { text: '未发布', type: 'info' },
}

async function fetchEvents() {
  loading.value = true
  loadError.value = ''
  try {
    events.value = await listEvents()
  } catch (error) {
    loadError.value = error?.response?.data?.message || '活动加载失败'
    events.value = []
  } finally {
    loading.value = false
  }
}

function dateOf(value) {
  return (value || '').slice(0, 10)
}

onMounted(fetchEvents)
</script>

<template>
  <div class="events-page">
    <header class="events-hero">
      <h1>主题活动</h1>
      <p>
        平台定期发起科普创作征集。创作者在活动期间提交作品，评审员人工评分，
        活动结束后由管理员确认并公示优秀作品。
      </p>
    </header>

    <div v-loading="loading">
      <el-alert
        v-if="loadError && !loading"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
      />

      <el-empty
        v-else-if="!loading && events.length === 0"
        description="暂无已发布的活动"
      />

      <div v-else class="event-grid">
        <RouterLink v-for="event in events" :key="event.id" :to="`/events/${event.id}`" class="event-card">
          <div class="event-card__cover">
            <img v-if="event.coverUrl" :src="event.coverUrl" :alt="event.title" loading="lazy" />
            <div v-else class="event-card__cover-fallback">{{ event.title.charAt(0) }}</div>
            <el-tag
              class="event-card__status"
              :type="timeMeta[event.timeStatus]?.type || 'info'"
              effect="dark"
              size="small"
            >
              {{ timeMeta[event.timeStatus]?.text || event.timeStatus }}
            </el-tag>
          </div>
          <div class="event-card__body">
            <h3>{{ event.title }}</h3>
            <p>{{ event.description }}</p>
            <div class="event-card__meta">
              <span>{{ dateOf(event.startAt) }} ~ {{ dateOf(event.endAt) }}</span>
              <span>{{ event.submissionCount }} 件投稿</span>
            </div>
          </div>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.events-page {
  max-width: var(--sp-container);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.events-hero {
  padding: 30px 32px;
  margin-bottom: 22px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 84% 20%, var(--sp-primary-a16), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 64%);
}

.events-hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--sp-text-strong);
}

.events-hero p {
  margin: 0;
  max-width: 780px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.event-grid {
  display: grid;
  /* 桌面三列 / 中屏两列 / 手机一列 */
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-gap);
}

@media (max-width: 1024px) {
  .event-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .event-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.event-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.event-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 10px 24px var(--sp-primary-a12);
  transform: translateY(-2px);
}

.event-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.event-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.event-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 30px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.event-card__status {
  position: absolute;
  top: 12px;
  left: 12px;
}

.event-card__body {
  padding: 14px 16px 18px;
}

.event-card__body h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--sp-text-strong);
}

.event-card__body p {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--sp-text-faint);
  padding-top: 10px;
  border-top: 1px dashed var(--sp-border);
}
</style>
