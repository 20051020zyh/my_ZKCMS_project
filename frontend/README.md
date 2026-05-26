# 知识库平台前端

基于 Vue3 + Element Plus 构建的现代化知识库平台前端应用。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 的超集,提供类型支持
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Pinia** - Vue 状态管理
- **Vue Router** - Vue 官方路由
- **Axios** - HTTP 客户端

## 功能特性

### 首页布局
- ✅ 左侧可折叠侧边导航栏(带Logo)
- ✅ 顶部搜索栏和用户菜单
- ✅ 文章分类筛选
- ✅ 中间文章列表区域(支持分页)
- ✅ 右侧文章排行榜(阅读/点赞/收藏)
- ✅ 右侧精选文章展示

### 用户功能
- ✅ 用户登录/注册
- ✅ 个人中心
- ✅ 退出登录

### 文章功能
- ✅ 文章列表浏览
- ✅ 文章详情查看
- ✅ 文章搜索
- ✅ 文章分类筛选
- ✅ 文章点赞
- ✅ 热门文章/精选文章展示
- ✅ 文章排行榜

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口定义
│   │   ├── article.ts    # 文章相关接口
│   │   ├── category.ts   # 分类相关接口
│   │   └── user.ts       # 用户相关接口
│   ├── components/       # 公共组件
│   │   ├── Sidebar.vue          # 侧边导航栏
│   │   ├── HeaderBar.vue        # 顶部栏
│   │   ├── ArticleList.vue      # 文章列表
│   │   ├── ArticleRanking.vue   # 文章排行榜
│   │   └── BestArticles.vue     # 精选文章
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # 状态管理
│   │   └── user.ts       # 用户状态
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios 封装
│   ├── views/            # 页面视图
│   │   ├── Home.vue             # 首页
│   │   ├── Login.vue            # 登录页
│   │   └── ArticleDetail.vue    # 文章详情页
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 快速开始

### 环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 后端接口配置

前端通过 Vite 代理将 `/api` 请求转发到后端服务器:

- 前端地址: `http://localhost:5173`
- 后端地址: `http://localhost:8080`

确保后端服务已启动,否则会出现连接错误。

在 `vite.config.ts` 中可以修改代理配置:

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

## 主要API接口

### 文章接口
- `GET /article/pageList` - 获取文章列表(分页)
- `GET /article/detail` - 获取文章详情
- `GET /article/hot/list` - 获取热门文章
- `GET /article/best/list` - 获取精选文章
- `GET /article/rank` - 获取文章排行榜
- `POST /article/like/toggle` - 点赞/取消点赞

### 分类接口
- `GET /category/all/simple` - 获取所有分类

### 用户接口
- `POST /user/login` - 用户登录
- `POST /user/register` - 用户注册
- `GET /user/userInfo` - 获取用户信息
- `GET /user/logout` - 退出登录

## 设计规范

### 颜色方案
- 主色调: 紫色渐变 (#667eea → #764ba2)
- 背景色: #f5f7fa
- 文字色: #1f2937 (深), #6b7280 (中), #9ca3af (浅)

### 圆角规范
- 卡片: 12px
- 按钮: 8-10px
- 标签: 6-12px

### 阴影规范
- 卡片阴影: 0 2px 8px rgba(0, 0, 0, 0.04)
- 悬浮阴影: 0 4px 16px rgba(102, 126, 234, 0.15)

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## License

MIT
