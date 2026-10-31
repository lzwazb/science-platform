<script setup>
import { onMounted, ref } from 'vue'
import { listTopics } from '@/api/topic'

const loading = ref(false)
const loadError = ref('')
const topics = ref([])

async function fetchTopics() {
  loading.value = true
  loadError.value = ''
  try {
    topics.value = await listTopics()
  } catch (error) {
    loadError.value = error?.response?.data?.message || '专题加载失败'
    topics.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchTopics)
</script>

<template>
  <div class="topic-page">
    <header class="topic-hero">
      <h1>专题</h1>
      <p>把零散的知识串成一条线：每个专题围绕一个主题组织文章，按推荐顺序阅读。</p>
    </header>

    <div v-loading="loading">
      <el-alert
        v-if="loadError && !loading"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
      />

      <el-empty v-else-if="!loading && topics.length === 0" description="暂无已发布的专题" />

      <div v-else class="topic-grid">
        <RouterLink v-for="topic in topics" :key="topic.id" :to="`/topics/${topic.id}`" class="topic-card">
          <div class="topic-card__cover">
            <img v-if="topic.coverUrl" :src="topic.coverUrl" :alt="topic.title" loading="lazy" />
            <div v-else class="topic-card__cover-fallback">{{ topic.title.charAt(0) }}</div>
            <span class="topic-card__count">{{ topic.articleCount }} 篇</span>
          </div>
          <div class="topic-card__body">
            <h3>{{ topic.title }}</h3>
            <p>{{ topic.description }}</p>
          </div>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.topic-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 56px;
}

.topic-hero {
  padding: 30px 32px;
  margin-bottom: 22px;
  border-radius: 16px;
  border: 1px solid var(--sp-border);
  background:
    radial-gradient(circle at 82% 24%, var(--sp-primary-a15), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 64%);
}

.topic-hero h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--sp-text-strong);
}

.topic-hero p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.topic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.topic-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.topic-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 10px 24px var(--sp-primary-a12);
  transform: translateY(-2px);
}

.topic-card__cover {
  position: relative;
  aspect-ratio: 16 / 7;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.topic-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.topic-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 30px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.topic-card__count {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
}

.topic-card__body {
  padding: 14px 16px 18px;
}

.topic-card__body h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--sp-text-strong);
}

.topic-card__body p {
  margin: 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
