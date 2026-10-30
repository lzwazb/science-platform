<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  favorite as favoriteApi,
  getInteractionStatus,
  like as likeApi,
  unfavorite as unfavoriteApi,
  unlike as unlikeApi,
} from '@/api/interaction'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], required: true },
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const acting = ref(false)
const liked = ref(false)
const favorited = ref(false)
const likeCount = ref(0)
const favoriteCount = ref(0)
const commentCount = ref(0)

async function fetchStatus() {
  loading.value = true
  try {
    const data = await getInteractionStatus(props.targetType, Number(props.targetId))
    liked.value = data.liked
    favorited.value = data.favorited
    likeCount.value = data.likeCount
    favoriteCount.value = data.favoriteCount
    commentCount.value = data.commentCount
  } catch {
    // 状态获取失败不阻塞页面，按钮保持默认态
  } finally {
    loading.value = false
  }
}

/** 未登录时跳登录并带回跳地址 */
function requireLogin() {
  if (userStore.isLoggedIn) return true
  ElMessage.warning('请先登录')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function toggleLike() {
  if (!requireLogin() || acting.value) return
  acting.value = true
  try {
    if (liked.value) {
      const result = await unlikeApi(props.targetType, Number(props.targetId))
      liked.value = false
      likeCount.value = result.likeCount
    } else {
      const result = await likeApi(props.targetType, Number(props.targetId))
      liked.value = true
      likeCount.value = result.likeCount
    }
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

async function toggleFavorite() {
  if (!requireLogin() || acting.value) return
  acting.value = true
  try {
    if (favorited.value) {
      const result = await unfavoriteApi(props.targetType, Number(props.targetId))
      favorited.value = false
      favoriteCount.value = result.favoriteCount
      ElMessage.success('已取消收藏')
    } else {
      const result = await favoriteApi(props.targetType, Number(props.targetId))
      favorited.value = true
      favoriteCount.value = result.favoriteCount
      ElMessage.success('已收藏，可在「我的收藏」查看')
    }
  } catch {
    // 错误已提示
  } finally {
    acting.value = false
  }
}

function scrollToComments() {
  const el = document.getElementById('comment-section')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

defineExpose({ fetchStatus, refreshComments: (count) => { commentCount.value = count } })

watch(() => [props.targetType, props.targetId], fetchStatus)
onMounted(fetchStatus)
</script>

<template>
  <div class="interaction-bar" v-loading="loading">
    <el-button
      :type="liked ? 'primary' : 'default'"
      :plain="!liked"
      :loading="acting"
      @click="toggleLike"
    >
      {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
    </el-button>

    <el-button
      :type="favorited ? 'warning' : 'default'"
      :plain="!favorited"
      :loading="acting"
      @click="toggleFavorite"
    >
      {{ favorited ? '已收藏' : '收藏' }} {{ favoriteCount }}
    </el-button>

    <el-button @click="scrollToComments">评论 {{ commentCount }}</el-button>

    <span v-if="!userStore.isLoggedIn" class="interaction-bar__hint">
      登录后可点赞、收藏与评论
    </span>
  </div>
</template>

<style scoped>
.interaction-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.interaction-bar__hint {
  font-size: 12px;
  color: var(--sp-text-faint);
}
</style>
