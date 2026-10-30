<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleDetail } from '@/api/article'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const article = ref(null)
const loadError = ref('')

const articleId = computed(() => route.params.id)

/** 正文按段落切分后用 pre-wrap 渲染，不用 v-html，避免富文本带来的 XSS 风险 */
const paragraphs = computed(() => {
  if (!article.value?.content) return []
  return article.value.content.split(/\n+/).filter((p) => p.trim().length > 0)
})

async function fetchDetail() {
  loading.value = true
  loadError.value = ''
  article.value = null
  try {
    article.value = await getArticleDetail(articleId.value)
  } catch (error) {
    loadError.value = error?.response?.data?.message || '文章加载失败'
  } finally {
    loading.value = false
  }
}

watch(articleId, fetchDetail)
onMounted(fetchDetail)
</script>

<template>
  <div class="article-page" v-loading="loading">
    <el-breadcrumb separator="/" class="article-breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库</el-breadcrumb-item>
      <el-breadcrumb-item>{{ article?.title || '文章详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-result
      v-if="loadError && !loading"
      icon="warning"
      title="无法打开这篇文章"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/knowledge')">返回知识库</el-button>
      </template>
    </el-result>

    <template v-else-if="article">
      <article class="article-shell">
        <header class="article-head">
          <div class="article-head__meta">
            <el-tag size="small" effect="plain" type="primary">
              {{ article.categoryName || '未分类' }}
            </el-tag>
            <span>第 {{ article.versionNo }} 版</span>
            <span>{{ (article.publishedAt || '').slice(0, 10) }}</span>
            <span>阅读 {{ article.viewCount }}</span>
          </div>

          <h1 class="article-head__title">{{ article.title }}</h1>
          <p v-if="article.summary" class="article-head__summary">{{ article.summary }}</p>

          <div class="article-head__author">
            <el-avatar :size="30">{{ (article.authorName || '?').charAt(0) }}</el-avatar>
            <span>{{ article.authorName }}</span>
          </div>
        </header>

        <div v-if="article.coverUrl" class="article-cover">
          <img :src="article.coverUrl" :alt="article.title" />
        </div>

        <section class="article-body">
          <p v-for="(para, index) in paragraphs" :key="index">{{ para }}</p>
        </section>

        <footer class="article-foot">
          <div v-if="article.topics?.length" class="article-foot__topics">
            <span class="article-foot__label">所属专题</span>
            <RouterLink
              v-for="topic in article.topics"
              :key="topic.id"
              :to="`/topics/${topic.id}`"
              class="article-foot__topic"
            >
              {{ topic.title }}
            </RouterLink>
          </div>

          <el-divider />

          <div class="article-actions">
            <el-tooltip content="知识共建功能在阶段 6 开放" placement="top">
              <span>
                <el-button disabled>提交纠错 / 补充</el-button>
              </span>
            </el-tooltip>
            <el-tooltip content="社区互动在阶段 7 开放" placement="top">
              <span>
                <el-button disabled>点赞 / 收藏</el-button>
              </span>
            </el-tooltip>
            <span v-if="!userStore.isLoggedIn" class="article-actions__hint">
              登录后可参与知识共建与互动
            </span>
          </div>

          <p class="article-foot__note">
            本文为演示用科普内容，参考来源已在正文末尾标注。发现表述不准确？欢迎在功能开放后提交纠错建议。
          </p>
        </footer>
      </article>

      <!-- 阶段 5 起填充关联创意作品，此处先占位 -->
      <section class="article-related">
        <h2 class="article-related__title">相关创意作品</h2>
        <el-empty description="创意作品模块将在阶段 5 开放" :image-size="80" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.article-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 24px 56px;
}

.article-breadcrumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.article-shell {
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  padding: 32px 36px 28px;
}

.article-head__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--sp-text-faint);
}

.article-head__title {
  margin: 14px 0 12px;
  font-size: 27px;
  line-height: 1.45;
  color: var(--sp-text-strong);
  letter-spacing: 0.3px;
}

.article-head__summary {
  margin: 0 0 18px;
  padding: 12px 16px;
  border-left: 3px solid var(--sp-primary);
  background: var(--sp-primary-soft);
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text);
}

.article-head__author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.article-cover {
  margin: 22px 0 6px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--sp-border);
}

.article-cover img {
  width: 100%;
  display: block;
}

.article-body {
  margin-top: 24px;
}

.article-body p {
  margin: 0 0 16px;
  font-size: 15px;
  line-height: 1.95;
  color: var(--sp-text);
  text-align: justify;
}

.article-foot {
  margin-top: 28px;
}

.article-foot__topics {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.article-foot__label {
  font-size: 13px;
  color: var(--sp-text-muted);
}

.article-foot__topic {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 14px;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.article-foot__topic:hover {
  background: rgba(64, 158, 255, 0.22);
}

.article-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.article-actions__hint {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.article-foot__note {
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px dashed var(--sp-border);
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-faint);
}

.article-related {
  margin-top: 26px;
}

.article-related__title {
  margin: 0 0 12px;
  font-size: 17px;
  color: var(--sp-text-strong);
}

@media (max-width: 720px) {
  .article-shell {
    padding: 24px 20px 22px;
  }

  .article-head__title {
    font-size: 22px;
  }
}
</style>
