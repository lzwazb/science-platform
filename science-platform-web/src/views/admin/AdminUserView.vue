<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminUsers, updateUserRoles, updateUserStatus } from '@/api/adminUser'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const filters = reactive({
  page: 1,
  size: 10,
  keyword: '',
  roleCode: '',
  status: '',
})

const roleOptions = [
  { value: '', label: '全部角色' },
  { value: 'USER', label: '普通用户' },
  { value: 'CREATOR', label: '创作者' },
  { value: 'REVIEWER', label: '评审员' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'SUPER_ADMIN', label: '超级管理员' },
]

const roleLabels = {
  USER: '普通用户',
  CREATOR: '创作者',
  REVIEWER: '评审员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'ACTIVE', label: '正常' },
  { value: 'DISABLED', label: '已禁用' },
]

// ---------------- 角色编辑 ----------------

const roleVisible = ref(false)
const roleSubmitting = ref(false)
const currentUser = ref(null)
const roleForm = reactive({
  roleCodes: [],
  reason: '',
})

async function fetchRows() {
  loading.value = true
  try {
    const data = await listAdminUsers({
      page: filters.page,
      size: filters.size,
      keyword: filters.keyword || undefined,
      roleCode: filters.roleCode || undefined,
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

function openRoles(row) {
  currentUser.value = row
  roleForm.roleCodes = [...row.roles]
  roleForm.reason = ''
  roleVisible.value = true
}

async function submitRoles() {
  if (!currentUser.value) return
  if (!roleForm.reason.trim()) {
    ElMessage.warning('请填写调整原因')
    return
  }
  if (roleForm.roleCodes.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }

  roleSubmitting.value = true
  try {
    await updateUserRoles(currentUser.value.id, roleForm.roleCodes, roleForm.reason.trim())
    ElMessage.success('角色已更新，权限立即生效')
    roleVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示（例如「不能移除自己的超级管理员角色」）
  } finally {
    roleSubmitting.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = nextStatus === 'ACTIVE' ? '启用' : '禁用'

  let reason = ''
  try {
    const input = await ElMessageBox.prompt(
      `确定${actionText}账号「${row.username}」吗？禁用后该账号的登录与令牌会立即失效。`,
      `${actionText}账号`,
      {
        confirmButtonText: actionText,
        cancelButtonText: '取消',
        inputPlaceholder: '填写原因（可选，会记入操作日志）',
        inputValue: '',
      },
    )
    reason = input.value || ''
  } catch {
    return
  }

  try {
    await updateUserStatus(row.id, nextStatus, reason)
    ElMessage.success(`已${actionText}`)
    fetchRows()
  } catch {
    // 错误已提示（例如「不能禁用当前登录的账号」）
  }
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

function resetFilters() {
  filters.keyword = ''
  filters.roleCode = ''
  filters.status = ''
  filters.page = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__head">
      <div>
        <h1>用户与角色</h1>
        <p>
          仅超级管理员可访问。角色变更立即生效（每次请求都会重新读取角色），
          变更与启停操作都会记入操作日志。为防锁死，不能移除自己的超管角色、也不能禁用自己。
        </p>
      </div>
    </header>

    <div class="admin-toolbar">
      <el-input
        v-model="filters.keyword"
        placeholder="用户名或昵称"
        clearable
        style="width: 200px"
        @keyup.enter="handleFilterChange"
      />
      <el-select v-model="filters.roleCode" style="width: 140px" @change="handleFilterChange">
        <el-option v-for="opt in roleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filters.status" style="width: 130px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button @click="handleFilterChange">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column label="角色" min-width="220">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role"
            size="small"
            effect="plain"
            class="role-tag"
            :type="role === 'SUPER_ADMIN' ? 'danger' : role === 'ADMIN' ? 'warning' : 'info'"
          >
            {{ roleLabels[role] || role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ row.createdAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoles(row)">分配角色</el-button>
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
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

    <el-dialog v-model="roleVisible" title="分配角色" width="520px" destroy-on-close>
      <template v-if="currentUser">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="账号">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            {{ currentUser.status === 'ACTIVE' ? '正常' : '已禁用' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-width="80px" class="role-form">
          <el-form-item label="角色">
            <el-checkbox-group v-model="roleForm.roleCodes">
              <el-checkbox value="USER">普通用户</el-checkbox>
              <el-checkbox value="CREATOR">创作者</el-checkbox>
              <el-checkbox value="REVIEWER">评审员</el-checkbox>
              <el-checkbox value="ADMIN">管理员</el-checkbox>
              <el-checkbox value="SUPER_ADMIN">超级管理员</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="调整原因">
            <el-input
              v-model="roleForm.reason"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="必填，会写入操作日志"
            />
          </el-form-item>
        </el-form>

        <p class="role-note">
          注意：移除超级管理员角色会让该账号失去账号管理权限；系统禁止移除自己的超管角色以防止锁死。
        </p>
      </template>

      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRoles">保存</el-button>
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
  max-width: 760px;
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

.role-tag {
  margin-right: 6px;
}

.role-form {
  margin-top: 18px;
}

.role-note {
  margin: 8px 0 0;
  padding: 8px 12px;
  background: #fff7e6;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #8c6b2f;
}
</style>
