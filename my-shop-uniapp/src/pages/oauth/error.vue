<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import StatusPage from '../../components/StatusPage.vue'
import { redirectTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

const message = ref('Sign-in failed. Please try again later.')

onLoad((query) => {
  if (typeof query.message === 'string') {
    message.value = query.message
  }
})

function backToLogin(): void {
  redirectTo(Routes.login)
}
</script>

<template>
  <StatusPage
    eyebrow="Authorization failed"
    title="The sign-in flow could not be completed."
    :description="message"
  >
    <template #actions>
        <button class="btn-primary" @click="backToLogin">Back to sign in</button>
    </template>
  </StatusPage>
</template>
