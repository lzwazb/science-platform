<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listArticles } from '@/api/article'
import { listCategories } from '@/api/category'
import { themeOf } from '@/constants/dict'

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
          :style="{ '--cat': themeOf(article.categoryName).color }"
        >
          <div class="kb-card__cover">
            <img v-if="coverOf(article)" :src="coverOf(article)" :alt="article.title" loading="lazy" />
            <div v-else class="kb-card__cover-fallback">{{ article.title.charAt(0) }}</div>
          </div>
          <div class="kb-card__body">
            <div class="kb-card__meta">
              <span class="kb-card__cat">{{ article.categoryName || '未分类' }}</span>
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
  /* 原来 30/32 的内边距 + 26px 标题占了近 180px，而这里只有标题、说明和一个搜索框。
     压到 22/26、标题 21px 之后约 120px，把高度让给下面的内容网格。 */
  padding: 22px 26px;
  border-radius: 14px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 88% 12%, var(--sp-primary-a12), transparent 42%),
    linear-gradient(135deg, #f5f9ff 0%, #ffffff 70%);
}

.kb-hero h1 {
  margin: 0 0 6px;
  font-size: 21px;
  letter-spacing: -0.3px;
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
  /* 四列：1440 下每张卡约 285px，一屏能看到 8 张卡的完整信息。
     三列在 1440 下每张 400px，封面对 16:9 来说太大、信息密度偏低。 */
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1280px) {
  .kb-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .kb-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .kb-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* --------------------------------------------------------------------------
   文章卡片
   每张卡片带一个 --cat 变量（学科主题色），卡片本身不设边框，
   靠封面和留白建立层级；hover 时封面上浮一层同色柔光。
   -------------------------------------------------------------------------- */

.kb-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  background: #fff;
  text-decoration: none;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(24, 44, 68, 0.05);
  /* 纯阴影在浅色底上会显得悬浮，补一条几乎看不见的边框把卡片压住 */
  border: 1px solid rgba(24, 44, 68, 0.06);
  transition: box-shadow 0.22s ease, transform 0.22s ease, border-color 0.22s ease;
}

.kb-card:hover {
  box-shadow: 0 14px 30px -8px rgba(24, 44, 68, 0.18);
  border-color: rgba(24, 44, 68, 0.10);
  transform: translateY(-3px);
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
  transition: transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.kb-card:hover .kb-card__cover img {
  transform: scale(1.045);
}

/* 学科色极淡叠色：让同类文章的封面成组，同时不抢照片本身 */
.kb-card__cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--cat, var(--sp-primary));
  opacity: 0;
  mix-blend-mode: soft-light;
  transition: opacity 0.22s ease;
  pointer-events: none;
}

.kb-card:hover .kb-card__cover::after {
  opacity: 0.35;
}

.kb-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 32px;
  font-weight: 700;
  color: var(--cat, var(--sp-primary-dark));
}

.kb-card__body {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 13px 15px 14px;
  flex: 1;
}

.kb-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

/* 分类不再是统一蓝色的 tag，而是学科自己的颜色 + 一条竖线，更轻也更可辨 */
.kb-card__cat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--cat, var(--sp-primary-dark));
}

.kb-card__cat::before {
  content: '';
  width: 3px;
  height: 11px;
  border-radius: 2px;
  background: var(--cat, var(--sp-primary));
}

.kb-card__date {
  font-size: 11.5px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.kb-card__title {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.55;
  letter-spacing: -0.1px;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.18s ease;
  /* 两行截断时让两行长度接近，避免出现「第一行塞满、第二行只剩一个字」
     的孤字（如「…长期记 / 忆」）。不支持的浏览器会忽略这行，退回普通换行。 */
  text-wrap: balance;
}

.kb-card:hover .kb-card__title {
  color: var(--cat, var(--sp-primary-dark));
}

.kb-card__summary {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.65;
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
  font-size: 11.5px;
  color: var(--sp-text-faint);
  padding-top: 9px;
  border-top: 1px solid var(--sp-border);
}

.kb-pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--sp-gap-lg);
}
</style>
