<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  closeEvent,
  confirmEventAwards,
  createEvent,
  deleteEvent,
  getAdminEvent,
  listAdminEvents,
  publishEvent,
  revokeAward,
  updateEvent,
} from '@/api/event'

const loading = ref(false)
const rows = ref([])

const statusMeta = {
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '已发布', type: 'success' },
  CLOSED: { text: '已结束', type: 'warning' },
  CANCELLED: { text: '已取消', type: 'info' },
}

const timeMeta = {
  ONGOING: '进行中',
  NOT_STARTED: '未开始',
  ENDED: '已结束',
  UNPUBLISHED: '未发布',
}

const awardLabels = {
  FIRST: '一等奖',
  SECOND: '二等奖',
  THIRD: '三等奖',
  EXCELLENT: '优秀奖',
}

// ---------------- 活动表单 ----------------

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  title: '',
  description: '',
  rules: '',
  coverUrl: '',
  startAt: '',
  endAt: '',
  status: 'DRAFT',
})

const rules = {
  title: [
    { required: true, message: '请输入活动标题', trigger: 'blur' },
    { max: 150, message: '标题不能超过 150 个字符', trigger: 'blur' },
  ],
  startAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endAt: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

// ---------------- 获奖确认 ----------------

const awardVisible = ref(false)
const awardSubmitting = ref(false)
const currentEvent = ref(null)
const submissions = ref([])
const awardSelections = ref({})

async function fetchRows() {
  loading.value = true
  try {
    rows.value = await listAdminEvents()
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  form.title = ''
  form.description = ''
  form.rules = ''
  form.coverUrl = ''
  form.startAt = ''
  form.endAt = ''
  form.status = 'DRAFT'
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.title = row.title
  form.description = row.description || ''
  form.rules = ''
  form.coverUrl = row.coverUrl || ''
  form.startAt = row.startAt
  form.endAt = row.endAt
  form.status = row.status
  dialogVisible.value = true

  // 规则与完整字段从详情接口取
  getAdminEvent(row.id)
    .then((detail) => {
      form.rules = detail.rules || ''
      form.description = detail.description || ''
    })
    .catch(() => {})
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description,
      rules: form.rules,
      coverUrl: form.coverUrl.trim(),
      startAt: form.startAt,
      endAt: form.endAt,
      status: form.status,
    }
    if (dialogMode.value === 'create') {
      await createEvent(payload)
      ElMessage.success('活动已创建')
    } else {
      await updateEvent(editingId.value, payload)
      ElMessage.success('活动已更新')
    }
    dialogVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

async function handlePublish(row) {
  try {
    await publishEvent(row.id)
    ElMessage.success('活动已发布')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(
      `确定结束「${row.title}」吗？结束后停止投稿，可以开始确认优秀作品。`,
      '结束活动',
      { confirmButtonText: '结束', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await closeEvent(row.id)
    ElMessage.success('活动已结束')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除活动', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteEvent(row.id)
    ElMessage.success('活动已删除')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function openAwards(row) {
  currentEvent.value = row
  awardVisible.value = true
  awardSelections.value = {}
  try {
    const detail = await getAdminEvent(row.id)
    submissions.value = detail.mySubmissions || []
    // 回填已有的获奖标记
    const preset = {}
    submissions.value.forEach((item) => {
      if (item.awardLevel) {
        preset[item.id] = item.awardLevel
      }
    })
    awardSelections.value = preset
  } catch {
    submissions.value = []
  }
}

const reviewedSubmissions = computed(() => submissions.value.filter((s) => s.reviewCount > 0))

async function handleConfirmAwards() {
  if (!currentEvent.value) return
  const awards = Object.entries(awardSelections.value)
    .filter(([, level]) => level)
    .map(([submissionId, awardLevel]) => ({ submissionId: Number(submissionId), awardLevel }))

  if (awards.length === 0) {
    ElMessage.warning('请至少选择一件获奖作品')
    return
  }

  awardSubmitting.value = true
  try {
    await confirmEventAwards(currentEvent.value.id, awards)
    ElMessage.success('优秀作品已确认')
    awardVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示（例如「该投稿还没有任何评审记录」）
  } finally {
    awardSubmitting.value = false
  }
}

async function handleRevoke(item) {
  try {
    await revokeAward(item.id)
    ElMessage.success('已撤销获奖标记')
    openAwards(currentEvent.value)
    fetchRows()
  } catch {
    // 错误已提示
  }
}

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__head">
      <div>
        <h1>活动管理</h1>
        <p>
          活动需「发布」后才开始接受投稿。时间状态（未开始 / 进行中 / 已结束）由起止时间实时判定。
          优秀作品必须经过评审才能确认——未评审的投稿会被拒绝。
        </p>
      </div>
      <el-button type="primary" @click="openCreate">新建活动</el-button>
    </header>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="title" label="活动标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间状态" width="100">
        <template #default="{ row }">{{ timeMeta[row.timeStatus] || row.timeStatus }}</template>
      </el-table-column>
      <el-table-column label="起止时间" width="230">
        <template #default="{ row }">
          <span class="admin-time">{{ (row.startAt || '').slice(0, 16) }} ~ {{ (row.endAt || '').slice(0, 16) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="submissionCount" label="投稿" width="80" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handlePublish(row)">
            发布
          </el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            link
            type="warning"
            @click="handleClose(row)"
          >
            结束
          </el-button>
          <el-button link type="primary" @click="openAwards(row)">确认优秀作品</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建活动' : '编辑活动'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" maxlength="150" show-word-limit />
        </el-form-item>
        <el-form-item label="活动说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="参与规则" prop="rules">
          <el-input
            v-model="form.rules"
            type="textarea"
            :rows="5"
            maxlength="3000"
            show-word-limit
            placeholder="一条一行，前台按纯文本展示"
          />
        </el-form-item>
        <el-form-item label="封面" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="图片地址，例如 /images/events/autumn.svg" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startAt">
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endAt">
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="PUBLISHED">已发布</el-radio>
            <el-radio value="CLOSED">已结束</el-radio>
            <el-radio value="CANCELLED">已取消</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="awardVisible"
      :title="`确认优秀作品 · ${currentEvent?.title || ''}`"
      width="820px"
      destroy-on-close
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="只有已评审的投稿才能标为优秀"
        description="系统会校验每件入选作品至少有一条评审记录；没有评审的投稿将被拒绝，这是为了保证每个获奖作品都有人工评审依据。"
        class="award-tip"
      />

      <el-empty v-if="submissions.length === 0" description="本活动还没有投稿" :image-size="70" />

      <el-table v-else :data="submissions" border size="small" style="width: 100%">
        <el-table-column prop="workTitle" label="作品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="submitterName" label="投稿人" width="100" />
        <el-table-column label="评审" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.reviewCount > 0" size="small" type="success">
              {{ row.reviewCount }} 条 / 均分 {{ row.averageScore }}
            </el-tag>
            <el-tag v-else size="small" type="info">未评审</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="获奖等级" width="160">
          <template #default="{ row }">
            <el-select
              v-model="awardSelections[row.id]"
              placeholder="不获奖"
              clearable
              size="small"
              :disabled="row.reviewCount === 0"
            >
              <el-option label="一等奖" value="FIRST" />
              <el-option label="二等奖" value="SECOND" />
              <el-option label="三等奖" value="THIRD" />
              <el-option label="优秀奖" value="EXCELLENT" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="已获奖" width="110">
          <template #default="{ row }">
            <template v-if="row.awardLevel">
              <span class="award-badge">{{ awardLabels[row.awardLevel] || row.awardLevel }}</span>
              <el-button link type="danger" size="small" @click="handleRevoke(row)">撤销</el-button>
            </template>
            <span v-else>—</span>
          </template>
        </el-table-column>
      </el-table>

      <p class="award-note">
        可获奖投稿 {{ reviewedSubmissions.length }} / {{ submissions.length }} 件。
      </p>

      <template #footer>
        <el-button @click="awardVisible = false">取消</el-button>
        <el-button type="primary" :loading="awardSubmitting" @click="handleConfirmAwards">
          确认获奖名单
        </el-button>
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
  max-width: 720px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

.admin-time {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.award-tip {
  margin-bottom: 14px;
}

.award-badge {
  margin-right: 6px;
  padding: 1px 8px;
  border-radius: 8px;
  background: #fdf6ec;
  color: #b88230;
  font-size: 11px;
}

.award-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--sp-text-muted);
}
</style>
