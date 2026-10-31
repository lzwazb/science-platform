<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminWork, listAdminWorks, offlineWork } from '@/api/adminWork'
import { STATUS_META } from '@/constants/dict'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const filters = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: '',
})

const statusMeta = STATUS_META

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING', label: '待审核' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'OFFLINE', label: '已下架' },
]

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const offlineVisible = ref(false)
const offlineSubmitting = ref(false)
const offlineTarget = ref(null)
const offlineComment = ref('')

async function fetchRows() {
  loading.value = true
  try {
    const data = await listAdminWorks({
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

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getAdminWork(row.id)
  } catch {
    // 错误已提示
  } finally {
    detailLoading.value = false
  }
}

function openOffline(row) {
  offlineTarget.value = row
  offlineComment.value = ''
  offlineVisible.value = true
}

async function submitOffline() {
  if (!offlineTarget.value) return
  if (!offlineComment.value.trim()) {
    ElMessage.warning('请填写下架原因，作者会收到这条说明')
    return
  }

  offlineSubmitting.value = true
  try {
    await offlineWork(offlineTarget.value.id, offlineComment.value.trim())
    ElMessage.success('作品已下架，已通知作者')
    offlineVisible.value = false
    detailVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    offlineSubmitting.value = false
  }
}

async function handleDeleteConfirm() {
  // 作品不提供物理删除，仅提示
  await ElMessageBox.alert(
    '作品不提供删除操作：已发布作品请使用「下架」，作者的历史记录会完整保留。',
    '说明',
    { confirmButtonText: '知道了' },
  ).catch(() => {})
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
  filters.page = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="admin-page">
    <header class="sp-page-head">
      <div class="sp-page-head__text">
        <h1>作品管理</h1>
        <p>
          跨状态查看全部作品，可查看详情与下架违规内容。审核（待审核 → 通过 / 驳回）在
          <RouterLink to="/review/works">作品审核工作台</RouterLink>
          中进行，两者共用同一套状态流转逻辑。
        </p>
      </div>
    </header>

    <div class="admin-toolbar">
      <el-input
        v-model="filters.keyword"
        placeholder="作品标题关键词"
        clearable
        style="width: 220px"
        @keyup.enter="handleFilterChange"
      />
      <el-select v-model="filters.status" style="width: 140px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button @click="handleFilterChange">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <el-button link type="info" @click="handleDeleteConfirm">为什么不提供删除？</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="creatorName" label="作者" width="110" />
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createdAt }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            link
            type="danger"
            @click="openOffline(row)"
          >
            下架
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

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="作品详情" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ detail.creatorName }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="statusMeta[detail.status]?.type || 'info'">
                {{ statusMeta[detail.status]?.text || detail.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="浏览 / 点赞 / 收藏">
              {{ detail.viewCount }} / {{ detail.likeCount }} / {{ detail.favoriteCount }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="作品说明" :span="2">
              {{ detail.description || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="审核意见" :span="2">
              {{ detail.reviewComment || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="关联文章" :span="2">
              <template v-if="detail.articles?.length">
                <el-tag
                  v-for="article in detail.articles"
                  :key="article.id"
                  size="small"
                  effect="plain"
                  class="article-tag"
                >
                  {{ article.title }}
                </el-tag>
              </template>
              <span v-else>未关联</span>
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && detail.status === 'PUBLISHED'"
          type="danger"
          @click="openOffline(detail)"
        >
          下架此作品
        </el-button>
      </template>
    </el-dialog>

    <!-- 下架 -->
    <el-dialog v-model="offlineVisible" title="下架作品" width="520px" destroy-on-close>
      <p class="offline-hint">
        下架后作品立即从前台消失，但数据与历史记录保留。作者会收到一条包含原因说明的站内通知。
      </p>
      <el-form label-width="80px">
        <el-form-item label="作品">
          {{ offlineTarget?.title }}
        </el-form-item>
        <el-form-item label="下架原因">
          <el-input
            v-model="offlineComment"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="必填，这段说明会发送给作者"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="offlineVisible = false">取消</el-button>
        <el-button type="danger" :loading="offlineSubmitting" @click="submitOffline">
          确认下架
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sp-page-head a {
  color: var(--sp-primary-dark);
}

.admin-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.article-tag {
  margin: 0 6px 6px 0;
}

.offline-hint {
  margin: 0 0 16px;
  padding: 10px 14px;
  background: #fef0f0;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #a33;
}
</style>
