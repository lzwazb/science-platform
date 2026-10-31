<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { STATUS_META } from '@/constants/dict'
import {
  approveWork,
  getReviewWork,
  listReviewWorks,
  offlineWork,
  rejectWork,
} from '@/api/work'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const filters = reactive({
  page: 1,
  size: 10,
  status: 'PENDING',
})

const statusOptions = [
  { value: 'PENDING', label: '待审核' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'OFFLINE', label: '已下架' },
  { value: '', label: '全部' },
]

const statusMeta = STATUS_META

// 详情抽屉
const drawerVisible = ref(false)
const detailLoading = ref(false)
const current = ref(null)
const comment = ref('')
const acting = ref(false)

async function fetchRows() {
  loading.value = true
  try {
    const data = await listReviewWorks({
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

async function openDetail(row) {
  drawerVisible.value = true
  detailLoading.value = true
  current.value = null
  comment.value = ''
  try {
    current.value = await getReviewWork(row.id)
  } catch {
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove() {
  if (!current.value) return
  acting.value = true
  try {
    await approveWork(current.value.id, comment.value.trim())
    ElMessage.success('已通过审核')
    drawerVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

async function handleReject() {
  if (!current.value) return
  if (!comment.value.trim()) {
    ElMessage.warning('驳回时必须填写审核意见')
    return
  }
  acting.value = true
  try {
    await rejectWork(current.value.id, comment.value.trim())
    ElMessage.success('已驳回')
    drawerVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

async function handleOffline() {
  if (!current.value) return
  acting.value = true
  try {
    await offlineWork(current.value.id, comment.value.trim())
    ElMessage.success('已下架')
    drawerVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

function handleFilterChange() {
  filters.page = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="review-page">
    <header class="review-head">
      <div>
        <h1>作品审核</h1>
        <p>
          审核通过后作品才会出现在前台。驳回必须填写意见，作者会在「创作者中心」看到并据此修改。
          只有<strong>待审核</strong>状态的作品可以处理。
        </p>
      </div>
      <el-tag effect="plain" type="warning">评审工作台</el-tag>
    </header>

    <div class="review-toolbar">
      <el-select v-model="filters.status" style="width: 150px" @change="handleFilterChange">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <span class="review-toolbar__hint">共 {{ total }} 件</span>
      <el-button size="small" @click="fetchRows">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="64" />
      <el-table-column prop="title" label="作品标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="creatorName" label="创作者" width="120" />
      <el-table-column prop="categoryName" label="分类" width="110">
        <template #default="{ row }">{{ row.categoryName || '未分类' }}</template>
      </el-table-column>
      <el-table-column label="关联文章" width="100">
        <template #default="{ row }">{{ row.articleCount || 0 }} 篇</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
            {{ statusMeta[row.status]?.text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ row.createdAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="review-pagination">
      <el-pagination
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        layout="total, prev, pager, next"
        background
        @current-change="(p) => { filters.page = p; fetchRows() }"
      />
    </div>

    <el-drawer v-model="drawerVisible" title="作品审核" size="620px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="current">
          <h2 class="review-detail__title">{{ current.title }}</h2>
          <div class="review-detail__meta">
            <el-tag size="small" :type="statusMeta[current.status]?.type || 'info'">
              {{ statusMeta[current.status]?.text || current.status }}
            </el-tag>
            <span>创作者：{{ current.creatorName }}</span>
            <span>分类：{{ current.categoryName || '未分类' }}</span>
          </div>

          <p v-if="current.description" class="review-detail__desc">{{ current.description }}</p>

          <div v-if="current.coverUrl" class="review-detail__cover">
            <img :src="current.coverUrl" :alt="current.title" />
          </div>

          <section class="review-detail__section">
            <h3>作品内容</h3>
            <p class="review-detail__content">{{ current.content }}</p>
          </section>

          <section class="review-detail__section">
            <h3>关联科普文章</h3>
            <el-empty
              v-if="!current.articles?.length"
              description="未关联文章"
              :image-size="60"
            />
            <ul v-else class="review-detail__articles">
              <li v-for="article in current.articles" :key="article.id">
                <RouterLink :to="`/knowledge/${article.id}`" target="_blank">
                  {{ article.title }}
                </RouterLink>
                <span class="review-detail__article-cat">{{ article.categoryName }}</span>
              </li>
            </ul>
          </section>

          <section class="review-detail__section">
            <h3>审核意见</h3>
            <el-input
              v-model="comment"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="通过可不填；驳回必须填写具体原因，作者会看到"
            />
            <p v-if="current.reviewComment" class="review-detail__history">
              上一条审核意见：{{ current.reviewComment }}
            </p>
          </section>
        </template>
      </div>

      <template #footer>
        <div class="review-drawer-footer">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <template v-if="current">
            <template v-if="current.status === 'PENDING'">
              <el-button type="danger" plain :loading="acting" @click="handleReject">驳回</el-button>
              <el-button type="success" :loading="acting" @click="handleApprove">通过</el-button>
            </template>
            <el-button
              v-else-if="current.status === 'PUBLISHED'"
              type="warning"
              :loading="acting"
              @click="handleOffline"
            >
              下架
            </el-button>
          </template>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.review-page {
  max-width: var(--sp-container-wide);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.review-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.review-head p {
  margin: 0;
  max-width: 680px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.review-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.review-toolbar__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.review-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.review-detail__title {
  margin: 0 0 10px;
  font-size: 20px;
  line-height: 1.5;
  color: var(--sp-text-strong);
}

.review-detail__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--sp-text-muted);
}

.review-detail__desc {
  margin: 14px 0 0;
  padding: 10px 14px;
  background: var(--sp-primary-soft);
  border-left: 3px solid var(--sp-primary);
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text);
}

.review-detail__cover {
  margin-top: 16px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--sp-border);
}

.review-detail__cover img {
  width: 100%;
  display: block;
}

.review-detail__section {
  margin-top: 22px;
}

.review-detail__section h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--sp-text-strong);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--sp-border);
}

.review-detail__content {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: var(--sp-text);
  white-space: pre-wrap;
}

.review-detail__articles {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 2;
}

.review-detail__articles a {
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.review-detail__articles a:hover {
  text-decoration: underline;
}

.review-detail__article-cat {
  margin-left: 8px;
  font-size: 11px;
  color: var(--sp-text-faint);
}

.review-detail__history {
  margin: 10px 0 0;
  padding: 8px 12px;
  background: #fff7e6;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #8c6b2f;
}

.review-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
