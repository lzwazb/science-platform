<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWorkDetail, listWorks } from '@/api/work'
import InteractionBar from '@/components/InteractionBar.vue'
import CommentSection from '@/components/CommentSection.vue'
import ContentCard from '@/components/content/ContentCard.vue'
import { themeOf } from '@/constants/dict'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const work = ref(null)
/** 同一作者的其它作品，最多取 3 件做入口 */
const otherWorks = ref([])

const workId = computed(() => route.params.id)

/** 学科主题色：知识分类决定色，没有归类的作品回落品牌蓝 */
const catColor = computed(() => themeOf(work.value?.knowledgeCategoryName).color)

const paragraphs = computed(() => {
  if (!work.value?.content) return []
  return work.value.content.split(/\n+/).filter((p) => p.trim().length > 0)
})

/** 学科标签点击后跳到按学科筛选的作品列表 */
const sameCategoryWorksLink = computed(() => {
  const id = work.value?.knowledgeCategoryId
  return id
    ? { path: '/works', query: { knowledgeCategoryId: String(id) } }
    : { path: '/works' }
})

/** 「查看全部」：该学科下的科普文章 */
const sameCategoryArticlesLink = computed(() => {
  const id = work.value?.knowledgeCategoryId
  return id ? { path: '/knowledge', query: { categoryId: String(id) } } : { path: '/knowledge' }
})

/** 「查看全部」：该作者的全部作品 */
const creatorWorksLink = computed(() => {
  const id = work.value?.creatorId
  return id ? { path: '/works', query: { creatorId: String(id) } } : { path: '/works' }
})

async function fetchOtherWorks(creatorId, currentId) {
  if (!creatorId) {
    otherWorks.value = []
    return
  }
  try {
    const data = await listWorks({ page: 1, size: 6, creatorId })
    otherWorks.value = (data.records || [])
      .filter((item) => String(item.id) !== String(currentId))
      .slice(0, 3)
  } catch {
    otherWorks.value = []
  }
}

async function fetchDetail() {
  loading.value = true
  loadError.value = ''
  work.value = null
  otherWorks.value = []
  try {
    const detail = await getWorkDetail(workId.value)
    work.value = detail
    fetchOtherWorks(detail.creatorId, detail.id)
  } catch (error) {
    loadError.value = error?.response?.data?.message || '作品加载失败'
  } finally {
    loading.value = false
  }
}

watch(workId, fetchDetail)
onMounted(fetchDetail)
</script>

<template>
  <div class="work-page" v-loading="loading" :style="{ '--cat': catColor }">
    <el-breadcrumb separator="/" class="work-crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/works' }">创意作品</el-breadcrumb-item>
      <el-breadcrumb-item>{{ work?.title || '作品详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-result
      v-if="loadError && !loading"
      icon="warning"
      title="无法打开这件作品"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/works')">返回作品列表</el-button>
      </template>
    </el-result>

    <template v-else-if="work">
      <article class="work-shell">
        <header class="work-head">
          <div class="work-head__meta">
            <RouterLink
              v-if="work.knowledgeCategoryName"
              :to="sameCategoryWorksLink"
              class="work-head__cat"
              :title="`查看「${work.knowledgeCategoryName}」学科的全部作品`"
            >
              {{ work.knowledgeCategoryName }}
            </RouterLink>
            <span v-if="work.categoryName" class="work-head__form">{{ work.categoryName }}</span>
            <span>浏览 {{ work.viewCount }}</span>
            <span>{{ (work.reviewedAt || work.createdAt || '').slice(0, 10) }}</span>
          </div>

          <h1 class="work-head__title">{{ work.title }}</h1>
          <p v-if="work.description" class="work-head__desc">{{ work.description }}</p>
        </header>

        <div v-if="work.coverUrl" class="work-cover">
          <img :src="work.coverUrl" :alt="work.title" />
        </div>

        <section class="work-body">
          <p v-for="(para, index) in paragraphs" :key="index">{{ para }}</p>
        </section>

        <footer class="work-foot">
          <div class="work-author">
            <el-avatar :size="42">{{ (work.creatorName || '?').charAt(0) }}</el-avatar>
            <div class="work-author__text">
              <strong>{{ work.creatorName }}</strong>
              <p>{{ work.creatorBio || '这位创作者还没有填写简介' }}</p>
            </div>
          </div>

          <div v-if="otherWorks.length" class="work-author__more">
            <span class="work-author__more-label">他的其它作品</span>
            <RouterLink
              v-for="item in otherWorks"
              :key="item.id"
              :to="`/works/${item.id}`"
              class="work-author__more-item"
            >
              {{ item.title }}
            </RouterLink>
            <RouterLink :to="creatorWorksLink" class="work-author__more-all">
              查看全部 →
            </RouterLink>
          </div>

          <InteractionBar target-type="WORK" :target-id="work.id" class="work-interaction" />
        </footer>

        <CommentSection target-type="WORK" :target-id="work.id" />
      </article>

      <section class="work-related">
        <div class="work-related__head">
          <h2>同学科的科普文章</h2>
          <RouterLink v-if="work.knowledgeCategoryId" :to="sameCategoryArticlesLink" class="work-related__all">
            全部{{ work.knowledgeCategoryName }}文章 →
          </RouterLink>
        </div>

        <el-empty
          v-if="!work.articles?.length"
          :description="
            work.knowledgeCategoryName
              ? `这个学科下暂时还没有已发布的文章`
              : '这件作品还没有归入学科，所以没有可推荐的科普文章'
          "
          :image-size="80"
        />

        <div v-else class="work-related__grid">
          <ContentCard
            v-for="article in work.articles"
            :key="article.id"
            :to="`/knowledge/${article.id}`"
            :cover="article.coverUrl"
            :title="article.title"
            :summary="article.summary"
            :category="article.categoryName"
            :left="article.authorName"
            :right="article.viewCount ? `阅读 ${article.viewCount}` : ''"
          />
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.work-page {
  max-width: var(--sp-container-reading);
  margin: 0 auto;
  padding: 24px 24px 56px;
  /* 学科主题色由模板上的 :style 注入 */
  --cat: var(--sp-primary);
}

.work-crumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.work-shell {
  position: relative;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  padding: 32px 36px 26px;
}

/* 顶部学科色指示线，与文章详情同一手法：进门就知道这是哪一类内容 */
.work-shell::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: linear-gradient(90deg, var(--cat), transparent 72%);
}

.work-head__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--sp-text-faint);
}

/* 学科是这页的主身份，做成可点的学科色标签；作品形态退为中性小字 */
.work-head__cat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--cat);
  text-decoration: none;
}

.work-head__cat::before {
  content: '';
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: var(--cat);
}

.work-head__cat:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.work-head__form {
  padding: 1px 8px;
  border: 1px solid var(--sp-border);
  border-radius: 10px;
  font-size: 11.5px;
  color: var(--sp-text-muted);
  background: #fafcff;
}

.work-head__title {
  margin: 14px 0 12px;
  font-size: 27px;
  line-height: 1.45;
  letter-spacing: 0.3px;
  color: var(--sp-text-strong);
}

.work-head__desc {
  margin: 0 0 8px;
  padding: 12px 16px;
  border-left: 3px solid var(--cat);
  background: var(--sp-primary-soft);
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text);
}

.work-cover {
  margin: 22px 0 4px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--sp-border);
}

.work-cover img {
  width: 100%;
  display: block;
}

.work-body {
  margin-top: 24px;
}

.work-body p {
  margin: 0 0 16px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--sp-text);
  text-align: justify;
}

.work-foot {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed var(--sp-border);
}

.work-author {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.work-author__text strong {
  display: block;
  font-size: 14px;
  color: var(--sp-text-strong);
  margin-bottom: 4px;
}

.work-author__text p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}

.work-author__more {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-left: 54px;
}

.work-author__more-label {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.work-author__more-item {
  max-width: 220px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  padding: 3px 11px;
  border-radius: 12px;
  background: var(--sp-primary-soft);
  font-size: 12px;
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.work-author__more-item:hover {
  background: var(--sp-primary-a22);
}

.work-author__more-all {
  font-size: 12px;
  color: var(--sp-primary);
  text-decoration: none;
}

.work-author__more-all:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.work-interaction {
  margin-top: 16px;
}

.work-related {
  margin-top: 28px;
}

.work-related__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.work-related__head h2 {
  margin: 0;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.work-related__all {
  font-size: 12.5px;
  color: var(--sp-primary);
  text-decoration: none;
}

.work-related__all:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.work-related__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .work-related__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .work-shell {
    padding: 24px 20px 22px;
  }

  .work-head__title {
    font-size: 22px;
  }

  .work-author__more {
    padding-left: 0;
  }

  .work-related__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
