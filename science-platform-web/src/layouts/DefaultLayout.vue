<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <RouterLink to="/" class="brand">
          <span class="brand__mark">科</span>
          <span class="brand__text">
            <strong>科普知识共创与创意传播平台</strong>
            <small>Knowledge Co-creation &amp; Creative Communication</small>
          </span>
        </RouterLink>

        <nav class="app-nav">
          <RouterLink to="/" class="app-nav__item">首页</RouterLink>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 4 开放">知识库</span>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 5 开放">创意作品</span>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 8 开放">主题活动</span>
          <span class="app-nav__item app-nav__item--disabled" title="阶段 3 开放">登录 / 注册</span>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>

    <footer class="app-footer">
      <p>本科毕业设计 · 基于 Spring Boot + Vue 的科普知识共创与创意传播平台的设计与实现</p>
      <p class="app-footer__meta">阶段 2：数据库与基础规范</p>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--sp-border);
}

.app-header__inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--sp-text-strong);
}

.brand__mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--sp-primary) 0%, var(--sp-primary-dark) 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand__text strong {
  font-size: 16px;
  letter-spacing: 0.3px;
}

.brand__text small {
  font-size: 11px;
  color: var(--sp-text-muted);
  letter-spacing: 0.4px;
}

.app-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.app-nav__item {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--sp-text);
  text-decoration: none;
  transition: background 0.18s ease, color 0.18s ease;
}

.app-nav__item:hover {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
}

.app-nav__item.router-link-exact-active {
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-weight: 600;
}

.app-nav__item--disabled {
  color: var(--sp-text-faint);
  cursor: not-allowed;
}

.app-nav__item--disabled:hover {
  background: transparent;
  color: var(--sp-text-faint);
}

.app-main {
  flex: 1;
  width: 100%;
}

.app-footer {
  border-top: 1px solid var(--sp-border);
  padding: 18px 24px 24px;
  text-align: center;
  font-size: 12px;
  color: var(--sp-text-muted);
  background: #fff;
}

.app-footer p {
  margin: 2px 0;
}

.app-footer__meta {
  color: var(--sp-text-faint);
}

@media (max-width: 860px) {
  .app-header__inner {
    height: auto;
    padding: 12px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .app-nav {
    flex-wrap: wrap;
  }

  .brand__text small {
    display: none;
  }
}
</style>
