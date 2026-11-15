<script setup>
/**
 * 内容卡片：封面 + 学科色分类 + 标题 + 摘要 + 元信息。
 *
 * 规格来自知识库列表页的 `.kb-card`（第一批重构确立的卡片基线）：
 * 14px 圆角、几乎看不见的边框压住阴影、封面 16:9、
 * hover 时卡片上浮 3px、封面放大 1.045 并叠一层同学科的 soft-light 柔光。
 *
 * 抽成组件的目的是让作品详情、活动详情这类「附带内容区」与知识库用同一套卡片，
 * 而不是各自再写一份相近的样式。
 *
 * 用法：
 *   <ContentCard to="/knowledge/1" :cover="a.coverUrl" :title="a.title"
 *                :summary="a.summary" :category="a.categoryName"
 *                :left="a.authorName" :right="`阅读 ${a.viewCount}`" />
 *
 * 需要覆盖底部信息时用 #footer 插槽；需要在封面上加角标时用 #badge 插槽。
 */
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { themeOf } from '@/constants/dict'

const props = defineProps({
  /** 目标地址，必填 */
  to: { type: [String, Object], required: true },
  cover: { type: String, default: '' },
  title: { type: String, default: '' },
  summary: { type: String, default: '' },
  /** 分类名，用于取学科主题色；匹配不到时回落品牌蓝 */
  category: { type: String, default: '' },
  /** 显式指定主题色，优先于 category */
  categoryColor: { type: String, default: '' },
  /** 与学科并列的中性标签，例如作品形态「科普图文」 */
  tag: { type: String, default: '' },
  /** 元信息行右侧，例如日期或阅读数（左侧固定给学科） */
  metaRight: { type: [String, Number], default: '' },
  /** 底部左/右元信息 */
  left: { type: [String, Number], default: '' },
  right: { type: [String, Number], default: '' },
  /** 摘要行数，1 表示更紧凑（用于次要列表） */
  summaryLines: { type: Number, default: 2 },
})

const catColor = computed(() => props.categoryColor || themeOf(props.category).color)
const fallbackChar = computed(() => (props.title || '?').charAt(0))
const showFooter = computed(() => Boolean(props.left || props.right))
const showMeta = computed(() => Boolean(props.category || props.tag || props.metaRight))
</script>

<template>
  <RouterLink :to="to" class="content-card" :style="{ '--cat': catColor }">
    <div class="content-card__cover">
      <img v-if="cover" :src="cover" :alt="title" loading="lazy" />
      <div v-else class="content-card__cover-fallback">{{ fallbackChar }}</div>
      <slot name="badge" />
    </div>

    <div class="content-card__body">
      <div v-if="showMeta" class="content-card__meta">
        <span class="content-card__meta-left">
          <span v-if="tag" class="content-card__tag">{{ tag }}</span>
          <span v-if="category" class="content-card__cat">{{ category }}</span>
        </span>
        <span v-if="metaRight" class="content-card__meta-right">{{ metaRight }}</span>
      </div>

      <h3 class="content-card__title">{{ title }}</h3>
      <p
        v-if="summary"
        class="content-card__summary"
        :style="{ '-webkit-line-clamp': summaryLines }"
      >
        {{ summary }}
      </p>

      <slot name="footer">
        <div v-if="showFooter" class="content-card__footer">
          <span>{{ left }}</span>
          <span>{{ right }}</span>
        </div>
      </slot>
    </div>
  </RouterLink>
</template>

<style scoped>
.content-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  background: #fff;
  text-decoration: none;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(24, 44, 68, 0.05);
  /* 纯阴影在浅色底上会显得悬浮，补一条几乎看不见的边框把卡片压住 */
  border: 1px solid rgba(24, 44, 68, 0.06);
  transition: box-shadow 0.22s ease, transform 0.22s ease, border-color 0.22s ease;
}

.content-card:hover {
  box-shadow: 0 14px 30px -8px rgba(24, 44, 68, 0.18);
  border-color: rgba(24, 44, 68, 0.1);
  transform: translateY(-3px);
}

.content-card__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--sp-primary-soft);
  overflow: hidden;
}

.content-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.content-card:hover .content-card__cover img {
  transform: scale(1.045);
}

/* 学科色极淡叠色：让同类内容成组，同时不抢画面本身 */
.content-card__cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--cat, var(--sp-primary));
  opacity: 0;
  mix-blend-mode: soft-light;
  transition: opacity 0.22s ease;
  pointer-events: none;
}

.content-card:hover .content-card__cover::after {
  opacity: 0.35;
}

.content-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-size: 32px;
  font-weight: 700;
  color: var(--cat, var(--sp-primary-dark));
}

.content-card__body {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 13px 15px 14px;
  flex: 1;
}

.content-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.content-card__meta-left {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

/* 与学科并列的中性标签：形态这类非学科信息不该抢学科色 */
.content-card__tag {
  padding: 1px 7px;
  border: 1px solid var(--sp-border);
  border-radius: 9px;
  font-size: 11px;
  line-height: 1.5;
  color: var(--sp-text-muted);
  background: #fafcff;
  white-space: nowrap;
}

.content-card__meta-right {
  font-size: 11.5px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.content-card__cat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--cat, var(--sp-primary-dark));
}

.content-card__cat::before {
  content: '';
  width: 3px;
  height: 11px;
  border-radius: 2px;
  background: var(--cat, var(--sp-primary));
}

.content-card__title {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.55;
  letter-spacing: -0.1px;
  color: var(--sp-text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.18s ease;
  /* 两行截断时让两行长度接近，避免第二行只剩孤字 */
  text-wrap: balance;
}

.content-card:hover .content-card__title {
  color: var(--cat, var(--sp-primary-dark));
}

.content-card__summary {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--sp-text-muted);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.content-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
  color: var(--sp-text-faint);
  padding-top: 9px;
  border-top: 1px solid var(--sp-border);
}
</style>
