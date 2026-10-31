<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArticles } from '@/api/article'
import { listCategories } from '@/api/category'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const articles = ref([])
const categories = ref([])
const total = ref(0)

const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const activeCategoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)
const page = ref(route.query.page ? Number(route.query.page) : 1)
const size = ref(9)

/** 搜索框输入与已提交的搜索词分离，避免输入过程中反复请求 */
const searchInput = ref(keyword.value)

const activeCategoryName = computed(() => {
  if (!activeCategoryId.value) return '全部'
  return categories.value.find((c) => c.id === activeCategoryId.value)?.name || '全部'
})

async function fetchArticles() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listArticles({
      page: page.value,
      size: size.value,
      categoryId: activeCategoryId.value || undefined,
      keyword: keyword.value || undefined,
    })
    articles.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    loadError.value = error?.response?.data?.message || '加载失败，请稍后重试'
    articles.value = []
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

/** 把当前筛选条件写进地址栏，保证刷新与分享后状态不丢 */
function syncQuery() {
  const query = {}
  if (keyword.value) query.keyword = keyword.value
  if (activeCategoryId.value) query.categoryId = String(activeCategoryId.value)
  if (page.value > 1) query.page = String(page.value)
  router.replace({ path: '/knowledge', query })
}

function applyFilters() {
  keyword.value = searchInput.value.trim()
  page.value = 1
  syncQuery()
  fetchArticles()
}

function selectCategory(id) {
  activeCategoryId.value = activeCategoryId.value === id ? null : id
  page.value = 1
  syncQuery()
  fetchArticles()
}

function resetFilters() {
  keyword.value = ''
  searchInput.value = ''
  activeCategoryId.value = null
  page.value = 1
  syncQuery()
  fetchArticles()
}

function handlePageChange(newPage) {
  page.value = newPage
  syncQuery()
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function coverOf(article) {
  return article.coverUrl || ''
}

watch(
  () => route.query,
  () => {
    // 浏览器前进/后退时同步状态
    const q = route.query
    const nextKeyword = typeof q.keyword === 'string' ? q.keyword : ''
    const nextCategory = q.categoryId ? Number(q.categoryId) : null
    const nextPage = q.page ? Number(q.page) : 1
    if (nextKeyword !== keyword.value || nextCategory !== activeCategoryId.value || nextPage !== page.value) {
      keyword.value = nextKeyword
      searchInput.value = nextKeyword
      activeCategoryId.value = nextCategory
      page.value = nextPage
      fetchArticles()
    }
  },
)

onMounted(() => {
  fetchCategories()
  fetchArticles()
})
</script>

<template>
  <div class="kb-page">
    <header class="kb-hero">
      <h1>科普知识库</h1>
      <p>经过核对的科普文章，按主题分类与专题组织；发现错误可以提交纠错建议。</p>

      <div class="kb-search">
        <el-input
          v-model="searchInput"
          placeholder="搜索标题或摘要，例如：DNA、碳中和"
          size="large"
          clearable
          @keyup.enter="applyFilters"
        >
          <template #append>
            <el-button type="primary" @click="applyFilters">搜索</el-button>
          </template>
        </el-input>
      </div>
    </header>

    <section class="kb-filters">
      <span class="kb-filters__label">分类</span>
      <div class="kb-filters__tags">
        <el-tag
          :type="activeCategoryId === null ? 'primary' : 'info'"
          :effect="activeCategoryId === null ? 'dark' : 'plain'"
          class="kb-filters__tag"
          @click="selectCategory(null)"
        >
          全部
        </el-tag>
        <el-tag
          v-for="category in categories"
          :key="category.id"
          :type="activeCategoryId === category.id ? 'primary' : 'info'"
          :effect="activeCategoryId === category.id ? 'dark' : 'plain'"
          class="kb-filters__tag"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
        </el-tag>
      </div>
    </section>

    <section class="kb-result-bar">
      <span>
        共 <strong>{{ total }}</strong> 篇
        <template v-if="keyword">· 关键词「{{ keyword }}」</template>
        · {{ activeCategoryName }}
      </span>
      <el-button v-if="keyword || activeCategoryId" link type="primary" @click="resetFilters">
        清空筛选
      </el-button>
    </section>

    <div v-loading="loading">
      <el-alert
        v-if="loadError && !loading"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
        class="kb-alert"
      />

      <el-empty
        v-else-if="!loading && articles.length === 0"
        description="没有找到符合条件的文章，换个关键词试试"
      />

      <div v-else class="kb-grid">
        <RouterLink
          v-for="article in articles"
          :key="article.id"
          :to="`/knowledge/${article.id}`"
          class="kb-card"
        >
          <div class="kb-card__cover">
            <img v-if="coverOf(article)" :src="coverOf(article)" :alt="article.title" loading="lazy" />
            <div v-else class="kb-card__cover-fallback">{{ article.title.charAt(0) }}</div>
          </div>
          <div class="kb-card__body">
            <div class="kb-card__meta">
              <el-tag size="small" effect="plain" type="primary">{{ article.categoryName || '未分类' }}</el-tag>
              <span class="kb-card__date">{{ (article.publishedAt || '').slice(0, 10) }}</span>
            </div>
            <h3 class="kb-card__title">{{ article.title }}</h3>
            <p class="kb-card__summary">{{ article.summary }}</p>
            <div class="kb-card__footer">
              <span>{{ article.authorName }}</span>
              <span>阅读 {{ article.viewCount }}</span>
            </div>
          </div>
        </RouterLink>
      </div>
    </div>

    <div v-if="!loading && total > size" class="kb-pagination">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.kb-page {
  max-width: var(--sp-container);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.kb-hero {
  padding: 30px 32px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 84% 20%, var(--sp-primary-a16), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 64%);
}

.kb-hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--sp-text-strong);
}

.kb-hero p {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.kb-search {
  max-width: 520px;
}

.kb-filters {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-top: 22px;
}

.kb-filters__label {
  flex-shrink: 0;
  padding-top: 4px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.kb-filters__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.kb-filters__tag {
  cursor: pointer;
  transition: transform 0.15s ease;
}

.kb-filters__tag:hover {
  transform: translateY(-1px);
}

.kb-result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0 14px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.kb-alert {
  margin-bottom: 12px;
}

.kb-grid {
  display: grid;
  /* 桌面三列 / 中屏两列 / 手机一列（文档 4.3 节） */
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-gap);
}

@media (max-width: 1024px) {
  .kb-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kb-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

.kb-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  text-decoration: none;
  overflow: hidden;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.kb-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 10px 24px var(--sp-primary-a12);
  transform: translateY(-2px);
}

.kb-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.kb-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.kb-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 32px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.kb-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px 16px;
  flex: 1;
}

.kb-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.kb-card__date {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.kb-card__title {
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kb-card__summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.kb-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--sp-text-faint);
  padding-top: 8px;
  border-top: 1px dashed var(--sp-border);
}

.kb-pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--sp-gap-lg);
}
</style>
