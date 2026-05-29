# 知库CMS项目 - Code Wiki 文档

## 1. 项目概述

### 1.1 项目简介
知库CMS是一个功能完整的内容管理系统，采用前后端分离架构，包含文章管理、用户系统、评论、分类标签、权限管理、数据统计等核心功能。

### 1.2 项目结构
```
big_event/
├── frontend/                # 前端项目 (Vue 3)
├── src/                     # 后端项目 (Spring Boot 3)
│   ├── main/
│   │   ├── java/com/heima/big_event/
│   │   └── resources/
│   └── test/
├── pom.xml                  # Maven依赖配置
└── README.md
```

---

## 2. 技术栈

### 2.1 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.1.12 | 核心框架 |
| Spring Security | - | 安全认证框架 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| MySQL | - | 关系型数据库 |
| Redis | - | 缓存/分布式锁 |
| Redisson | 3.37.0 | Redis高级客户端 |
| Knife4j | 4.4.0 | API文档 (内置Swagger) |
| JWT | 0.12.6 | Token认证 |
| Druid | 1.2.24 | 数据库连接池 |
| Lombok | - | 简化实体类 |
| Spring Mail | - | 邮件服务 |
| AOP | - | 切面编程 |

### 2.2 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 渐进式前端框架 |
| TypeScript | - | 类型安全的JavaScript |
| Vite | - | 构建工具 |
| Element Plus | - | UI组件库 |
| Pinia | 3.0.4 | 状态管理 |
| Vue Router | 5.0.7 | 路由管理 |
| Axios | - | HTTP客户端 |
| ECharts | 5.5.0 | 图表可视化 |
| WangEditor | 5.x | 富文本编辑器 |

---

## 3. 后端架构详解

### 3.1 项目分层架构
采用标准的分层架构：
```
Controller (控制层) → Service (业务层) → Mapper (数据访问层)
```

### 3.2 核心包结构

```
com.heima.big_event/
├── AOPlogfile/              # AOP日志切面
├── anno/                    # 自定义注解
│   ├── RateLimit.java       # 限流注解
│   ├── State.java           # 状态校验注解
│   └── StateValidator.java
├── aspect/                  # 切面实现
│   └── RateLimitAspect.java # 限流切面
├── config/                  # 配置类
│   ├── DruidConfig.java     # Druid配置
│   ├── MailConfig.java      # 邮件配置
│   ├── MyMetaObjectHandler.java # MyBatis Plus自动填充
│   ├── MybatisPlusConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java  # Spring Security配置
│   ├── SwaggerConfig.java
│   └── WebConfig.java       # Web配置 (拦截器、CORS)
├── controller/              # 控制器层
│   ├── article/             # 文章相关控制器
│   │   ├── ArticleController.java
│   │   ├── ArticleCollectController.java
│   │   ├── ArticleCommentController.java
│   │   ├── ArticleReportController.java
│   │   └── TagsController.java
│   ├── category/            # 分类控制器
│   ├── comment/             # 评论相关
│   ├── common/              # 通用控制器
│   │   ├── FileUploadController.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── IndexController.java
│   ├── other/               # 其他功能
│   │   └── LeaveMessageController.java
│   ├── system/              # 系统管理
│   │   ├── SysConfigController.java
│   │   ├── SysNoticeController.java
│   │   ├── SysPermissionController.java
│   │   ├── SysRoleController.java
│   │   ├── SysRolePermissionController.java
│   │   └── SysUserRoleController.java
│   └── user/                # 用户控制器
│       └── UserController.java
├── exception/               # 异常处理
│   └── BusinessException.java
├── filter/                  # 过滤器
│   └── RequestBodyCacheFilter.java
├── interceptor/             # 拦截器
│   ├── JwtInterceptor.java  # JWT认证拦截器
│   └── SensitiveWordInterceptor.java # 敏感词拦截器
├── mapper/                  # 数据访问层
│   ├── article/
│   ├── category/
│   ├── comment/
│   ├── dataStatistics/      # 数据统计
│   ├── system/
│   └── user/
├── pojo/                    # 实体类
│   ├── VO/                  # 视图对象
│   ├── dto/                 # 数据传输对象
│   ├── validation/          # 校验分组
│   ├── Article.java
│   ├── User.java
│   ├── Category.java
│   ├── PageBean.java
│   ├── Result.java          # 统一响应结果
│   └── ...                  # 其他实体
├── service/                 # 业务层
│   ├── article/
│   ├── category/
│   ├── comment/
│   ├── dataStatistics/
│   ├── impl/                # 实现类
│   ├── others/
│   ├── system/
│   └── user/
├── utils/                   # 工具类
│   ├── Others/
│   │   ├── AliOssUtil.java  # 阿里云OSS
│   │   ├── EmailUtil.java
│   │   ├── Md5Util.java
│   │   ├── RedisUtil.java   # Redis工具
│   │   └── ThreadLocalUtil.java # ThreadLocal工具
│   ├── Permission/          # 权限相关
│   ├── RequestWrapper/      # 请求包装
│   ├── Task/                # 定时任务
│   │   ├── ArticlePublishTask.java
│   │   ├── DailyStatTask.java
│   │   ├── NoticePublishTask.java
│   │   └── SyncTask.java
│   ├── JwtUtil.java         # JWT工具
│   ├── MaintenanceFilter.java
│   └── SensitiveWordUtil.java
└── BigEventApplication.java # 启动类
```

### 3.3 核心类说明

#### 3.3.1 BigEventApplication.java
主启动类，启用了定时任务和异步支持：
```java
@SpringBootApplication
@EnableScheduling    // 启用定时任务
@EnableAsync         // 启用异步
public class BigEventApplication { ... }
```

#### 3.3.2 Result.java
统一响应结果类，封装API返回格式。

#### 3.3.3 User.java
用户实体类，包含用户名、密码、昵称、邮箱等字段。

#### 3.3.4 Article.java
文章实体类，包含文章标题、内容、分类、状态等字段。

### 3.4 关键配置类

#### 3.4.1 WebConfig.java
配置拦截器和CORS：
- 注册JWT拦截器
- 注册敏感词拦截器
- 配置跨域支持

#### 3.4.2 SecurityConfig.java
Spring Security配置，主要用于：
- 禁用CSRF
- 配置维护模式过滤器
- 允许所有请求通过（权限控制主要通过自定义拦截器实现）

---

## 4. 后端核心功能模块

### 4.1 用户模块
- 用户注册/登录
- 个人信息管理
- 头像上传
- JWT Token认证

### 4.2 文章模块 (核心)
- 文章增删改查
- 文章列表分页查询
- 文章详情查看
- 文章搜索
- 文章分类/标签管理
- 文章点赞/收藏
- 文章评论
- 文章状态管理（草稿/已发布）
- 文章回收站
- 热门/精选文章
- 文章排行榜
- 定时发布文章
- 批量操作（管理员）

**缓存策略**：
- 文章列表缓存（Redis）
- 文章详情缓存（Redis）
- 分布式锁防止缓存击穿
- 随机过期时间防止缓存雪崩

### 4.3 分类与标签模块
- 分类管理
- 标签管理
- 文章分类关联

### 4.4 评论模块
- 评论发表
- 评论点赞
- 评论举报
- 评论审核（管理员）

### 4.5 系统管理模块
- 用户管理
- 角色管理
- 权限管理
- 公告管理
- 系统配置
- 数据统计

### 4.6 数据统计模块
- 访问日志记录
- 日统计数据
- 文章趋势分析

### 4.7 其他功能
- 文件上传（阿里云OSS）
- 敏感词过滤
- 限流控制
- 邮件服务
- 维护模式

---

## 5. 前端架构详解

### 5.1 项目结构
```
frontend/
├── public/
│   └── favicon.svg
├── src/
│   ├── api/                 # API接口定义
│   │   ├── admin.ts
│   │   ├── article.ts
│   │   ├── articleReport.ts
│   │   ├── category.ts
│   │   ├── comment.ts
│   │   ├── leaveMessage.ts
│   │   ├── tags.ts
│   │   └── user.ts
│   ├── components/          # 公共组件
│   │   ├── ArticleList.vue
│   │   ├── ArticleRanking.vue
│   │   ├── BestArticles.vue
│   │   ├── HeaderBar.vue
│   │   └── Sidebar.vue
│   ├── router/              # 路由配置
│   │   └── index.ts
│   ├── stores/              # Pinia状态管理
│   │   └── user.ts
│   ├── types/               # TypeScript类型定义
│   │   └── element-plus.d.ts
│   ├── utils/               # 工具函数
│   │   ├── export.ts
│   │   ├── navigate.ts
│   │   └── request.ts       # Axios封装
│   ├── views/               # 页面视图
│   │   ├── admin/           # 管理后台页面
│   │   │   ├── AdminLayout.vue
│   │   │   ├── AdminLogin.vue
│   │   │   ├── ArticleManage.vue
│   │   │   ├── CategoryManage.vue
│   │   │   ├── CommentManage.vue
│   │   │   ├── Dashboard.vue
│   │   │   ├── NoticeManage.vue
│   │   │   ├── PermissionManage.vue
│   │   │   ├── ReportManage.vue
│   │   │   ├── RoleManage.vue
│   │   │   ├── Settings.vue
│   │   │   └── UserManage.vue
│   │   ├── About.vue
│   │   ├── ArticleDetail.vue
│   │   ├── CreateArticle.vue
│   │   ├── Home.vue
│   │   ├── Join.vue
│   │   ├── Login.vue
│   │   ├── NoticeCenter.vue
│   │   └── Profile.vue
│   ├── App.vue
│   └── main.ts
├── .gitignore
├── README.md
├── index.html
├── package-lock.json
├── package.json
├── tsconfig.app.json
├── tsconfig.json
├── tsconfig.node.json
└── vite.config.ts
```

### 5.2 核心页面说明

| 页面 | 路径 | 说明 |
|------|------|------|
| 首页 | / | 文章列表、分类筛选、排行榜 |
| 登录 | /login | 用户登录 |
| 文章详情 | /article/:id | 查看文章内容、评论 |
| 写文章 | /article/create | 创建/编辑文章 |
| 个人中心 | /profile | 用户信息管理 |
| 管理后台 | /admin | 后台管理入口 |

### 5.3 路由配置
路由包含普通用户路由和管理员路由，管理员路由需要权限校验。

---

## 6. 数据库设计要点

### 6.1 核心数据表
- `user` - 用户表
- `article` - 文章表
- `category` - 分类表
- `tags` - 标签表
- `article_tags` - 文章-标签关联表
- `article_like` - 文章点赞表
- `article_collect` - 文章收藏表
- `article_comment` - 文章评论表
- `comment_like` - 评论点赞表
- `article_report` - 文章举报表
- `comment_report` - 评论举报表
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户-角色关联表
- `sys_role_permission` - 角色-权限关联表
- `sys_notice` - 公告表
- `sys_config` - 系统配置表
- `daily_stat` - 日统计表
- `visit_log` - 访问日志表
- `leave_message` - 留言表

---

## 7. API文档

### 7.1 访问方式
后端集成了Knife4j，启动后可访问：
- Swagger UI: http://localhost:8081/swagger-ui.html
- Knife4j: http://localhost:8081/doc.html

### 7.2 主要API接口

#### 用户相关
- `POST /user/login` - 用户登录
- `POST /user/register` - 用户注册
- `GET /user/userInfo` - 获取用户信息
- `PUT /user/update` - 更新用户信息
- `PATCH /user/updateAvatar` - 更新头像
- `PATCH /user/updatePwd` - 更新密码

#### 文章相关
- `POST /article/add` - 新增文章
- `GET /article/pageList` - 文章列表分页
- `GET /article/detail` - 文章详情
- `PUT /article/update` - 更新文章
- `DELETE /article/delete` - 删除文章
- `GET /article/search` - 文章搜索
- `GET /article/hot/list` - 热门文章
- `GET /article/best/list` - 精选文章
- `GET /article/rank` - 文章排行
- `POST /article/like/toggle` - 点赞/取消点赞
- `POST /article/trash` - 移入回收站

#### 分类相关
- `GET /category/all/simple` - 获取所有分类

#### 管理后台
- 各类管理接口，需权限认证

---

## 8. 项目运行方式

### 8.1 后端运行

#### 前置条件
- JDK 17+
- MySQL 8.0+
- Redis 6.0+

#### 配置
1. 复制 `application-dev.yml.example` 为 `application-dev.yml`
2. 配置数据库连接信息
3. 配置Redis连接信息
4. 配置阿里云OSS（可选）
5. 配置邮件服务（可选）

#### 启动方式
```bash
# Maven方式
mvn spring-boot:run

# 或打包后运行
mvn package
java -jar target/big-event-0.0.1-SNAPSHOT.jar
```

默认端口：8081

### 8.2 前端运行

#### 前置条件
- Node.js 16+
- npm 7+

#### 安装依赖
```bash
cd frontend
npm install
```

#### 开发模式
```bash
npm run dev
```

访问地址：http://localhost:5173

#### 生产构建
```bash
npm run build
```

### 8.3 代理配置
前端通过Vite代理将 `/api` 请求转发到后端：
- 前端地址：http://localhost:5173
- 后端地址：http://localhost:8081

可在 `vite.config.ts` 中修改代理配置。

---

## 9. 核心技术实现

### 9.1 JWT认证流程
1. 用户登录 → 后端验证 → 生成JWT Token
2. 前端存储Token (localStorage)
3. 后续请求携带Token (Authorization header)
4. JwtInterceptor拦截验证 → ThreadLocal存储用户信息

### 9.2 Redis缓存策略
- 文章列表缓存
- 文章详情缓存
- 使用分布式锁防止缓存击穿
- 随机过期时间防止缓存雪崩

### 9.3 权限控制
- 基于角色的访问控制 (RBAC)
- `@RequirePermission` 自定义注解
- 权限校验切面

### 9.4 敏感词过滤
- SensitiveWordInterceptor拦截器
- 对用户输入内容进行过滤

### 9.5 定时任务
- 文章定时发布
- 每日数据统计
- 公告定时发布
- 数据同步任务

---

## 10. 开发注意事项

### 10.1 后端开发
- 使用MyBatis Plus进行数据库操作
- 统一使用Result封装返回结果
- 使用ThreadLocalUtil获取当前登录用户
- 缓存更新时注意清理相关缓存数据

### 10.2 前端开发
- 使用TypeScript进行类型检查
- 使用Pinia管理全局状态
- API调用使用封装的request.ts
- 使用Element Plus组件库
- 路由权限在router/index.ts中配置

---

## 11. 关键文件索引

| 文件 | 路径 | 说明 |
|------|------|------|
| 启动类 | [BigEventApplication.java](file:///e:/heimaspringboot/big_event/src/main/java/com/heima/big_event/BigEventApplication.java) | Spring Boot启动类 |
| 主配置 | [application.yml](file:///e:/heimaspringboot/big_event/src/main/resources/application.yml) | Spring Boot配置 |
| Web配置 | [WebConfig.java](file:///e:/heimaspringboot/big_event/src/main/java/com/heima/big_event/config/WebConfig.java) | 拦截器、CORS配置 |
| 安全配置 | [SecurityConfig.java](file:///e:/heimaspringboot/big_event/src/main/java/com/heima/big_event/config/SecurityConfig.java) | Spring Security配置 |
| 文章控制器 | [ArticleController.java](file:///e:/heimaspringboot/big_event/src/main/java/com/heima/big_event/controller/article/ArticleController.java) | 文章核心控制器 |
| 用户实体 | [User.java](file:///e:/heimaspringboot/big_event/src/main/java/com/heima/big_event/pojo/User.java) | 用户实体类 |
| 前端入口 | [main.ts](file:///e:/heimaspringboot/big_event/frontend/src/main.ts) | 前端应用入口 |
| 路由配置 | [index.ts](file:///e:/heimaspringboot/big_event/frontend/src/router/index.ts) | 路由配置 |
| Vite配置 | [vite.config.ts](file:///e:/heimaspringboot/big_event/frontend/vite.config.ts) | Vite构建配置 |
| Maven依赖 | [pom.xml](file:///e:/heimaspringboot/big_event/pom.xml) | 后端依赖配置 |
| NPM依赖 | [package.json](file:///e:/heimaspringboot/big_event/frontend/package.json) | 前端依赖配置 |

---

## 12. 总结

知库CMS是一个功能完善、架构清晰的内容管理系统。采用前后端分离架构，后端使用Spring Boot 3 + MyBatis Plus + Redis等技术栈，前端使用Vue 3 + TypeScript + Element Plus技术栈。系统功能涵盖文章管理、用户系统、权限管理、数据统计等多个方面，适合作为学习项目或二次开发的基础。

