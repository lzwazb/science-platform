<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTopic,
  deleteTopic,
  getAdminTopic,
  listAdminTopics,
  updateTopic,
  updateTopicArticles,
} from '@/api/topic'
import { listAdminArticles } from '@/api/article'

const loading = ref(false)
const rows = ref([])

const statusMeta = {
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFFLINE: { text: '已下架', type: 'warning' },
}

// ---------------- 专题表单 ----------------

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  title: '',
  description: '',
  coverUrl: '',
  sortOrder: 0,
  status: 'DRAFT',
})

const rules = {
  title: [
    { required: true, message: '请输入专题标题', trigger: 'blur' },
    { max: 100, message: '专题标题不能超过 100 个字符', trigger: 'blur' },
  ],
}

// ---------------- 文章编排 ----------------

const articlesVisible = ref(false)
const articlesSubmitting = ref(false)
const articleOptions = ref([])
const selectedArticleIds = ref([])
const currentTopic = ref(null)

async function fetchRows() {
  loading.value = true
  try {
    rows.value = await listAdminTopics()
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
  form.coverUrl = ''
  form.sortOrder = 0
  form.status = 'DRAFT'
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.title = row.title
  form.description = row.description || ''
  form.coverUrl = row.coverUrl || ''
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status || 'DRAFT'
  dialogVisible.value = true
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
      coverUrl: form.coverUrl.trim(),
      sortOrder: form.sortOrder,
      status: form.status,
    }
    if (dialogMode.value === 'create') {
      await createTopic(payload)
      ElMessage.success('专题已创建')
    } else {
      await updateTopic(editingId.value, payload)
      ElMessage.success('专题已更新')
    }
    dialogVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除专题「${row.title}」吗？专题与文章的关联会一并清除，文章本身不受影响。`,
      '删除专题',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteTopic(row.id)
    ElMessage.success('专题已删除')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function openArticles(row) {
  currentTopic.value = row
  articlesVisible.value = true
  articlesSubmitting.value = false
  selectedArticleIds.value = []
  articleOptions.value = []

  try {
    // 待选文章列表：一次取足够多的已发布文章，管理规模下够用
    const data = await listAdminArticles({ page: 1, size: 100, status: 'PUBLISHED' })
    articleOptions.value = data.records || []

    const detail = await getAdminTopic(row.id)
    selectedArticleIds.value = (detail.articles || []).map((a) => a.id)
  } catch {
    // 错误已提示
  }
}

async function handleSaveArticles() {
  if (!currentTopic.value) return
  articlesSubmitting.value = true
  try {
    await updateTopicArticles(currentTopic.value.id, selectedArticleIds.value)
    ElMessage.success('专题文章已更新')
    articlesVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    articlesSubmitting.value = false
  }
}

const selectedCount = computed(() => selectedArticleIds.value.length)

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__head">
      <div>
        <h1>专题管理</h1>
        <p>专题把文章按主题组织，顺序即前台阅读顺序。只有「已发布」状态的专题对前台可见。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建专题</el-button>
    </header>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="title" label="专题标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="description" label="简介" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '—' }}</template>
      </el-table-column>
      <el-table-column prop="articleCount" label="文章数" width="90" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="openArticles(row)">编排文章</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建专题' : '编辑专题'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="封面" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="图片地址，例如 /images/topics/stars.svg" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="PUBLISHED">已发布</el-radio>
            <el-radio value="OFFLINE">已下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="articlesVisible"
      :title="`编排文章 · ${currentTopic?.title || ''}`"
      width="640px"
      destroy-on-close
    >
      <p class="articles-hint">
        选择要放进本专题的已发布文章，列表顺序即专题内阅读顺序（当前已选 {{ selectedCount }} 篇）。
      </p>
      <el-select
        v-model="selectedArticleIds"
        multiple
        filterable
        placeholder="选择文章"
        style="width: 100%"
      >
        <el-option
          v-for="article in articleOptions"
          :key="article.id"
          :label="`[${article.categoryName || '未分类'}] ${article.title}`"
          :value="article.id"
        />
      </el-select>

      <template #footer>
        <el-button @click="articlesVisible = false">取消</el-button>
        <el-button type="primary" :loading="articlesSubmitting" @click="handleSaveArticles">
          保存编排
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
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

.articles-hint {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}
</style>
