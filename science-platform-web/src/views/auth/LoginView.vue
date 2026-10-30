<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// 演示账号，方便答辩现场快速切换角色
const demoAccounts = [
  { username: 'user01', label: '普通用户' },
  { username: 'creator01', label: '创作者' },
  { username: 'reviewer01', label: '评审员' },
  { username: 'admin01', label: '管理员' },
  { username: 'superadmin', label: '超级管理员' },
]

function fillDemo(account) {
  form.username = account.username
  form.password = '123456'
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const user = await userStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success(`欢迎回来，${user.nickname}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch {
    // 错误提示已由 request.js 统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-card__head">
        <span class="auth-card__mark">科</span>
        <h1>登录</h1>
        <p>科普知识共创与创意传播平台</p>
      </header>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>

        <el-button
          type="primary"
          class="auth-card__submit"
          :loading="submitting"
          @click="handleSubmit"
        >
          登录
        </el-button>
      </el-form>

      <div class="auth-card__footer">
        <span>还没有账号？</span>
        <RouterLink to="/register">立即注册</RouterLink>
      </div>

      <el-divider content-position="left">
        <span class="auth-card__divider">演示账号（密码均为 123456）</span>
      </el-divider>
      <div class="demo-list">
        <el-tag
          v-for="account in demoAccounts"
          :key="account.username"
          class="demo-list__item"
          type="info"
          effect="plain"
          @click="fillDemo(account)"
        >
          {{ account.username }} · {{ account.label }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 68px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background:
    radial-gradient(circle at 18% 22%, rgba(64, 158, 255, 0.16), transparent 42%),
    radial-gradient(circle at 82% 78%, rgba(64, 158, 255, 0.12), transparent 46%);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 32px 32px 26px;
  background: #fff;
  border: 1px solid var(--sp-border);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(31, 39, 51, 0.07);
}

.auth-card__head {
  text-align: center;
  margin-bottom: 22px;
}

.auth-card__mark {
  display: inline-grid;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--sp-primary) 0%, var(--sp-primary-dark) 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.auth-card__head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.auth-card__head p {
  margin: 0;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.auth-card__submit {
  width: 100%;
  margin-top: 4px;
}

.auth-card__footer {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.auth-card__footer a {
  margin-left: 4px;
  color: var(--sp-primary-dark);
  text-decoration: none;
  font-weight: 600;
}

.auth-card__divider {
  font-size: 12px;
  color: var(--sp-text-faint);
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-list__item {
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease;
}

.demo-list__item:hover {
  border-color: var(--sp-primary);
  color: var(--sp-primary-dark);
}
</style>
