<script setup>
import { onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'

/**
 * 首页（开发档案附录 A 要求）：
 * 显示项目名称、后端连接状态和简单的功能导航占位。
 *
 * 后端状态通过真实调用 GET /api/v1/health 获得，不使用静态假数据。
 */
const loading = ref(true)
const connected = ref(false)
const health = ref(null)
const errorMessage = ref('')
const checkedAt = ref('')

// 七个核心模块（开发档案 1.3 / 第 14.2 节），此处仅作导航占位，随阶段逐步开放
const modules = [
  { name: '科普知识库与专题探索', detail: '分类、文章详情、关键词搜索、专题浏览', stage: '阶段 4' },
  { name: '创作者中心与作品管理', detail: '作品发布、编辑、分类与知识文章关联', stage: '阶段 5' },
  { name: '知识共建与纠错', detail: '提交补充/纠错建议、人工审核、历史版本', stage: '阶段 6' },
  { name: '社区互动', detail: '评论、点赞、收藏与内容反馈', stage: '阶段 7' },
  { name: '主题创作活动', detail: '活动发布、作品投稿、人工评审与优秀作品展示', stage: '阶段 8' },
  { name: '消息通知与个人中心', detail: '个人资料、我的作品/贡献/收藏、站内消息', stage: '阶段 9' },
  { name: '后台管理与系统管理', detail: '分类、文章、活动、评论反馈与角色权限管理', stage: '后续阶段' },
]

async function checkBackend() {
  loading.value = true
  errorMessage.value = ''
  try {
    health.value = await getHealth()
    connected.value = true
  } catch (error) {
    connected.value = false
    health.value = null
    errorMessage.value =
      error?.response?.status === undefined
        ? '无法连接后端服务，请确认 Spring Boot 已在 8080 端口启动'
        : `后端返回异常状态：${error.response.status}`
  } finally {
    loading.value = false
    checkedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }
}

onMounted(checkBackend)
</script>

<template>
  <div class="home">
    <section class="hero">
      <p class="hero__eyebrow">本科毕业设计 · 项目骨架已就绪</p>
      <h1 class="hero__title">科普知识共创与创意传播平台</h1>
      <p class="hero__subtitle">
        让科普知识在阅读、共创、创作与传播之间流动起来：用户既阅读经过核对的科普文章，也能提交补充与纠错建议，
        创作与文章相关的创意作品，并参与主题活动。
      </p>
      <p class="hero__stack">
        Spring Boot 3.5 · Java 21 · Maven · MyBatis-Plus · MySQL 8 · Vue 3 · Vite · Element Plus · Axios
      </p>
    </section>

    <section class="status-card">
      <div class="status-card__head">
        <h2>前后端连通性</h2>
        <el-button size="small" :loading="loading" @click="checkBackend">重新检测</el-button>
      </div>

      <el-alert
        v-if="!loading && connected"
        type="success"
        :closable="false"
        show-icon
        title="后端连接正常"
        :description="`接口 GET /api/v1/health 调用成功，服务端时间 ${health?.serverTime || '-'}`"
      />
      <el-alert
        v-else-if="!loading && !connected"
        type="error"
        :closable="false"
        show-icon
        title="后端未连接"
        :description="errorMessage"
      />
      <div v-else class="status-card__loading">正在检测后端连接……</div>

      <el-descriptions
        v-if="connected && health"
        class="status-card__detail"
        :column="2"
        border
        size="small"
      >
        <el-descriptions-item label="服务状态">{{ health.status }}</el-descriptions-item>
        <el-descriptions-item label="应用名称">{{ health.application }}</el-descriptions-item>
        <el-descriptions-item label="当前阶段">{{ health.stage }}</el-descriptions-item>
        <el-descriptions-item label="检测时间">{{ checkedAt }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="modules">
      <div class="modules__head">
        <h2>核心功能模块</h2>
        <p>以下为开发档案确定的七个模块，正在按下表阶段逐个实现，当前尚未开放入口。</p>
      </div>

      <div class="modules__grid">
        <article v-for="item in modules" :key="item.name" class="module-card">
          <div class="module-card__top">
            <h3>{{ item.name }}</h3>
            <el-tag size="small" type="info" effect="plain">{{ item.stage }}</el-tag>
          </div>
          <p class="module-card__detail">{{ item.detail }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.hero {
  padding: 40px 36px;
  border-radius: 18px;
  background:
    radial-gradient(circle at 82% 18%, rgba(64, 158, 255, 0.18), transparent 46%),
    linear-gradient(135deg, #f4f9ff 0%, #ffffff 62%);
  border: 1px solid var(--sp-border);
}

.hero__eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 1.6px;
  color: var(--sp-primary-dark);
  font-weight: 600;
}

.hero__title {
  margin: 0 0 14px;
  font-size: 32px;
  line-height: 1.3;
  color: var(--sp-text-strong);
  letter-spacing: 0.5px;
}

.hero__subtitle {
  margin: 0 0 16px;
  max-width: 780px;
  font-size: 14px;
  line-height: 1.85;
  color: var(--sp-text);
}

.hero__stack {
  margin: 0;
  font-size: 12px;
  color: var(--sp-text-muted);
  letter-spacing: 0.2px;
}

.status-card {
  margin-top: 24px;
  padding: 22px 24px;
  border: 1px solid var(--sp-border);
  border-radius: 14px;
  background: #fff;
}

.status-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.status-card__head h2 {
  margin: 0;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.status-card__loading {
  font-size: 13px;
  color: var(--sp-text-muted);
}

.status-card__detail {
  margin-top: 16px;
}

.modules {
  margin-top: 32px;
}

.modules__head h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--sp-text-strong);
}

.modules__head p {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--sp-text-muted);
}

.modules__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.module-card {
  padding: 16px 18px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.module-card:hover {
  border-color: rgba(64, 158, 255, 0.45);
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.1);
}

.module-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.module-card__top h3 {
  margin: 0;
  font-size: 15px;
  color: var(--sp-text-strong);
}

.module-card__detail {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--sp-text-muted);
}
</style>
