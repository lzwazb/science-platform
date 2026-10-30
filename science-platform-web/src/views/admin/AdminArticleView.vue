<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createArticle,
  deleteArticle,
  getAdminArticle,
  listAdminArticles,
  offlineArticle,
  publishArticle,
  updateArticle,
} from '@/api/article'
import { listCategories } from '@/api/category'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ref([])

const filters = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: '',
})

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'OFFLINE', label: '已下架' },
]

const statusMeta = {
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFFLINE: { text: '已下架', type: 'warning' },
}

// ---------------- 表单 ----------------

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  coverUrl: '',
  categoryId: null,
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 150, message: '标题不能超过 150 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
  summary: [{ max: 500, message: '摘要不能超过 500 个字符', trigger: 'blur' }],
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建文章' : '编辑文章'))

async function fetchRows() {
  loading.value = true
  try {
    const data = await listAdminArticles({
      page: filters.page,
      size: filters.size,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
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

async function fetchCategories() {
  try {
    categories.value = await listCategories({ type: 'ARTICLE' })
  } catch {
    categories.value = []
  }
}

function resetForm() {
  form.title = ''
  form.summary = ''
  form.content = ''
  form.coverUrl = ''
  form.categoryId = null
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
    const detail = await getAdminArticle(row.id)
    editingId.value = detail.id
    form.title = detail.title
    form.summary = detail.summary || ''
    form.content = detail.content
    form.coverUrl = detail.coverUrl || ''
    form.categoryId = detail.categoryId
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
      summary: form.summary.trim(),
      content: form.content,
      coverUrl: form.coverUrl.trim(),
      categoryId: form.categoryId,
    }
    if (dialogMode.value === 'create') {
      await createArticle(payload)
      ElMessage.success('已保存为草稿，可在列表中发布')
    } else {
      await updateArticle(editingId.value, payload)
      ElMessage.success('文章已更新')
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
    await publishArticle(row.id)
    ElMessage.success('文章已发布')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm(`确定下架《${row.title}》吗？下架后前台立即不可见。`, '下架文章', {
      confirmButtonText: '下架',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await offlineArticle(row.id)
    ElMessage.success('文章已下架')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除《${row.title}》吗？删除后列表与前台都不再显示，历史数据仍保留在库中。`,
      '删除文章',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteArticle(row.id)
    ElMessage.success('文章已删除')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

function handleSearch() {
  filters.page = 1
  fetchRows()
}

function handleReset() {
  filters.status = ''
  filters.keyword = ''
  filters.page = 1
  fetchRows()
}

function handlePageChange(p) {
  filters.page = p
  fetchRows()
}

onMounted(() => {
  fetchCategories()
  fetchRows()
})
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__head">
      <div>
        <h1>文章管理</h1>
        <p>新建的文章先保存为草稿，发布后才对前台可见。删除为逻辑删除，历史数据仍保留。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建文章</el-button>
    </header>

    <div class="admin-toolbar">
      <el-select v-model="filters.status" placeholder="状态" style="width: 140px" @change="handleSearch">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索标题或摘要"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      />
      <el-button @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="110">
        <template #default="{ row }">{{ row.categoryName || '未分类' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="versionNo" label="版本" width="72" />
      <el-table-column prop="authorName" label="发布者" width="110" />
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ row.publishedAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status !== 'PUBLISHED'"
            link
            type="success"
            @click="handlePublish(row)"
          >
            发布
          </el-button>
          <el-button v-else link type="warning" @click="handleOffline(row)">下架</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="admin-pagination">
      <el-pagination
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="150" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择文章分类" clearable style="width: 100%">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="列表页展示，建议 60–120 字"
          />
        </el-form-item>
        <el-form-item label="封面" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="图片地址，例如 /images/articles/dna.svg" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="纯文本，段落之间用空行分隔"
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
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
