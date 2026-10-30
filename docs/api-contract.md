# 接口约定（API Contract）

> 基线：开发档案第 8 章。本文件随阶段推进持续更新，**代码与本文档保持同步**。
> 最后更新：2026-10-30（阶段 2）

## 1. 全局约定

### 1.1 前缀与风格

- 所有接口统一前缀 `/api/v1`
- 资源名使用复数名词；HTTP 方法表达动作：`GET` 查询、`POST` 创建/动作、`PUT` 修改、`DELETE` 删除
- 分页参数：`page`（从 1 开始）、`size`（默认 10）
- 常见筛选参数：`keyword`、`categoryId`、`status`

### 1.2 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-10-30 13:13:05"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 业务码，与 HTTP 状态码语义一致 |
| `message` | string | 成功为 `success`，失败为用户可读中文说明 |
| `data` | any | 业务数据，失败时为 `null` |
| `timestamp` | string | 服务端响应时间，格式 `yyyy-MM-dd HH:mm:ss` |

分页数据放在 `data` 内：

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 10
}
```

### 1.3 状态码

| HTTP | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录或令牌失效 |
| 403 | 已登录但无权限 |
| 404 | 资源不存在 |
| 500 | 服务端异常（不返回堆栈） |

### 1.4 认证

- 阶段 3 起启用 JWT，请求头携带 `Authorization: Bearer <token>`
- 操作者身份**一律从后端认证上下文获取**，不接受前端传入 `reviewerId`、`roleCode` 等身份字段作为授权依据
- 前端路由守卫只改善体验，真实授权在后端完成

### 1.5 错误响应示例

错误同样使用统一结构，`data` 为 `null`，`code` 与 HTTP 状态码一致。以下为阶段 2 实测输出。

**404 · 访问不存在的路径**

```json
{
  "code": 404,
  "message": "请求的资源不存在",
  "data": null,
  "timestamp": "2026-10-30 13:21:19"
}
```

**405 · 请求方法不支持**

```json
{
  "code": 405,
  "message": "不支持该请求方法：POST",
  "data": null,
  "timestamp": "2026-10-30 13:21:19"
}
```

参数校验失败（阶段 3 起出现，由 Bean Validation 产生）：

```json
{
  "code": 400,
  "message": "username 不能为空；password 长度需在 6 到 32 之间",
  "data": null,
  "timestamp": "2026-10-30 13:21:19"
}
```

多个字段错误用中文分号 `；` 连接，字段名沿用 DTO 属性名。

**异常处理实现位置**：`science-platform-server/src/main/java/com/science/platform/common/exception/GlobalExceptionHandler.java`

处理范围：业务异常、`@RequestBody` 校验、表单绑定校验、方法参数约束、缺少必填参数、参数类型不匹配、JSON 解析失败、方法不支持、路径不存在、兜底异常。兜底分支写完整堆栈到日志，对外只返回「服务端异常，请稍后重试」。

## 2. 已实现接口

### 2.1 健康检查

```
GET /api/v1/health
```

无需认证。

**响应示例**（实测输出，2026-10-30 13:21:12）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "application": "science-platform-server",
    "project": "基于 Spring Boot + Vue 的科普知识共创与创意传播平台",
    "stage": "阶段 2：数据库与基础规范",
    "serverTime": "2026-10-30 13:21:12",
    "db": {
      "status": "UP",
      "database": "science_platform",
      "publishedArticleCount": 8
    }
  },
  "timestamp": "2026-10-30 13:21:12"
}
```

`db` 段为阶段 2 新增：每次调用真实执行一次 `SELECT COUNT(*)`，使健康检查能反映「服务在跑但数据库断开」的情况，而不是只证明进程存活。

**实现位置**：`science-platform-server/src/main/java/com/science/platform/modules/system/HealthController.java`

## 3. 规划中的接口清单

以下路径来自开发档案 8.3 节，**尚未实现**。实际实现时以本表为准，并在完成后移入第 2 节。

| 模块 | 方法 | 路径 | 用途 | 计划阶段 |
| --- | --- | --- | --- | --- |
| 认证 | POST | `/auth/register` | 公开注册普通用户 | 3 |
| 认证 | POST | `/auth/login` | 登录并返回认证信息 | 3 |
| 认证 | GET | `/auth/me` | 获取当前登录用户 | 3 |
| 用户 | GET | `/users/me` | 查看个人资料 | 3 |
| 用户 | PUT | `/users/me` | 更新可编辑个人资料 | 3 |
| 知识 | GET | `/articles` | 分页查询已发布文章 | 4 |
| 知识 | GET | `/articles/{id}` | 查看文章详情 | 4 |
| 知识 | GET | `/articles/search?keyword=` | 关键词搜索 | 4 |
| 分类 | GET | `/categories?type=ARTICLE` | 公开分类列表 | 4 |
| 专题 | GET | `/topics` | 专题列表 | 4 |
| 专题 | GET | `/topics/{id}` | 专题详情及文章 | 4 |
| 知识管理 | POST | `/admin/articles` | 管理员创建文章 | 4 |
| 知识管理 | PUT | `/admin/articles/{id}` | 管理员修改文章 | 4 |
| 知识管理 | POST | `/admin/articles/{id}/publish` | 发布文章 | 4 |
| 知识贡献 | POST | `/contributions` | 提交知识贡献 | 6 |
| 知识贡献 | GET | `/contributions/mine` | 查看本人贡献记录 | 6 |
| 知识审核 | GET | `/reviews/contributions` | 查看待审核贡献 | 6 |
| 知识审核 | POST | `/reviews/contributions/{id}/approve` | 通过贡献并更新文章版本 | 6 |
| 知识审核 | POST | `/reviews/contributions/{id}/reject` | 驳回贡献并填写意见 | 6 |
| 版本 | GET | `/articles/{id}/versions` | 查看文章版本列表 | 6 |
| 版本 | GET | `/articles/{id}/versions/{versionNo}` | 查看某历史版本 | 6 |
| 作品 | GET | `/works` | 公开作品列表 | 5 |
| 作品 | GET | `/works/{id}` | 作品详情 | 5 |
| 作品 | POST | `/works` | 创作者创建作品 | 5 |
| 作品 | PUT | `/works/{id}` | 编辑本人作品 | 5 |
| 作品 | DELETE | `/works/{id}` | 删除/下架本人作品 | 5 |
| 作品关联 | PUT | `/works/{id}/articles` | 更新作品关联文章 | 5 |
| 互动 | POST | `/interactions/likes` | 点赞 | 7 |
| 互动 | DELETE | `/interactions/likes` | 取消点赞 | 7 |
| 互动 | POST | `/interactions/favorites` | 收藏 | 7 |
| 互动 | DELETE | `/interactions/favorites` | 取消收藏 | 7 |
| 评论 | GET | `/comments` | 按目标查询评论 | 7 |
| 评论 | POST | `/comments` | 发表评论 | 7 |
| 评论 | DELETE | `/comments/{id}` | 删除本人评论或由管理员处理 | 7 |
| 反馈 | POST | `/feedback` | 提交内容反馈 | 7 |
| 活动 | GET | `/events` | 活动列表 | 8 |
| 活动 | GET | `/events/{id}` | 活动详情 | 8 |
| 活动管理 | POST | `/admin/events` | 管理员创建活动 | 8 |
| 投稿 | POST | `/events/{id}/submissions` | 提交作品参加活动 | 8 |
| 投稿 | GET | `/events/{id}/submissions/mine` | 查看本人活动投稿 | 8 |
| 评审 | POST | `/reviews/submissions/{id}` | 评审活动投稿 | 8 |
| 通知 | GET | `/notifications` | 查看本人消息 | 9 |
| 通知 | PUT | `/notifications/{id}/read` | 标记本人消息已读 | 9 |
| 后台 | GET | `/admin/users` | 超级管理员查询用户 | 9 |
| 后台 | PUT | `/admin/users/{id}/roles` | 分配用户角色 | 9 |

## 4. 前端调用约定

- Axios 实例：`science-platform-web/src/api/request.js`，`baseURL = '/api/v1'`，超时 10s
- 响应拦截器自动解包 `{ code, message, data }`，业务代码直接拿到 `data`
- 开发期由 Vite 代理把 `/api` 转发到 `http://localhost:8080`（见 `vite.config.js`），业务代码中**不允许**硬编码后端地址

## 5. 分页实现约定（阶段 2 起）

后端使用 MyBatis-Plus `PaginationInnerInterceptor`，配置位置 `config/MybatisPlusConfig.java`：

| 配置 | 取值 | 理由 |
| --- | --- | --- |
| `maxLimit` | 100 | 防止前端传入超大 `size` 拖垮数据库 |
| `overflow` | false | 页码超出总页数时返回空列表，不回落到首页，避免分页行为难以预期 |
| 数据库类型 | MYSQL | 生成 `LIMIT` 分页 SQL |

请求参数统一为 `page`（从 1 开始）、`size`（默认 10），响应 `data` 结构为：

```json
{
  "records": [],
  "total": 8,
  "pageNum": 1,
  "pageSize": 3
}
```

阶段 2 已通过 `ArticlePaginationTest` 实测：8 条已发布数据按 `size=3` 分页，首页 3 条、末页 2 条、页码 10 返回空列表且 `total` 仍为 8——若分页插件未生效，首页会返回全部 8 条，测试即失败。
