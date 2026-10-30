# 科普知识共创与创意传播平台

> 本科毕业设计 · 基于 Spring Boot + Vue 的科普知识共创与创意传播平台的设计与实现

当前进度：**阶段 6（知识共建与审核）已完成**。见 [开发日志](docs/development-log.md)。

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
| `/login`、`/register` | 登录、注册 | 未登录 |
| `/profile` | 个人中心：资料查看与编辑 | 登录用户 |
| `/admin/articles` | 后台文章管理：新建、编辑、发布、下架、删除 | 管理员及以上 |
| `/admin/categories` | 后台分类管理 | 管理员及以上 |
| `/admin/topics` | 后台专题管理与文章编排 | 管理员及以上 |

后台左侧菜单里灰色的项（作品、活动、反馈、用户）是后续阶段的功能，尚未开放。

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
| 7 | 社区互动 | ⬜ 未开始 |
| 8 | 主题活动 | ⬜ 未开始 |
| 9 | 个人中心与消息 | ⬜ 未开始 |
| 10 | 测试与收尾 | ⬜ 未开始 |

## 安全提示

- 不要提交本地数据库密码、令牌密钥或真实用户数据。`application.yml` 中的敏感项请用环境变量注入；本地覆盖配置命名为 `application-local.yml`，已被 `.gitignore` 忽略。
- 默认演示账号仅用于本地开发；首次使用后应修改密码。
- 科普示例文章必须经过人工核对并注明来源，不得把虚构来源当作可信引用。

## 文档索引

- [环境核验记录](docs/environment.md)
- [认证与权限设计](docs/认证与权限设计.md)
- [接口约定](docs/api-contract.md)
- [数据库设计（ER 图与设计决策）](docs/数据库设计/README.md)
- [数据字典](docs/数据库设计/数据字典.md)
- [需求分析（论文材料）](docs/thesis/需求分析.md)
- [开发日志](docs/development-log.md)
