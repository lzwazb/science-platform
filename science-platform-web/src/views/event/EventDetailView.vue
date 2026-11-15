<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEventDetail, listEventSubmissions, submitWorkToEvent } from '@/api/event'
import { listMyWorks } from '@/api/work'
import { useUserStore } from '@/stores/user'
import { SUBMISSION_STATUS, TIME_STATUS } from '@/constants/dict'
import { formatDate, formatDateTime, remainingTo } from '@/utils/datetime'
import ContentCard from '@/components/content/ContentCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const event = ref(null)
/** 公开参赛作品列表（不含评审得分） */
const submissions = ref([])

const submitVisible = ref(false)
const submitting = ref(false)
const myWorks = ref([])
const selectedWorkId = ref(null)
const worksLoading = ref(false)

const eventId = computed(() => route.params.id)

const timeMeta = TIME_STATUS
const submissionMeta = SUBMISSION_STATUS

const awardLabels = {
  FIRST: '一等奖',
  SECOND: '二等奖',
  THIRD: '三等奖',
  EXCELLENT: '优秀奖',
}

const canSubmit = computed(() => Boolean(event.value?.acceptingSubmission))
const alreadySubmittedIds = computed(() =>
  new Set((event.value?.mySubmissions || []).map((item) => item.workId)),
)

/** 可投稿的作品：本人已发布、且尚未报名本活动 */
const availableWorks = computed(() =>
  myWorks.value.filter((work) => !alreadySubmittedIds.value.has(work.id)),
)

/** 规则文本每行自带「1. 」编号，拆成列表时去掉，交给 <ol> 编号 */
const ruleLines = computed(() => {
  const raw = event.value?.rules || ''
  return raw
    .split(/\n+/)
    .map((line) => line.replace(/^\s*\d+\s*[.、)]\s*/, '').trim())
    .filter(Boolean)
})

const countdown = computed(() => remainingTo(event.value?.endAt))

/**
 * 参赛作品区只列未获奖的投稿：获奖作品在上面的「优秀作品」里已经带奖牌展示过一次，
 * 同一张卡片在一屏里出现两次会显得重复。
 */
const nonAwardedSubmissions = computed(() => submissions.value.filter((item) => !item.awardLevel))

async function fetchSubmissions() {
  try {
    submissions.value = await listEventSubmissions(eventId.value)
  } catch {
    submissions.value = []
  }
}

async function fetchDetail() {
  loading.value = true
  loadError.value = ''
  event.value = null
  submissions.value = []
  try {
    event.value = await getEventDetail(eventId.value)
    fetchSubmissions()
  } catch (error) {
    loadError.value = error?.response?.data?.message || '活动加载失败'
  } finally {
    loading.value = false
  }
}

async function openSubmit() {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  submitVisible.value = true
  selectedWorkId.value = null
  worksLoading.value = true
  try {
    const data = await listMyWorks({ page: 1, size: 100, status: 'PUBLISHED' })
    myWorks.value = data.records || []
  } catch {
    myWorks.value = []
  } finally {
    worksLoading.value = false
  }
}

async function handleSubmit() {
  if (!selectedWorkId.value) {
    ElMessage.warning('请选择要投稿的作品')
    return
  }
  submitting.value = true
  try {
    await submitWorkToEvent(eventId.value, selectedWorkId.value)
    ElMessage.success('投稿成功，等待评审')
    submitVisible.value = false
    fetchDetail()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

function goWork(workId) {
  router.push(`/works/${workId}`)
}

/**
 * 底部投稿条只在滚过头部按钮之后出现：
 * 首屏已经有「提交作品参赛」，两条并存既重复又挡住正文。
 */
const showBar = ref(false)

function onScroll() {
  showBar.value = window.scrollY > 280
}

watch(eventId, fetchDetail)
onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  fetchDetail()
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <div class="event-detail" v-loading="loading">
    <el-breadcrumb separator="/" class="event-crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/events' }">主题活动</el-breadcrumb-item>
      <el-breadcrumb-item>{{ event?.title || '活动详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-result
      v-if="loadError && !loading"
      icon="warning"
      title="无法打开这个活动"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/events')">返回活动列表</el-button>
      </template>
    </el-result>

    <template v-else-if="event">
      <header class="event-hero">
        <div class="event-hero__cover">
          <img v-if="event.coverUrl" :src="event.coverUrl" :alt="event.title" />
          <div v-else class="event-hero__cover-fallback">{{ event.title.charAt(0) }}</div>
          <div v-if="event.timeStatus === 'ENDED'" class="event-hero__mask">
            <span>已结束</span>
          </div>
        </div>

        <div class="event-hero__body">
          <div class="event-hero__tags">
            <span
              v-if="event.timeStatus !== 'ENDED'"
              class="status-chip"
              :class="`status-chip--${(event.timeStatus || '').toLowerCase()}`"
            >
              {{ timeMeta[event.timeStatus]?.text || event.timeStatus }}
            </span>
            <span v-if="event.timeStatus === 'ONGOING'" class="event-hero__countdown">
              {{ countdown.text }}
            </span>
          </div>

          <h1>{{ event.title }}</h1>
          <p class="event-hero__desc">{{ event.description }}</p>

          <div class="event-hero__meta">
            <span>征集时间 {{ formatDate(event.startAt) }} — {{ formatDate(event.endAt) }}</span>
            <span class="event-hero__dot">·</span>
            <span>{{ event.submissionCount }} 件投稿</span>
          </div>

          <div class="event-hero__actions">
            <el-button v-if="canSubmit" type="primary" @click="openSubmit">
              提交作品参赛
            </el-button>
            <el-button v-else-if="event.timeStatus === 'ENDED'" disabled>活动已结束</el-button>
            <el-button v-else disabled>当前不接受投稿</el-button>
          </div>
        </div>
      </header>

      <section v-if="ruleLines.length" class="event-section">
        <h2>参与规则</h2>
        <ol class="event-rules">
          <li v-for="(line, index) in ruleLines" :key="index">{{ line }}</li>
        </ol>
      </section>

      <section v-if="event.awards?.length" class="event-section">
        <h2>优秀作品</h2>
        <div class="award-grid">
          <article
            v-for="award in event.awards"
            :key="award.submissionId"
            class="award-card"
            :class="`award-card--${(award.awardLevel || '').toLowerCase()}`"
            @click="goWork(award.workId)"
          >
            <div class="award-card__cover">
              <img
                v-if="award.workCover"
                :src="award.workCover"
                :alt="award.workTitle"
                loading="lazy"
              />
              <div v-else class="award-card__cover-fallback">
                {{ award.workTitle.charAt(0) }}
              </div>
              <span class="award-card__medal">
                {{ awardLabels[award.awardLevel] || award.awardLevel }}
              </span>
            </div>
            <div class="award-card__body">
              <h3>{{ award.workTitle }}</h3>
              <p>{{ award.workDescription }}</p>
              <div class="award-card__meta">
                <span>{{ award.creatorName }}</span>
                <span v-if="award.averageScore">平均分 {{ award.averageScore }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="event-section">
        <div class="event-section__head">
          <h2>
            参赛作品
            <span class="event-section__count">{{ submissions.length }}</span>
          </h2>
          <span v-if="!event.awards?.length && event.timeStatus === 'ENDED'" class="event-section__hint">
            优秀作品名单由管理员在活动结束后确认
          </span>
        </div>

        <p v-if="!submissions.length" class="event-empty">
          还没有作品报名这个活动。活动期间投稿的作品会按投稿时间显示在这里。
        </p>

        <p v-else-if="!nonAwardedSubmissions.length" class="event-empty">
          本次活动的 {{ submissions.length }} 件投稿均已获奖，见上方「优秀作品」。
        </p>

        <div v-else class="event-submissions">
          <ContentCard
            v-for="item in nonAwardedSubmissions"
            :key="item.id"
            :to="`/works/${item.workId}`"
            :cover="item.workCover"
            :title="item.workTitle"
            :summary="item.workDescription"
            :category="item.knowledgeCategoryName"
            :left="item.submitterName"
            :right="formatDateTime(item.submittedAt)"
          />
        </div>
      </section>

      <section v-if="event.mySubmissions?.length" class="event-section">
        <h2>我的投稿</h2>
        <div class="mine-list">
          <div v-for="item in event.mySubmissions" :key="item.id" class="mine-item">
            <div class="mine-item__main">
              <RouterLink :to="`/works/${item.workId}`" class="mine-item__title">
                {{ item.workTitle }}
              </RouterLink>
              <span class="mine-item__time">{{ formatDateTime(item.submittedAt) }} 投稿</span>
            </div>

            <div class="mine-item__meta">
              <span
                class="mine-item__status"
                :class="`mine-item__status--${(item.status || '').toLowerCase()}`"
              >
                {{ submissionMeta[item.status]?.text || item.status }}
              </span>
              <span class="mine-item__score">
                平均分 <strong>{{ item.averageScore ?? '—' }}</strong>
              </span>
              <span class="mine-item__reviews">评审 {{ item.reviewCount }} 条</span>
              <span v-if="item.awardLevel" class="mine-item__award">
                {{ awardLabels[item.awardLevel] || item.awardLevel }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- 滚过首屏后仍然能投稿：把入口贴在视口底部 -->
      <transition name="bar-fade">
        <div v-if="canSubmit && showBar" class="submit-bar">
          <div class="submit-bar__text">
            <span>征集至 {{ formatDate(event.endAt) }} 截止</span>
            <span class="submit-bar__sep">·</span>
            <span class="submit-bar__extra">已有 {{ event.submissionCount }} 件投稿</span>
          </div>
          <el-button type="primary" @click="openSubmit">提交作品参赛</el-button>
        </div>
      </transition>
    </template>

    <el-dialog v-model="submitVisible" title="提交作品参赛" width="520px" destroy-on-close>
      <p class="submit-hint">
        只能提交本人<strong>已发布</strong>且尚未报名本活动的作品。同一作品在同一活动只能投稿一次。
      </p>

      <div v-loading="worksLoading">
        <el-empty
          v-if="!worksLoading && availableWorks.length === 0"
          description="没有可投稿的作品。先在创作者中心创作一件，通过审核发布后就能参加活动"
          :image-size="70"
        />

        <el-select
          v-else
          v-model="selectedWorkId"
          placeholder="选择要投稿的作品"
          style="width: 100%"
        >
          <el-option
            v-for="work in availableWorks"
            :key="work.id"
            :label="work.title"
            :value="work.id"
          />
        </el-select>
      </div>

      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认投稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.event-detail {
  max-width: var(--sp-container-reading);
  margin: 0 auto;
  /* 底部留出浮动投稿条的高度，滚到页尾时不会压住最后一张卡片 */
  padding: 24px 24px 96px;
}

.event-crumb {
  margin-bottom: 18px;
  font-size: 13px;
}

/* ---------------- 头部 ---------------- */

.event-hero {
  display: flex;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  background: #fff;
}

.event-hero__cover {
  position: relative;
  flex-shrink: 0;
  width: 240px;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--sp-border);
  background: var(--sp-primary-soft);
}

.event-hero__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.event-hero__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 30px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.event-hero__mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(16, 32, 52, 0.46);
}

.event-hero__mask span {
  padding: 4px 14px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 16px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #fff;
}

.event-hero__body {
  min-width: 0;
  flex: 1;
}

.event-hero__tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.status-chip {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11.5px;
  font-weight: 600;
}

.status-chip--ongoing {
  background: rgba(38, 161, 105, 0.12);
  color: #1f7a4d;
}

.status-chip--ended {
  background: rgba(90, 106, 128, 0.12);
  color: #53627a;
}

.status-chip--not_started {
  background: rgba(90, 106, 128, 0.12);
  color: #53627a;
}

.event-hero__countdown {
  font-size: 11.5px;
  font-weight: 600;
  color: #1f7a4d;
  font-variant-numeric: tabular-nums;
}

.event-hero__body h1 {
  margin: 0 0 10px;
  font-size: 24px;
  line-height: 1.42;
  letter-spacing: -0.2px;
  color: var(--sp-text-strong);
}

.event-hero__desc {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.85;
  color: var(--sp-text-muted);
}

.event-hero__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.event-hero__dot {
  color: var(--sp-border);
}

.event-hero__actions {
  margin-top: 16px;
}

/* ---------------- 区段 ---------------- */

.event-section {
  margin-top: 28px;
}

.event-section h2 {
  margin: 0 0 14px;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.event-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.event-section__head h2 {
  margin: 0;
}

.event-section__count {
  margin-left: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.event-section__hint {
  font-size: 12px;
  color: var(--sp-text-faint);
}

/* 规则：编号交给 ol，行距放宽，不再是一整块 pre-wrap 文本 */
.event-rules {
  margin: 0;
  padding: 18px 22px 18px 40px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fbfdff;
  font-size: 13px;
  line-height: 1.95;
  color: var(--sp-text);
}

.event-rules li + li {
  margin-top: 4px;
}

.event-rules li::marker {
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.event-empty {
  margin: 0;
  padding: 34px 20px;
  border: 1px dashed var(--sp-border);
  border-radius: 12px;
  text-align: center;
  font-size: 13px;
  color: var(--sp-text-faint);
}

/* ---------------- 参赛作品 ---------------- */

.event-submissions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

/* ---------------- 优秀作品（奖牌色） ---------------- */

.award-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.award-card {
  border: 1px solid rgba(24, 44, 68, 0.06);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(24, 44, 68, 0.05);
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;
}

.award-card:hover {
  border-color: rgba(24, 44, 68, 0.1);
  box-shadow: 0 14px 30px -8px rgba(24, 44, 68, 0.18);
  transform: translateY(-3px);
}

.award-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.award-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.award-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

/* 一/二/三等奖用金、银、铜，优秀奖用品牌蓝：一眼分得出层级 */
.award-card__medal {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: rgba(90, 106, 128, 0.92);
}

.award-card--first .award-card__medal {
  background: linear-gradient(135deg, #e0a92b, #c8860f);
}

.award-card--second .award-card__medal {
  background: linear-gradient(135deg, #b6bfcc, #8e99a9);
}

.award-card--third .award-card__medal {
  background: linear-gradient(135deg, #cb8f5b, #a9663a);
}

.award-card--excellent .award-card__medal {
  background: linear-gradient(135deg, #5b9bd8, #347fd1);
}

.award-card--first {
  border-color: rgba(200, 134, 15, 0.34);
}

.award-card__body {
  padding: 13px 15px 15px;
}

.award-card__body h3 {
  margin: 0 0 6px;
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.award-card__body p {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.award-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 9px;
  border-top: 1px solid var(--sp-border);
  font-size: 11.5px;
  color: var(--sp-text-faint);
}

/* ---------------- 我的投稿 ---------------- */

.mine-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mine-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 16px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
}

.mine-item__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mine-item__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--sp-text-strong);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-item__title:hover {
  color: var(--sp-primary);
}

.mine-item__time {
  font-size: 11.5px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.mine-item__meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--sp-text-muted);
}

.mine-item__status {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11.5px;
  font-weight: 600;
}

.mine-item__status--submitted {
  background: rgba(90, 106, 128, 0.1);
  color: #53627a;
}

.mine-item__status--under_review,
.mine-item__status--reviewed {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.mine-item__status--selected {
  background: rgba(38, 161, 105, 0.12);
  color: #1f7a4d;
}

.mine-item__status--not_selected,
.mine-item__status--withdrawn {
  background: rgba(90, 106, 128, 0.1);
  color: #53627a;
}

.mine-item__score strong {
  color: var(--sp-text-strong);
  font-variant-numeric: tabular-nums;
}

.mine-item__award {
  padding: 2px 9px;
  border-radius: 10px;
  background: linear-gradient(135deg, #e0a92b, #c8860f);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

/* ---------------- 底部投稿条 ---------------- */

.submit-bar {
  position: sticky;
  bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 30px;
  padding: 13px 18px;
  border: 1px solid rgba(24, 44, 68, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 28px -10px rgba(24, 44, 68, 0.28);
  backdrop-filter: blur(6px);
}

.bar-fade-enter-active,
.bar-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.bar-fade-enter-from,
.bar-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.submit-bar__text {
  font-size: 12.5px;
  color: var(--sp-text-muted);
  font-variant-numeric: tabular-nums;
}

.submit-bar__sep {
  color: var(--sp-border);
  margin: 0 4px;
}

.submit-hint {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

@media (max-width: 960px) {
  .event-submissions,
  .award-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .event-hero {
    flex-direction: column;
    padding: 20px;
  }

  .event-hero__cover {
    width: 100%;
  }

  .event-hero__body h1 {
    font-size: 20px;
  }

  .event-submissions,
  .award-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .mine-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .mine-item__meta {
    flex-wrap: wrap;
    gap: 10px;
  }

  .submit-bar {
    gap: 10px;
    padding: 10px 12px;
  }

  .submit-bar__text {
    font-size: 11.5px;
    line-height: 1.45;
  }

  /* 窄屏只留「截止日期」，投稿数在正文里也能看到 */
  .submit-bar__sep,
  .submit-bar__extra {
    display: none;
  }

  .event-rules {
    padding: 16px 18px 16px 34px;
  }
}
</style>
