<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateMyProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  avatar: '',
  bio: '',
})

const rules = {
  nickname: [
    { required: true, message: '昵称不能为空', trigger: 'blur' },
    { max: 20, message: '昵称长度不能超过 20 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  bio: [{ max: 500, message: '个人简介不能超过 500 个字符', trigger: 'blur' }],
}

const roleLabels = {
  USER: '普通用户',
  CREATOR: '创作者',
  REVIEWER: '评审员',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

function syncForm(profile) {
  if (!profile) return
  form.nickname = profile.nickname ?? ''
  form.email = profile.email ?? ''
  form.avatar = profile.avatar ?? ''
  form.bio = profile.bio ?? ''
}

watch(() => userStore.profile, syncForm, { immediate: true })

onMounted(async () => {
  loading.value = true
  try {
    await userStore.fetchMe()
  } finally {
    loading.value = false
  }
})

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
    // 后端返回最新资料，直接回写 store，避免再请求一次
    userStore.profile = updated
    ElMessage.success('资料已更新')
  } catch {
    // 错误提示已由 request.js 统一处理
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <el-page-header :icon="null" title="个人中心" content="基本资料" class="profile-page__header" />

    <div class="profile-grid">
      <aside class="profile-card">
        <div class="profile-card__avatar">
          <el-avatar :size="64" :src="userStore.profile?.avatar || undefined">
            {{ userStore.profile?.nickname?.charAt(0) || '?' }}
          </el-avatar>
        </div>
        <h2 class="profile-card__name">{{ userStore.profile?.nickname || '-' }}</h2>
        <p class="profile-card__username">@{{ userStore.profile?.username }}</p>

        <div class="profile-card__roles">
          <el-tag
            v-for="role in userStore.roles"
            :key="role"
            size="small"
            effect="plain"
            type="primary"
          >
            {{ roleLabels[role] || role }}
          </el-tag>
        </div>

        <el-descriptions :column="1" size="small" class="profile-card__meta">
          <el-descriptions-item label="账号状态">
            <el-tag :type="userStore.profile?.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ userStore.profile?.status === 'ACTIVE' ? '正常' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ userStore.profile?.createdAt || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <p class="profile-card__note">
          用户名与角色不可自行修改；如需调整请联系平台管理员。
        </p>
      </aside>

      <section class="profile-form-card">
        <h3 class="profile-form-card__title">编辑资料</h3>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="88px"
          label-position="left"
        >
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="可选，用于接收通知" maxlength="100" />
          </el-form-item>

          <el-form-item label="头像地址" prop="avatar">
            <el-input v-model="form.avatar" placeholder="图片 URL，可留空" maxlength="255" />
          </el-form-item>

          <el-form-item label="个人简介" prop="bio">
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="介绍一下你自己，创作者会在这里展示创作方向"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            <el-button @click="syncForm(userStore.profile)">重置</el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.profile-page__header {
  margin-bottom: 20px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 18px;
  align-items: start;
}

.profile-card,
.profile-form-card {
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  padding: 22px 22px 24px;
}

.profile-card {
  text-align: center;
}

.profile-card__avatar {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.profile-card__name {
  margin: 0 0 4px;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.profile-card__username {
  margin: 0 0 14px;
  font-size: 12px;
  color: var(--sp-text-muted);
}

.profile-card__roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin-bottom: 18px;
}

.profile-card__meta {
  text-align: left;
  border-top: 1px solid var(--sp-border);
  padding-top: 14px;
}

.profile-card__note {
  margin: 16px 0 0;
  padding-top: 14px;
  border-top: 1px dashed var(--sp-border);
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-faint);
  text-align: left;
}

.profile-form-card__title {
  margin: 0 0 18px;
  font-size: 16px;
  color: var(--sp-text-strong);
}

@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
