# 开发日志

> 记录每次任务的真实操作、命令、验证结果与遗留问题。
> 原则：未实际运行验证的内容必须标注「未验证」，不虚构测试结果。

---

## 2026-10-30 · 阶段 0：环境核验

**目标**：核实 JDK、Maven、Node.js、npm、MySQL 版本与端口，不假设环境。

**实际执行的命令**

```powershell
java -version; javac -version; echo $env:JAVA_HOME
mvn -v
node -v; npm -v
mysql --version
git --version
Test-NetConnection 127.0.0.1 -Port 3306 / 8080 / 5173
```

**实测结果**

| 组件 | 版本 |
| --- | --- |
| JDK | 21.0.6（Oracle，`C:\Program Files\java\jdk-21`） |
| Maven | 3.9.9（`D:\javatools\apache-maven-3.9.9`） |
| Node.js | v22.19.0 |
| npm | 11.6.4 |
| MySQL | 8.0.42，3306 已监听 |
| Git | 2.47.1 |

**发现的问题与处理**

- **Maven 中央仓库直连失败**（`repo1.maven.org`、`repo.maven.apache.org` TLS 连接被中断）。
  处理：测试阿里云 / 腾讯云 / 华为云镜像均可达，采用阿里云镜像，写入项目级 `maven-settings.xml`。
- 本地仓库指向 `F:\毕业设计\.m2repo`，保持项目自包含（已加入 `.gitignore`）。
- 因此后续所有 Maven 命令必须带 `-s ..\maven-settings.xml`。

**详细记录**：[environment.md](environment.md)

**遗留**：MySQL 账号密码未知，阶段 2 前需要主人提供。

---

## 2026-10-30 · 阶段 1：项目骨架与前后端连通

**目标**（开发档案第 11 章 + 附录 A）：搭建后端 / 前端工程、健康检查接口、Vite 代理、首页展示项目名与后端连接状态，并完成真实调用验证。

### 1. 后端骨架

由 `spring-boot-starter-parent 3.5.16` 手工组织（Spring Initializr 需外网，改用本地生成 + 阿里云镜像解析依赖）。

新建文件：

| 文件 | 说明 |
| --- | --- |
| `science-platform-server/pom.xml` | Spring Boot 3.5.16 + Java 21；依赖 web、validation、test |
| `src/main/java/com/science/platform/SciencePlatformApplication.java` | 启动类 |
| `src/main/java/com/science/platform/common/ApiResponse.java` | 统一响应结构（档案 8.1） |
| `src/main/java/com/science/platform/config/CorsConfig.java` | 开发期跨域，按白名单放开，不无条件允许 `*` |
| `src/main/java/com/science/platform/modules/system/HealthController.java` | `GET /api/v1/health` |
| `src/main/resources/application.yml` | 端口 8080、Jackson 时区与格式、CORS 白名单 |
| `src/main/resources/application-example.yml` | 示例配置，数据库段默认注释，密码用占位符 |

**构建命令与真实输出**

```powershell
cd science-platform-server
mvn -s ..\maven-settings.xml -DskipTests clean package
```

结果：`BUILD SUCCESS`，exit code 0，产物 `target/science-platform-server-1.0.0.jar`（22,669,217 字节）。

### 2. 前端骨架

```powershell
npm create vite@latest science-platform-web -- --template vue   # create-vite v9.3.3
cd science-platform-web
npm install                                                     # 35 packages
npm install element-plus axios vue-router pinia                  # +91 packages
```

锁定版本：Vue 3.5.44 / Vite 8.3.5 / Vue Router 5.4.0 / Pinia 4.1.0 / Element Plus 2.14.7 / Axios 1.21.0。

新建 / 修改文件：

| 文件 | 说明 |
| --- | --- |
| `vite.config.js` | 端口 5173、`@` 别名、`/api` 代理到 `localhost:8080` |
| `index.html` | 中文 lang、项目标题与 description |
| `src/main.js` | 接入 Pinia、Router、Element Plus（中文 locale） |
| `src/router/index.js` | 首页 + 404 两条路由，`afterEach` 设置标题 |
| `src/api/request.js` | Axios 实例：`baseURL=/api/v1`、10s 超时、令牌携带、统一解包与错误提示 |
| `src/api/health.js` | `getHealth()` |
| `src/layouts/DefaultLayout.vue` | 顶栏 + 内容区 + 页脚，蓝白科技风 |
| `src/views/public/HomeView.vue` | 项目介绍、后端连接状态（真实调用）、七模块导航占位 |
| `src/views/NotFoundView.vue` | 404 页面 |
| `src/App.vue`、`src/style.css` | 根组件与主题变量 |
| 删除 | `components/HelloWorld.vue`、`assets/{hero.png,vue.svg,vite.svg}`（脚手架示例） |

**构建命令与真实输出**

```powershell
npm run build
```

结果：exit code 0，1659 modules transformed，`✓ built in 400ms`。
产物：`dist/index.html 0.69 kB`、`dist/assets/index-*.js 1,015.12 kB`（gzip 328.59 kB）。

> 已知提示：Element Plus 全量引入导致主 chunk 超过 500 kB，Vite 给出警告。**不影响功能**，
> 计划在阶段 10 改为按需引入以缩减体积。

### 3. 连通性验证（真实运行）

```powershell
# 后端
java -jar target\science-platform-server-1.0.0.jar

# 前端
npm run dev
```

验证项与结果：

| 验证项 | 命令 | 结果 |
| --- | --- | --- |
| 后端启动 | 轮询 `http://localhost:8080/api/v1/health` | ✅ HTTP 200，`status=UP` |
| 统一响应结构 | 同上 | ✅ 含 `code/message/data/timestamp`，timestamp 为 `2026-10-30 13:13:05` |
| 中文编码 | 按 UTF-8 解码响应体 | ✅ 中文正常（PowerShell 控制台按 ISO-8859-1 显示才乱码，浏览器不受影响） |
| 前端启动 | `http://localhost:5173/` | ✅ HTTP 200 |
| Vite 代理 | `http://localhost:5173/api/v1/health` | ✅ HTTP 200，返回与直连后端一致 |
| 前端模块编译 | 请求 main.js / router / 三个 .vue / 两个 api 模块 | ✅ 全部 200 |
| 依赖运行时导出 | node 动态 import 检查 | ✅ `createRouter`/`createWebHistory`/`createPinia`/`ElMessage` 均为 function |
| 页面组件树渲染 | Vite SSR + `renderToString` 实测 | ✅ 渲染成功，len=5894；含项目标题、连通性区块、核心功能模块；模块卡片 7 个；Element Plus 组件正常输出 |

> SSR 渲染验证只用于确认组件能正确挂载与输出，脚本为一次性临时文件，验证后已删除，**不进入项目**。

### 4. 一处自查修复

首次 health 响应的 `timestamp` 输出为 `2026-10-30T13:12:36.4564512`（`spring.jackson.date-format`
只作用于 `java.util.Date`，不作用于 `LocalDateTime`）。已在 `ApiResponse.timestamp` 上添加
`@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 并重新构建，实测输出 `2026-10-30 13:13:05`。

### 5. 本阶段产出文件清单

```
毕业设计/
├── .gitignore                                   新增
├── README.md                                    新增
├── maven-settings.xml                           新增
├── docs/
│   ├── environment.md                           新增
│   ├── api-contract.md                          新增
│   ├── development-log.md                       新增（本文件）
│   ├── thesis/                                  新建空目录
│   └── screenshots/                             新建空目录
├── database/                                    新建空目录（阶段 2 填充）
├── science-platform-server/                     新增（Maven 项目）
└── science-platform-web/                        新增（Vite 项目）
```

### 6. 遗留问题

| 编号 | 问题 | 影响 | 计划 |
| --- | --- | --- | --- |
| 1 | MySQL 账号密码未提供，`science_platform` 库未创建 | 阻塞阶段 2 | 需主人提供本地 MySQL 凭据 |
| 2 | Git 仓库未初始化 | 无版本管理，无法回滚 | 待主人确认后 `git init` + 首次提交 |
| 3 | Element Plus 全量引入，主包 1MB | 首屏体积偏大，不影响演示 | 阶段 10 改按需引入 |
| 4 | 前端尚未做登录态（`stores/user.js` 未创建） | 无 | 阶段 3 |
| 5 | 无后端单元测试 | 阶段 1 仅骨架 | 阶段 3、6 补关键路径测试 |

### 7. 下一步建议

进入**阶段 2：数据库与基础规范**。需要先确认 MySQL 连接信息，然后：

1. 输出 ER 图与表关系说明，生成 `database/schema.sql`、`database/seed.sql`（17 张表，含唯一约束）
2. 引入 MyBatis-Plus，配置分页插件、逻辑删除、字段自动填充
3. 统一异常处理（`@RestControllerAdvice`）、业务异常与错误码
4. 同步更新 `docs/api-contract.md` 的分页与错误结构
5. 写 `docs/thesis/需求分析.md`、`docs/thesis/数据库设计/`（论文材料，档案第 13 章要求阶段 2 起留存）
