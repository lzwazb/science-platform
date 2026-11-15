<script setup>
/**
 * 空态引导：不是一个图标加「暂无数据」，而是告诉用户这里本该有什么、下一步做什么。
 *
 * 用法：
 *   <EmptyGuide title="还没有作品"
 *               description="任何登录用户都可以创作……"
 *               action-text="去创作者中心"
 *               action-to="/creator/works" />
 *
 * 需要自定义操作时用 #action 插槽。
 */
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  actionText: { type: String, default: '' },
  actionTo: { type: [String, Object], default: '' },
})
</script>

<template>
  <div class="empty-guide">
    <div class="empty-guide__mark" aria-hidden="true">
      <slot name="icon">+</slot>
    </div>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div class="empty-guide__actions">
      <slot name="action">
        <RouterLink v-if="actionTo && actionText" :to="actionTo">
          <el-button type="primary" plain>{{ actionText }}</el-button>
        </RouterLink>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.empty-guide {
  padding: 40px 24px 44px;
  border: 1px dashed var(--sp-border);
  border-radius: 14px;
  background: #fbfdff;
  text-align: center;
}

.empty-guide__mark {
  width: 40px;
  height: 40px;
  margin: 0 auto 14px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--sp-primary-soft);
  color: var(--sp-primary-dark);
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
}

.empty-guide h3 {
  margin: 0 0 8px;
  font-size: 15px;
  color: var(--sp-text-strong);
}

.empty-guide p {
  margin: 0 auto;
  max-width: 460px;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.empty-guide__actions {
  margin-top: 16px;
}

.empty-guide__actions:empty {
  display: none;
}

.empty-guide__actions a {
  text-decoration: none;
}
</style>
