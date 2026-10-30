<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { handleFeedback, listAdminFeedback } from '@/api/interaction'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const filters = reactive({
  page: 1,
  size: 10,
  status: '',
})

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'RESOLVED', label: '已处理' },
  { value: 'REJECTED', label: '已驳回' },
]

const statusMeta = {
  PENDING: { text: '待处理', type: 'warning' },
  PROCESSING: { text: '处理中', type: 'primary' },
  RESOLVED: { text: '已处理', type: 'success' },
  REJECTED: { text: '已驳回', type: 'info' },
}

const typeLabels = {
  WRONG_CONTENT: '内容错误',
  COPYRIGHT: '版权问题',
  SPAM: '垃圾信息',
  SUGGESTION: '改进建议',
  OTHER: '其他',
}

const targetLabels = {
  ARTICLE: '文章',
  WORK: '作品',
  COMMENT: '评论',
}

const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref(null)
const handleForm = reactive({
  status: 'RESOLVED',
  handleComment: '',
})

async function fetchRows() {
  loading.value = true
  try {
    const data = await listAdminFeedback({
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

function openHandle(row) {
  current.value = row
  handleForm.status = row.status === 'PENDING' ? 'RESOLVED' : 'PROCESSING'
  handleForm.handleComment = row.handleComment || ''
  dialogVisible.value = true
}

async function submitHandle() {
  if (!current.value) return
  submitting.value = true
  try {
    await handleFeedback(current.value.id, {
      status: handleForm.status,
      handleComment: handleForm.handleComment.trim(),
    })
    ElMessage.success('反馈已处理')
    dialogVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__head">
      <div>
        <h1>内容反馈</h1>
        <p>用户对文章、作品或评论提交的反馈。处理结果会记入操作日志，重要操作可追溯。</p>
      </div>
      <el-button size="small" @click="fetchRows">刷新</el-button>
    </header>

    <div class="admin-toolbar">
      <el-select v-model="filters.status" style="width: 150px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <span class="admin-toolbar__hint">共 {{ total }} 条</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column label="反馈类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ typeLabels[row.feedbackType] || row.feedbackType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="feedback-target">{{ targetLabels[row.targetType] || row.targetType }}：{{ row.targetTitle }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="反馈说明" min-width="240" show-overflow-tooltip />
      <el-table-column prop="userName" label="提交人" width="110" />
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
          <el-button
            link
            type="primary"
            :disabled="row.status === 'RESOLVED' || row.status === 'REJECTED'"
            @click="openHandle(row)"
          >
            处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <el-pagination
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="(p) => { filters.page = p; fetchRows() }"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="处理反馈" width="560px" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="反馈类型">
            {{ typeLabels[current.feedbackType] || current.feedbackType }}
          </el-descriptions-item>
          <el-descriptions-item label="目标内容">
            {{ targetLabels[current.targetType] }}：{{ current.targetTitle }}
          </el-descriptions-item>
          <el-descriptions-item label="提交人">{{ current.userName }}</el-descriptions-item>
          <el-descriptions-item label="反馈说明">{{ current.description }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-width="80px" class="feedback-form">
          <el-form-item label="处理结果">
            <el-radio-group v-model="handleForm.status">
              <el-radio value="RESOLVED">已处理</el-radio>
              <el-radio value="PROCESSING">处理中</el-radio>
              <el-radio value="REJECTED">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明">
            <el-input
              v-model="handleForm.handleComment"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="简要说明处理方式，便于后续追溯"
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.admin-page__head h1 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--sp-text-strong);
}

.admin-page__head p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

.admin-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.admin-toolbar__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.feedback-target {
  font-size: 13px;
  color: var(--sp-text);
}

.feedback-form {
  margin-top: 18px;
}
</style>
