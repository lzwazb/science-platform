<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="not-found">
    <el-result icon="warning" title="404" sub-title="页面不存在或尚未开放">
      <template #extra>
        <RouterLink to="/">
          <el-button type="primary">返回首页</el-button>
        </RouterLink>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.not-found {
  max-width: 720px;
  margin: 60px auto;
  padding: 0 24px;
}
</style>
