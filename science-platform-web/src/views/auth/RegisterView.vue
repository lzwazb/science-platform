<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
})

function validateConfirm(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在 3 到 20 个字符之间', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字和下划线',
      trigger: 'blur',
    },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称长度不能超过 20 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6 到 32 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await register({
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim() || undefined,
      password: form.password,
    })
    ElMessage.success('注册成功，请使用新账号登录')
    router.replace({ path: '/login' })
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
        <h1>注册</h1>
        <p>注册后为普通用户，可浏览内容、提交知识贡献与参与互动</p>
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
          <el-input v-model="form.username" placeholder="字母、数字或下划线，3-20 位" autocomplete="username" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示给其他用户的名字" />
        </el-form-item>
        <el-form-item label="邮箱（可选）" prop="email">
          <el-input v-model="form.email" placeholder="用于接收通知，可留空" autocomplete="email" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6-32 位"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-button
          type="primary"
          class="auth-card__submit"
          :loading="submitting"
          @click="handleSubmit"
        >
          注册
        </el-button>
      </el-form>

      <div class="auth-card__footer">
        <span>已有账号？</span>
        <RouterLink to="/login">返回登录</RouterLink>
      </div>

      <p class="auth-card__hint">
        创作者、评审员与管理员账号由平台管理员分配，不开放自助注册。
      </p>
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
    radial-gradient(circle at 18% 22%, var(--sp-primary-a16), transparent 42%),
    radial-gradient(circle at 82% 78%, var(--sp-primary-a12), transparent 46%);
}

.auth-card {
  width: 100%;
  max-width: 440px;
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
  line-height: 1.6;
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

.auth-card__hint {
  margin: 16px 0 0;
  padding-top: 14px;
  border-top: 1px dashed var(--sp-border);
  font-size: 12px;
  line-height: 1.7;
  color: var(--sp-text-faint);
  text-align: center;
}
</style>
