<script setup lang="ts">
import StatusPage from '../../components/StatusPage.vue'
import { navigateTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

function backToMarket(): void {
  navigateTo(Routes.market)
}
</script>

<template>
  <StatusPage
    code="403"
    eyebrow="Access denied"
    title="This account cannot open the requested page."
    description="Use an account with the required role, or return to the storefront."
  >
    <template #actions>
        <button class="btn-primary" @click="backToMarket">Back to market</button>
    </template>
  </StatusPage>
</template>
