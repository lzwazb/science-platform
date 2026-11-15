<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createWork,
  deleteWork,
  getMyWork,
  listMyWorks,
  submitWork,
  updateWork,
} from '@/api/work'
import { listCategories } from '@/api/category'
import { STATUS_META, themeOf } from '@/constants/dict'
import { formatDate } from '@/utils/datetime'
import EmptyGuide from '@/components/content/EmptyGuide.vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const categories = ref([])
const knowledgeCategories = ref([])

const statusMeta = STATUS_META

/**
 * 作品按状态分组展示，不再混在一张表里。
 *
 * 一次取回本人的全部作品（上限 100）再分组：作品数量本来就不大，
 * 而「按状态分组」和「服务端分页」是冲突的——翻页后每组会缺条目。
 * 超过 100 件时这里只显示前 100 件，需要真分页再改成按组查询。
 */
const GROUP_ORDER = ['DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'OFFLINE']

const groups = computed(() =>
  GROUP_ORDER.map((status) => ({
    status,
    label: statusMeta[status]?.text || status,
    items: rows.value.filter((row) => row.status === status),
  })).filter((group) => group.items.length > 0),
)

const statusSummary = computed(() =>
  GROUP_ORDER.map((status) => ({
    status,
    label: statusMeta[status]?.text || status,
    count: rows.value.filter((row) => row.status === status).length,
  })).filter((item) => item.count > 0),
)

// ---------------- 编辑表单 ----------------

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const form = reactive({
  title: '',
  description: '',
  content: '',
  coverUrl: '',
  categoryId: null,
  knowledgeCategoryId: null,
})

const rules = {
  title: [
    { required: true, message: '请输入作品标题', trigger: 'blur' },
    { max: 150, message: '标题不能超过 150 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入作品内容或说明', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择作品分类', trigger: 'change' }],
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建作品' : '编辑作品'))

async function fetchRows() {
  loading.value = true
  try {
    const data = await listMyWorks({ page: 1, size: 100 })
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchOptions() {
  try {
    // 作品形态分类（图文 / 海报 / 漫画 / 短视频）与知识分类（学科）是两套：
    // 前者是必填的作品形态，后者是选填的学科归属，一起并发取回
    const [cats, knowledgeCats] = await Promise.all([
      listCategories({ type: 'WORK' }),
      listCategories({ type: 'ARTICLE' }),
    ])
    categories.value = cats
    knowledgeCategories.value = knowledgeCats || []
  } catch {
    categories.value = []
    knowledgeCategories.value = []
  }
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.content = ''
  form.coverUrl = ''
  form.categoryId = null
  form.knowledgeCategoryId = null
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  try {
    const detail = await getMyWork(row.id)
    editingId.value = detail.id
    form.title = detail.title
    form.description = detail.description || ''
    form.content = detail.content
    form.coverUrl = detail.coverUrl || ''
    form.categoryId = detail.categoryId
    form.knowledgeCategoryId = detail.knowledgeCategoryId || null
    dialogVisible.value = true
  } catch {
    // 错误已提示
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      content: form.content,
      coverUrl: form.coverUrl.trim(),
      categoryId: form.categoryId,
      knowledgeCategoryId: form.knowledgeCategoryId,
    }
    if (dialogMode.value === 'create') {
      await createWork(payload)
      ElMessage.success('作品已保存为草稿，可在列表中提交审核')
    } else {
      const result = await updateWork(editingId.value, payload)
      if (result.status === 'PENDING') {
        ElMessage.warning('作品已修改并自动重新送审，等待评审员处理')
      } else {
        ElMessage.success('作品已更新')
      }
    }
    dialogVisible.value = false
    fetchRows()
  } catch {
    // 错误已提示
  } finally {
    submitting.value = false
  }
}

async function handleSubmitReview(row) {
  try {
    await ElMessageBox.confirm(
      `确定提交《${row.title}》进入审核吗？提交后需等待评审员处理。`,
      '提交审核',
      { confirmButtonText: '提交', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  try {
    await submitWork(row.id)
    ElMessage.success('已提交审核')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除《${row.title}》吗？已被活动投稿引用的作品无法删除。`,
      '删除作品',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteWork(row.id)
    ElMessage.success('作品已删除')
    fetchRows()
  } catch {
    // 错误已提示
  }
}

onMounted(() => {
  fetchOptions()
  fetchRows()
})
</script>

<template>
  <div class="creator-page">
    <header class="creator-head">
      <div>
        <h1>创作者中心</h1>
        <p>
          作品保存后先进入草稿，提交审核通过才会公开。
          <strong>已发布的作品被编辑后会自动重新送审</strong>，这样前台内容始终经过审核。
        </p>
      </div>
      <div class="creator-head__actions">
        <RouterLink to="/works">
          <el-button plain>查看作品广场</el-button>
        </RouterLink>
        <el-button v-if="rows.length" type="primary" @click="openCreate">新建作品</el-button>
      </div>
    </header>

    <div v-loading="loading">
      <!-- 空态：新建入口放在页面中部，而不是让人找右上角按钮 -->
      <EmptyGuide
        v-if="!loading && rows.length === 0"
        title="还没有创作过作品"
        description="把科普知识做成图文、海报、漫画或短视频。保存后是草稿，提交审核通过就会出现在作品广场，也可以用来参加主题活动。"
      >
        <template #action>
          <el-button type="primary" @click="openCreate">新建第一件作品</el-button>
        </template>
      </EmptyGuide>

      <template v-else-if="rows.length">
        <div class="creator-summary">
          <span class="creator-summary__total">共 {{ total }} 件</span>
          <span
            v-for="item in statusSummary"
            :key="item.status"
            class="creator-summary__item"
            :class="`creator-summary__item--${item.status.toLowerCase()}`"
          >
            {{ item.label }} <strong>{{ item.count }}</strong>
          </span>
          <span class="creator-summary__note">按状态分组，最近编辑的在前</span>
        </div>

        <section v-for="group in groups" :key="group.status" class="creator-group">
          <h2 class="creator-group__title">
            {{ group.label }}
            <span>{{ group.items.length }}</span>
          </h2>

          <div class="creator-list">
            <article v-for="row in group.items" :key="row.id" class="work-row">
              <div class="work-row__cover">
                <img v-if="row.coverUrl" :src="row.coverUrl" :alt="row.title" />
                <span v-else>{{ row.title.charAt(0) }}</span>
              </div>

              <div class="work-row__main">
                <h3>
                  {{ row.title }}
                  <span
                    v-if="row.knowledgeCategoryName"
                    class="work-row__cat"
                    :style="{ '--cat': themeOf(row.knowledgeCategoryName).color }"
                  >
                    {{ row.knowledgeCategoryName }}
                  </span>
                </h3>
                <p class="work-row__desc">{{ row.description || '还没有填写简介' }}</p>
                <p v-if="row.reviewComment" class="work-row__review">
                  <span class="work-row__review-label">审核意见</span>{{ row.reviewComment }}
                </p>
              </div>

              <div class="work-row__side">
                <span class="work-row__date">更新于 {{ formatDate(row.updatedAt) }}</span>
                <span class="work-row__views">{{ row.viewCount || 0 }} 次浏览</span>
              </div>

              <div class="work-row__actions">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button
                  v-if="['DRAFT', 'REJECTED'].includes(row.status)"
                  link
                  type="success"
                  @click="handleSubmitReview(row)"
                >
                  提交审核
                </el-button>
                <el-button
                  v-if="row.status === 'PUBLISHED'"
                  link
                  type="info"
                  @click="$router.push(`/works/${row.id}`)"
                >
                  查看
                </el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="form.title" maxlength="150" show-word-limit />
        </el-form-item>
        <el-form-item label="作品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择作品分类" clearable style="width: 100%">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="一句话说明这件作品讲什么"
          />
        </el-form-item>
        <el-form-item label="封面" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="图片地址，例如 /images/works/dna-score.svg" />
        </el-form-item>
        <el-form-item label="所属学科" prop="knowledgeCategoryId">
          <el-select
            v-model="form.knowledgeCategoryId"
            clearable
            filterable
            placeholder="可选：这件作品属于哪个学科（不选也可以）"
            style="width: 100%"
          >
            <el-option
              v-for="category in knowledgeCategories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
          <p class="form-hint">
            选填。选了学科后，该学科下的读者能在作品广场按分类找到它；不选也不影响发布。
          </p>
        </el-form-item>
        <el-form-item label="作品内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="作品说明、创作思路或图文描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.creator-page {
  max-width: var(--sp-container-wide);
  margin: 0 auto;
  padding: var(--sp-gap-lg) 20px var(--sp-gap-xl);
}

.creator-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.creator-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--sp-text-strong);
}

.creator-head p {
  margin: 0;
  max-width: 640px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--sp-text-muted);
}

.creator-head__actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ---------------- 状态摘要 ---------------- */

.creator-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 18px;
  border: 1px solid var(--sp-border);
  border-radius: 12px;
  background: #fbfdff;
  font-size: 12.5px;
  color: var(--sp-text-muted);
}

.creator-summary__total {
  font-weight: 600;
  color: var(--sp-text-strong);
}

.creator-summary__item {
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(90, 106, 128, 0.08);
  color: #53627a;
}

.creator-summary__item strong {
  margin-left: 3px;
  font-variant-numeric: tabular-nums;
}

.creator-summary__item--draft {
  background: rgba(90, 106, 128, 0.12);
}

.creator-summary__item--pending {
  background: rgba(209, 143, 40, 0.16);
  color: #8a5f14;
}

.creator-summary__item--published {
  background: rgba(38, 161, 105, 0.14);
  color: #1f7a4d;
}

.creator-summary__item--rejected {
  background: rgba(224, 100, 92, 0.16);
  color: #a8382f;
}

.creator-summary__note {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--sp-text-faint);
}

/* ---------------- 分组清单 ---------------- */

.creator-group + .creator-group {
  margin-top: 22px;
}

.creator-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--sp-text-strong);
}

.creator-group__title span {
  font-size: 12px;
  font-weight: 600;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.creator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.work-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border: 1px solid rgba(24, 44, 68, 0.07);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.work-row:hover {
  border-color: rgba(24, 44, 68, 0.12);
  box-shadow: 0 8px 20px -10px rgba(24, 44, 68, 0.22);
}

.work-row__cover {
  flex-shrink: 0;
  width: 96px;
  height: 54px;
  border-radius: 8px;
  background: var(--sp-primary-soft);
  overflow: hidden;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: var(--sp-primary-dark);
}

.work-row__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.work-row__main {
  min-width: 0;
  flex: 1;
}

.work-row__main h3 {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0 0 4px;
  font-size: 14px;
  color: var(--sp-text-strong);
}

.work-row__cat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--cat, var(--sp-primary-dark));
  white-space: nowrap;
}

.work-row__cat::before {
  content: '';
  width: 3px;
  height: 10px;
  border-radius: 2px;
  background: var(--cat, var(--sp-primary));
}

.work-row__desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--sp-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-row__review {
  margin: 6px 0 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--sp-text-muted);
}

.work-row__review-label {
  margin-right: 6px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(209, 143, 40, 0.14);
  color: #8a5f14;
  font-size: 11px;
}

.work-row__side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.work-row__date {
  font-size: 11.5px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.work-row__views {
  font-size: 11.5px;
  color: var(--sp-text-faint);
  font-variant-numeric: tabular-nums;
}

.work-row__actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  padding-left: 12px;
  border-left: 1px solid var(--sp-border);
}

.form-hint {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--sp-text-faint);
}

@media (max-width: 860px) {
  .creator-head {
    flex-direction: column;
  }

  .creator-summary__note {
    margin-left: 0;
  }

  /* 窄屏：封面与标题一行，更新时间/浏览数与操作各自独占一行。
     不然右侧两列 flex-shrink:0 会把标题挤成一字一行。 */
  .work-row {
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 10px 12px;
  }

  .work-row__main {
    flex: 1 1 0;
  }

  .work-row__side {
    order: 3;
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    gap: 14px;
    padding-top: 2px;
  }

  .work-row__actions {
    order: 4;
    width: 100%;
    padding-left: 0;
    padding-top: 8px;
    border-left: 0;
    border-top: 1px solid var(--sp-border);
  }
}
</style>
