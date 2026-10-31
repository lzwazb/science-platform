<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArticleDetail } from '@/api/article'
import { listArticleVersions, submitContribution } from '@/api/contribution'
import { useUserStore } from '@/stores/user'
import InteractionBar from '@/components/InteractionBar.vue'
import CommentSection from '@/components/CommentSection.vue'

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

// ---------------- 提交纠错 ----------------

const contributeVisible = ref(false)
const contributing = ref(false)
const contributeFormRef = ref(null)

const contributeForm = reactive({
  contributionType: 'CORRECTION',
  suggestedContent: '',
  reason: '',
  referenceSource: '',
})

const contributeRules = {
  contributionType: [{ required: true, message: '请选择建议类型', trigger: 'change' }],
  suggestedContent: [
    { required: true, message: '请填写建议内容', trigger: 'blur' },
    { max: 5000, message: '建议内容不能超过 5000 个字符', trigger: 'blur' },
  ],
  reason: [
    { required: true, message: '请说明提交理由', trigger: 'blur' },
    { max: 1000, message: '提交理由不能超过 1000 个字符', trigger: 'blur' },
  ],
  referenceSource: [
    { required: true, message: '请填写参考来源，便于评审核实', trigger: 'blur' },
    { max: 500, message: '参考来源不能超过 500 个字符', trigger: 'blur' },
  ],
}

function openContribute() {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  contributeForm.contributionType = 'CORRECTION'
  contributeForm.suggestedContent = ''
  contributeForm.reason = ''
  contributeForm.referenceSource = ''
  contributeVisible.value = true
}

async function handleContribute() {
  if (!contributeFormRef.value) return
  const valid = await contributeFormRef.value.validate().catch(() => false)
  if (!valid) return

  contributing.value = true
  try {
    await submitContribution({
      articleId: Number(articleId.value),
      contributionType: contributeForm.contributionType,
      suggestedContent: contributeForm.suggestedContent.trim(),
      reason: contributeForm.reason.trim(),
      referenceSource: contributeForm.referenceSource.trim(),
    })
    contributeVisible.value = false
    ElMessage.success('建议已提交，可在「我的贡献」中查看审核进度')
  } catch {
    // 错误已提示
  } finally {
    contributing.value = false
  }
}

// ---------------- 版本历史 ----------------

const versionsVisible = ref(false)
const versionsLoading = ref(false)
const versions = ref([])

async function openVersions() {
  versionsVisible.value = true
  if (versions.value.length > 0) return
  versionsLoading.value = true
  try {
    versions.value = await listArticleVersions(articleId.value)
  } catch {
    versions.value = []
  } finally {
    versionsLoading.value = false
  }
}

watch(articleId, () => {
  versions.value = []
  fetchDetail()
})
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
            <el-button type="primary" @click="openContribute">提交纠错 / 补充</el-button>
            <el-button @click="openVersions">版本历史</el-button>
          </div>

          <InteractionBar target-type="ARTICLE" :target-id="article.id" class="article-interaction" />

          <p class="article-foot__note">
            发现表述不准确或缺少来源？点击「提交纠错 / 补充」，填写理由与参考来源后进入审核流程。
            建议被采纳时，正式文章会更新，本页的版本历史会记录这次修改。
          </p>
        </footer>

        <CommentSection target-type="ARTICLE" :target-id="article.id" />
      </article>

      <!-- 提交纠错对话框 -->
      <el-dialog v-model="contributeVisible" title="提交纠错 / 补充建议" width="640px" destroy-on-close>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="建议会进入审核流程"
          description="评审员会对照原文核实你的建议；通过后才更新正式文章并保存历史版本。请尽量附上可查证的参考来源。"
          class="contribute-tip"
        />

        <el-form
          ref="contributeFormRef"
          :model="contributeForm"
          :rules="contributeRules"
          label-width="90px"
        >
          <el-form-item label="建议类型" prop="contributionType">
            <el-radio-group v-model="contributeForm.contributionType">
              <el-radio value="SUPPLEMENT">补充知识</el-radio>
              <el-radio value="CORRECTION">纠正错误</el-radio>
              <el-radio value="MODIFICATION">修改建议</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="建议内容" prop="suggestedContent">
            <el-input
              v-model="contributeForm.suggestedContent"
              type="textarea"
              :rows="5"
              maxlength="5000"
              show-word-limit
              placeholder="可以写建议修改的片段，也可以给出改写后的完整段落"
            />
          </el-form-item>

          <el-form-item label="提交理由" prop="reason">
            <el-input
              v-model="contributeForm.reason"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="说明原文哪里不准确、为什么不准确"
            />
          </el-form-item>

          <el-form-item label="参考来源" prop="referenceSource">
            <el-input
              v-model="contributeForm.referenceSource"
              maxlength="500"
              placeholder="教材、机构官网或公开文献，便于评审核实"
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="contributeVisible = false">取消</el-button>
          <el-button type="primary" :loading="contributing" @click="handleContribute">
            提交审核
          </el-button>
        </template>
      </el-dialog>

      <!-- 版本历史抽屉 -->
      <el-drawer v-model="versionsVisible" title="版本历史" size="520px">
        <div v-loading="versionsLoading">
          <el-empty
            v-if="!versionsLoading && versions.length === 0"
            description="这篇文章还没有经过纠错更新"
            :image-size="80"
          />

          <el-timeline v-else>
            <el-timeline-item
              v-for="version in versions"
              :key="version.id"
              :timestamp="version.createdAt"
              placement="top"
            >
              <div class="version-item">
                <strong>第 {{ version.versionNo }} 版</strong>
                <p>{{ version.changeNote || '内容更新' }}</p>
                <span class="version-item__meta">
                  操作人：{{ version.operatorName || '—' }}
                  <template v-if="version.sourceContributionId">
                    · 来源建议 #{{ version.sourceContributionId }}
                  </template>
                </span>
              </div>
            </el-timeline-item>
          </el-timeline>

          <p class="version-note">
            当前页面展示的是第 {{ article?.versionNo }} 版（最新版）。历史上的每一版都在这里留有快照。
          </p>
        </div>
      </el-drawer>

      <!-- 关联创意作品（阶段 5 起） -->
      <section class="article-related">
        <h2 class="article-related__title">相关创意作品</h2>

        <el-empty
          v-if="!article.works?.length"
          description="还没有创作者为这篇文章创作作品"
          :image-size="80"
        />

        <div v-else class="article-related__list">
          <RouterLink
            v-for="work in article.works"
            :key="work.id"
            :to="`/works/${work.id}`"
            class="article-work-card"
          >
            <div v-if="work.coverUrl" class="article-work-card__cover">
              <img :src="work.coverUrl" :alt="work.title" loading="lazy" />
            </div>
            <div class="article-work-card__body">
              <h3>{{ work.title }}</h3>
              <p>{{ work.description }}</p>
              <span class="article-work-card__author">{{ work.creatorName }}</span>
            </div>
          </RouterLink>
        </div>
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
  background: var(--sp-primary-a22);
}

.article-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.article-interaction {
  margin-top: 16px;
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

.article-related__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.article-work-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.article-work-card:hover {
  border-color: var(--sp-primary-a50);
  box-shadow: 0 8px 20px var(--sp-primary-a10);
  transform: translateY(-2px);
}

.article-work-card__cover {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--sp-primary-soft);
}

.article-work-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.article-work-card__body {
  padding: 12px 14px 14px;
}

.article-work-card__body h3 {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--sp-text-strong);
}

.article-work-card__body p {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-work-card__author {
  font-size: 11px;
  color: var(--sp-text-faint);
}

.contribute-tip {
  margin-bottom: 18px;
}

.version-item strong {
  font-size: 14px;
  color: var(--sp-text-strong);
}

.version-item p {
  margin: 6px 0 4px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--sp-text);
}

.version-item__meta {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.version-note {
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px dashed var(--sp-border);
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-faint);
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
