<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWorkDetail } from '@/api/work'
import InteractionBar from '@/components/InteractionBar.vue'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const work = ref(null)

const workId = computed(() => route.params.id)

const paragraphs = computed(() => {
  if (!work.value?.content) return []
  return work.value.content.split(/\n+/).filter((p) => p.trim().length > 0)
})

async function fetchDetail() {
  loading.value = true
  loadError.value = ''
  work.value = null
  try {
    work.value = await getWorkDetail(workId.value)
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
  <div class="work-page" v-loading="loading">
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
            <el-tag size="small" effect="plain" type="primary">{{ work.categoryName || '未分类' }}</el-tag>
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
            <el-avatar :size="40">{{ (work.creatorName || '?').charAt(0) }}</el-avatar>
            <div class="work-author__text">
              <strong>{{ work.creatorName }}</strong>
              <p>{{ work.creatorBio || '这位创作者还没有填写简介' }}</p>
            </div>
          </div>

          <InteractionBar target-type="WORK" :target-id="work.id" class="work-interaction" />
        </footer>

        <CommentSection target-type="WORK" :target-id="work.id" />
      </article>

      <section class="work-articles">
        <h2 class="work-articles__title">这件作品关联的科普文章</h2>

        <el-empty
          v-if="!work.articles?.length"
          description="作者还没有为这件作品关联科普文章"
          :image-size="80"
        />

        <div v-else class="work-articles__list">
          <RouterLink
            v-for="article in work.articles"
            :key="article.id"
            :to="`/knowledge/${article.id}`"
            class="work-article-card"
          >
            <div v-if="article.coverUrl" class="work-article-card__cover">
              <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
            </div>
            <div class="work-article-card__body">
              <el-tag size="small" effect="plain" type="primary">{{ article.categoryName || '未分类' }}</el-tag>
              <h3>{{ article.title }}</h3>
              <p>{{ article.summary }}</p>
            </div>
          </RouterLink>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.work-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 24px 56px;
}

.work-crumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.work-shell {
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  padding: 30px 34px 24px;
}

.work-head__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--sp-text-faint);
}

.work-head__title {
  margin: 14px 0 10px;
  font-size: 25px;
  line-height: 1.45;
  color: var(--sp-text-strong);
}

.work-head__desc {
  margin: 0 0 8px;
  padding: 12px 16px;
  border-left: 3px solid var(--sp-primary);
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
  margin-top: 22px;
}

.work-body p {
  margin: 0 0 15px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--sp-text);
  text-align: justify;
}

.work-foot {
  margin-top: 26px;
  padding-top: 18px;
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

.work-interaction {
  margin-top: 16px;
}

.work-articles {
  margin-top: 26px;
}

.work-articles__title {
  margin: 0 0 12px;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.work-articles__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-article-card {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.work-article-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 8px 20px var(--sp-primary-a10);
}

.work-article-card__cover {
  flex-shrink: 0;
  width: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.work-article-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.work-article-card__body h3 {
  margin: 8px 0 6px;
  font-size: 15px;
  color: var(--sp-text-strong);
}

.work-article-card__body p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 720px) {
  .work-shell {
    padding: 22px 20px 20px;
  }

  .work-head__title {
    font-size: 21px;
  }

  .work-article-card__cover {
    display: none;
  }
}
</style>
