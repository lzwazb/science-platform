<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notification'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const onlyUnread = ref(false)

const typeMeta = {
  REVIEW_RESULT: { label: '审核结果', type: 'success' },
  COMMENT_REPLY: { label: '评论回复', type: 'primary' },
  EVENT_NOTICE: { label: '活动通知', type: 'warning' },
  SYSTEM: { label: '系统消息', type: 'info' },
}

const unreadCount = computed(() => rows.value.filter((n) => !n.read).length)

async function fetchRows() {
  loading.value = true
  try {
    const data = await listNotifications({
      page: page.value,
      size: size.value,
      onlyUnread: onlyUnread.value,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleRead(item) {
  if (item.read) return
  try {
    await markNotificationRead(item.id)
    item.read = true
  } catch {
    // 错误已提示
  }
}

async function handleOpen(item) {
  await handleRead(item)
  if (item.linkPath) {
    router.push(item.linkPath)
  }
}

async function handleReadAll() {
  try {
    await markAllNotificationsRead()
    ElMessage.success('已全部标记为已读')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

function handleFilterChange() {
  page.value = 1
  fetchRows()
}

onMounted(fetchRows)
</script>

<template>
  <div class="notify-page">
    <header class="notify-head">
      <div>
        <h1>站内消息</h1>
        <p>
          审核结果、评论回复与活动通知都会送到这里。点击消息可直接跳转到对应的业务页面。
        </p>
      </div>
      <div class="notify-head__actions">
        <el-tag v-if="unreadCount > 0" type="danger" effect="plain">
          本页未读 {{ unreadCount }}
        </el-tag>
        <el-button size="small" @click="handleReadAll">全部已读</el-button>
        <el-button size="small" @click="fetchRows">刷新</el-button>
      </div>
    </header>

    <div class="notify-toolbar">
      <el-radio-group v-model="onlyUnread" @change="handleFilterChange">
        <el-radio-button :value="false">全部</el-radio-button>
        <el-radio-button :value="true">仅未读</el-radio-button>
      </el-radio-group>
      <span class="notify-toolbar__hint">共 {{ total }} 条</span>
    </div>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && rows.length === 0"
        :description="onlyUnread ? '没有未读消息' : '还没有收到任何消息'"
      />

      <ul v-else class="notify-list">
        <li
          v-for="item in rows"
          :key="item.id"
          class="notify-item"
          :class="{ 'notify-item--unread': !item.read }"
        >
          <span class="notify-item__dot" :class="{ 'notify-item__dot--on': !item.read }" />

          <div class="notify-item__main">
            <div class="notify-item__head">
              <el-tag size="small" :type="typeMeta[item.type]?.type || 'info'" effect="plain">
                {{ item.typeLabel || typeMeta[item.type]?.label || '消息' }}
              </el-tag>
              <strong>{{ item.title }}</strong>
              <span class="notify-item__time">{{ item.createdAt }}</span>
            </div>
            <p class="notify-item__content">{{ item.content }}</p>
            <div class="notify-item__actions">
              <el-button
                v-if="item.linkPath"
                link
                type="primary"
                size="small"
                @click="handleOpen(item)"
              >
                查看详情
              </el-button>
              <el-button v-if="!item.read" link size="small" @click="handleRead(item)">
                标记已读
              </el-button>
              <span v-else class="notify-item__read">已读</span>
            </div>
          </div>
        </li>
      </ul>
    </div>

    <div v-if="!loading && total > size" class="notify-pagination">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="(p) => { page = p; fetchRows() }"
      />
    </div>
  </div>
</template>

<style scoped>
.notify-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 28px 24px 52px;
}

.notify-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.notify-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.notify-head p {
  margin: 0;
  max-width: 560px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.notify-head__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.notify-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.notify-toolbar__hint {
  font-size: 12px;
  color: var(--sp-text-muted);
}

.notify-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notify-item {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.notify-item--unread {
  border-color: rgba(64, 158, 255, 0.4);
  background: #f8fbff;
}

.notify-item:hover {
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.08);
}

.notify-item__dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: transparent;
}

.notify-item__dot--on {
  background: var(--sp-primary);
}

.notify-item__main {
  flex: 1;
  min-width: 0;
}

.notify-item__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.notify-item__head strong {
  font-size: 14px;
  color: var(--sp-text-strong);
}

.notify-item__time {
  margin-left: auto;
  font-size: 11px;
  color: var(--sp-text-faint);
}

.notify-item__content {
  margin: 8px 0 6px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--sp-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.notify-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notify-item__read {
  font-size: 11px;
  color: var(--sp-text-faint);
}

.notify-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
