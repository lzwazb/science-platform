<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCategory, deleteCategory, listAdminCategories, updateCategory } from '@/api/category'

const loading = ref(false)
const rows = ref([])
const filterType = ref('')

const typeOptions = [
  { value: '', label: '全部类型' },
  { value: 'ARTICLE', label: '文章分类' },
  { value: 'WORK', label: '作品分类' },
]

const typeLabels = { ARTICLE: '文章分类', WORK: '作品分类' }

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  name: '',
  type: 'ARTICLE',
  description: '',
  sortOrder: 0,
  status: 'ENABLED',
})

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '分类名称不能超过 50 个字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }],
}

async function fetchRows() {
  loading.value = true
  try {
    rows.value = await listAdminCategories({ type: filterType.value || undefined })
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  form.name = ''
  form.type = filterType.value || 'ARTICLE'
  form.description = ''
  form.sortOrder = 0
  form.status = 'ENABLED'
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.description = row.description || ''
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status || 'ENABLED'
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      type: form.type,
      description: form.description.trim(),
      sortOrder: form.sortOrder,
      status: form.status,
    }
    if (dialogMode.value === 'create') {
      await createCategory(payload)
      ElMessage.success('分类已创建')
    } else {
      await updateCategory(editingId.value, payload)
      ElMessage.success('分类已更新')
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
      `确定删除分类「${row.name}」吗？该分类下若仍有文章，删除会被拒绝，请先移动文章或改为停用。`,
      '删除分类',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    fetchRows()
  } catch {
    // 错误已提示，例如「该分类下还有 N 篇文章」
  }
}

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="sp-page-head">
      <div class="sp-page-head__text">
        <h1>分类管理</h1>
        <p>文章与作品共用一张分类表，通过类型区分。分类下仍有文章时不允许删除，建议改为停用。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建分类</el-button>
    </header>

    <div class="admin-toolbar">
      <el-select v-model="filterType" style="width: 160px" @change="fetchRows">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="name" label="分类名称" min-width="160" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeLabels[row.type] || row.type }}</template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '—' }}</template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建分类' : '编辑分类'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="ARTICLE">文章分类</el-radio>
            <el-radio value="WORK">作品分类</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
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
.admin-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
