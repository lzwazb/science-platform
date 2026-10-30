<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveContribution,
  getReviewContribution,
  listReviewContributions,
  rejectContribution,
} from '@/api/contribution'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const filters = reactive({
  page: 1,
  size: 10,
  status: 'PENDING',
})

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: '', label: '全部' },
]

const statusMeta = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  WITHDRAWN: { text: '已撤回', type: 'info' },
}

const typeLabels = {
  SUPPLEMENT: '补充',
  CORRECTION: '纠错',
  MODIFICATION: '修改建议',
}

const drawerVisible = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const comment = ref('')
const finalContent = ref('')
const acting = ref(false)

const canOperate = computed(() => current.value?.status === 'PENDING')

async function fetchRows() {
  loading.value = true
  try {
    const data = await listReviewContributions({
      page: filters.page,
      size: filters.size,
      status: filters.status || undefined,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  current.value = null
  comment.value = ''
  finalContent.value = ''
  try {
    const detail = await getReviewContribution(row.id)
    current.value = detail
    // 默认把原文放进「最终正文」编辑框，评审员在此基础上合并建议
    finalContent.value = detail.articleContent || ''
  } catch {
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/** 一键把建议内容填入最终正文（覆盖式），随后可手工微调 */
function applySuggestionAsContent() {
  if (!current.value) return
  finalContent.value = current.value.suggestedContent
  ElMessage.info('已用建议内容替换正文，请按需再调整')
}

async function handleApprove() {
  if (!current.value) return
  if (!finalContent.value.trim()) {
    ElMessage.warning('最终正文不能为空')
    return
  }
  acting.value = true
  try {
    await approveContribution(current.value.id, {
      comment: comment.value.trim(),
      finalContent: finalContent.value,
    })
    ElMessage.success('已通过，文章已更新并生成历史版本')
    drawerVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

async function handleReject() {
  if (!current.value) return
  if (!comment.value.trim()) {
    ElMessage.warning('驳回时必须填写审核意见')
    return
  }
  acting.value = true
  try {
    await rejectContribution(current.value.id, comment.value.trim())
    ElMessage.success('已驳回，提交者会收到通知')
    drawerVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="review-page">
    <header class="review-head">
      <div>
        <h1>知识贡献审核</h1>
        <p>
          审核通过会在<strong>同一事务</strong>内完成：保存文章历史版本 → 更新正文与版本号 → 修改建议状态 → 记录操作日志 → 通知提交者。
          任一步失败整体回滚。
        </p>
      </div>
      <el-tag effect="plain" type="warning">核心流程</el-tag>
    </header>

    <div class="review-toolbar">
      <el-select v-model="filters.status" style="width: 150px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <span class="review-toolbar__hint">共 {{ total }} 条</span>
      <el-button size="small" @click="fetchRows">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column label="目标文章" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.articleTitle }}</template>
      </el-table-column>
      <el-table-column label="类型" width="96">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">
            {{ typeLabels[row.contributionType] || row.contributionType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitterName" label="提交人" width="110" />
      <el-table-column prop="reason" label="提交理由" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ row.createdAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="review-pagination">
      <el-pagination
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="(p) => { filters.page = p; fetchRows() }"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="审核知识贡献" size="900px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="current">
          <div class="review-meta">
            <el-tag size="small" :type="statusMeta[current.status]?.type || 'info'">
              {{ statusMeta[current.status]?.text || current.status }}
            </el-tag>
            <el-tag size="small" effect="plain">
              {{ typeLabels[current.contributionType] || current.contributionType }}
            </el-tag>
            <span>提交人：{{ current.submitterName }}</span>
            <span>文章当前第 {{ current.articleVersionNo }} 版</span>
          </div>

          <h2 class="review-title">{{ current.articleTitle }}</h2>

          <div class="review-compare">
            <section class="review-pane">
              <h3>文章当前正文</h3>
              <div class="review-pane__body">{{ current.articleContent }}</div>
            </section>

            <section class="review-pane">
              <h3>提交者建议</h3>
              <div class="review-pane__body">{{ current.suggestedContent }}</div>
            </section>
          </div>

          <section class="review-block">
            <h3>提交理由</h3>
            <p>{{ current.reason }}</p>
          </section>

          <section class="review-block">
            <h3>参考来源</h3>
            <p class="review-source">{{ current.referenceSource }}</p>
          </section>

          <template v-if="canOperate">
            <section class="review-block">
              <h3>
                最终采用的正文
                <el-button link type="primary" size="small" @click="applySuggestionAsContent">
                  用建议内容替换
                </el-button>
              </h3>
              <el-input
                v-model="finalContent"
                type="textarea"
                :rows="10"
                placeholder="默认是文章当前正文；请把建议合并进来，形成最终要发布的完整正文"
              />
              <p class="review-hint">
                这段内容会替换文章正文，原正文会自动存为历史版本，提交者也会收到通知。
              </p>
            </section>

            <section class="review-block">
              <h3>审核意见</h3>
              <el-input
                v-model="comment"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="通过可不填；驳回必须填写具体原因"
              />
            </section>
          </template>

          <section v-else class="review-block">
            <h3>处理结果</h3>
            <p v-if="current.reviewerName">审核人：{{ current.reviewerName }}</p>
            <p v-if="current.reviewedAt">处理时间：{{ current.reviewedAt }}</p>
            <p v-if="current.reviewComment" class="review-comment">{{ current.reviewComment }}</p>
          </section>
        </template>
      </div>

      <template #footer>
        <div class="review-footer">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <template v-if="canOperate">
            <el-button type="danger" plain :loading="acting" @click="handleReject">驳回</el-button>
            <el-button type="success" :loading="acting" @click="handleApprove">
              通过并更新文章
            </el-button>
          </template>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.review-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px 24px 52px;
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

.review-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.review-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--sp-text-muted);
}

.review-title {
  margin: 12px 0 16px;
  font-size: 19px;
  color: var(--sp-text-strong);
}

.review-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.review-pane {
  border: 1px solid var(--sp-border);
  border-radius: 10px;
  overflow: hidden;
}

.review-pane h3 {
  margin: 0;
  padding: 9px 12px;
  font-size: 13px;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  border-bottom: 1px solid var(--sp-border);
}

.review-pane__body {
  padding: 12px 14px;
  max-height: 280px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.85;
  color: var(--sp-text);
  white-space: pre-wrap;
}

.review-block {
  margin-top: 20px;
}

.review-block h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--sp-text-strong);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--sp-border);
}

.review-block p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text);
}

.review-source {
  word-break: break-all;
  color: var(--sp-text-muted) !important;
}

.review-hint {
  margin: 8px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--sp-text-faint);
}

.review-comment {
  padding: 10px 14px;
  background: #fff7e6;
  border-radius: 8px;
  color: #8c6b2f !important;
}

.review-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .review-compare {
    grid-template-columns: 1fr;
  }
}
</style>
