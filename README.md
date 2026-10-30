# 科普知识共创与创意传播平台

> 本科毕业设计 · 基于 Spring Boot + Vue 的科普知识共创与创意传播平台的设计与实现

当前进度：**阶段 1（项目骨架）已完成**。见 [开发日志](docs/development-log.md)。

## 项目结构

```
毕业设计/
├── science-platform-server/     # Spring Boot 3.5 + Java 21 后端
├── science-platform-web/        # Vue 3 + Vite 前端
├── database/                    # 数据库初始化脚本（阶段 2 起填充）
├── docs/                        # 环境记录、接口文档、测试用例、开发日志与论文材料
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

### 1. 启动后端

```powershell
cd science-platform-server
mvn -s ..\maven-settings.xml spring-boot:run
```

后端启动在 `http://localhost:8080`，健康检查：

```powershell
curl http://localhost:8080/api/v1/health
```

预期返回：

```json
{
  "code": 200,
  "message": "success",
  "data": { "status": "UP", "application": "science-platform-server", "...": "..." },
  "timestamp": "2026-10-30 13:12:00"
}
```

### 2. 启动前端

```powershell
cd science-platform-web
npm install
npm run dev
```

前端启动在 `http://localhost:5173`。开发期所有 `/api` 请求由 Vite 代理转发到 `http://localhost:8080`，前端代码中不硬编码后端地址。

### 3. 构建

```powershell
# 后端（打包为可执行 jar）
cd science-platform-server
mvn -s ..\maven-settings.xml clean package

# 前端（产出 dist/）
cd science-platform-web
npm run build
```

### 4. 数据库（阶段 2 起）

```powershell
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

数据库连接配置说明见 `science-platform-server/src/main/resources/application-example.yml`。

## 阶段进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 0 | 环境核验 | ✅ 已完成 |
| 1 | 项目骨架与前后端连通 | ✅ 已完成 |
| 2 | 数据库与基础规范 | ⬜ 未开始 |
| 3 | 登录和角色权限 | ⬜ 未开始 |
| 4 | 科普知识库 | ⬜ 未开始 |
| 5 | 作品管理 | ⬜ 未开始 |
| 6 | 知识共建核心（贡献/审核/版本） | ⬜ 未开始 |
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
- [接口约定](docs/api-contract.md)
- [开发日志](docs/development-log.md)
