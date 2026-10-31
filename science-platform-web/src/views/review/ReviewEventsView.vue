<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminEvents, listEventSubmissionsForReview, reviewSubmission } from '@/api/event'

const loading = ref(false)
const eventsLoading = ref(false)
const events = ref([])
const selectedEventId = ref(null)
const submissions = ref([])

const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const form = reactive({
  score: 5,
  comment: '',
})

const currentEvent = computed(() => events.value.find((e) => e.id === selectedEventId.value) || null)

async function fetchEvents() {
  eventsLoading.value = true
  try {
    events.value = await listAdminEvents()
    if (events.value.length > 0 && !selectedEventId.value) {
      // 默认选第一个可评审的活动（非草稿）
      const candidate = events.value.find((e) => e.status !== 'DRAFT') || events.value[0]
      selectedEventId.value = candidate.id
      fetchSubmissions()
    }
  } catch {
    events.value = []
  } finally {
    eventsLoading.value = false
  }
}

async function fetchSubmissions() {
  if (!selectedEventId.value) return
  loading.value = true
  try {
    submissions.value = await listEventSubmissionsForReview(selectedEventId.value)
  } catch {
    submissions.value = []
  } finally {
    loading.value = false
  }
}

function openReview(row) {
  current.value = row
  form.score = 5
  form.comment = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!current.value) return
  if (!form.comment.trim()) {
    ElMessage.warning('请填写评语')
    return
  }
  submitting.value = true
  try {
    await reviewSubmission(current.value.id, {
      score: form.score,
      comment: form.comment.trim(),
    })
    ElMessage.success('评审已提交')
    dialogVisible.value = false
    fetchSubmissions()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

function handleEventChange() {
  fetchSubmissions()
}

onMounted(fetchEvents)
</script>

<template>
  <div class="review-page">
    <header class="review-head">
      <div>
        <h1>活动投稿评审</h1>
        <p>
          评分统一 1–5 分，评语必填。同一评审员对同一投稿只保留一份有效评审——重新提交会覆盖上次结果。
          获奖名单由管理员在后台确认，评审员只负责打分。
        </p>
      </div>
      <el-tag effect="plain" type="warning">评审工作台</el-tag>
    </header>

    <div class="review-toolbar">
      <el-select
        v-model="selectedEventId"
        placeholder="选择活动"
        style="width: 320px"
        :loading="eventsLoading"
        @change="handleEventChange"
      >
        <el-option
          v-for="event in events"
          :key="event.id"
          :label="`${event.title}（${event.status === 'DRAFT' ? '草稿' : event.timeStatus === 'ONGOING' ? '进行中' : event.timeStatus === 'ENDED' ? '已结束' : '未开始'}）`"
          :value="event.id"
          :disabled="event.status === 'DRAFT'"
        />
      </el-select>
      <span class="review-toolbar__hint">共 {{ submissions.length }} 件投稿</span>
      <el-button size="small" @click="fetchSubmissions">刷新</el-button>
    </div>

    <el-alert
      v-if="currentEvent && currentEvent.timeStatus === 'ONGOING'"
      type="info"
      :closable="false"
      show-icon
      title="活动进行中也可以提前评审"
      description="评审与投稿时间互不影响；活动结束后管理员才会确认获奖名单。"
      class="review-tip"
    />

    <el-table v-loading="loading" :data="submissions" border stripe style="width: 100%">
      <el-table-column prop="id" label="投稿 ID" width="90" />
      <el-table-column prop="workTitle" label="作品" min-width="200" show-overflow-tooltip />
      <el-table-column prop="submitterName" label="投稿人" width="110" />
      <el-table-column label="评审情况" width="150">
        <template #default="{ row }">
          <el-tag v-if="row.reviewCount > 0" size="small" type="success">
            {{ row.reviewCount }} 条 · 均分 {{ row.averageScore }}
          </el-tag>
          <el-tag v-else size="small" type="info">暂无评审</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="我的评审" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.reviewedByMe" size="small" type="warning">已评</el-tag>
          <span v-else class="review-muted">未评</span>
        </template>
      </el-table-column>
      <el-table-column label="获奖" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.awardLevel" size="small" type="danger">
            {{ { FIRST: '一等奖', SECOND: '二等奖', THIRD: '三等奖', EXCELLENT: '优秀奖' }[row.awardLevel] }}
          </el-tag>
          <span v-else class="review-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="投稿时间" width="170">
        <template #default="{ row }">{{ row.submittedAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReview(row)">
            {{ row.reviewedByMe ? '修改评分' : '评分' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="投稿评分" width="560px" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="作品">{{ current.workTitle }}</el-descriptions-item>
          <el-descriptions-item label="投稿人">{{ current.submitterName }}</el-descriptions-item>
          <el-descriptions-item label="作品说明">{{ current.workDescription || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-width="70px" class="review-form">
          <el-form-item label="评分">
            <el-rate v-model="form.score" :max="5" show-score score-template="{value} 分" />
          </el-form-item>
          <el-form-item label="评语">
            <el-input
              v-model="form.comment"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="说明打分依据，例如科学性、可读性、创意"
            />
          </el-form-item>
        </el-form>

        <p v-if="current.reviewedByMe" class="review-warn">
          你已经评过这件作品，本次提交会覆盖上次的分数与评语。
        </p>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评审</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-page {
  max-width: var(--sp-container-wide);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.review-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.review-head p {
  margin: 0;
  max-width: 720px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.review-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.review-toolbar__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.review-tip {
  margin-bottom: 14px;
}

.review-muted {
  color: var(--sp-text-faint);
}

.review-form {
  margin-top: 18px;
}

.review-warn {
  margin: 8px 0 0;
  padding: 8px 12px;
  background: #fff7e6;
  border-radius: 6px;
  font-size: 12px;
  color: #8c6b2f;
}

@media (max-width: 768px) {
  .review-page {
    padding: var(--sp-gap) 16px var(--sp-gap-lg);
  }
}
</style>
