<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTopicDetail } from '@/api/topic'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const topic = ref(null)

const topicId = computed(() => route.params.id)

async function fetchDetail() {
  loading.value = true
  loadError.value = ''
  topic.value = null
  try {
    topic.value = await getTopicDetail(topicId.value)
  } catch (error) {
    loadError.value = error?.response?.data?.message || '专题加载失败'
  } finally {
    loading.value = false
  }
}

watch(topicId, fetchDetail)
onMounted(fetchDetail)
</script>

<template>
  <div class="topic-detail" v-loading="loading">
    <el-breadcrumb separator="/" class="topic-detail__crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/topics' }">专题</el-breadcrumb-item>
      <el-breadcrumb-item>{{ topic?.title || '专题详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-result
      v-if="loadError && !loading"
      icon="warning"
      title="无法打开这个专题"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/topics')">返回专题列表</el-button>
      </template>
    </el-result>

    <template v-else-if="topic">
      <header class="topic-header">
        <div v-if="topic.coverUrl" class="topic-header__cover">
          <img :src="topic.coverUrl" :alt="topic.title" />
        </div>
        <div class="topic-header__text">
          <h1>{{ topic.title }}</h1>
          <p>{{ topic.description }}</p>
          <span class="topic-header__count">共 {{ topic.articleCount }} 篇文章</span>
        </div>
      </header>

      <el-empty
        v-if="!topic.articles?.length"
        description="这个专题还没有已发布的文章"
      />

      <ol v-else class="topic-articles">
        <li v-for="(article, index) in topic.articles" :key="article.id" class="topic-articles__item">
          <RouterLink :to="`/knowledge/${article.id}`" class="topic-articles__link">
            <span class="topic-articles__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="topic-articles__content">
              <h3>{{ article.title }}</h3>
              <p>{{ article.summary }}</p>
              <div class="topic-articles__meta">
                <el-tag size="small" effect="plain" type="primary">{{ article.categoryName }}</el-tag>
                <span>{{ article.authorName }}</span>
                <span>{{ (article.publishedAt || '').slice(0, 10) }}</span>
              </div>
            </div>
          </RouterLink>
        </li>
      </ol>
    </template>
  </div>
</template>

<style scoped>
.topic-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 24px 56px;
}

.topic-detail__crumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.topic-header {
  display: flex;
  gap: 20px;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background: #fff;
  margin-bottom: 22px;
}

.topic-header__cover {
  flex-shrink: 0;
  width: 180px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--sp-border);
}

.topic-header__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.topic-header__text h1 {
  margin: 0 0 10px;
  font-size: 24px;
  color: var(--sp-text-strong);
}

.topic-header__text p {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.topic-header__count {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.topic-articles {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-articles__link {
  display: flex;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.topic-articles__link:hover {
  border-color: rgba(64, 158, 255, 0.5);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.1);
}

.topic-articles__index {
  flex-shrink: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--sp-primary);
  line-height: 1.3;
  font-variant-numeric: tabular-nums;
}

.topic-articles__content h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--sp-text-strong);
}

.topic-articles__content p {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.topic-articles__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--sp-text-faint);
}

@media (max-width: 720px) {
  .topic-header {
    flex-direction: column;
  }

  .topic-header__cover {
    width: 100%;
  }
}
</style>
