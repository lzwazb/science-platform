<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEventDetail, submitWorkToEvent } from '@/api/event'
import { listMyWorks } from '@/api/work'
import { useUserStore } from '@/stores/user'
import { TIME_STATUS } from '@/constants/dict'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const event = ref(null)

const submitVisible = ref(false)
const submitting = ref(false)
const myWorks = ref([])
const selectedWorkId = ref(null)
const worksLoading = ref(false)

const eventId = computed(() => route.params.id)

const timeMeta = TIME_STATUS

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

async function fetchDetail() {
  loading.value = true
  loadError.value = ''
  event.value = null
  try {
    event.value = await getEventDetail(eventId.value)
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

watch(eventId, fetchDetail)
onMounted(fetchDetail)
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
      <header class="event-header">
        <div v-if="event.coverUrl" class="event-header__cover">
          <img :src="event.coverUrl" :alt="event.title" />
        </div>
        <div class="event-header__text">
          <div class="event-header__tags">
            <el-tag :type="timeMeta[event.timeStatus]?.type || 'info'" effect="dark" size="small">
              {{ timeMeta[event.timeStatus]?.text || event.timeStatus }}
            </el-tag>
            <span class="event-header__range">
              {{ (event.startAt || '').slice(0, 16) }} ~ {{ (event.endAt || '').slice(0, 16) }}
            </span>
          </div>
          <h1>{{ event.title }}</h1>
          <p>{{ event.description }}</p>
          <div class="event-header__actions">
            <el-button v-if="canSubmit" type="primary" @click="openSubmit">提交作品参赛</el-button>
            <el-tag v-else type="info" effect="plain">
              {{ event.timeStatus === 'ENDED' ? '活动已结束，停止投稿' : '当前不接受投稿' }}
            </el-tag>
            <span class="event-header__count">已有 {{ event.submissionCount }} 件投稿</span>
          </div>
        </div>
      </header>

      <section v-if="event.rules" class="event-section">
        <h2>参与规则</h2>
        <div class="event-rules">{{ event.rules }}</div>
      </section>

      <section v-if="event.mySubmissions?.length" class="event-section">
        <h2>我的投稿</h2>
        <el-table :data="event.mySubmissions" border size="small" style="width: 100%">
          <el-table-column prop="workTitle" label="作品" min-width="200" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'SELECTED' ? 'success' : 'info'">
                {{ row.status === 'SELECTED' ? '已获奖' : row.status === 'REVIEWED' ? '已评审' : '待评审' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="平均分" width="90">
            <template #default="{ row }">{{ row.averageScore ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="评审数" width="90">
            <template #default="{ row }">{{ row.reviewCount }}</template>
          </el-table-column>
          <el-table-column label="获奖" width="100">
            <template #default="{ row }">{{ awardLabels[row.awardLevel] || '—' }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section class="event-section">
        <h2>优秀作品</h2>
        <el-empty
          v-if="!event.awards?.length"
          description="活动结束后由管理员确认并公示优秀作品"
          :image-size="80"
        />

        <div v-else class="award-grid">
          <article v-for="award in event.awards" :key="award.submissionId" class="award-card">
            <div class="award-card__cover" @click="goWork(award.workId)">
              <img v-if="award.workCover" :src="award.workCover" :alt="award.workTitle" loading="lazy" />
              <div v-else class="award-card__cover-fallback">{{ award.workTitle.charAt(0) }}</div>
              <span class="award-card__level">{{ awardLabels[award.awardLevel] || award.awardLevel }}</span>
            </div>
            <div class="award-card__body">
              <h3 @click="goWork(award.workId)">{{ award.workTitle }}</h3>
              <p>{{ award.workDescription }}</p>
              <div class="award-card__meta">
                <span>{{ award.creatorName }}</span>
                <span v-if="award.averageScore">平均分 {{ award.averageScore }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>
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
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 24px 56px;
}

.event-crumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.event-header {
  display: flex;
  gap: 22px;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background: #fff;
  margin-bottom: 22px;
}

.event-header__cover {
  flex-shrink: 0;
  width: 220px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--sp-border);
}

.event-header__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.event-header__tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.event-header__range {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.event-header__text h1 {
  margin: 0 0 10px;
  font-size: 24px;
  color: var(--sp-text-strong);
}

.event-header__text p {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.85;
  color: var(--sp-text-muted);
}

.event-header__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.event-header__count {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.event-section {
  margin-top: 26px;
}

.event-section h2 {
  margin: 0 0 12px;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.event-rules {
  padding: 16px 18px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  font-size: 13px;
  line-height: 1.9;
  color: var(--sp-text);
  white-space: pre-wrap;
}

.award-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.award-card {
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.award-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 8px 20px var(--sp-primary-a10);
}

.award-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  cursor: pointer;
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

.award-card__level {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(230, 162, 60, 0.92);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.award-card__body {
  padding: 12px 14px 14px;
}

.award-card__body h3 {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--sp-text-strong);
  cursor: pointer;
}

.award-card__body p {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.7;
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
  font-size: 11px;
  color: var(--sp-text-faint);
}

.submit-hint {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

@media (max-width: 760px) {
  .event-header {
    flex-direction: column;
  }

  .event-header__cover {
    width: 100%;
  }
}
</style>

