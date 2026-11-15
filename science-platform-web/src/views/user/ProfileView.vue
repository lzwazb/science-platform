<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateMyProfile } from '@/api/user'
import { getMyOverview } from '@/api/notification'
import { listMySubmissions } from '@/api/event'
import { listMyFeedback } from '@/api/interaction'
import { listMyWorks } from '@/api/work'
import { useUserStore } from '@/stores/user'
import { STATUS_META } from '@/constants/dict'
import EmptyGuide from '@/components/content/EmptyGuide.vue'

const userStore = useUserStore()

/** 概况已经折进顶部，默认落在「我的作品」 */
const activeTab = ref('work')

const overview = ref(null)
const overviewLoading = ref(false)

const roleLabels = {
  USER: '普通用户',
  CREATOR: '创作者',
  REVIEWER: '评审员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

// 创作不设角色门槛：任何登录用户都可以有作品，作品能否公开由审核决定

// ---------------- 顶部指标 ----------------

/**
 * 四个最常看的数字放在头像右侧的一行里，替代原来独立的「概况」tab。
 * 每个指标都能点进去看明细。
 */
const headerStats = computed(() => {
  const o = overview.value
  if (!o) return []
  return [
    {
      key: 'contribution',
      label: '知识贡献',
      value: o.contributionCount,
      hint: `${o.pendingContributionCount} 待审 / ${o.approvedContributionCount} 采纳`,
      to: '/contributions/mine',
    },
    {
      key: 'work',
      label: '创意作品',
      value: o.workCount,
      hint: `${o.publishedWorkCount} 已发布`,
      to: '/creator/works',
    },
    { key: 'favorite', label: '收藏', value: o.favoriteCount, hint: '文章与作品', to: '/favorites' },
    {
      key: 'submission',
      label: '活动投稿',
      value: o.submissionCount,
      hint: '参与过的活动',
      to: '/events',
    },
  ]
})

const unreadCount = computed(() => overview.value?.unreadNotificationCount ?? 0)

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
const submissions = ref([])
const feedbacks = ref([])
const works = ref([])
const loaded = reactive({ submission: false, feedback: false, work: false })

const statusMeta = STATUS_META

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

/** 打开反馈指向的内容（新标签，不打断当前页） */
function openTarget(row) {
  const path = row.linkType === 'WORK' ? `/works/${row.linkId}` : `/knowledge/${row.linkId}`
  window.open(path, '_blank', 'noopener')
}

async function loadTab(tab) {
  if (loaded[tab]) return
  tabLoading.value = true
  try {
    if (tab === 'submission') {
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
  if (tab !== 'profile') {
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
  loadTab('work')
})
</script>

<template>
  <div class="profile-page">
    <!-- 身份 + 四个数字放一行：概况不再单独占一个 tab -->
    <header class="profile-head" v-loading="overviewLoading">
      <div class="profile-head__identity">
        <el-avatar :size="52" :src="userStore.profile?.avatar || undefined">
          {{ userStore.profile?.nickname?.charAt(0) || '?' }}
        </el-avatar>
        <div class="profile-head__text">
          <h1>{{ userStore.profile?.nickname || '-' }}</h1>
          <p>
            @{{ userStore.profile?.username }}
            <template v-if="userStore.profile?.bio">
              <span class="profile-head__dot">·</span>{{ userStore.profile.bio }}
            </template>
          </p>
        </div>
        <div class="profile-head__roles">
          <span v-for="role in userStore.roles" :key="role" class="role-chip">
            {{ roleLabels[role] || role }}
          </span>
        </div>
      </div>

      <div class="profile-head__stats">
        <RouterLink
          v-for="item in headerStats"
          :key="item.key"
          :to="item.to"
          class="stat"
          :title="item.hint"
        >
          <strong>{{ item.value ?? 0 }}</strong>
          <span>{{ item.label }}</span>
        </RouterLink>

        <RouterLink to="/notifications" class="stat stat--notice">
          <strong>{{ unreadCount }}</strong>
          <span>未读消息</span>
        </RouterLink>
      </div>

      <div class="profile-head__links">
        <RouterLink to="/contributions/mine">我的贡献</RouterLink>
        <span>·</span>
        <RouterLink to="/favorites">我的收藏</RouterLink>
        <span>·</span>
        <RouterLink to="/notifications">站内消息</RouterLink>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="profile-tabs" @tab-change="handleTabChange">
      <!-- 我的作品 -->
      <el-tab-pane label="我的作品" name="work">
        <div v-loading="tabLoading">
          <EmptyGuide
            v-if="works.length === 0"
            title="还没有作品"
            description="任何登录用户都可以创作。把科普知识做成图文、海报、漫画或短视频，提交审核通过后就会出现在作品广场。"
            action-text="去创作者中心"
            action-to="/creator/works"
          />

          <template v-else>
            <el-table :data="works" size="small">
              <el-table-column prop="title" label="作品" min-width="220" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <span class="status-pill" :class="`status-pill--${row.status?.toLowerCase()}`">
                    {{ statusMeta[row.status]?.text || row.status }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="viewCount" label="浏览" width="80" align="right" />
              <el-table-column label="审核意见" min-width="180">
                <template #default="{ row }">
                  <span class="muted">{{ row.reviewComment || '—' }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="tab-foot">
              <RouterLink to="/creator/works">前往创作者中心管理 →</RouterLink>
            </div>
          </template>
        </div>
      </el-tab-pane>

      <!-- 我的投稿 -->
      <el-tab-pane label="我的投稿" name="submission">
        <div v-loading="tabLoading">
          <EmptyGuide
            v-if="submissions.length === 0"
            title="还没有参加过活动"
            description="主题活动会按学期发起。活动期间可以把已发布的作品投进去，评审员打分，结束后公示优秀作品。"
            action-text="看看进行中的活动"
            action-to="/events"
          />

          <el-table v-else :data="submissions" size="small">
            <el-table-column prop="eventTitle" label="活动" min-width="180" show-overflow-tooltip />
            <el-table-column prop="workTitle" label="作品" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <span class="status-pill" :class="`status-pill--${row.status?.toLowerCase()}`">
                  {{ statusMeta[row.status]?.text || row.status }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="平均分" width="90" align="right">
              <template #default="{ row }">{{ row.averageScore ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="获奖" width="96">
              <template #default="{ row }">
                <span v-if="row.awardLevel" class="award-chip">
                  {{ awardLabels[row.awardLevel] || row.awardLevel }}
                </span>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 我的反馈 -->
      <el-tab-pane label="我的反馈" name="feedback">
        <div v-loading="tabLoading">
          <EmptyGuide
            v-if="feedbacks.length === 0"
            title="还没有提交过反馈"
            description="读到不准确的内容，可以在文章详情页提交纠错建议；评论或作品有问题，可以在对应页面提交内容反馈。"
            action-text="去知识库逛逛"
            action-to="/knowledge"
          />

          <el-table v-else :data="feedbacks" size="small">
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                {{ typeLabels[row.feedbackType] || row.feedbackType }}
              </template>
            </el-table-column>
            <el-table-column label="目标内容" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <!-- linkType/linkId 由后端给：评论类反馈会回溯到所属文章或作品 -->
                <el-link
                  v-if="row.linkType && row.linkId"
                  type="primary"
                  :underline="false"
                  @click="openTarget(row)"
                >
                  {{ row.targetTitle }}
                </el-link>
                <span v-else>{{ row.targetTitle }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="description"
              label="反馈说明"
              min-width="220"
              show-overflow-tooltip
            />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <span class="status-pill" :class="`status-pill--${row.status?.toLowerCase()}`">
                  {{ statusMeta[row.status]?.text || row.status }}
                </span>
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

        <p class="profile-note">用户名与角色不可自行修改；如需调整请联系平台管理员。</p>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: var(--sp-container-narrow);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

/* ---------------- 顶部：身份 + 指标一行 ---------------- */

.profile-head {
  padding: 20px 24px 16px;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
  margin-bottom: 14px;
}

.profile-head__identity {
  display: flex;
  align-items: center;
  gap: 14px;
}

.profile-head__text {
  min-width: 0;
}

.profile-head__text h1 {
  margin: 0 0 4px;
  font-size: 19px;
  color: var(--sp-text-strong);
}

.profile-head__text p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--sp-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 460px;
}

.profile-head__dot {
  margin: 0 6px;
  color: var(--sp-border);
}

.profile-head__roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-left: auto;
  flex-shrink: 0;
}

.role-chip {
  padding: 2px 9px;
  border-radius: 10px;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-size: 11.5px;
}

/* 四个数字排成一行，替代原来独立的「概况」tab */
.profile-head__stats {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 0;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--sp-border);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1 1 0;
  min-width: 96px;
  padding: 2px 14px;
  border-left: 1px solid var(--sp-border);
  text-decoration: none;
  transition: background 0.18s ease;
}

.stat:first-child {
  padding-left: 0;
  border-left: 0;
}

.stat:hover {
  background: #f7fafd;
}

.stat strong {
  font-size: 20px;
  line-height: 1.2;
  color: var(--sp-text-strong);
  font-variant-numeric: tabular-nums;
}

.stat span {
  font-size: 11.5px;
  color: var(--sp-text-faint);
}

.stat--notice strong {
  color: var(--sp-primary-dark);
}

.profile-head__links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--sp-border);
}

.profile-head__links a {
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.profile-head__links a:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ---------------- tab 内容 ---------------- */

.profile-tabs {
  padding: 0 4px;
}

/* 三张表统一：浅表头、无竖线、行高一致，不再用 el-table 默认外观 */
.profile-tabs :deep(.el-table) {
  --el-table-border-color: var(--sp-border);
  --el-table-header-bg-color: #f7fafd;
  font-size: 13px;
}

.profile-tabs :deep(.el-table th.el-table__cell) {
  background: #f7fafd;
  color: var(--sp-text-muted);
  font-weight: 600;
  font-size: 12px;
}

.profile-tabs :deep(.el-table td.el-table__cell) {
  padding: 9px 0;
  color: var(--sp-text);
}

.profile-tabs :deep(.el-table .cell) {
  line-height: 1.6;
}

/* 去掉表格底部那条多余的分割线 */
.profile-tabs :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.profile-tabs :deep(.el-table__row:hover > td.el-table__cell) {
  background: #f7fafd;
}

.status-pill {
  display: inline-block;
  padding: 1px 9px;
  border-radius: 10px;
  font-size: 11.5px;
  font-weight: 600;
  background: rgba(90, 106, 128, 0.1);
  color: #53627a;
}

.status-pill--published {
  background: rgba(38, 161, 105, 0.12);
  color: #1f7a4d;
}

.status-pill--pending,
.status-pill--processing {
  background: rgba(209, 143, 40, 0.14);
  color: #8a5f14;
}

.status-pill--rejected,
.status-pill--disabled {
  background: rgba(224, 100, 92, 0.14);
  color: #a8382f;
}

.status-pill--draft,
.status-pill--offline,
.status-pill--resolved {
  background: rgba(90, 106, 128, 0.1);
  color: #53627a;
}

.award-chip {
  padding: 1px 8px;
  border-radius: 9px;
  background: linear-gradient(135deg, #e0a92b, #c8860f);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.muted {
  color: var(--sp-text-faint);
}

.tab-foot {
  margin-top: 12px;
  font-size: 13px;
}

.tab-foot a {
  color: var(--sp-primary-dark);
  text-decoration: none;
}

.tab-foot a:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.profile-note {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-faint);
}

.profile-form {
  max-width: 560px;
}

@media (max-width: 768px) {
  .profile-page {
    padding: var(--sp-gap) 16px var(--sp-gap-lg);
  }

  .profile-head__text p {
    white-space: normal;
  }

  /* 窄屏两列网格：flex-wrap 会让第三个之后的奇数列仍带着左侧分隔线 */
  .profile-head__stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 0;
  }

  .stat {
    min-width: 0;
    padding: 0 12px;
    border-left: 1px solid var(--sp-border);
  }

  .stat:nth-child(odd) {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
