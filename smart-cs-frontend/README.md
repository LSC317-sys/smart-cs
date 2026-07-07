# SmartCS 前端项目

基于 Vue 3 + Element Plus 的 RAG 智能客服系统前端。

## 技术栈

- **Vue 3.5** - 渐进式 JavaScript 框架
- **Vite 6** - 下一代前端构建工具
- **Vue Router 4** - 官方路由管理器
- **Pinia 2** - Vue 状态管理库
- **Element Plus 2.9** - Vue 3 UI 组件库
- **Axios** - HTTP 客户端
- **Marked** - Markdown 解析器

## 目录结构

```
smart-cs-frontend/
├── public/              # 静态资源
├── src/
│   ├── api/             # API 接口
│   │   ├── request.js   # Axios 封装
│   │   ├── auth.js      # 认证接口
│   │   ├── chat.js      # 聊天接口
│   │   ├── knowledge.js # 知识库接口
│   │   └── admin.js     # 管理接口
│   ├── layouts/         # 布局组件
│   │   └── MainLayout.vue
│   ├── router/          # 路由配置
│   │   └── index.js
│   ├── stores/          # Pinia 状态管理
│   │   └── user.js
│   ├── styles/          # 样式文件
│   │   └── main.scss
│   ├── views/           # 页面组件
│   │   ├── Login.vue    # 登录/注册
│   │   ├── Chat.vue     # 智能问答
│   │   ├── Knowledge.vue# 知识库管理
│   │   └── Admin.vue    # 系统管理
│   ├── App.vue          # 根组件
│   └── main.js          # 入口文件
├── index.html           # HTML 模板
├── package.json         # 依赖配置
└── vite.config.js       # Vite 配置
```

## 快速开始

### 1. 安装依赖

```bash
cd smart-cs-frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 3. 构建生产版本

```bash
npm run build
```

## API 代理配置

开发环境通过 Vite 代理转发请求到后端：

```js
// vite.config.js
proxy: {
  '/api': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true
  },
  '/chat': {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true
  }
}
```

## 功能模块

### 登录注册
- 用户名/密码登录
- 用户注册
- JWT Token 认证

### 智能问答
- 单轮问答
- Markdown 渲染
- 历史消息展示

### 知识库管理
- 文档上传（支持 txt/md/pdf/doc/docx）
- 文档列表查看
- 文档删除
- 统计信息展示

### 系统管理
- 统计概览
- 模型配置信息
- 系统信息

## 后端 API

确保后端服务运行中：

| 服务 | 端口 | 状态 |
|------|------|------|
| Gateway | 8080 | ✅ |
| Chat | 8081 | ✅ |
| Knowledge | 8082 | ✅ |
| User | 8083 | ✅ |
| Admin | 8084 | ✅ |

## 注意事项

1. **中文编码**：所有文件使用 UTF-8 编码
2. **API 路径**：通过 Gateway (8080) 访问所有 API
3. **登录问题**：首次使用需要先注册账号
