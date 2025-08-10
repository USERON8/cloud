# Vue 3 + Vite 前端项目

基于 Vue 3 + Vite + Element Plus + Pinia 的云库存管理系统前端项目。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Vue Router** - Vue.js 官方路由管理器
- **Axios** - HTTP 客户端
- **Pinia** - Vue 状态管理

## 项目结构

```
frontend/app/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API 接口
│   │   ├── auth.js        # 认证相关 API
│   │   ├── user.js        # 用户相关 API
│   │   ├── stock.js       # 库存相关 API
│   │   ├── product.js     # 产品相关 API
│   │   ├── order.js       # 订单相关 API
│   │   └── axiosInstance.js  # Axios 实例配置
│   ├── components/        # Vue 组件
│   │   ├── HomePage.vue        # 首页组件
│   │   ├── LoginPage.vue       # 登录组件
│   │   ├── MerchantLogin.vue   # 商户登录组件
│   │   ├── UserHome.vue        # 用户主页组件
│   │   ├── MerchantHome.vue    # 商户主页组件
│   │   ├── AdminHome.vue       # 管理员主页组件
│   │   ├── StockManager.vue    # 库存管理组件
│   │   └── Statistics.vue      # 统计报表组件
│   ├── router/            # 路由配置
│   │   └── index.js       # 路由定义
│   ├── store/             # 状态管理 (Pinia)
│   │   ├── modules/       # 模块化状态
│   │   │   ├── auth.js    # 认证状态管理
│   │   │   ├── user.js    # 用户状态管理
│   │   │   ├── stock.js   # 库存状态管理
│   │   │   ├── product.js # 产品状态管理
│   │   │   └── order.js   # 订单状态管理
│   │   ├── app.js         # 应用根状态
│   │   └── index.js       # 状态管理入口
│   ├── views/             # 页面视图
│   │   ├── auth/          # 认证相关页面
│   │   │   ├── Login.vue     # 登录页面
│   │   │   └── Register.vue  # 注册页面
│   │   ├── user/          # 用户相关页面
│   │   │   ├── Dashboard.vue # 用户仪表板
│   │   │   └── Profile.vue   # 用户个人资料
│   │   ├── merchant/      # 商户相关页面
│   │   │   ├── Dashboard.vue # 商户仪表板
│   │   │   └── Products.vue  # 商户产品管理
│   │   └── admin/         # 管理员相关页面
│   │       ├── Dashboard.vue # 管理员仪表板
│   │       └── Users.vue     # 用户管理
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── index.html             # HTML 模板
├── package.json           # 项目配置
├── vite.config.js         # Vite 配置
└── README.md              # 项目文档
```

## 快速开始

### 环境要求

- Node.js >= 16
- pnpm >= 7

### 安装依赖

```bash
pnpm install
```

### 开发环境启动

```bash
pnpm run dev
```

访问地址：http://localhost:3000

### 生产环境构建

```bash
pnpm run build
```

构建产物在 `dist` 目录下。

### 预览构建结果

```bash
pnpm run preview
```

## 开发配置

### Vite 配置说明

```javascript
// vite.config.js
export default defineConfig({
    plugins: [vue()],
    server: {
        port: 3000,
        host: '0.0.0.0',
        proxy: {
            // 代理认证服务 API
            '/auth': {
                target: 'http://localhost:80',
                changeOrigin: true
            },
            // 代理用户服务 API
            '/user': {
                target: 'http://localhost:80',
                changeOrigin: true
            },
            // 代理库存服务 API
            '/stock': {
                target: 'http://localhost:80',
                changeOrigin: true
            }
        }
    }
})
```

### API 配置

```javascript
// src/api/axiosInstance.js
const request = axios.create({
    baseURL: '', // 使用相对路径，通过 Vite 代理
    timeout: 10000
})
```

### 路由配置

```javascript
// src/router/index.js
const routes = [
    { path: '/', redirect: '/auth/login' },
    { path: '/auth/login', component: () => import('../views/auth/Login.vue') },
    { path: '/auth/register', component: () => import('../views/auth/Register.vue') },
    // 用户路由
    {
        path: '/user',
        component: () => import('../views/user/Dashboard.vue'),
        children: [
            { path: 'profile', component: () => import('../views/user/Profile.vue') }
        ]
    },
    // 商户路由
    {
        path: '/merchant',
        component: () => import('../views/merchant/Dashboard.vue'),
        children: [
            { path: 'products', component: () => import('../views/merchant/Products.vue') }
        ]
    },
    // 管理员路由
    {
        path: '/admin',
        component: () => import('../views/admin/Dashboard.vue'),
        children: [
            { path: 'users', component: () => import('../views/admin/Users.vue') }
        ]
    }
]
```

## 功能模块

### 1. 认证管理 (`/auth`)

- **功能**：用户登录、注册、登出
- **组件**：`Login.vue`, `Register.vue`
- **API**：
    - `POST /auth/login` - 用户登录
    - `POST /auth/register` - 用户注册
    - `POST /auth/register-and-login` - 注册并登录
    - `POST /auth/change-password` - 修改密码
    - `GET /auth/user-info` - 获取当前用户信息
    - `GET /auth/validate-token` - 验证token有效性
    - `GET /auth/logout` - 用户登出
    - `POST /auth/refresh-token` - 刷新token

### 2. 用户管理 (`/user`)

- **功能**：用户个人资料管理、密码修改
- **组件**：`Dashboard.vue`, `Profile.vue`
- **状态管理**：`store/modules/user.js`
- **API**：
    - `GET /auth/user-info` - 获取用户信息
    - `POST /auth/change-password` - 修改密码

### 3. 商户管理 (`/merchant`)

- **功能**：商户产品管理
- **组件**：`Dashboard.vue`, `Products.vue`
- **状态管理**：`store/modules/product.js`
- **API**：
    - 产品相关操作（待后端完善）

### 4. 管理员管理 (`/admin`)

- **功能**：用户管理
- **组件**：`Dashboard.vue`, `Users.vue`
- **状态管理**：`store/modules/user.js`
- **API**：
    - 用户管理相关操作（待后端完善）

### 5. 库存管理 (`/stock`)

- **功能**：商品库存的增删改查
- **组件**：`StockManager.vue`
- **状态管理**：`store/modules/stock.js`
- **API**：
    - `GET /stock/product/{id}` - 根据商品ID查询库存
    - `POST /stock/page` - 分页查询库存

### 6. 统计报表 (`/statistics`)

- **功能**：库存数据统计和图表展示
- **组件**：`Statistics.vue`
- **特性**：支持图表集成（可扩展 ECharts）

## 组件开发规范

### Vue 3 Composition API

使用 `<script setup>` 语法：

```vue

<script setup>
  import {ref, onMounted} from 'vue'

  // 响应式数据
  const loading = ref(false)
  const tableData = ref([])

  // 生命周期
  onMounted(() => {
    loadData()
  })

  // 方法定义
  const loadData = async () => {
    // 业务逻辑
  }
</script>
```

### Element Plus 组件使用

```vue

<template>
  <el-table :data="tableData" :loading="loading">
    <el-table-column prop="productName" label="商品名称"/>
    <el-table-column prop="quantity" label="库存数量"/>
  </el-table>
</template>
```

### 图标使用

```vue

<script setup>
  import {Search, Refresh} from '@element-plus/icons-vue'
</script>

<template>
  <el-button type="primary">
    <el-icon>
      <Search/>
    </el-icon>
    搜索
  </el-button>
</template>
```

## API 接口规范

### 请求格式

```javascript
// GET 请求
const response = await stockApi.getByProductId(productId)

// POST 请求
const response = await stockApi.pageQuery({
    current: 1,
    size: 10,
    productName: '商品名称'
})
```

### 响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      "..."
    ],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

## 样式规范

### CSS 作用域

使用 `scoped` 样式：

```vue

<style scoped>
  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 20px;
  }
</style>
```

### 响应式设计

```css
/* 移动端适配 */
@media (max-width: 768px) {
    .header-content {
        flex-direction: column;
    }
}
```

## 部署说明

### 开发环境

1. 启动后端微服务（网关在 80 端口）
2. 运行 `pnpm run dev`
3. 访问 http://localhost:3000

### 生产环境

1. 构建项目：`pnpm run build`
2. 将 `dist` 目录部署到 Nginx
3. 配置 Nginx 代理后端 API

### Nginx 配置示例

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    
    # 前端静态资源
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /stock/ {
        proxy_pass http://localhost:80/stock/;
    }
}
```

## 开发工具推荐

### IDE 配置

- **VSCode** + Vue Language Features (Volar)
- 禁用 Vetur 扩展（Vue 2 专用）

### 浏览器插件

- Vue.js devtools - Vue 组件调试
- Element Plus Helper - 组件文档快速查看

## 常见问题

### 1. 图标不显示

确保安装了图标包：

```bash
pnpm add @element-plus/icons-vue
```

### 2. API 请求失败

检查 Vite 代理配置和后端服务状态：

```javascript
// vite.config.js
proxy: {
    '/stock'
:
    {
        target: 'http://localhost:80',
            changeOrigin
    :
        true
    }
}
```

### 3. 路由跳转异常

确保路由配置正确：

```javascript
// 使用编程式导航
router.push('/stock')

// 或使用 router-link
< router - link
to = "/stock" > 库存管理 < /router-link>
```

## 扩展功能

### 1. 状态管理（Pinia）

```javascript
// stores/stock.js
import {defineStore} from 'pinia'

export const useStockStore = defineStore('stock', {
    state: () => ({
        stockList: []
    }),
    actions: {
        async fetchStockList() {
            // API 调用
        }
    }
})
```

### 2. 图表集成（ECharts）

```bash
pnpm add echarts vue-echarts
```

### 3. 国际化（Vue I18n）

```bash
pnpm add vue-i18n@9
```

## 更新日志

- **v1.0.0** - 初始版本，包含库存管理和统计报表功能
- **v1.1.0** - 新增完整的用户认证和权限管理系统
    - 支持 Vue 3 + Vite + Element Plus + Pinia
    - 集成路由和 API 代理配置
    - 实现基于角色的访问控制（RBAC）
    - 添加完整的状态管理（Pinia）
    - 实现用户、商户、管理员三种角色的界面和功能

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交代码
4. 发起 Pull Request

## 许可证

MIT License
