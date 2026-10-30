<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyContributions, withdrawContribution } from '@/api/contribution'

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
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'WITHDRAWN', label: '已撤回' },
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

const detailVisible = ref(false)
const current = ref(null)

async function fetchRows() {
  loading.value = true
  try {
    const data = await listMyContributions({
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

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function handleWithdraw(row) {
  try {
    await ElMessageBox.confirm(
      '撤回后这条建议会被标记为「已撤回」，不再进入审核。确定撤回吗？',
      '撤回建议',
      { confirmButtonText: '撤回', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await withdrawContribution(row.id)
    ElMessage.success('已撤回')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="mine-page">
    <header class="mine-head">
      <div>
        <h1>我的知识贡献</h1>
        <p>
          你提交的补充、纠错与修改建议都会进入审核流程。通过后正式文章会更新，并保存历史版本；
          驳回时会附上评审意见，方便你调整后重新提交。
        </p>
      </div>
      <RouterLink to="/knowledge">
        <el-button plain>去知识库找问题</el-button>
      </RouterLink>
    </header>

    <div class="mine-toolbar">
      <el-select v-model="filters.status" style="width: 150px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <span class="mine-toolbar__hint">共 {{ total }} 条</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column label="目标文章" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <RouterLink :to="`/knowledge/${row.articleId}`" class="mine-link">
            {{ row.articleTitle }}
          </RouterLink>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ typeLabels[row.contributionType] || row.contributionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="提交理由" min-width="200" show-overflow-tooltip />
      <el-table-column label="审核意见" min-width="200">
        <template #default="{ row }">
          <span :class="{ 'mine-muted': !row.reviewComment }">{{ row.reviewComment || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ row.createdAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 'PENDING'"
            link
            type="danger"
            @click="handleWithdraw(row)"
          >
            撤回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="mine-pagination">
      <el-pagination
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="(p) => { filters.page = p; fetchRows() }"
      />
    </div>

    <el-drawer v-model="detailVisible" title="贡献详情" size="520px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="目标文章">{{ current.articleTitle }}</el-descriptions-item>
          <el-descriptions-item label="建议类型">
            {{ typeLabels[current.contributionType] || current.contributionType }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusMeta[current.status]?.type || 'info'">
              {{ statusMeta[current.status]?.text || current.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ current.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="current.reviewedAt" label="处理时间">
            {{ current.reviewedAt }}
          </el-descriptions-item>
        </el-descriptions>

        <section class="mine-detail__section">
          <h3>提交理由</h3>
          <p>{{ current.reason }}</p>
        </section>

        <section v-if="current.reviewComment" class="mine-detail__section">
          <h3>审核意见</h3>
          <p class="mine-detail__comment">{{ current.reviewComment }}</p>
        </section>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.mine-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px 24px 52px;
}

.mine-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.mine-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.mine-head p {
  margin: 0;
  max-width: 680px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.mine-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.mine-toolbar__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.mine-link {
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.mine-link:hover {
  text-decoration: underline;
}

.mine-muted {
  color: var(--sp-text-faint);
}

.mine-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.mine-detail__section {
  margin-top: 20px;
}

.mine-detail__section h3 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--sp-text-strong);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--sp-border);
}

.mine-detail__section p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text);
}

.mine-detail__comment {
  padding: 10px 14px;
  background: #fff7e6;
  border-radius: 8px;
  color: #8c6b2f !important;
}
</style>
