<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'

const url = ref('')

onLoad((query) => {
  if (typeof query.url === 'string') {
    url.value = decodeURIComponent(query.url)
  }
})
</script>

<template>
  <view class="page">
    <web-view v-if="url" :src="url" />
    <view v-else class="empty">
      <text>暂无可打开的地址</text>
    </view>
  </view>
</template>

<style scoped>
.page {
  width: 100%;
  height: 100vh;
}

.empty {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
}
</style>
