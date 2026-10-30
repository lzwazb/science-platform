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

### 2.14 知识贡献（提交与个人）

需登录。游客不能提交贡献。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/v1/contributions` | 提交补充 / 纠错 / 修改建议 |
| GET | `/api/v1/contributions/mine` | 我的贡献，支持 `status` 筛选 |
| GET | `/api/v1/contributions/{id}` | 查看本人某条贡献，**非本人 403** |
| POST | `/api/v1/contributions/{id}/withdraw` | 撤回仍在审核中的建议 |

**请求体**（四个字段全部必填）：

| 字段 | 类型 | 校验 |
| --- | --- | --- |
| `articleId` | long | 必须指向已发布文章 |
| `contributionType` | string | `SUPPLEMENT` / `CORRECTION` / `MODIFICATION` |
| `suggestedContent` | string | 非空，≤ 5000 字 |
| `reason` | string | 非空，≤ 1000 字 |
| `referenceSource` | string | 非空，≤ 500 字（档案「推荐必填」按必填补齐） |

**实测**：四字段缺失时返回 `400 reason 提交理由不能为空；referenceSource 参考来源不能为空；suggestedContent 建议内容不能为空`，且数据库无新增记录。

### 2.15 知识贡献审核

需 `REVIEWER`、`ADMIN` 或 `SUPER_ADMIN`。创作者访问返回 403（实测）。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/v1/review/contributions` | 列表，**默认 `status=PENDING`**，可按 `articleId` 过滤 |
| GET | `/api/v1/review/contributions/{id}` | 审核详情，含文章当前正文供对照 |
| POST | `/api/v1/review/contributions/{id}/approve` | 通过，body `{"comment": "...", "finalContent": "..."}` |
| POST | `/api/v1/review/contributions/{id}/reject` | 驳回，`comment` 必填 |

**审核通过的事务边界**（开发档案 5.1，本项目最关键的约束）：

```
BEGIN
  1. 校验贡献状态为 PENDING，读取目标文章
  2. 写历史版本快照（version_no = 文章当前版本号，内容是「被覆盖之前的旧正文」）
  3. 更新文章：正文替换为 finalContent，version_no + 1
  4. 条件更新贡献状态 PENDING → APPROVED（带 status 条件，防并发重复处理）
  5. 写操作日志 CONTRIBUTION_APPROVE
  6. 生成站内通知（接收人 = 提交者）
COMMIT
```

任一步失败整体回滚。第 4 步的 `affected == 0`（被并发处理）也会抛异常触发回滚，
避免出现「版本快照已写但状态没改」的中间态。

**`finalContent` 的作用**：开发档案 5.1 要求「通过时必须明确最终采用的修改内容」。
评审员在通过时可提交自己合并后的完整正文；不传则后端以 `suggestedContent` 作为新正文。

**实测（T08）**：对文章 8 提交纠错并通过后，
文章 `version_no` 1 → 2、正文 523 → 596 字；
`article_version` 新增 `version_no=1` 快照，内容为变更前的 523 字，`source_contribution_id` 指向该贡献；
`sys_notification` 新增 1 条 `REVIEW_RESULT` 通知给提交者；`sys_operation_log` 新增 1 条审核记录。

### 2.16 文章版本历史

```
GET /api/v1/articles/{id}/versions
GET /api/v1/articles/{id}/versions/{versionNo}
```

无需认证（已发布文章的版本历史公开，体现内容可追溯）。未发布文章要求评审员及以上，否则 404。

列表按 `versionNo` 倒序；不存在的版本返回 `404 该版本不存在`。

**版本号语义**：`science_article.version_no = N` 表示当前是第 N 版；
`article_version` 里存的是**已被覆盖的旧内容**，所以最多到 `N-1`。
详见 [数据库设计](数据库设计/README.md#51-为什么-article_versionversion_no-表示变更前的内容)。

### 2.17 点赞与收藏

| 方法 | 路径 | 认证 | 用途 |
| --- | --- | --- | --- |
| POST | `/api/v1/interactions/likes` | 需登录 | 点赞 |
| DELETE | `/api/v1/interactions/likes` | 需登录 | 取消点赞 |
| POST | `/api/v1/interactions/favorites` | 需登录 | 收藏 |
| DELETE | `/api/v1/interactions/favorites` | 需登录 | 取消收藏 |
| GET | `/api/v1/interactions/status` | **公开** | 查互动状态与计数 |
| GET | `/api/v1/interactions/favorites/mine` | 需登录 | 我的收藏 |

写操作的 body（DELETE 也带 body）：`{"targetType": "ARTICLE|WORK", "targetId": 1}`。

**幂等设计**：

- 重复点赞返回 `200` 且 `likeCount` 不变，**不会报错也不会产生第二条记录**
- 取消未点赞的内容同样 `200`
- 防重复靠数据库唯一索引 `uk_like(user_id, target_type, target_id)`，捕获 `DuplicateKeyException` 后当作「已点赞」返回，而不是先查后插（并发下先查后插会漏判）

**目标校验**：`targetType + targetId` 必须指向**已发布**的文章或作品，否则 `404`。
实测给不存在的 ID 与给草稿文章点赞都返回 `404 文章不存在或未发布`。

**状态查询**（游客可访问）：

```json
{
  "targetType": "ARTICLE",
  "targetId": 1,
  "liked": false,
  "favorited": false,
  "likeCount": 3,
  "favoriteCount": 1,
  "commentCount": 2
}
```

游客能看计数，但 `liked` / `favorited` 恒为 `false`（没有身份可比对）。

**我的收藏**：文章与作品混排，按收藏时间倒序，每项带 `available` 标记——
目标已下架/删除时为 `false`，前端显示「内容已下架」且不可点击。

### 2.18 评论

| 方法 | 路径 | 认证 | 用途 |
| --- | --- | --- | --- |
| GET | `/api/v1/comments?targetType=&targetId=&page=&size=` | **公开** | 按目标查评论 |
| POST | `/api/v1/comments` | 需登录 | 发表评论或回复 |
| DELETE | `/api/v1/comments/{id}` | 需登录 | 删除（本人或管理员） |

请求体：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `targetType` | string | `ARTICLE` / `WORK` |
| `targetId` | long | 目标 ID |
| `content` | string | 非空，≤ 1000 字 |
| `parentId` | long | 可选，回复某条评论 |

**只做一层回复**：对「回复」再次回复时，后端把它挂到同一个根评论下（`parentId` 归一化到根），
避免无限嵌套带来的排序与渲染复杂度。实测：三级回复的 `parentId` 返回的是一级评论 ID。

**删除权限**：本人可删，管理员可删任意评论，其他人 `403 只能删除自己的评论`。
删除是状态位 `DELETED` 而非物理删除，保留楼层结构。

响应中每条评论带 `mine` 字段，前端据此决定是否显示删除按钮。

### 2.19 内容反馈

| 方法 | 路径 | 认证 | 用途 |
| --- | --- | --- | --- |
| POST | `/api/v1/feedback` | 需登录 | 提交反馈 |
| GET | `/api/v1/feedback/mine` | 需登录 | 我的反馈 |
| GET | `/api/v1/admin/feedback` | ADMIN 及以上 | 后台反馈列表 |
| POST | `/api/v1/admin/feedback/{id}/handle` | ADMIN 及以上 | 处理反馈 |

**发布反馈**：`targetType` 支持 `ARTICLE` / `WORK` / `COMMENT`；
`feedbackType` 取 `WRONG_CONTENT` / `COPYRIGHT` / `SPAM` / `SUGGESTION` / `OTHER`；`description` 必填。

针对 `COMMENT` 的反馈，后台列表的 `targetTitle` 会显示「评论：<内容前 30 字>」，方便定位。

**处理**：body `{"status": "RESOLVED|PROCESSING|REJECTED", "handleComment": "..."}`。
已处理完（`RESOLVED` / `REJECTED`）的反馈不能再次处理 → `400 该反馈已处理完毕，不能再次处理`。
处理动作写入 `sys_operation_log`（`action = FEEDBACK_HANDLE`）。

普通用户访问后台反馈接口返回 `403`（实测）。

### 2.20 活动公开接口

| 方法 | 路径 | 认证 | 用途 |
| --- | --- | --- | --- |
| GET | `/api/v1/events` | 公开 | 活动列表（含时间状态） |
| GET | `/api/v1/events/{id}` | 公开 | 活动详情（规则、优秀作品、我的投稿） |
| GET | `/api/v1/events/{id}/awards` | 公开 | 优秀作品 |

**时间状态不落库**：`timeStatus` 每次读取时按当前时间计算，取值：

| 值 | 含义 | 判定 |
| --- | --- | --- |
| `NOT_STARTED` | 未开始 | 当前时间 < `startAt` |
| `ONGOING` | 进行中 | `startAt` ≤ 当前时间 ≤ `endAt` |
| `ENDED` | 已结束 | 管理员手动结束，或当前时间 > `endAt` |
| `UNPUBLISHED` | 未发布 | 草稿或已取消 |

如果把时间状态也存成字段，时间一过就会出现「状态字段还是进行中但实际已过期」的不一致，
所以这里用现算。`acceptingSubmission` 只在 `ONGOING` 时为 `true`。

> 自查记录：初版 `timeStatusOf` 只判断 `PUBLISHED`，导致**已手动结束的活动被报成「未发布」**。
> 修正为「草稿/已取消 → 未发布，已结束 → 已结束，其余按时间判断」。

草稿与已取消的活动对前台返回 `404`，不出现在列表里。

### 2.21 活动投稿（创作者）

| 方法 | 路径 | 认证 | 用途 |
| --- | --- | --- | --- |
| POST | `/api/v1/events/{id}/submissions` | CREATOR 及以上 | 提交作品，body `{"workId": 1}` |
| GET | `/api/v1/events/{id}/submissions/mine` | 需登录 | 我在该活动的投稿 |
| GET | `/api/v1/events/submissions/mine` | 需登录 | 我的全部投稿 |

> 注意：`GET /api/v1/events/*/submissions/**` 在安全配置里**先于** `/events/**` 的公开规则匹配，
> 因此「我的投稿」不会被当作公开接口放行。实测未登录访问返回 401。

**四道校验**（T11）：

| 场景 | 结果 |
| --- | --- |
| 活动未开始 | `400 活动尚未开始，暂不能投稿` |
| 活动已结束 | `400 活动已结束，不能再投稿` |
| 活动是草稿 | `400 活动未发布，不能投稿` |
| 作品不属于自己 | `403 只能提交自己的作品` |
| 作品不是已发布状态 | `400 只有已发布的作品可以投稿` |
| 同一作品重复投稿同一活动 | `400 该作品已报名本活动，不能重复投稿` |
| 普通用户投稿 | `403`（角色限制） |

重复投稿同时有唯一索引 `uk_event_work(event_id, work_id)` 兜底，并发下也不会产生两条。

### 2.22 活动评审与获奖确认

| 方法 | 路径 | 认证 | 用途 |
| --- | --- | --- | --- |
| GET | `/api/v1/review/events/{eventId}/submissions` | REVIEWER 及以上 | 投稿列表（含均分、我是否已评） |
| POST | `/api/v1/review/submissions/{id}/review` | REVIEWER 及以上 | 打分，body `{"score": 1-5, "comment": "..."}` |
| POST | `/api/v1/admin/events/{id}/awards` | **ADMIN 及以上** | 确认优秀作品 |
| POST | `/api/v1/admin/events/submissions/{id}/revoke-award` | ADMIN 及以上 | 撤销获奖标记 |

**评审是 upsert**：同一评审员对同一投稿只保留一份有效评审，重复提交会**覆盖**上次的分数与评语，
不会新增记录（唯一索引 `uk_submission_reviewer` 兜底）。实测两次评分后记录数不变、分数被更新。

评分范围 1–5，评语必填。越界或缺少评语返回 400。

**获奖确认的关键约束**（开发档案 FR-EV-05「不得把未评审作品自动标为优秀」）：

每个被选中的投稿**必须至少有一条评审记录**，否则整体拒绝：
`400 投稿 #4 还没有任何评审记录，不能直接标为优秀`。

其他规则：

| 场景 | 结果 |
| --- | --- |
| 投稿不属于该活动 | `400 投稿 3 不属于本活动` |
| 同一投稿重复获奖 | `400 同一投稿不能重复获奖` |
| 评审员提交获奖名单 | `403`（仅管理员） |
| 确认成功后 | 写入 `award_level` 与 `status = SELECTED`，记 `EVENT_AWARD_CONFIRM` 操作日志 |

获奖作品会出现在活动详情的 `awards` 里，带 `averageScore`（评审均分），便于答辩演示时说明评审依据。

## 3. 规划中的接口清单

以下路径来自开发档案 8.3 节，**尚未实现**。实际实现时以本表为准，并在完成后移入第 2 节。
（阶段 3 已实现认证与个人资料 5 个接口，阶段 4 实现知识库与后台内容维护 20 个接口，
阶段 5 实现作品公开 / 创作者 / 审核 13 个接口，阶段 6 实现知识贡献 / 审核 / 版本 8 个接口，
阶段 7 实现互动 / 评论 / 反馈 15 个接口，阶段 8 实现活动相关 13 个接口，见 2.2–2.22。）

| 模块 | 方法 | 路径 | 用途 | 计划阶段 |
| --- | --- | --- | --- | --- |
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
