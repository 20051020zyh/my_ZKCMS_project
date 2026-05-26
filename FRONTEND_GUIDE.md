# 大事件项目 - 前端系统使用说明

## 项目概述

本项目是一个完整的内容管理系统(CMS)，包含前台展示系统和后台管理系统两大部分。

## 技术栈

### 前端
- Vue 3.5 + TypeScript
- Element Plus UI组件库
- Vue Router 5 (路由管理)
- Pinia (状态管理)
- Axios (HTTP请求)
- ECharts (数据可视化)

### 后端
- Spring Boot 3.x
- MyBatis Plus
- Redis (缓存)
- JWT (身份认证)
- Knife4j (API文档)

## 系统架构

### 1. 前台系统 (普通用户)

#### 主要功能
- **文章浏览**：查看文章列表、文章详情
- **文章搜索**：关键词搜索文章
- **分类浏览**：按分类筛选文章
- **用户互动**：点赞、收藏、评论
- **用户中心**：查看个人信息、修改资料

#### 页面结构
```
/                     # 首页
/login                # 登录页
/article/:id          # 文章详情页
```

#### 主要组件
- `HeaderBar.vue` - 顶部搜索栏和用户菜单
- `Sidebar.vue` - 左侧导航栏
- `ArticleList.vue` - 文章列表
- `ArticleRanking.vue` - 文章排行榜
- `BestArticles.vue` - 精选文章

### 2. 后台管理系统 (管理员)

#### 主要功能
- **控制台**：数据统计、访问趋势图表
- **文章管理**：增删改查文章、批量操作
- **分类管理**：管理文章分类
- **评论管理**：审核和管理评论
- **用户管理**：管理用户信息
- **公告管理**：发布系统公告
- **角色权限**：管理角色和权限
- **系统设置**：维护模式等配置

#### 页面结构
```
/admin                # 后台管理首页
/admin/dashboard      # 控制台
/admin/articles       # 文章管理
/admin/categories     # 分类管理
/admin/comments       # 评论管理
/admin/users          # 用户管理
/admin/notices        # 公告管理
/admin/roles          # 角色权限
/admin/settings       # 系统设置
```

## API接口说明

### 用户相关接口
- `POST /user/login` - 用户登录
- `POST /user/register` - 用户注册
- `GET /user/userInfo` - 获取用户信息
- `PUT /user/update` - 更新用户信息
- `PATCH /user/updateAvatar` - 更新头像
- `PUT /user/updatePwd` - 更新密码
- `GET /user/logout` - 退出登录

### 文章相关接口
- `GET /article/pageList` - 获取文章列表(分页)
- `GET /article/detail` - 获取文章详情
- `POST /article/add` - 新增文章
- `PUT /article/update` - 更新文章
- `DELETE /article/delete` - 删除文章
- `POST /article/like/toggle` - 点赞/取消点赞
- `GET /article/like/check` - 检查是否已点赞
- `POST /article/collect/toggle` - 收藏/取消收藏
- `GET /article/collect/check` - 检查是否已收藏
- `GET /article/hot/list` - 热门文章列表
- `GET /article/best/list` - 精选文章列表
- `GET /article/rank` - 文章排行榜
- `GET /article/search` - 搜索文章
- `GET /article/trend` - 访问趋势数据

### 分类相关接口
- `GET /category/all/simple` - 获取所有分类
- `GET /category/get/user/list` - 获取用户分类列表
- `POST /category/add` - 新增分类
- `PUT /category/update` - 更新分类
- `DELETE /category/delete` - 删除分类

### 评论相关接口
- `POST /article/comment/add` - 发表评论
- `GET /article/comment/list` - 获取评论列表
- `DELETE /article/comment/delete` - 删除评论
- `POST /comment/like/add` - 点赞评论
- `GET /comment/like/check` - 检查是否已点赞评论

### 系统管理接口
- `GET /sysConfig/get` - 获取系统配置
- `PUT /sysConfig/update` - 更新系统配置
- `GET /sysNotice/list` - 获取公告列表
- `POST /sysNotice/add` - 新增公告
- `PUT /sysNotice/update` - 更新公告
- `DELETE /sysNotice/delete` - 删除公告
- `GET /sysRole/page` - 获取角色列表
- `POST /sysRole/add` - 新增角色
- `GET /sysPermission/list` - 获取权限列表

## 启动说明

### 1. 后端启动
```bash
cd E:\heimaspringboot\big_event
mvn spring-boot:run
# 或者使用IDE运行 BigEventApplication.java
```

后端默认运行在: `http://localhost:8081`

### 2. 前端启动
```bash
cd E:\heimaspringboot\big_event\frontend
npm install
npm run dev
```

前端默认运行在: `http://localhost:5173`

### 3. 访问系统
- 前台系统: http://localhost:5173
- 后台管理: http://localhost:5173/admin
- API文档: http://localhost:8081/doc.html

## 登录说明

### 默认管理员账号
需要根据数据库中的实际账号登录。确保用户已分配管理员角色和相应权限。

### 登录流程
1. 访问登录页面
2. 输入用户名和密码
3. 登录成功后，系统会自动获取用户信息
4. 根据用户角色可以访问后台管理系统

## 权限控制

### 前端权限
- 需要登录的操作会预先检查登录状态
- 未登录用户点击需要登录的功能时，会弹出提示并跳转到登录页
- 401错误会自动捕获并提示用户登录

### 后端权限
- 使用 `@RequirePermission` 注解控制接口访问
- JWT Token验证
- Redis黑名单管理
- 角色权限验证

## 数据库连接

确保后端已正确配置数据库连接：
- 配置文件: `src/main/resources/application.yml`
- 环境配置: `application-dev.yml`, `application-prod.yml`

## 常见问题

### 1. 前端无法连接后端
- 检查后端是否正常启动
- 检查代理配置: `vite.config.ts` 中的 proxy 设置
- 确认端口是否正确(前端5173, 后端8081)

### 2. 登录后无法访问后台
- 检查用户是否已分配管理员角色
- 检查角色是否有相应权限
- 查看浏览器控制台和后端日志

### 3. 接口返回401错误
- Token可能已过期，需要重新登录
- 检查localStorage中是否有token
- 清除浏览器缓存后重试

## 开发说明

### 添加新页面
1. 在 `src/views` 下创建Vue组件
2. 在 `src/router/index.ts` 中添加路由
3. 如需API调用，在 `src/api` 下创建对应的API文件

### 添加新接口
1. 在 `src/api` 目录下添加API方法
2. 使用 `request` 工具发送HTTP请求
3. 在组件中导入并调用API方法

## 已完成功能清单

✅ 用户登录/注册
✅ 用户信息管理
✅ 文章列表浏览
✅ 文章详情查看
✅ 文章搜索
✅ 分类筛选
✅ 文章点赞
✅ 文章收藏
✅ 文章排行榜
✅ 精选文章
✅ 登录权限检查
✅ 401错误处理
✅ 后台管理系统布局
✅ 控制台数据统计
✅ 文章管理(CRUD)
✅ 分类管理(CRUD)
✅ 公告管理(CRUD)
✅ 系统设置
✅ 路由守卫
✅ 响应式布局

## 待开发功能

✅ 评论管理完善
✅ 用户管理完善
✅ 角色权限管理完善
✅ 文章编辑器集成（wangEditor富文本编辑器）
✅ 图片上传功能（含认证校验）
✅ 数据导出功能（CSV/JSON）
✅ 更多统计图表（访问趋势、分类饼图、周数据统计）

## 联系支持

如有问题，请查看：
- 后端API文档: http://localhost:8081/doc.html
- 前端控制台日志
- 后端日志输出
