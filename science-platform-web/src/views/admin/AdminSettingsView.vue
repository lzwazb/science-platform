<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAnnouncement,
  deleteAnnouncement,
  getSiteSettings,
  listAdminAnnouncements,
  offlineAnnouncement,
  publishAnnouncement,
  updateAnnouncement,
  updateSiteSettings,
} from '@/api/system'

const activeTab = ref('site')

// ---------------- 平台基础信息 ----------------

const settingsLoading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  name: '',
  subtitle: '',
  description: '',
  banner: '',
  contact: '',
})

const rules = {
  name: [
    { required: true, message: '平台名称不能为空', trigger: 'blur' },
    { max: 60, message: '平台名称不能超过 60 个字符', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '平台简介不能为空', trigger: 'blur' },
    { max: 500, message: '平台简介不能超过 500 个字符', trigger: 'blur' },
  ],
  subtitle: [{ max: 120, message: '副标题不能超过 120 个字符', trigger: 'blur' }],
  banner: [{ max: 120, message: '横幅文案不能超过 120 个字符', trigger: 'blur' }],
  contact: [{ max: 100, message: '联系邮箱不能超过 100 个字符', trigger: 'blur' }],
}

async function fetchSettings() {
  settingsLoading.value = true
  try {
    const data = await getSiteSettings()
    form.name = data.name || ''
    form.subtitle = data.subtitle || ''
    form.description = data.description || ''
    form.banner = data.banner || ''
    form.contact = data.contact || ''
  } catch {
    // 错误已提示
  } finally {
    settingsLoading.value = false
  }
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateSiteSettings({
      name: form.name.trim(),
      subtitle: form.subtitle.trim(),
      description: form.description.trim(),
      banner: form.banner.trim(),
      contact: form.contact.trim(),
    })
    ElMessage.success('平台设置已保存，前台立即生效')
  } catch {
    // 错误已提示
  } finally {
    saving.value = false
  }
}

// ---------------- 公告 ----------------

const announcements = ref([])
const announcementLoading = ref(false)
const statusFilter = ref('')

const statusMeta = {
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFFLINE: { text: '已下架', type: 'warning' },
}

const dialogVisible = ref(false)
const dialogMode = ref('create')
const dialogSubmitting = ref(false)
const editingId = ref(null)
const announcementFormRef = ref(null)
const announcementForm = reactive({
  title: '',
  content: '',
  sortOrder: 0,
})

const announcementRules = {
  title: [
    { required: true, message: '公告标题不能为空', trigger: 'blur' },
    { max: 150, message: '标题不能超过 150 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '公告正文不能为空', trigger: 'blur' }],
}

async function fetchAnnouncements() {
  announcementLoading.value = true
  try {
    announcements.value = await listAdminAnnouncements(statusFilter.value || undefined)
  } catch {
    announcements.value = []
  } finally {
    announcementLoading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.sortOrder = 0
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  announcementForm.title = row.title
  announcementForm.content = row.content
  announcementForm.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

async function submitAnnouncement() {
  if (!announcementFormRef.value) return
  const valid = await announcementFormRef.value.validate().catch(() => false)
  if (!valid) return

  dialogSubmitting.value = true
  try {
    const payload = {
      title: announcementForm.title.trim(),
      content: announcementForm.content,
      sortOrder: announcementForm.sortOrder,
      status: 'DRAFT',
    }
    if (dialogMode.value === 'create') {
      await createAnnouncement(payload)
      ElMessage.success('公告已保存为草稿，发布后前台可见')
    } else {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('公告已更新')
    }
    dialogVisible.value = false
    fetchAnnouncements()
  } catch {
    // 错误已提示
  } finally {
    dialogSubmitting.value = false
  }
}

async function handlePublish(row) {
  try {
    await publishAnnouncement(row.id)
    ElMessage.success('公告已发布')
    fetchAnnouncements()
  } catch {
    // 错误已提示
  }
}

async function handleOffline(row) {
  try {
    await ElMessageBox.confirm(`下架「${row.title}」后前台立即不可见，确定吗？`, '下架公告', {
      confirmButtonText: '下架',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await offlineAnnouncement(row.id)
    ElMessage.success('公告已下架')
    fetchAnnouncements()
  } catch {
    // 错误已提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除公告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteAnnouncement(row.id)
    ElMessage.success('公告已删除')
    fetchAnnouncements()
  } catch {
    // 错误已提示
  }
}

function handleTabChange(tab) {
  if (tab === 'announcement' && announcements.value.length === 0) {
    fetchAnnouncements()
  }
}

onMounted(() => {
  fetchSettings()
  fetchAnnouncements()
})
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__head">
      <div>
        <h1>系统设置</h1>
        <p>
          仅超级管理员可访问。平台名称、简介与横幅会即时反映到首页；
          公告需「发布」后前台才可见，下架后立即消失但数据保留。
        </p>
      </div>
      <el-tag type="danger" effect="plain">超级管理员专属</el-tag>
    </header>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 平台基础信息 -->
      <el-tab-pane label="平台信息" name="site">
        <div v-loading="settingsLoading" class="settings-card">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
            <el-form-item label="平台名称" prop="name">
              <el-input v-model="form.name" maxlength="60" show-word-limit />
            </el-form-item>
            <el-form-item label="副标题" prop="subtitle">
              <el-input v-model="form.subtitle" maxlength="120" show-word-limit />
            </el-form-item>
            <el-form-item label="平台简介" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="首页横幅" prop="banner">
              <el-input v-model="form.banner" maxlength="120" show-word-limit />
            </el-form-item>
            <el-form-item label="联系邮箱" prop="contact">
              <el-input v-model="form.contact" maxlength="100" placeholder="可选，会展示在页脚" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
              <el-button @click="fetchSettings">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 公告管理 -->
      <el-tab-pane label="系统公告" name="announcement">
        <div class="announcement-toolbar">
          <el-select
            v-model="statusFilter"
            placeholder="全部状态"
            clearable
            style="width: 150px"
            @change="fetchAnnouncements"
          >
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
          <el-button @click="fetchAnnouncements">刷新</el-button>
          <el-button type="primary" @click="openCreate">新建公告</el-button>
        </div>

        <el-table v-loading="announcementLoading" :data="announcements" border stripe>
          <el-table-column prop="id" label="ID" width="64" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
                {{ row.statusLabel || statusMeta[row.status]?.text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column prop="publisherName" label="创建人" width="110" />
          <el-table-column label="发布时间" width="170">
            <template #default="{ row }">
              {{ row.publishedAt || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
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
              <el-button
                v-else
                link
                type="warning"
                @click="handleOffline(row)"
              >
                下架
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建公告' : '编辑公告'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="announcementFormRef" :model="announcementForm" :rules="announcementRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="announcementForm.title" maxlength="150" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="8"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="announcementForm.sortOrder" :min="0" :max="9999" />
          <span class="dialog-hint">数值越大越靠前</span>
        </el-form-item>
      </el-form>

      <p class="dialog-note">
        保存后默认为草稿状态，需要在列表中点「发布」才会出现在前台公告栏。
      </p>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="submitAnnouncement">
          保存
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

.settings-card {
  max-width: 640px;
  padding: 22px 24px 6px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
}

.announcement-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.announcement-toolbar .el-button:last-child {
  margin-left: auto;
}

.dialog-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--sp-text-faint);
}

.dialog-note {
  margin: 4px 0 0;
  padding: 8px 12px;
  background: #f4f9ff;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}
</style>
