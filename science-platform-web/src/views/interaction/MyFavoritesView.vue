<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyFavorites } from '@/api/interaction'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)

async function fetchFavorites() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await listMyFavorites({ page: page.value, size: size.value })
    items.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    loadError.value = error?.response?.data?.message || '加载失败'
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function pathOf(item) {
  return item.targetType === 'WORK' ? `/works/${item.targetId}` : `/knowledge/${item.targetId}`
}

function typeLabel(item) {
  return item.targetType === 'WORK' ? '作品' : '文章'
}

function open(item) {
  if (!item.available) return
  router.push(pathOf(item))
}

onMounted(fetchFavorites)
</script>

<template>
  <div class="favorites-page">
    <header class="favorites-head">
      <div>
        <h1>我的收藏</h1>
        <p>收藏的文章与作品按时间倒序排列；内容若已下架或删除，会标注为不可访问。</p>
      </div>
      <el-button size="small" @click="fetchFavorites">刷新</el-button>
    </header>

    <div v-loading="loading">
      <el-alert
        v-if="loadError && !loading"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
      />

      <el-empty
        v-else-if="!loading && items.length === 0"
        description="还没有收藏任何内容"
      >
        <RouterLink to="/knowledge">
          <el-button type="primary" plain>去知识库看看</el-button>
        </RouterLink>
      </el-empty>

      <div v-else class="favorites-grid">
        <article
          v-for="item in items"
          :key="`${item.targetType}-${item.targetId}`"
          class="favorite-card"
          :class="{ 'favorite-card--disabled': !item.available }"
          @click="open(item)"
        >
          <div class="favorite-card__cover">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy" />
            <div v-else class="favorite-card__cover-fallback">{{ item.title.charAt(0) }}</div>
          </div>
          <div class="favorite-card__body">
            <div class="favorite-card__meta">
              <el-tag size="small" effect="plain" :type="item.targetType === 'WORK' ? 'warning' : 'primary'">
                {{ typeLabel(item) }}
              </el-tag>
              <el-tag v-if="!item.available" size="small" type="info">内容已下架</el-tag>
              <span class="favorite-card__time">{{ (item.createdAt || '').slice(0, 10) }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary }}</p>
          </div>
        </article>
      </div>
    </div>

    <div v-if="!loading && total > size" class="favorites-pagination">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="(p) => { page = p; fetchFavorites() }"
      />
    </div>
  </div>
</template>

<style scoped>
.favorites-page {
  max-width: var(--sp-container);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.favorites-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.favorites-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.favorites-head p {
  margin: 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.favorite-card {
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.favorite-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 10px 24px var(--sp-primary-a12);
  transform: translateY(-2px);
}

.favorite-card--disabled {
  cursor: not-allowed;
  opacity: 0.66;
}

.favorite-card--disabled:hover {
  transform: none;
  box-shadow: none;
  border-color: var(--sp-border);
}

.favorite-card__cover {
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.favorite-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.favorite-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 30px;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.favorite-card__body {
  padding: 12px 14px 16px;
}

.favorite-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.favorite-card__time {
  margin-left: auto;
  font-size: 11px;
  color: var(--sp-text-faint);
}

.favorite-card__body h3 {
  margin: 0 0 6px;
  font-size: 15px;
  line-height: 1.5;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.favorite-card__body p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.favorites-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
