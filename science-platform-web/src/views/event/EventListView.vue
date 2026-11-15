<script setup>
import { computed, onMounted, ref } from 'vue'
import { listEvents } from '@/api/event'
import { TIME_STATUS } from '@/constants/dict'
import { formatDate, remainingTo } from '@/utils/datetime'

const loading = ref(false)
const loadError = ref('')
const events = ref([])

const timeMeta = TIME_STATUS

const ongoingCount = computed(
  () => events.value.filter((event) => event.timeStatus === 'ONGOING').length,
)

/** 进行中的活动给出「距结束 N 天」，当天结束则显示「今天截止」 */
function countdownText(event) {
  const remaining = remainingTo(event.endAt)
  if (remaining.ended) return '活动已结束'
  return remaining.days <= 0 ? '今天截止' : `距结束 ${remaining.days} 天`
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

onMounted(fetchEvents)
</script>

<template>
  <div class="events-page">
    <header class="events-hero">
      <div class="events-hero__text">
        <h1>主题活动</h1>
        <p>
          平台定期发起科普创作征集。创作者在活动期间提交作品，评审员人工评分，
          活动结束后由管理员确认并公示优秀作品。
        </p>
      </div>
      <div class="events-hero__stats">
        <div class="events-hero__stat">
          <strong>{{ ongoingCount }}</strong>
          <span>进行中</span>
        </div>
        <div class="events-hero__stat">
          <strong>{{ events.length }}</strong>
          <span>全部活动</span>
        </div>
      </div>
    </header>

    <div v-loading="loading">
      <el-alert
        v-if="loadError && !loading"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
      />

      <!-- 空态：不只是「暂无数据」，给出这个页面能做什么 -->
      <div v-else-if="!loading && events.length === 0" class="events-empty">
        <h2>当前没有进行中的活动</h2>
        <p>
          活动由管理员按学期发起，公告会同步在首页发布。等下一次征集开始时，
          你可以把自己创作的图文、漫画或短视频投进去。
        </p>
        <div class="events-empty__actions">
          <RouterLink to="/knowledge">
            <el-button type="primary">先去知识库找灵感</el-button>
          </RouterLink>
          <RouterLink to="/works">
            <el-button plain>看看别人的作品</el-button>
          </RouterLink>
        </div>
      </div>

      <div v-else class="event-grid">
        <RouterLink
          v-for="event in events"
          :key="event.id"
          :to="`/events/${event.id}`"
          class="event-card"
          :class="{ 'event-card--ended': event.timeStatus === 'ENDED' }"
        >
          <div class="event-card__cover">
            <img v-if="event.coverUrl" :src="event.coverUrl" :alt="event.title" loading="lazy" />
            <div v-else class="event-card__cover-fallback">{{ event.title.charAt(0) }}</div>

            <!-- 状态蒙层：已结束的活动把封面压暗，避免读者以为还能投稿 -->
            <div v-if="event.timeStatus === 'ENDED'" class="event-card__mask">
              <span>已结束</span>
            </div>

            <span
              v-if="event.timeStatus !== 'ENDED'"
              class="event-card__status"
              :class="`event-card__status--${(event.timeStatus || '').toLowerCase()}`"
            >
              {{ timeMeta[event.timeStatus]?.text || event.timeStatus }}
            </span>

            <span v-if="event.timeStatus === 'ONGOING'" class="event-card__countdown">
              {{ countdownText(event) }}
            </span>
          </div>

          <div class="event-card__body">
            <h3>{{ event.title }}</h3>
            <p>{{ event.description }}</p>
            <div class="event-card__meta">
              <span class="event-card__range">
                {{ formatDate(event.startAt) }} — {{ formatDate(event.endAt) }}
              </span>
              <span class="event-card__count">{{ event.submissionCount }} 件投稿</span>
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 30px;
  margin-bottom: 22px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 84% 20%, var(--sp-primary-a16), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 64%);
}

.events-hero__text h1 {
  margin: 0 0 8px;
  font-size: 24px;
  letter-spacing: -0.2px;
  color: var(--sp-text-strong);
}

.events-hero__text p {
  margin: 0;
  max-width: 720px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.events-hero__stats {
  display: flex;
  gap: 26px;
  flex-shrink: 0;
  padding-left: 26px;
  border-left: 1px solid var(--sp-border);
}

.events-hero__stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.events-hero__stat strong {
  font-size: 24px;
  line-height: 1.1;
  color: var(--sp-text-strong);
  font-variant-numeric: tabular-nums;
}

.events-hero__stat span {
  font-size: 11.5px;
  color: var(--sp-text-faint);
}

/* 两个活动时两列是稳的；管理员增加活动后继续沿用两列，卡片不会被压窄 */
.event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.event-card {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(24, 44, 68, 0.06);
  border-radius: 16px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  box-shadow: 0 1px 2px rgba(24, 44, 68, 0.05);
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;
}

.event-card:hover {
  border-color: rgba(24, 44, 68, 0.1);
  box-shadow: 0 14px 30px -8px rgba(24, 44, 68, 0.18);
  transform: translateY(-3px);
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
  transition: transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.event-card:hover .event-card__cover img {
  transform: scale(1.04);
}

.event-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 32px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.event-card__mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(16, 32, 52, 0.46);
}

.event-card__mask span {
  padding: 6px 18px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #fff;
}

.event-card__status {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11.5px;
  font-weight: 600;
  color: #fff;
  backdrop-filter: blur(2px);
}

.event-card__status--ongoing {
  background: rgba(38, 161, 105, 0.92);
}

.event-card__status--ended {
  background: rgba(90, 106, 128, 0.82);
}

.event-card__status--not_started {
  background: rgba(90, 106, 128, 0.82);
}

/* 倒计时放在封面右下，是进行中活动最有用的一个数字 */
.event-card__countdown {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.94);
  font-size: 11.5px;
  font-weight: 600;
  color: #1f5f3f;
  font-variant-numeric: tabular-nums;
}

.event-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 15px 18px 16px;
  flex: 1;
}

.event-card__body h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.45;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-wrap: balance;
}

.event-card__body p {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.event-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--sp-border);
  font-size: 11.5px;
  color: var(--sp-text-faint);
}

.event-card__range {
  font-variant-numeric: tabular-nums;
}

.event-card__count {
  flex-shrink: 0;
}

/* 已结束的活动整体降一档，让进行中的卡片先被看到 */
.event-card--ended {
  background: #fcfcfd;
}

.event-card--ended .event-card__body h3 {
  color: var(--sp-text);
}

.events-empty {
  padding: 54px 32px;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  background: #fff;
  text-align: center;
}

.events-empty h2 {
  margin: 0 0 10px;
  font-size: 18px;
  color: var(--sp-text-strong);
}

.events-empty p {
  margin: 0 auto 22px;
  max-width: 520px;
  font-size: 13px;
  line-height: 1.85;
  color: var(--sp-text-muted);
}

.events-empty__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.events-empty__actions a {
  text-decoration: none;
}

@media (max-width: 900px) {
  .events-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .events-hero__stats {
    padding-left: 0;
    border-left: 0;
    padding-top: 16px;
    border-top: 1px solid var(--sp-border);
    width: 100%;
  }

  .event-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
