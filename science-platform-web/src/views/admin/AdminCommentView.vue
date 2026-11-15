<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAdminComment, listAdminComments } from '@/api/interaction'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const filters = reactive({
  page: 1,
  size: 10,
  targetType: '',
  status: '',
  keyword: '',
})

const targetOptions = [
  { value: '', label: '全部目标' },
  { value: 'ARTICLE', label: '文章' },
  { value: 'WORK', label: '作品' },
]

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'VISIBLE', label: '正常' },
  { value: 'HIDDEN', label: '已隐藏' },
  { value: 'DELETED', label: '已删除' },
]

const targetLabels = {
  ARTICLE: '文章',
  WORK: '作品',
}

const statusMeta = {
  VISIBLE: { text: '正常', type: 'success' },
  HIDDEN: { text: '已隐藏', type: 'warning' },
  DELETED: { text: '已删除', type: 'danger' },
}

/** 打开评论所在的内容（新标签，保留后台页面） */
function openTarget(row) {
  const path = row.targetType === 'WORK' ? `/works/${row.targetId}` : `/knowledge/${row.targetId}`
  window.open(path, '_blank', 'noopener')
}

async function fetchRows() {
  loading.value = true
  try {
    const data = await listAdminComments({
      page: filters.page,
      size: filters.size,
      targetType: filters.targetType || undefined,
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
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

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除这条评论吗？删除后楼层结构保留，评论内容不再对外可见。`,
      '删除评论',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteAdminComment(row.id)
    ElMessage.success('评论已删除')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="sp-page-head">
      <div class="sp-page-head__text">
        <h1>评论管理</h1>
        <p>跨内容查看全部评论，可按目标、状态与关键词检索。删除为逻辑删除，记录保留可追溯。</p>
      </div>
      <el-button size="small" @click="fetchRows">刷新</el-button>
    </header>

    <div class="admin-toolbar">
      <el-select v-model="filters.targetType" style="width: 140px" @change="handleFilterChange">
        <el-option v-for="opt in targetOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filters.status" style="width: 140px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索评论内容"
        clearable
        style="width: 240px"
        @keyup.enter="handleFilterChange"
        @clear="handleFilterChange"
      />
      <el-button type="primary" plain @click="handleFilterChange">搜索</el-button>
      <span class="admin-toolbar__hint">共 {{ total }} 条</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column label="目标" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag size="small" effect="plain" class="comment-target__type">
            {{ targetLabels[row.targetType] || row.targetType }}
          </el-tag>
          <el-link type="primary" :underline="false" @click="openTarget(row)">
            {{ row.targetTitle }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="评论内容" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="row.parentId" size="small" type="info" effect="plain" class="comment-reply-tag">
            回复
          </el-tag>
          <span>{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="评论者" width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评论时间" width="170">
        <template #default="{ row }">{{ row.createdAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            :disabled="row.status === 'DELETED'"
            @click="handleDelete(row)"
          >
            删除
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
  </div>
</template>

<style scoped>
.comment-target__type {
  margin-right: 6px;
}

.comment-reply-tag {
  margin-right: 6px;
}
</style>
