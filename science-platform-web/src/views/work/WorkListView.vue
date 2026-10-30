<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listWorks } from '@/api/work'
import { listCategories } from '@/api/category'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const works = ref([])
const categories = ref([])
const total = ref(0)

const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const searchInput = ref(keyword.value)
const activeCategoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)
const page = ref(route.query.page ? Number(route.query.page) : 1)
const size = ref(9)

const activeCategoryName = computed(() => {
  if (!activeCategoryId.value) return '全部'
  return categories.value.find((c) => c.id === activeCategoryId.value)?.name || '全部'
})

async function fetchWorks() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listWorks({
      page: page.value,
      size: size.value,
      categoryId: activeCategoryId.value || undefined,
      keyword: keyword.value || undefined,
    })
    works.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    loadError.value = error?.response?.data?.message || '加载失败，请稍后重试'
    works.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    categories.value = await listCategories({ type: 'WORK' })
  } catch {
    categories.value = []
  }
}

function syncQuery() {
  const query = {}
  if (keyword.value) query.keyword = keyword.value
  if (activeCategoryId.value) query.categoryId = String(activeCategoryId.value)
  if (page.value > 1) query.page = String(page.value)
  router.replace({ path: '/works', query })
}

function applyFilters() {
  keyword.value = searchInput.value.trim()
  page.value = 1
  syncQuery()
  fetchWorks()
}

function selectCategory(id) {
  activeCategoryId.value = activeCategoryId.value === id ? null : id
  page.value = 1
  syncQuery()
  fetchWorks()
}

function handlePageChange(p) {
  page.value = p
  syncQuery()
  fetchWorks()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(
  () => route.query,
  () => {
    const q = route.query
    const nextKeyword = typeof q.keyword === 'string' ? q.keyword : ''
    const nextCategory = q.categoryId ? Number(q.categoryId) : null
    const nextPage = q.page ? Number(q.page) : 1
    if (nextKeyword !== keyword.value || nextCategory !== activeCategoryId.value || nextPage !== page.value) {
      keyword.value = nextKeyword
      searchInput.value = nextKeyword
      activeCategoryId.value = nextCategory
      page.value = nextPage
      fetchWorks()
    }
  },
)

onMounted(() => {
  fetchCategories()
  fetchWorks()
})
</script>

<template>
  <div class="works-page">
    <header class="works-hero">
      <div class="works-hero__text">
        <h1>创意作品</h1>
        <p>创作者把科普知识做成图文、海报、漫画与短视频，每件作品都关联到具体的科普文章。</p>
      </div>
      <RouterLink to="/creator/works" class="works-hero__cta">
        <el-button type="primary" plain>我是创作者</el-button>
      </RouterLink>
    </header>

    <section class="works-filters">
      <el-input
        v-model="searchInput"
        placeholder="搜索作品标题或简介"
        clearable
        class="works-filters__search"
        @keyup.enter="applyFilters"
      >
        <template #append>
          <el-button @click="applyFilters">搜索</el-button>
        </template>
      </el-input>

      <div class="works-filters__tags">
        <el-tag
          :type="activeCategoryId === null ? 'primary' : 'info'"
          :effect="activeCategoryId === null ? 'dark' : 'plain'"
          class="works-filters__tag"
          @click="selectCategory(null)"
        >
          全部
        </el-tag>
        <el-tag
          v-for="category in categories"
          :key="category.id"
          :type="activeCategoryId === category.id ? 'primary' : 'info'"
          :effect="activeCategoryId === category.id ? 'dark' : 'plain'"
          class="works-filters__tag"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
        </el-tag>
      </div>
    </section>

    <section class="works-result-bar">
      <span>共 <strong>{{ total }}</strong> 件作品 · {{ activeCategoryName }}</span>
    </section>

    <div v-loading="loading">
      <el-alert
        v-if="loadError && !loading"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
      />

      <el-empty
        v-else-if="!loading && works.length === 0"
        description="还没有符合条件的作品"
      />

      <div v-else class="works-grid">
        <RouterLink v-for="work in works" :key="work.id" :to="`/works/${work.id}`" class="work-card">
          <div class="work-card__cover">
            <img v-if="work.coverUrl" :src="work.coverUrl" :alt="work.title" loading="lazy" />
            <div v-else class="work-card__cover-fallback">{{ work.title.charAt(0) }}</div>
          </div>
          <div class="work-card__body">
            <div class="work-card__meta">
              <el-tag size="small" effect="plain" type="primary">{{ work.categoryName || '未分类' }}</el-tag>
              <span v-if="work.articleCount" class="work-card__link-count">
                关联 {{ work.articleCount }} 篇文章
              </span>
            </div>
            <h3 class="work-card__title">{{ work.title }}</h3>
            <p class="work-card__desc">{{ work.description }}</p>
            <div class="work-card__footer">
              <span>{{ work.creatorName }}</span>
              <span>浏览 {{ work.viewCount }}</span>
            </div>
          </div>
        </RouterLink>
      </div>
    </div>

    <div v-if="!loading && total > size" class="works-pagination">
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
.works-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 56px;
}

.works-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 32px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 84% 20%, rgba(64, 158, 255, 0.16), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 64%);
}

.works-hero__text h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--sp-text-strong);
}

.works-hero__text p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.works-hero__cta {
  flex-shrink: 0;
  text-decoration: none;
}

.works-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 22px;
}

.works-filters__search {
  max-width: 360px;
}

.works-filters__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.works-filters__tag {
  cursor: pointer;
}

.works-result-bar {
  margin: 18px 0 14px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.work-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.work-card:hover {
  border-color: rgba(64, 158, 255, 0.5);
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.12);
  transform: translateY(-2px);
}

.work-card__cover {
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.work-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.work-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 32px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.work-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px 16px;
  flex: 1;
}

.work-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.work-card__link-count {
  font-size: 12px;
  color: var(--sp-primary-dark);
}

.work-card__title {
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.work-card__desc {
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

.work-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--sp-text-faint);
  padding-top: 8px;
  border-top: 1px dashed var(--sp-border);
}

.works-pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
