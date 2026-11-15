<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listWorks } from '@/api/work'
import { listCategories } from '@/api/category'
import ContentCard from '@/components/content/ContentCard.vue'

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
/** 学科筛选：从作品详情页的学科标签跳进来时带上 */
const activeKnowledgeId = ref(
  route.query.knowledgeCategoryId ? Number(route.query.knowledgeCategoryId) : null,
)
/** 作者筛选：从作品详情页的「查看全部」跳进来时带上 */
const activeCreatorId = ref(route.query.creatorId ? Number(route.query.creatorId) : null)
const page = ref(route.query.page ? Number(route.query.page) : 1)
const size = ref(9)

const activeCategoryName = computed(() => {
  if (!activeCategoryId.value) return '全部'
  return categories.value.find((c) => c.id === activeCategoryId.value)?.name || '全部'
})

/** 学科名与作者名不在分类接口里，从当前结果里回读，避免多一次请求 */
const activeKnowledgeName = computed(
  () =>
    works.value.find((item) => item.knowledgeCategoryId === activeKnowledgeId.value)
      ?.knowledgeCategoryName || '该学科',
)

const activeCreatorName = computed(
  () => works.value.find((item) => item.creatorId === activeCreatorId.value)?.creatorName || '该作者',
)

const hasExtraFilter = computed(() => Boolean(activeKnowledgeId.value || activeCreatorId.value))

async function fetchWorks() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listWorks({
      page: page.value,
      size: size.value,
      categoryId: activeCategoryId.value || undefined,
      knowledgeCategoryId: activeKnowledgeId.value || undefined,
      creatorId: activeCreatorId.value || undefined,
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
  if (activeKnowledgeId.value) query.knowledgeCategoryId = String(activeKnowledgeId.value)
  if (activeCreatorId.value) query.creatorId = String(activeCreatorId.value)
  if (page.value > 1) query.page = String(page.value)
  router.replace({ path: '/works', query })
}

/** 清掉学科/作者筛选，回到作品广场的全部作品 */
function clearExtraFilters() {
  activeKnowledgeId.value = null
  activeCreatorId.value = null
  page.value = 1
  syncQuery()
  fetchWorks()
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
    const nextKnowledge = q.knowledgeCategoryId ? Number(q.knowledgeCategoryId) : null
    const nextCreator = q.creatorId ? Number(q.creatorId) : null
    const nextPage = q.page ? Number(q.page) : 1
    if (
      nextKeyword !== keyword.value ||
      nextCategory !== activeCategoryId.value ||
      nextKnowledge !== activeKnowledgeId.value ||
      nextCreator !== activeCreatorId.value ||
      nextPage !== page.value
    ) {
      keyword.value = nextKeyword
      searchInput.value = nextKeyword
      activeCategoryId.value = nextCategory
      activeKnowledgeId.value = nextKnowledge
      activeCreatorId.value = nextCreator
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
        <p>创作者把科普知识做成图文、海报、漫画与短视频；作品可以归入某个学科，也可以先发布、之后再归类。</p>
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
      <span>
        共 <strong>{{ total }}</strong> 件作品<template v-if="activeCategoryName !== '全部'">
          · {{ activeCategoryName }}</template
        >
      </span>
      <div v-if="hasExtraFilter" class="works-result-bar__filters">
        <span v-if="activeKnowledgeId" class="works-filter-chip">
          学科：{{ activeKnowledgeName }}
        </span>
        <span v-if="activeCreatorId" class="works-filter-chip">作者：{{ activeCreatorName }}</span>
        <el-button link type="primary" @click="clearExtraFilters">清空筛选</el-button>
      </div>
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
        <ContentCard
          v-for="work in works"
          :key="work.id"
          :to="`/works/${work.id}`"
          :cover="work.coverUrl || ''"
          :title="work.title"
          :summary="work.description"
          :tag="work.categoryName || '未分类'"
          :category="work.knowledgeCategoryName"
          :left="work.creatorName"
          :right="`浏览 ${work.viewCount}`"
        />
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
  max-width: var(--sp-container);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
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
    radial-gradient(circle at 84% 20%, var(--sp-primary-a16), transparent 46%),
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 18px 0 14px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.works-result-bar__filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.works-filter-chip {
  padding: 2px 10px;
  border-radius: 10px;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-size: 11.5px;
}

.works-grid {
  display: grid;
  /* 桌面三列 / 中屏两列 / 手机一列 */
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1280px) {
  .works-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .works-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .works-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* 卡片本体统一由 ContentCard 提供，这里只保留网格与分页 */

.works-pagination {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
