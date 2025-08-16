<template>
  <div class="login-container">
    <!-- 页面加载时自动跳转到OAuth2登录 -->
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

// 路由实例
const router = useRouter()

// 组件挂载时直接跳转到OAuth2登录页面
onMounted(() => {
  router.replace('/oauth2/login')
})
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 450px;
  padding: 40px 30px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 2rem;
  margin-bottom: 10px;
  color: var(--primary-text-color);
}

.login-header p {
  font-size: 1rem;
  color: var(--secondary-text-color);
}

.login-form {
  margin-bottom: 20px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-password {
  font-size: 0.9rem;
}

.login-button {
  margin-top: 10px;
  font-size: 1.1rem;
  padding: 12px;
}

.login-footer {
  text-align: center;
  margin-top: 30px;
}

.switch-login {
  position: relative;
  margin-bottom: 20px;
  color: var(--secondary-text-color);
}

.switch-login::before,
.switch-login::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background-color: var(--border-color);
}

.switch-login::before {
  left: 0;
}

.switch-login::after {
  right: 0;
}

.login-providers {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}

.login-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 5px;
  font-size: 0.9rem;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }
  
  .login-header h2 {
    font-size: 1.5rem;
  }
  
  .login-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .login-links {
    flex-direction: column;
    gap: 10px;
  }
}
</style>