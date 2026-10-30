<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateMyProfile } from '@/api/user'
import { getMyOverview } from '@/api/notification'
import { listMyContributions } from '@/api/contribution'
import { listMyFavorites } from '@/api/interaction'
import { listMySubmissions } from '@/api/event'
import { listMyFeedback } from '@/api/interaction'
import { listMyWorks } from '@/api/work'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const activeTab = ref('overview')

const overview = ref(null)
const overviewLoading = ref(false)

const roleLabels = {
  USER: '普通用户',
  CREATOR: '创作者',
  REVIEWER: '评审员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

const canCreate = computed(() => userStore.hasAnyRole(['CREATOR', 'ADMIN', 'SUPER_ADMIN']))

// ---------------- 概况 ----------------

const statCards = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    { key: 'contribution', label: '知识贡献', value: o.contributionCount, hint: `${o.pendingContributionCount} 条待审核 · ${o.approvedContributionCount} 条已采纳`, to: '/contributions/mine' },
    { key: 'work', label: '创意作品', value: o.workCount, hint: `${o.publishedWorkCount} 件已发布`, to: '/creator/works', visible: canCreate.value },
    { key: 'favorite', label: '收藏', value: o.favoriteCount, hint: '文章与作品', to: '/favorites' },
    { key: 'submission', label: '活动投稿', value: o.submissionCount, hint: '参与过的主题活动', to: '/events' },
    { key: 'feedback', label: '内容反馈', value: o.feedbackCount, hint: '提交的反馈', to: '/notifications', visible: false },
    { key: 'notification', label: '未读消息', value: o.unreadNotificationCount, hint: '待查看', to: '/notifications' },
  ].filter((c) => c.visible !== false)
})

async function fetchOverview() {
  overviewLoading.value = true
  try {
    overview.value = await getMyOverview()
  } catch {
    overview.value = null
  } finally {
    overviewLoading.value = false
  }
}

// ---------------- 各 tab 数据 ----------------

const tabLoading = ref(false)
const contributions = ref([])
const favorites = ref([])
const submissions = ref([])
const feedbacks = ref([])
const works = ref([])
const loaded = reactive({ contribution: false, favorite: false, submission: false, feedback: false, work: false })

const statusMeta = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  WITHDRAWN: { text: '已撤回', type: 'info' },
  DRAFT: { text: '草稿', type: 'info' },
  PUBLISHED: { text: '已发布', type: 'success' },
  OFFLINE: { text: '已下架', type: 'warning' },
  RESOLVED: { text: '已处理', type: 'success' },
  PROCESSING: { text: '处理中', type: 'primary' },
  SUBMITTED: { text: '待评审', type: 'info' },
  REVIEWED: { text: '已评审', type: 'primary' },
  SELECTED: { text: '已获奖', type: 'danger' },
}

const typeLabels = {
  SUPPLEMENT: '补充',
  CORRECTION: '纠错',
  MODIFICATION: '修改建议',
  WRONG_CONTENT: '内容错误',
  COPYRIGHT: '版权问题',
  SPAM: '垃圾信息',
  SUGGESTION: '改进建议',
  OTHER: '其他',
}

const awardLabels = { FIRST: '一等奖', SECOND: '二等奖', THIRD: '三等奖', EXCELLENT: '优秀奖' }

async function loadTab(tab) {
  if (loaded[tab]) return
  tabLoading.value = true
  try {
    if (tab === 'contribution') {
      const data = await listMyContributions({ page: 1, size: 20 })
      contributions.value = data.records || []
    } else if (tab === 'favorite') {
      const data = await listMyFavorites({ page: 1, size: 20 })
      favorites.value = data.records || []
    } else if (tab === 'submission') {
      submissions.value = (await listMySubmissions()) || []
    } else if (tab === 'feedback') {
      const data = await listMyFeedback({ page: 1, size: 20 })
      feedbacks.value = data.records || []
    } else if (tab === 'work') {
      const data = await listMyWorks({ page: 1, size: 20 })
      works.value = data.records || []
    }
    loaded[tab] = true
  } catch {
    // 错误已提示
  } finally {
    tabLoading.value = false
  }
}

function handleTabChange(tab) {
  if (tab !== 'overview' && tab !== 'profile') {
    loadTab(tab)
  }
}

// ---------------- 资料编辑 ----------------

const formRef = ref(null)
const saving = ref(false)
const form = reactive({ nickname: '', email: '', avatar: '', bio: '' })

const profileRules = {
  nickname: [
    { required: true, message: '昵称不能为空', trigger: 'blur' },
    { max: 20, message: '昵称长度不能超过 20 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  bio: [{ max: 500, message: '个人简介不能超过 500 个字符', trigger: 'blur' }],
}

function syncForm() {
  const p = userStore.profile
  if (!p) return
  form.nickname = p.nickname ?? ''
  form.email = p.email ?? ''
  form.avatar = p.avatar ?? ''
  form.bio = p.bio ?? ''
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const updated = await updateMyProfile({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      avatar: form.avatar.trim(),
      bio: form.bio.trim(),
    })
    userStore.profile = updated
    ElMessage.success('资料已更新')
  } catch {
    // 错误已提示
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await userStore.fetchMe()
  syncForm()
  fetchOverview()
})
</script>

<template>
  <div class="profile-page">
    <header class="profile-header">
      <el-avatar :size="56" :src="userStore.profile?.avatar || undefined">
        {{ userStore.profile?.nickname?.charAt(0) || '?' }}
      </el-avatar>
      <div class="profile-header__text">
        <h1>{{ userStore.profile?.nickname || '-' }}</h1>
        <p>@{{ userStore.profile?.username }}</p>
      </div>
      <div class="profile-header__roles">
        <el-tag v-for="role in userStore.roles" :key="role" size="small" effect="plain">
          {{ roleLabels[role] || role }}
        </el-tag>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="profile-tabs" @tab-change="handleTabChange">
      <!-- 概况 -->
      <el-tab-pane label="概况" name="overview">
        <div v-loading="overviewLoading" class="stat-grid">
          <RouterLink
            v-for="card in statCards"
            :key="card.key"
            :to="card.to"
            class="stat-card"
          >
            <span class="stat-card__label">{{ card.label }}</span>
            <strong class="stat-card__value">{{ card.value }}</strong>
            <span class="stat-card__hint">{{ card.hint }}</span>
          </RouterLink>
        </div>

        <p class="profile-note">
          个人中心汇总了你在平台上的各类参与记录。左侧标签可查看明细。
        </p>
      </el-tab-pane>

      <!-- 我的作品 -->
      <el-tab-pane v-if="canCreate" label="我的作品" name="work">
        <div v-loading="tabLoading">
          <el-empty v-if="works.length === 0" description="还没有作品" :image-size="70" />
          <el-table v-else :data="works" border size="small">
            <el-table-column prop="title" label="作品" min-width="200" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
                  {{ statusMeta[row.status]?.text || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="80" />
            <el-table-column label="审核意见" min-width="160">
              <template #default="{ row }">
                <span :class="{ 'profile-muted': !row.reviewComment }">{{ row.reviewComment || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="profile-link">
            <RouterLink to="/creator/works">前往创作者中心管理 →</RouterLink>
          </div>
        </div>
      </el-tab-pane>

      <!-- 我的贡献 -->
      <el-tab-pane label="我的贡献" name="contribution">
        <div v-loading="tabLoading">
          <el-empty v-if="contributions.length === 0" description="还没有提交过知识贡献" :image-size="70" />
          <el-table v-else :data="contributions" border size="small">
            <el-table-column prop="articleTitle" label="目标文章" min-width="200" show-overflow-tooltip />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ typeLabels[row.contributionType] || row.contributionType }}</template>
            </el-table-column>
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
                  {{ statusMeta[row.status]?.text || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核意见" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span :class="{ 'profile-muted': !row.reviewComment }">{{ row.reviewComment || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="profile-link">
            <RouterLink to="/contributions/mine">查看全部贡献记录 →</RouterLink>
          </div>
        </div>
      </el-tab-pane>

      <!-- 我的收藏 -->
      <el-tab-pane label="我的收藏" name="favorite">
        <div v-loading="tabLoading">
          <el-empty v-if="favorites.length === 0" description="还没有收藏" :image-size="70" />
          <el-table v-else :data="favorites" border size="small">
            <el-table-column label="类型" width="80">
              <template #default="{ row }">{{ row.targetType === 'WORK' ? '作品' : '文章' }}</template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="!row.available" size="small" type="info">已下架</el-tag>
                <span v-else class="profile-muted">可访问</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="profile-link">
            <RouterLink to="/favorites">查看全部收藏 →</RouterLink>
          </div>
        </div>
      </el-tab-pane>

      <!-- 我的投稿 -->
      <el-tab-pane label="我的投稿" name="submission">
        <div v-loading="tabLoading">
          <el-empty v-if="submissions.length === 0" description="还没有参加活动" :image-size="70" />
          <el-table v-else :data="submissions" border size="small">
            <el-table-column prop="eventTitle" label="活动" min-width="180" show-overflow-tooltip />
            <el-table-column prop="workTitle" label="作品" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
                  {{ statusMeta[row.status]?.text || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="平均分" width="90">
              <template #default="{ row }">{{ row.averageScore ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="获奖" width="96">
              <template #default="{ row }">{{ awardLabels[row.awardLevel] || '—' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 我的反馈 -->
      <el-tab-pane label="我的反馈" name="feedback">
        <div v-loading="tabLoading">
          <el-empty v-if="feedbacks.length === 0" description="还没有提交过反馈" :image-size="70" />
          <el-table v-else :data="feedbacks" border size="small">
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ typeLabels[row.feedbackType] || row.feedbackType }}</template>
            </el-table-column>
            <el-table-column prop="targetTitle" label="目标内容" min-width="180" show-overflow-tooltip />
            <el-table-column prop="description" label="反馈说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <el-tag size="small" :type="statusMeta[row.status]?.type || 'info'">
                  {{ statusMeta[row.status]?.text || row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 资料编辑 -->
      <el-tab-pane label="编辑资料" name="profile">
        <el-form
          ref="formRef"
          :model="form"
          :rules="profileRules"
          label-width="88px"
          class="profile-form"
        >
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="100" placeholder="可选，用于接收通知" />
          </el-form-item>
          <el-form-item label="头像地址" prop="avatar">
            <el-input v-model="form.avatar" maxlength="255" placeholder="图片 URL，可留空" />
          </el-form-item>
          <el-form-item label="个人简介" prop="bio">
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="创作者会在这里展示创作方向"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            <el-button @click="syncForm">重置</el-button>
          </el-form-item>
        </el-form>

        <p class="profile-note">
          用户名与角色不可自行修改；如需调整请联系平台管理员。
        </p>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 52px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 24px;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  margin-bottom: 18px;
}

.profile-header__text h1 {
  margin: 0 0 4px;
  font-size: 20px;
  color: var(--sp-text-strong);
}

.profile-header__text p {
  margin: 0;
  font-size: 12px;
  color: var(--sp-text-muted);
}

.profile-header__roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-left: auto;
}

.profile-tabs {
  padding: 0 4px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.stat-card:hover {
  border-color: rgba(64, 158, 255, 0.5);
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

.stat-card__label {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.stat-card__value {
  font-size: 26px;
  color: var(--sp-text-strong);
  line-height: 1.1;
}

.stat-card__hint {
  font-size: 11px;
  color: var(--sp-text-faint);
}

.profile-note {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-faint);
}

.profile-muted {
  color: var(--sp-text-faint);
}

.profile-link {
  margin-top: 12px;
  font-size: 13px;
}

.profile-link a {
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.profile-link a:hover {
  text-decoration: underline;
}

.profile-form {
  max-width: 560px;
}
</style>
