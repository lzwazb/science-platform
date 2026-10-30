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

- 使用 JWT（HS512），登录后在请求头携带 `Authorization: Bearer <token>`
- 默认有效期 120 分钟，过期或非法令牌统一返回 401
- 操作者身份**一律从后端认证上下文获取**，不接受前端传入 `reviewerId`、`roleCode` 等身份字段作为授权依据
- 前端路由守卫只改善体验，真实授权在后端完成
- 详细设计见 [认证与权限设计](认证与权限设计.md)

**受保护接口一览**（阶段 3 的授权规则，越具体的规则越靠前）：

| 路径模式 | 访问要求 |
| --- | --- |
| `OPTIONS /**` | 放行（CORS 预检） |
| `/api/v1/health`、`/api/v1/auth/login`、`/api/v1/auth/register` | 公开 |
| `GET /api/v1/articles/**` 等只读内容接口 | 公开（游客可浏览） |
| `/api/v1/admin/users/**` | `SUPER_ADMIN` |
| `/api/v1/admin/**` | `ADMIN` 及以上 |
| 其他 | 需登录 |

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

### 2.2 注册

```
POST /api/v1/auth/register
Content-Type: application/json
```

无需认证。只能注册普通用户（`USER` 角色），请求体中没有角色字段。

| 字段 | 类型 | 必填 | 校验 |
| --- | --- | --- | --- |
| `username` | string | 是 | 3–20 位，仅字母/数字/下划线，全局唯一 |
| `password` | string | 是 | 6–32 位 |
| `nickname` | string | 是 | ≤ 20 位 |
| `email` | string | 否 | 邮箱格式，≤ 100 位 |

**成功响应**（实测）：

```json
{
  "code": 200,
  "message": "注册成功，请登录",
  "data": {
    "id": 7,
    "username": "test9388",
    "nickname": "测试用户",
    "avatar": null,
    "email": "test9388@example.com",
    "bio": null,
    "status": "ACTIVE",
    "roles": ["USER"],
    "createdAt": "2026-10-30 13:30:28"
  },
  "timestamp": "2026-10-30 13:30:28"
}
```

**失败响应**：用户名被占用 → `400 用户名已被占用`；字段校验失败 → `400`，消息为中文拼接（实测 `nickname 昵称不能为空；username 用户名长度需在 3 到 20 个字符之间；password 密码长度需在 6 到 32 个字符之间`）。

### 2.3 登录

```
POST /api/v1/auth/login
Content-Type: application/json
```

无需认证。

```json
{ "username": "admin01", "password": "123456" }
```

**成功响应**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "user": { "id": 2, "username": "admin01", "nickname": "内容管理员", "roles": ["ADMIN"], "createdAt": "2026-01-05 09:05:00" }
  },
  "timestamp": "2026-10-30 13:28:34"
}
```

**失败响应**：

| 场景 | 状态码 | message |
| --- | --- | --- |
| 账号不存在 | 401 | 用户名或密码错误 |
| 密码错误 | 401 | 用户名或密码错误 |
| 账号被禁用 | 403 | 账号已被禁用，请联系管理员 |

前两种必须返回完全相同的 message，防止被用来枚举用户名。

### 2.4 获取当前登录用户

```
GET /api/v1/auth/me
Authorization: Bearer <token>
```

返回结构与注册响应中的 `data` 一致（`UserProfileVO`）。用户 ID 取自令牌，不接受请求参数。

### 2.5 个人资料

```
GET /api/v1/users/me      # 查看本人资料
PUT /api/v1/users/me      # 更新本人资料
Authorization: Bearer <token>
```

`PUT` 只接受以下字段，未传或传 `null` 表示不修改：

| 字段 | 类型 | 校验 |
| --- | --- | --- |
| `nickname` | string | ≤ 20 位，不允许纯空白 |
| `avatar` | string | ≤ 255 位 |
| `email` | string | 邮箱格式，≤ 100 位 |
| `bio` | string | ≤ 500 位 |

用户名、角色、账号状态**不在可修改范围内**（开发档案 FR-ME-01）。

实测：`PUT` 非法邮箱返回 `400 email 邮箱格式不正确`。

### 2.6 文章列表与搜索

```
GET /api/v1/articles?page=1&size=10&categoryId=&keyword=
GET /api/v1/articles/search?keyword=&page=1&size=10
```

无需认证（游客可访问）。**只返回 `status = PUBLISHED` 且未删除的文章。**

| 参数 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `page` | int | 1 | 页码，≥ 1 |
| `size` | int | 10 | 每页条数，1–100 |
| `categoryId` | long | — | 按分类筛选 |
| `keyword` | string | — | 匹配标题或摘要 |

排序：`published_at DESC, id DESC`（同一时间用 id 兜底，保证分页稳定）。

**响应示例**（实测）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 8,
        "title": "疫苗为什么有效：一次预演换来长期记忆",
        "summary": "疫苗通过引入安全的抗原……",
        "coverUrl": "/images/articles/vaccine.svg",
        "categoryId": 6,
        "categoryName": "健康医学",
        "authorId": 2,
        "authorName": "内容管理员",
        "status": "PUBLISHED",
        "publishedAt": "2026-09-20 09:00:00",
        "viewCount": 720,
        "versionNo": 1
      }
    ],
    "total": 8,
    "pageNum": 1,
    "pageSize": 3
  },
  "timestamp": "2026-10-30 13:40:48"
}
```

**列表项不含 `content`**，避免列表接口返回大量长文本。

**关键词转义**：`%`、`_`、`\` 会被转义后再拼进 LIKE。实测 `keyword=%` 返回 0 条而非全部 8 条。这不是防注入（查询已参数化），而是保证搜索结果符合用户预期。

### 2.7 文章详情

```
GET /api/v1/articles/{id}
```

无需认证。返回正文、分类名、作者昵称、所属已发布专题。

**副作用**：每次访问把 `view_count` +1（档案 FR-KB-03 的「阅读量」为可选项，此处做最小实现）。

文章不存在、或状态不是 `PUBLISHED` → `404 文章不存在或未发布`。草稿与已下架文章对游客和普通用户都不可见。

### 2.8 分类

```
GET /api/v1/categories?type=ARTICLE
```

无需认证。只返回 `status = ENABLED` 的分类，按 `sortOrder` 升序。`type` 可选，取 `ARTICLE` / `WORK`，为空返回全部类型。

### 2.9 专题

```
GET /api/v1/topics        # 列表，附带 articleCount
GET /api/v1/topics/{id}   # 详情，含按 sort_order 排列的文章
```

无需认证。只返回 `status = PUBLISHED` 的专题；专题内文章只含已发布文章。

`articleCount` 统计的是**专题内已发布文章数**，不含草稿。专题详情里的文章顺序取 `topic_article.sort_order`，而不是文章发布时间——实测专题 2 返回顺序为 DNA(3) → 光合作用(4) → 疫苗(8)，与发布时间顺序不同。

### 2.10 后台内容维护

以下接口均需 `ADMIN` 或 `SUPER_ADMIN`。普通用户返回 403（实测）。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/v1/admin/articles` | 文章列表，含草稿与已下架；支持 `status`、`categoryId`、`keyword` |
| GET | `/api/v1/admin/articles/{id}` | 查看文章（允许草稿），不增加阅读量 |
| POST | `/api/v1/admin/articles` | 创建文章，初始状态 `DRAFT` |
| PUT | `/api/v1/admin/articles/{id}` | 编辑文章 |
| POST | `/api/v1/admin/articles/{id}/publish` | 发布 |
| POST | `/api/v1/admin/articles/{id}/offline` | 下架 |
| DELETE | `/api/v1/admin/articles/{id}` | 删除（逻辑删除） |
| GET | `/api/v1/admin/categories` | 分类列表，含已停用 |
| POST | `/api/v1/admin/categories` | 创建分类 |
| PUT | `/api/v1/admin/categories/{id}` | 编辑分类 |
| DELETE | `/api/v1/admin/categories/{id}` | 删除分类 |
| GET | `/api/v1/admin/topics` | 专题列表，含草稿 |
| GET | `/api/v1/admin/topics/{id}` | 专题详情，含草稿文章 |
| POST | `/api/v1/admin/topics` | 创建专题 |
| PUT | `/api/v1/admin/topics/{id}` | 编辑专题 |
| DELETE | `/api/v1/admin/topics/{id}` | 删除专题（同时清理关联） |
| PUT | `/api/v1/admin/topics/{id}/articles` | 整体替换专题内文章，body 为 `{"articleIds": [5, 6]}` |

**业务规则实测**：

| 场景 | 结果 |
| --- | --- |
| 重复发布已发布文章 | `400 文章已是发布状态` |
| 删除分类但分类下仍有文章 | `400 该分类下还有 2 篇文章，请先移动文章或将分类改为停用` |
| 同一类型下创建同名分类 | `400 同一类型下已存在同名分类` |
| 分类类型不是 ARTICLE/WORK | `400 type 分类类型只能是 ARTICLE 或 WORK` |
| 给专题设置不存在的文章 ID | `400 部分文章不存在，请刷新后重试`，且**整体回滚**（原有关联不变） |
| 新建文章后前台列表 | 仍为 8 篇，草稿不可见；公开详情 404 |
| 发布后前台列表 | 变为 9 篇 |
| 下架后前台列表 | 恢复 8 篇 |

**编辑已发布文章不触发重新审核**：文章由管理员直接维护，与「作品编辑后需重新审核」的规则不同。
**编辑文章不改 `version_no`**：版本号只在「知识贡献被采纳」的审核流程中递增（阶段 6），保证版本历史与审核记录一一对应。

### 2.11 作品公开接口

```
GET /api/v1/works?page=1&size=12&categoryId=&keyword=
GET /api/v1/works/{id}
```

无需认证。**只返回 `status = PUBLISHED` 的作品**——草稿、待审核、已驳回、已下架的作品在列表和详情中一律不可见。

作品详情返回创作者昵称与简介、以及关联的科普文章列表（`articles`），体现「知识与作品关联」这一项目特色。

**反向关联**：`GET /api/v1/articles/{id}` 的响应里有 `works` 字段，返回该文章关联的已发布作品。实测文章 7 返回「碳足迹的一天：从早餐到熄灯」。

### 2.12 创作者中心

需 `CREATOR`、`ADMIN` 或 `SUPER_ADMIN`。普通用户返回 403（实测）。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/v1/creator/works` | 我的作品分页，含草稿与驳回；支持 `status` 筛选 |
| GET | `/api/v1/creator/works/{id}` | 我的作品详情，允许草稿；**非本人返回 403** |
| POST | `/api/v1/creator/works` | 创建作品，初始 `DRAFT` |
| PUT | `/api/v1/creator/works/{id}` | 编辑作品 |
| POST | `/api/v1/creator/works/{id}/submit` | 提交审核 |
| DELETE | `/api/v1/creator/works/{id}` | 删除（逻辑删除） |
| PUT | `/api/v1/creator/works/{id}/articles` | 更新关联文章，body `{"articleIds": [1, 2]}` |

> **路径变更说明**：开发档案 8.3 把「创建作品」写在 `POST /api/v1/works`，这里改为 `/api/v1/creator/works`。
> 原因是 `/api/v1/works/**` 在安全配置里是公开路径前缀，把写操作混进公开前缀会让授权规则难以表达；
> 拆到 `/creator/**` 后可以给整块路径统一加角色限制，语义也更清楚。

**状态流转**：

```
创建 → DRAFT ──submit──> PENDING ──approve──> PUBLISHED ──offline──> OFFLINE
                            └──reject──> REJECTED ──submit──> PENDING
PUBLISHED ──编辑──> PENDING（自动重新送审）
```

**业务规则实测**：

| 场景 | 结果 |
| --- | --- |
| 用户 B 查看 / 修改 / 删除用户 A 的作品 | `403`（「只能操作自己的作品」「只能查看自己的作品」） |
| 重复提交审核 | `400 作品已在审核中，请勿重复提交` |
| 提交已发布作品 | `400 作品已发布，如需修改请编辑后重新提交审核` |
| 驳回但不填意见 | `400 驳回时必须填写审核意见` |
| 重新提交后 | `status=PENDING`，**旧审核意见与审核时间被清空** |
| 编辑已发布作品 | 自动变 `PENDING`，从公开列表消失 |
| 删除被活动投稿引用的作品 | `400 该作品已被 1 个活动投稿引用，不能删除` |
| 关联未发布的文章 | `400 只能关联已发布的科普文章` |
| 关联不存在的文章 | `400 部分关联文章不存在` |
| 作品分类选了文章分类 | `400 所选分类不是作品分类` |

### 2.13 作品审核

需 `REVIEWER`、`ADMIN` 或 `SUPER_ADMIN`。创作者访问返回 403（实测）。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/v1/review/works` | 作品列表，**默认 `status=PENDING`** |
| GET | `/api/v1/review/works/{id}` | 审核详情，含作品全文与关联文章 |
| POST | `/api/v1/review/works/{id}/approve` | 通过，body `{"comment": "..."}`（意见可空） |
| POST | `/api/v1/review/works/{id}/reject` | 驳回，body `{"comment": "..."}`（意见必填） |
| POST | `/api/v1/review/works/{id}/offline` | 下架已发布作品 |

**并发防护**：三个动作都要求操作前状态为 `PENDING`（下架要求 `PUBLISHED`），
否则返回 `400 该作品当前状态为已发布，不能进行审核`。这样可以防止两位评审员同时点「通过」造成状态错乱。

## 3. 规划中的接口清单

以下路径来自开发档案 8.3 节，**尚未实现**。实际实现时以本表为准，并在完成后移入第 2 节。
（阶段 3 已实现认证与个人资料 5 个接口，阶段 4 实现知识库与后台内容维护 20 个接口，
阶段 5 实现作品公开 / 创作者 / 审核 13 个接口，见 2.2–2.13。）

| 模块 | 方法 | 路径 | 用途 | 计划阶段 |
| --- | --- | --- | --- | --- |
| 知识贡献 | POST | `/contributions` | 提交知识贡献 | 6 |
| 知识贡献 | GET | `/contributions/mine` | 查看本人贡献记录 | 6 |
| 知识审核 | GET | `/reviews/contributions` | 查看待审核贡献 | 6 |
| 知识审核 | POST | `/reviews/contributions/{id}/approve` | 通过贡献并更新文章版本 | 6 |
| 知识审核 | POST | `/reviews/contributions/{id}/reject` | 驳回贡献并填写意见 | 6 |
| 版本 | GET | `/articles/{id}/versions` | 查看文章版本列表 | 6 |
| 版本 | GET | `/articles/{id}/versions/{versionNo}` | 查看某历史版本 | 6 |
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
