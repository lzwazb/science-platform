<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createComment, deleteComment, listComments } from '@/api/interaction'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], required: true },
})

const emit = defineEmits(['count-change'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const comments = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const content = ref('')
const replyTo = ref(null)
const replyContent = ref('')

async function fetchComments(reset = false) {
  if (reset) {
    page.value = 1
  }
  loading.value = true
  try {
    const data = await listComments({
      targetType: props.targetType,
      targetId: Number(props.targetId),
      page: page.value,
      size: size.value,
    })
    comments.value = data.records || []
    total.value = data.total || 0
    emit('count-change', total.value)
  } catch {
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function requireLogin() {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录后再评论')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function handleSubmit() {
  if (!requireLogin()) return
  const text = content.value.trim()
  if (!text) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  submitting.value = true
  try {
    await createComment({
      targetType: props.targetType,
      targetId: Number(props.targetId),
      content: text,
    })
    content.value = ''
    ElMessage.success('评论已发布')
    fetchComments(true)
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

function startReply(comment) {
  if (!requireLogin()) return
  replyTo.value = replyTo.value?.id === comment.id ? null : comment
  replyContent.value = ''
}

async function handleReply() {
  if (!replyTo.value) return
  const text = replyContent.value.trim()
  if (!text) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  submitting.value = true
  try {
    await createComment({
      targetType: props.targetType,
      targetId: Number(props.targetId),
      content: text,
      parentId: replyTo.value.id,
    })
    replyTo.value = null
    replyContent.value = ''
    ElMessage.success('回复已发布')
    fetchComments(true)
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(comment) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteComment(comment.id)
    ElMessage.success('评论已删除')
    fetchComments(true)
  } catch {
    // 错误已提示
  }
}

function handlePageChange(p) {
  page.value = p
  fetchComments()
}

watch(() => [props.targetType, props.targetId], () => fetchComments(true))
onMounted(() => fetchComments(true))
</script>

<template>
  <section id="comment-section" class="comment-section">
    <header class="comment-section__head">
      <h2>评论 <span class="comment-section__count">{{ total }}</span></h2>
    </header>

    <div class="comment-editor">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        :placeholder="userStore.isLoggedIn ? '说点什么…' : '登录后可发表评论'"
      />
      <div class="comment-editor__footer">
        <el-button type="primary" :loading="submitting" @click="handleSubmit">发表评论</el-button>
      </div>
    </div>

    <div v-loading="loading" class="comment-list">
      <el-empty
        v-if="!loading && comments.length === 0"
        description="还没有评论，来写第一条吧"
        :image-size="80"
      />

      <article v-for="comment in comments" :key="comment.id" class="comment-item">
        <el-avatar :size="34">{{ (comment.userName || '?').charAt(0) }}</el-avatar>

        <div class="comment-item__main">
          <div class="comment-item__head">
            <strong>{{ comment.userName }}</strong>
            <span class="comment-item__time">{{ comment.createdAt }}</span>
          </div>
          <p class="comment-item__content">{{ comment.content }}</p>

          <div class="comment-item__actions">
            <el-button link type="primary" size="small" @click="startReply(comment)">
              {{ replyTo?.id === comment.id ? '取消回复' : '回复' }}
            </el-button>
            <el-button
              v-if="comment.mine || userStore.isAdmin"
              link
              type="danger"
              size="small"
              @click="handleDelete(comment)"
            >
              删除
            </el-button>
          </div>

          <div v-if="replyTo?.id === comment.id" class="comment-reply-editor">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="2"
              maxlength="1000"
              show-word-limit
              :placeholder="`回复 @${comment.userName}`"
            />
            <div class="comment-reply-editor__footer">
              <el-button size="small" @click="replyTo = null">取消</el-button>
              <el-button size="small" type="primary" :loading="submitting" @click="handleReply">
                发布回复
              </el-button>
            </div>
          </div>

          <div v-if="comment.replies?.length" class="comment-replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="comment-reply">
              <strong>{{ reply.userName }}</strong>
              <span class="comment-reply__time">{{ reply.createdAt }}</span>
              <p>{{ reply.content }}</p>
              <el-button
                v-if="reply.mine || userStore.isAdmin"
                link
                type="danger"
                size="small"
                @click="handleDelete(reply)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div v-if="total > size" class="comment-pagination">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<style scoped>
.comment-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--sp-border);
  scroll-margin-top: 90px;
}

.comment-section__head h2 {
  margin: 0 0 14px;
  font-size: 17px;
  color: var(--sp-text-strong);
}

.comment-section__count {
  color: var(--sp-text-faint);
  font-weight: 400;
  font-size: 14px;
}

.comment-editor__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-item__main {
  flex: 1;
  min-width: 0;
}

.comment-item__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.comment-item__head strong {
  font-size: 13px;
  color: var(--sp-text-strong);
}

.comment-item__time {
  font-size: 11px;
  color: var(--sp-text-faint);
}

.comment-item__content {
  margin: 6px 0 8px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--sp-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-item__actions {
  display: flex;
  gap: 4px;
}

.comment-reply-editor {
  margin-top: 10px;
}

.comment-reply-editor__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.comment-replies {
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--sp-bg);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-reply strong {
  font-size: 12px;
  color: var(--sp-text-strong);
}

.comment-reply__time {
  margin-left: 8px;
  font-size: 11px;
  color: var(--sp-text-faint);
}

.comment-reply p {
  margin: 4px 0 2px;
  font-size: 13px;
  line-height: 1.75;
  color: var(--sp-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
