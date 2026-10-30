# 科普知识共创与创意传播平台

> 本科毕业设计 · 基于 Spring Boot + Vue 的科普知识共创与创意传播平台的设计与实现

当前进度：**全部 11 个阶段（0–10）已完成，验收清单 20 条全部通过**。见 [开发日志](docs/development-log.md)。

## 项目结构

```
毕业设计/
├── science-platform-server/     # Spring Boot 3.5 + Java 21 后端
├── science-platform-web/        # Vue 3 + Vite 前端
├── database/                    # schema.sql 与 seed.sql
├── docs/                        # 环境记录、接口文档、数据库设计、需求分析、开发日志
├── maven-settings.xml           # 项目专用 Maven 配置（阿里云镜像 + 项目内本地仓库）
└── .gitignore                   # 忽略构建产物、依赖、本地配置与真实密码
```

## 开发前提

版本要求与已核验的本机环境见 [docs/environment.md](docs/environment.md)。

| 组件 | 本机版本 |
| --- | --- |
| JDK | 21.0.6（Oracle） |
| Maven | 3.9.9 |
| Node.js | v22.19.0 |
| npm | 11.6.4 |
| MySQL | 8.0.42 |

## 本地开发

### 1. 初始化数据库

```powershell
cd database
mysql -u root -p --default-character-set=utf8mb4 -e "source schema.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source seed.sql"
```

演示账号与数据说明见 [database/README.md](database/README.md)。所有演示账号密码均为 `123456`。

### 2. 配置数据库密码与 JWT 密钥

复制 `science-platform-server/src/main/resources/application-local-example.yml` 为 `application-local.yml`，填入本机数据库密码与 JWT 密钥。
该文件已被 `.gitignore` 忽略，**不要把真实密码或密钥提交到 Git**。

JWT 密钥至少 32 字节，可用下面的命令生成：

```powershell
$b = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

也可以用环境变量覆盖：`DB_USERNAME`、`DB_PASSWORD`、`APP_JWT_SECRET`。

> 密钥未配置时后端会**拒绝启动**并给出明确提示，这是有意设计，避免弱密钥上线。

### 3. 启动后端

```powershell
cd science-platform-server
mvn -s ..\maven-settings.xml spring-boot:run
```

后端启动在 `http://localhost:8080`，健康检查：

```powershell
curl http://localhost:8080/api/v1/health
```

预期返回（含数据库状态与已发布文章数）：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "db": { "status": "UP", "database": "science_platform", "publishedArticleCount": 8 }
  },
  "timestamp": "2026-10-30 13:21:12"
}
```

### 4. 启动前端

```powershell
cd science-platform-web
npm install
npm run dev
```

前端启动在 `http://localhost:5173`。开发期所有 `/api` 请求由 Vite 代理转发到 `http://localhost:8080`，前端代码中不硬编码后端地址。

首页会真实调用健康检查接口，展示后端连接状态、数据库连接状态与已发布文章数。

右上角可登录。登录页内置了五个演示账号的一键填充（密码均为 `123456`）：

| 账号 | 角色 | 能做什么 |
| --- | --- | --- |
| `user01` | 普通用户 | 浏览、提交知识贡献、评论点赞收藏 |
| `creator01` / `creator02` | 创作者 | 额外可发布作品、参加活动投稿 |
| `reviewer01` | 评审员 | 审核知识贡献与作品、评审活动投稿 |
| `admin01` | 管理员 | 管理分类、文章、作品、活动、反馈 |
| `superadmin` | 超级管理员 | 额外可管理账号与角色 |

登录后右上角显示昵称与角色，可进入「个人中心」修改昵称、头像、邮箱与简介。

## 主要页面

| 路径 | 说明 | 访问要求 |
| --- | --- | --- |
| `/` | 首页：平台介绍、前后端与数据库连通状态、模块导航 | 所有人 |
| `/knowledge` | 知识库：分类筛选、关键词搜索、分页 | 所有人 |
| `/knowledge/:id` | 文章详情：正文、所属专题、阅读量 | 所有人 |
| `/topics` | 专题列表，显示每个专题的文章数 | 所有人 |
| `/topics/:id` | 专题详情，按推荐顺序列出文章 | 所有人 |
| `/works` | 创意作品广场：分类筛选、搜索、分页 | 所有人 |
| `/works/:id` | 作品详情，含关联的科普文章与创作者信息 | 所有人 |
| `/creator/works` | 创作者中心：我的作品、新建、编辑、提交审核 | 创作者及以上 |
| `/review/works` | 作品审核工作台：通过、驳回、下架 | 评审员及以上 |
| `/contributions/mine` | 我的知识贡献：状态跟踪、审核意见、撤回 | 登录用户 |
| `/review/contributions` | 知识贡献审核：原文与建议对照、通过并更新文章 | 评审员及以上 |
| `/favorites` | 我的收藏：文章与作品混排，标记已下架内容 | 登录用户 |
| `/events` | 主题活动列表，显示时间状态与投稿数 | 所有人 |
| `/events/:id` | 活动详情：参与规则、投稿入口、优秀作品展示 | 所有人 |
| `/notifications` | 站内消息：审核结果、评论回复、活动通知 | 登录用户 |
| `/profile` | 个人中心：概况、我的作品/贡献/收藏/投稿/反馈、编辑资料 | 登录用户 |
| `/login`、`/register` | 登录、注册 | 未登录 |
| `/admin/articles` | 后台文章管理：新建、编辑、发布、下架、删除 | 管理员及以上 |
| `/admin/categories` | 后台分类管理 | 管理员及以上 |
| `/admin/topics` | 后台专题管理与文章编排 | 管理员及以上 |
| `/admin/events` | 后台活动管理：发布、结束、确认优秀作品 | 管理员及以上 |
| `/admin/feedback` | 内容反馈处理 | 管理员及以上 |
| `/admin/users` | 用户与角色管理 | 超级管理员 |
| `/review/contributions` | 知识贡献审核 | 评审员及以上 |
| `/review/works` | 作品审核 | 评审员及以上 |
| `/review/events` | 活动投稿评审 | 评审员及以上 |
| `/creator/works` | 创作者中心 | 创作者及以上 |

### 5. 构建与测试

```powershell
# 后端打包
cd science-platform-server
mvn -s ..\maven-settings.xml clean package

# 后端测试（需要数据库已初始化）
mvn -s ..\maven-settings.xml test

# 前端构建
cd science-platform-web
npm run build
```

> 后端测试使用真实 MySQL 连接（`@SpringBootTest`），运行前请确认 `science_platform` 库已按 schema.sql + seed.sql 初始化。

## 阶段进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 环境核验 | ✅ 已完成 |
| 1 | 项目骨架与前后端连通 | ✅ 已完成 |
| 2 | 数据库与基础规范 | ✅ 已完成 |
| 3 | 登录和角色权限 | ✅ 已完成 |
| 4 | 科普知识库 | ✅ 已完成 |
| 5 | 作品管理 | ✅ 已完成 |
| 6 | 知识共建核心（贡献/审核/版本） | ✅ 已完成 |
| 7 | 社区互动 | ✅ 已完成 |
| 8 | 主题活动 | ✅ 已完成 |
| 9 | 个人中心与消息 | ✅ 已完成 |
| 10 | 测试与收尾 | ✅ 已完成 |

## 验收结论

对照开发档案 12.1「功能验收清单」的 **20 条全部通过**，详见 [验收报告](docs/验收报告.md)。

自动化测试共 **109 个用例**，覆盖档案 12.2 的 T01–T12 全部场景：

```
Tests run: 109, Failures: 0, Errors: 0
BUILD SUCCESS
```

| 测试类 | 用例数 | 覆盖内容 |
| --- | --- | --- |
| `AuthAndSecurityTest` | 10 | T01 登录、T02 错误密码、T03 越权、令牌伪造、防枚举 |
| `ArticlePaginationTest` | 5 | 数据库连通、分页生效、逻辑删除过滤 |
| `KnowledgeApiTest` | 13 | T04 搜索分页、草稿不可见、后台权限 |
| `WorkApiTest` | 18 | T05 作品归属、审核状态流转、活动引用保护 |
| `ContributionApiTest` | 13 | T06 提交校验、T07 驳回、T08 通过并生成版本 |
| `ContributionTransactionTest` | 2 | **T09 事务回滚**（mock 写快照失败验证整体回滚） |
| `InteractionApiTest` | 13 | **T10 重复点赞**、评论权限、反馈处理 |
| `EventApiTest` | 21 | **T11 活动时间校验**、评审 upsert、未评审不得获奖 |
| `NotificationApiTest` | 14 | **T12 消息隔离**、防锁死规则、禁用后令牌失效 |

## 交付物清单

| 类别 | 内容 |
| --- | --- |
| 源码 | 后端 130+ Java 文件、前端 40+ Vue/JS 文件 |
| 数据库 | `database/schema.sql`（20 张表）、`database/seed.sql`（演示数据） |
| 测试 | 9 个测试类、109 个用例 |
| 文档 | 环境记录、接口约定、数据库设计、数据字典、需求分析、认证设计、验收报告、答辩演示脚本、截图清单、开发日志 |

## 安全提示

- 不要提交本地数据库密码、令牌密钥或真实用户数据。`application.yml` 中的敏感项请用环境变量注入；本地覆盖配置命名为 `application-local.yml`，已被 `.gitignore` 忽略。
- 默认演示账号仅用于本地开发；首次使用后应修改密码。
- 科普示例文章必须经过人工核对并注明来源，不得把虚构来源当作可信引用。

## 已知限制

论文「不足与展望」可用的素材：

| 类别 | 限制 |
| --- | --- |
| 检索 | `LIKE '%关键词%'` 无法命中前缀索引，未引入全文检索引擎 |
| 令牌 | 签发后无法主动撤销，无刷新令牌机制，无登录失败次数限制 |
| 活动 | 到期后管理状态不自动变 CLOSED（只体现在 `timeStatus`） |
| 投稿 | 不支持撤回（状态枚举已预留 `WITHDRAWN`） |
| 前端 | Element Plus 全量引入（已拆为独立 chunk，主包 878 kB → 16.78 kB） |
| 用户管理 | 按角色筛选在内存中进行，数据量大时结果不准 |

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [环境核验记录](docs/environment.md) | 本机版本、端口、依赖源可达性与处理方式 |
| [接口约定](docs/api-contract.md) | 全部 75 个接口的请求/响应、错误结构、业务规则实测 |
| [数据库设计](docs/数据库设计/README.md) | ER 图、关键关系、设计决策取舍（论文材料） |
| [数据字典](docs/数据库设计/数据字典.md) | 20 张表逐字段说明（论文材料） |
| [需求分析](docs/thesis/需求分析.md) | 背景、可行性、角色权限、功能需求、用例（论文材料） |
| [认证与权限设计](docs/认证与权限设计.md) | JWT 方案选型、认证流程、授权规则、安全措施（论文材料） |
| [验收报告](docs/验收报告.md) | 对照 12.1 清单逐条核对，含实测证据 |
| [答辩演示脚本](docs/答辩演示脚本.md) | 七幕演示流程 + 常见追问的回答要点 |
| [截图清单](docs/screenshots/README.md) | 需要采集的 28 张图与每张应体现的内容 |
| [开发日志](docs/development-log.md) | 逐阶段的实现记录、验证结果、踩坑与处理 |
