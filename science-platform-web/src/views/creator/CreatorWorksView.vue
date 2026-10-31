<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createWork,
  deleteWork,
  getMyWork,
  listMyWorks,
  submitWork,
  updateWork,
} from '@/api/work'
import { listArticles } from '@/api/article'
import { listCategories } from '@/api/category'
import { STATUS_META } from '@/constants/dict'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ref([])
const articleOptions = ref([])

const filters = reactive({
  page: 1,
  size: 10,
  status: '',
})

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING', label: '待审核' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'OFFLINE', label: '已下架' },
]

const statusMeta = STATUS_META

// ---------------- 编辑表单 ----------------

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  title: '',
  description: '',
  content: '',
  coverUrl: '',
  categoryId: null,
  articleIds: [],
})

const rules = {
  title: [
    { required: true, message: '请输入作品标题', trigger: 'blur' },
    { max: 150, message: '标题不能超过 150 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入作品内容或说明', trigger: 'blur' }],
}

const dialogTitle = computed(() =>
  dialogMode.value === 'create' ? '新建作品' : '编辑作品',
)

async function fetchRows() {
  loading.value = true
  try {
    const data = await listMyWorks({
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

async function fetchOptions() {
  try {
    const [cats, articles] = await Promise.all([
      listCategories({ type: 'WORK' }),
      listArticles({ page: 1, size: 100 }),
    ])
    categories.value = cats
    articleOptions.value = articles.records || []
  } catch {
    categories.value = []
    articleOptions.value = []
  }
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.content = ''
  form.coverUrl = ''
  form.categoryId = null
  form.articleIds = []
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  try {
    const detail = await getMyWork(row.id)
    editingId.value = detail.id
    form.title = detail.title
    form.description = detail.description || ''
    form.content = detail.content
    form.coverUrl = detail.coverUrl || ''
    form.categoryId = detail.categoryId
    form.articleIds = (detail.articles || []).map((a) => a.id)
    dialogVisible.value = true
  } catch {
    // 错误已提示
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      content: form.content,
      coverUrl: form.coverUrl.trim(),
      categoryId: form.categoryId,
      articleIds: form.articleIds,
    }
    if (dialogMode.value === 'create') {
      await createWork(payload)
      ElMessage.success('作品已保存为草稿，可在列表中提交审核')
    } else {
      const result = await updateWork(editingId.value, payload)
      if (result.status === 'PENDING') {
        ElMessage.warning('作品已修改并自动重新送审，等待评审员处理')
      } else {
        ElMessage.success('作品已更新')
      }
    }
    dialogVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

async function handleSubmitReview(row) {
  try {
    await ElMessageBox.confirm(
      `确定提交《${row.title}》进入审核吗？提交后需等待评审员处理。`,
      '提交审核',
      { confirmButtonText: '提交', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  try {
    await submitWork(row.id)
    ElMessage.success('已提交审核')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除《${row.title}》吗？已被活动投稿引用的作品无法删除。`,
      '删除作品',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteWork(row.id)
    ElMessage.success('作品已删除')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

onMounted(() => {
  fetchOptions()
  fetchRows()
})
</script>

<template>
  <div class="creator-page">
    <header class="creator-head">
      <div>
        <h1>创作者中心</h1>
        <p>
          作品保存后先进入草稿，提交审核通过才会公开。
          <strong>已发布的作品被编辑后会自动重新送审</strong>，这样前台内容始终经过审核。
        </p>
      </div>
      <div class="creator-head__actions">
        <RouterLink to="/works">
          <el-button plain>查看作品广场</el-button>
        </RouterLink>
        <el-button type="primary" @click="openCreate">新建作品</el-button>
      </div>
    </header>

    <div class="creator-toolbar">
      <el-select v-model="filters.status" style="width: 150px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <span class="creator-toolbar__hint">共 {{ total }} 件作品</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column label="作品" min-width="260">
        <template #default="{ row }">
          <div class="creator-cell">
            <div class="creator-cell__cover">
              <img v-if="row.coverUrl" :src="row.coverUrl" :alt="row.title" />
              <span v-else>{{ row.title.charAt(0) }}</span>
            </div>
            <div class="creator-cell__text">
              <strong>{{ row.title }}</strong>
              <p>{{ row.description || '—' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="关联文章" width="120">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.articleTitles?.length"
            :content="row.articleTitles.join('、')"
            placement="top"
          >
            <span>{{ row.articleCount }} 篇</span>
          </el-tooltip>
          <span v-else class="creator-muted">未关联</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" min-width="200">
        <template #default="{ row }">
          <span :class="{ 'creator-muted': !row.reviewComment }">
            {{ row.reviewComment || '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="['DRAFT', 'REJECTED'].includes(row.status)"
            link
            type="success"
            @click="handleSubmitReview(row)"
          >
            提交审核
          </el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            link
            type="info"
            @click="$router.push(`/works/${row.id}`)"
          >
            查看
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="creator-pagination">
      <el-pagination
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="(p) => { filters.page = p; fetchRows() }"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="form.title" maxlength="150" show-word-limit />
        </el-form-item>
        <el-form-item label="作品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择作品分类" clearable style="width: 100%">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="一句话说明这件作品讲什么"
          />
        </el-form-item>
        <el-form-item label="封面" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="图片地址，例如 /images/works/dna-score.svg" />
        </el-form-item>
        <el-form-item label="关联文章" prop="articleIds">
          <el-select
            v-model="form.articleIds"
            multiple
            filterable
            placeholder="选择要关联的已发布科普文章"
            style="width: 100%"
          >
            <el-option
              v-for="article in articleOptions"
              :key="article.id"
              :label="`[${article.categoryName || '未分类'}] ${article.title}`"
              :value="article.id"
            />
          </el-select>
          <p class="form-hint">
            只能关联已发布的科普文章。关联后，文章详情页会展示这件作品。
          </p>
        </el-form-item>
        <el-form-item label="作品内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="作品说明、创作思路或图文描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.creator-page {
  max-width: var(--sp-container-wide);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.creator-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.creator-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.creator-head p {
  margin: 0;
  max-width: 640px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.creator-head__actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.creator-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.creator-toolbar__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.creator-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.creator-cell__cover {
  flex-shrink: 0;
  width: 56px;
  height: 36px;
  border-radius: 6px;
  background: var(--sp-primary-soft);
  overflow: hidden;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.creator-cell__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.creator-cell__text strong {
  display: block;
  font-size: 14px;
  color: var(--sp-text-strong);
  margin-bottom: 2px;
}

.creator-cell__text p {
  margin: 0;
  font-size: 12px;
  color: var(--sp-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

.creator-muted {
  color: var(--sp-text-faint);
}

.creator-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-hint {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--sp-text-faint);
}

@media (max-width: 860px) {
  .creator-head {
    flex-direction: column;
  }
}
</style>
