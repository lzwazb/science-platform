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

---

## 2026-10-30 · 版本管理初始化

**触发**：主人确认可以初始化 Git 仓库。

```powershell
git init                                  # Initialized empty Git repository in F:/毕业设计/.git/
git add -A                                # 32 个文件
git -c core.safecrlf=false commit -m "..."  # b1cfd59
```

新增 `.gitattributes`（`* text=auto eol=lf`，Windows 脚本保留 CRLF，二进制不转换），消除跨平台换行警告。

**确认未进入版本库**：`node_modules/`、`target/`、`dist/`、`.m2repo/`、`doc.docx`、`application-local.yml`（后者在阶段 2 创建）。
首次提交覆盖阶段 0 与阶段 1 的全部成果。

---

## 2026-10-30 · 阶段 2：数据库与基础规范

**目标**（开发档案第 11 章）：建库建表、演示数据、统一异常、分页与接口文档，完成后示例数据可查询。

### 1. MySQL 连接核验

```powershell
mysql -u root -e "SELECT VERSION(), @@character_set_server, @@port;"
```

实测：`8.0.42` / `utf8mb4` / `3306`。库中已有 `center`、`cl515882190` 等其他数据库，本次只新建 `science_platform`，未触碰任何既有库。

### 2. schema.sql：20 张表

按档案第 7 章的表清单实现，分组为：账号权限 3 张、知识库 4 张、可追溯 2 张、创意作品 2 张、社区互动 4 张、主题活动 3 张、消息日志 2 张。

**执行**

```powershell
cd database
mysql -u root --default-character-set=utf8mb4 -e "source schema.sql"   # exit=0
```

**实测结果**：20 张表、29 个外键、72 个索引，中文表注释正常。

> 踩坑记录：`mysql.exe` 无法打开含中文的路径（`Failed to open file 'F:\毕业设计\...' error: 2`）。
> 解决方式：切到 `database` 目录用相对文件名执行 `source schema.sql`。此结论已写入 `database/README.md`。

关键设计决策（详见 [数据库设计说明](数据库设计/README.md)）：

- `article_version.version_no` 表示「变更前的旧内容」，因此 `science_article.version_no = N` 时历史表最多到 `N-1`
- 点赞、收藏、专题文章、作品文章、活动投稿、评审均用唯一索引兜底，不依赖「先查后插」
- 逻辑删除只给用户、文章、专题、作品、活动；评论用 `status` 表达隐藏与删除两种语义
- 外键取默认 `RESTRICT`，物理删除被引用数据会被数据库拒绝，逼迫业务走软删除

### 3. seed.sql：演示数据

**执行过程中修正了两个自身错误**：

| 错误 | 现象 | 修正 |
| --- | --- | --- |
| creator02 的密码哈希写成 `$b$10$...`（漏了 `2`） | 该账号无法登录 | 改为 `$2b$10$...` |
| 文章 5 正文里的 `Let's Encrypt` 未转义单引号 | `ERROR 1064 ... near 's Encrypt）'` | 改为 `Let''s Encrypt` |

**实测结果**：exit=0，20 张表行数分别为

```
sys_role 5 | sys_user 6 | sys_user_role 10 | science_category 10 | science_topic 3
science_article 8 | topic_article 6 | article_version 1 | knowledge_contribution 3
creator_work 4 | work_article_relation 5 | interaction_comment 6 | interaction_like 8
interaction_favorite 4 | content_feedback 2 | creative_event 2 | event_submission 3
event_review 2 | sys_notification 7 | sys_operation_log 6
```

**中文与关联校验**：文章列表联表查询（分类、作者、状态、版本号）输出正常，文章 3 显示 `version_no = 2`，与其被采纳的纠错贡献一致；贡献表联表显示三条记录分别为 PENDING / APPROVED / REJECTED，审核人显示「评审员小林」。

演示账号密码统一 `123456`，BCrypt 哈希由本机生成后写入（`$2b$10$0XyI3B6qurZ9...`，已在生成时校验 `compareSync` 返回 true）。

### 4. 后端接入 MyBatis-Plus

| 变更 | 文件 |
| --- | --- |
| 新增依赖 | `pom.xml`：`mybatis-plus-spring-boot3-starter 3.5.18.1`、`mybatis-plus-jsqlparser 3.5.18.1`、`mysql-connector-j` |
| 分页插件 | `config/MybatisPlusConfig.java`（`maxLimit=100`、`overflow=false`） |
| 公共字段填充 | `config/MyMetaObjectHandler.java` |
| 数据源与 MP 配置 | `application.yml`（profile=local、逻辑删除、驼峰映射、SQL 日志） |
| 本地密码隔离 | `application-local.yml`（gitignore）+ `application-local-example.yml`（提交） |
| 实体与 Mapper | `modules/knowledge/entity/ScienceArticle.java`、`modules/knowledge/mapper/ScienceArticleMapper.java` |
| 健康检查升级 | `modules/system/service/SystemHealthService.java`、`HealthController.java` |
| 统一异常 | `common/exception/BusinessException.java`、`GlobalExceptionHandler.java` |

**构建踩坑**：首次编译报 `找不到符号: 类 PaginationInnerInterceptor`。原因是 MyBatis-Plus 3.5.9 起把 jsqlparser 从 extension 拆成独立模块，`PaginationInnerInterceptor` 需要 `mybatis-plus-jsqlparser` 依赖。补上后 `BUILD SUCCESS`（jar 31,047,018 字节）。

**编译乱码说明**：Maven 输出中文路径显示为 `F:/��ҵ���/`，属控制台编码问题，不影响构建结果；已通过 `MAVEN_OPTS=-Dfile.encoding=UTF-8` 改善。

### 5. 验证（真实运行）

| 验证项 | 命令 / 方式 | 实测结果 |
| --- | --- | --- |
| 后端启动并连库 | `java -jar target\science-platform-server-1.0.0.jar` | HTTP 200，`db.status=UP`，`publishedArticleCount=8` |
| 数据库不可用时不误报 | 健康检查内真实执行 count 查询 | 查询失败会抛异常 → 全局处理器返回 500，不会出现「进程活着就报 UP」 |
| 404 统一结构 | `GET /api/v1/not-exist` | `{"code":404,"message":"请求的资源不存在","data":null,...}` |
| 405 统一结构 | `POST /api/v1/health` | `{"code":405,"message":"不支持该请求方法：POST",...}` |
| 前端代理与状态展示 | `GET http://localhost:5173/api/v1/health` | HTTP 200，返回含 `db` 段，首页显示数据库状态与文章数 |
| 前端编译 | 请求 `HomeView.vue` 模块 | HTTP 200 |
| 后端测试 | `mvn -s ..\maven-settings.xml test` | `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` |

**测试覆盖**（`src/test/java/com/science/platform/modules/knowledge/ArticlePaginationTest.java`）：

1. `countPublishedArticles()` 返回 8
2. 第 1 页 `size=3` 只返回 3 条，`total=8`
3. 第 3 页返回 2 条（末页不满）
4. 页码 10 返回空列表且 `total` 仍为 8（验证 `overflow=false`）
5. 逻辑删除条件自动拼接：查询结果不含 `deleted=1`，且 `created_at` 有值

> 第 2–4 条是分页插件生效与否的判定点：插件未生效时首页会返回全部 8 条，测试即失败。

### 6. 本阶段产出文件

```
新增   database/schema.sql                                    20 张表
新增   database/seed.sql                                      演示数据
重写   database/README.md                                     执行方式、演示账号、数据用途
新增   docs/数据库设计/README.md                              ER 图与设计决策（论文材料）
新增   docs/数据库设计/数据字典.md                            逐表逐字段说明（论文材料）
新增   docs/thesis/需求分析.md                                需求分析（论文材料）
新增   .gitattributes                                         统一换行符
新增   science-platform-server/src/main/java/com/science/platform/config/MybatisPlusConfig.java
新增   science-platform-server/src/main/java/com/science/platform/config/MyMetaObjectHandler.java
新增   science-platform-server/src/main/java/com/science/platform/common/exception/BusinessException.java
新增   science-platform-server/src/main/java/com/science/platform/common/exception/GlobalExceptionHandler.java
新增   science-platform-server/src/main/java/com/science/platform/modules/knowledge/entity/ScienceArticle.java
新增   science-platform-server/src/main/java/com/science/platform/modules/knowledge/mapper/ScienceArticleMapper.java
新增   science-platform-server/src/main/java/com/science/platform/modules/system/service/SystemHealthService.java
新增   science-platform-server/src/test/java/com/science/platform/modules/knowledge/ArticlePaginationTest.java
新增   science-platform-server/src/main/resources/application-local-example.yml
新增   science-platform-server/src/main/resources/application-local.yml （已被 gitignore，本地使用）
修改   science-platform-server/pom.xml                        MyBatis-Plus、jsqlparser、MySQL 驱动
重写   science-platform-server/src/main/resources/application.yml  数据源、MP、profile
重写   science-platform-server/src/main/java/com/science/platform/modules/system/HealthController.java  加 db 段
修改   science-platform-web/src/views/public/HomeView.vue     展示数据库状态与文章数
修改   science-platform-web/src/layouts/DefaultLayout.vue     页脚阶段标识
重写   docs/api-contract.md                                   错误响应示例、分页实现约定、health 新结构
重写   README.md                                              初始化步骤、演示账号、阶段进度
```

### 7. 遗留问题

| 编号 | 问题 | 影响 |
| --- | --- | --- |
| 1 | 后端测试依赖真实 MySQL，干净环境需先初始化库 | 交付时须在 README 写明，已写 |
| 2 | `LIKE '%关键词%'` 无法命中前缀索引 | 演示规模无影响，论文「不足与展望」说明 |
| 3 | Element Plus 全量引入，主包 1MB | 不影响功能，阶段 10 处理 |
| 4 | 前端尚未有登录态，`stores/` 目录未建 | 阶段 3 |
| 5 | 场景图 `/images/...` 封面路径尚未落地，前端暂未引用 | 阶段 4 统一处理静态资源 |

### 8. 下一步建议

进入**阶段 3：登录和角色权限**：

1. 引入 Spring Security + JWT（密码用 BCrypt，与 seed 中的哈希格式一致），说明认证流程与接口授权方式
2. 实现注册、登录、`/auth/me`、个人资料读写
3. 搭建角色授权：`@PreAuthorize` 或拦截器 + 方法级校验，保证普通用户调用管理接口返回 403
4. 前端实现登录页、注册页、Pinia 用户 store、路由守卫与令牌携带（`request.js` 已预留）
5. 补权限测试：T01 正常登录、T02 错误密码、T03 越权管理

---

## 2026-10-30 · 阶段 3：登录和角色权限

**目标**（开发档案第 11 章）：实现注册、登录、密码哈希、认证、角色授权与演示账号，达成「不同角色能登录且越权请求被拒绝」。

### 1. 依赖与配置

| 变更 | 说明 |
| --- | --- |
| `spring-boot-starter-security` | 认证授权基础，同时提供 `BCryptPasswordEncoder` |
| `jjwt-api / impl / jackson 0.12.7` | JWT 签发与校验；impl 与 jackson 仅运行期需要 |
| `spring-security-test` | 测试用 |
| `lombok` | 见下文说明 |
| `spring-security-crypto` | 由 starter 传递引入 |

**为什么此时引入 Lombok**：本阶段新增 7 个实体/DTO/VO，加上后续还有十余张表的实体，手写访问器会让业务代码被样板淹没。选择 Lombok 后把阶段 2 的 `ScienceArticle` 也改为 `@Data` 保持一致。已在 pom 注释中说明理由，答辩时可解释为「编译期生成字节码，不影响运行时」。

**JWT 密钥管理**：`application.yml` 中写 `${APP_JWT_SECRET:}`，本地密钥放在 `application-local.yml`（gitignore），示例文件给出生成命令。密钥长度不足 32 字节时 `JwtTokenProvider` 在 `@PostConstruct` 阶段直接抛异常，应用拒绝启动，避免弱密钥上线。

### 2. 后端实现

| 文件 | 职责 |
| --- | --- |
| `security/JwtProperties.java` | 绑定 `app.jwt` |
| `security/JwtTokenProvider.java` | HS512 签发与解析，密钥长度自检 |
| `security/LoginUser.java` | 放入 SecurityContext 的主体（id / username / nickname / roles） |
| `security/JwtAuthenticationFilter.java` | 解析 Bearer 令牌，**每次请求查库校验用户状态与角色** |
| `security/SecurityConfig.java` | 过滤器链、无状态会话、授权规则、401/403 处理器 |
| `security/RestAuthenticationEntryPoint.java` | 401 统一 JSON |
| `security/RestAccessDeniedHandler.java` | 403 统一 JSON |
| `security/SecurityUtils.java` | 从上下文取当前用户，未登录抛 401 |
| `config/CorsConfig.java` | 改为提供 `CorsConfigurationSource` Bean，供 Security 复用（否则预检请求被拦成 401） |
| `config/JacksonConfig.java` | 统一 `LocalDateTime` 格式 |
| `modules/auth/**` | 注册、登录、`/auth/me` |
| `modules/user/**` | 用户实体、Mapper、资料读写 |

**授权规则顺序**（关键）：

```java
.requestMatchers("/api/v1/admin/users/**").hasRole("SUPER_ADMIN")   // 必须在前
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

顺序写反会导致普通管理员也能进超管接口。

### 3. 两个自查修复

| 问题 | 现象 | 处理 |
| --- | --- | --- |
| 时间格式不一致 | 登录响应里 `timestamp` 是 `2026-10-30 13:28:43`，而 `user.createdAt` 是 `2026-01-05T09:05:00` | 根因是 `spring.jackson.date-format` 只作用于 `java.util.Date`。新增 `JacksonConfig` 注册 `LocalDateTime` 的序列化器，实测两者统一 |
| store 直接访问 localStorage | SSR 渲染检查时报 `ReferenceError: localStorage is not defined` | 改为 `typeof localStorage === 'undefined'` 判断后读取。浏览器不受影响，同时让 store 可在无 DOM 环境初始化 |

> 备注：SSR 渲染检查用的临时脚本遇到 Element Plus 的 `IdInjection` 警告，需要在脚本里 `app.provide(ID_INJECTION_KEY, ...)`。这属于检查工具的环境问题，**不是应用缺陷**（浏览器端不需要）。脚本为一次性文件，验证后已删除。

### 4. 验证（真实运行）

#### 4.1 接口级验证（PowerShell + curl 实测）

| 场景 | 期望 | 实测 |
| --- | --- | --- |
| 正常登录 `admin01/123456` | 200 + 令牌 + roles=[ADMIN] | ✅ `expiresIn=7200`，`expiresIn` 与配置一致 |
| 错误密码 | 401 | ✅ `用户名或密码错误` |
| 不存在的账号 | 401，提示与上一条相同 | ✅ 两条 message 完全一致 |
| 无令牌访问 `/users/me` | 401 + 统一 JSON | ✅ `{"code":401,"message":"请先登录后再进行该操作",...}` |
| 伪造令牌 | 401 | ✅ |
| 篡改签名（保留 payload） | 401 | ✅ |
| 有效令牌访问 `/users/me` | 200 | ✅ 返回 user01 资料 |
| 普通用户访问 `/admin/users` | 403 | ✅ `{"code":403,"message":"没有权限执行该操作",...}` |
| 管理员访问 `/admin/users` | 403（仅超管） | ✅ |
| 注册新用户 | 200 + 自动赋 USER 角色 | ✅ 实测 `roles=["USER"]` |
| 重复用户名注册 | 400 | ✅ `用户名已被占用` |
| 多字段校验失败 | 400 + 中文拼接 | ✅ `nickname 昵称不能为空；username 用户名长度需在 3 到 20 个字符之间；password 密码长度需在 6 到 32 个字符之间` |
| `PUT /users/me` 更新昵称与简介 | 200 + 返回最新资料 | ✅ |
| `PUT /users/me` 非法邮箱 | 400 | ✅ `email 邮箱格式不正确` |
| **禁用账号后登录** | 403 | ✅ `账号已被禁用，请联系管理员` |
| **禁用账号用未过期旧令牌访问** | 401 | ✅ 证明「每请求查库」的设计生效，权限变更即时生效 |

> 「禁用账号」测试通过直接改库完成（`UPDATE sys_user SET status='DISABLED'`），验证后已把测试账号从库中清理，演示数据保持 6 个账号。

#### 4.2 自动化测试

```powershell
mvn -s ..\maven-settings.xml test
```

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0  -- AuthAndSecurityTest
Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0  -- ArticlePaginationTest
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`AuthAndSecurityTest` 走完整的 Security 过滤器链、使用真实签发的 JWT，覆盖：

1. T01 正常登录（含断言响应中不存在 `passwordHash`）
2. T02 错误密码，且**断言「账号不存在」与「密码错误」的 message 相等**
3. T03 普通用户访问超管接口 403
4. T03 管理员访问超管接口 403
5. 未认证访问 401 且为统一 JSON 结构
6. 伪造令牌 401
7. 有效令牌解析出正确用户
8. 同一账号两次登录的令牌互相独立且都可用
9. 公开接口无需令牌
10. 登录失败响应中不含任何 `token` 字样

#### 4.3 前端

| 验证项 | 结果 |
| --- | --- |
| `npm run build` | ✅ exit 0，业务代码已按路由做动态 import，主包从 1015 kB 降到 874 kB |
| 9 个新增/改动模块编译 | ✅ 全部 200 |
| 走 5173 代理登录 `reviewer01` | ✅ HTTP 200，`roles=USER,REVIEWER`，令牌长度 271 |
| SSR 渲染三页 | ✅ 登录页 len=5576（含演示账号、注册入口）、注册页 len=6144（含确认密码、角色说明）、个人中心 len=7018（含编辑表单、不可修改提示） |

### 5. 本阶段产出文件

```
新增   security/JwtProperties.java
新增   security/JwtTokenProvider.java
新增   security/LoginUser.java
新增   security/JwtAuthenticationFilter.java
新增   security/SecurityConfig.java
新增   security/RestAuthenticationEntryPoint.java
新增   security/RestAccessDeniedHandler.java
新增   security/SecurityUtils.java
新增   config/CorsProperties.java
新增   config/JacksonConfig.java
重写   config/CorsConfig.java                         改为 CorsConfigurationSource Bean
新增   modules/auth/AuthController.java
新增   modules/auth/dto/LoginRequest.java
新增   modules/auth/dto/RegisterRequest.java
新增   modules/auth/service/AuthService.java
新增   modules/auth/vo/LoginResponse.java
新增   modules/user/UserController.java
新增   modules/user/dto/UpdateProfileRequest.java
新增   modules/user/entity/SysUser.java
新增   modules/user/entity/SysRole.java
新增   modules/user/entity/SysUserRole.java
新增   modules/user/mapper/SysUserMapper.java         含角色联表查询
新增   modules/user/mapper/SysRoleMapper.java
新增   modules/user/mapper/SysUserRoleMapper.java
新增   modules/user/service/UserService.java
新增   modules/user/vo/UserProfileVO.java
新增   src/test/java/.../AuthAndSecurityTest.java     10 个用例
修改   modules/knowledge/entity/ScienceArticle.java   改用 Lombok @Data
修改   pom.xml                                        Security、jjwt、lombok、security-test
修改   application.yml                                app.jwt 配置段
修改   application-local.yml（未入库）                 本机密钥
修改   application-local-example.yml                  密钥生成命令
新增   前端 src/api/auth.js
新增   前端 src/api/user.js
新增   前端 src/stores/user.js
新增   前端 src/views/auth/LoginView.vue
新增   前端 src/views/auth/RegisterView.vue
新增   前端 src/views/user/ProfileView.vue
重写   前端 src/router/index.js                        路由守卫
重写   前端 src/layouts/DefaultLayout.vue              登录态与用户菜单
重写   前端 src/api/request.js                         401 跳转登录
新增   docs/认证与权限设计.md                          论文材料
```

### 6. 遗留问题

| 编号 | 问题 | 影响 | 处理计划 |
| --- | --- | --- | --- |
| 1 | 令牌签发后无法主动撤销（无黑名单） | 改密码后旧令牌可用至过期 | P2，论文「不足与展望」说明 |
| 2 | 无刷新令牌机制 | 过期需重新登录 | 2 小时有效期，演示无影响 |
| 3 | 无登录失败次数限制 | 理论上可暴力尝试 | 答辩范围外，论文提及 |
| 4 | 测试依赖真实数据库与 seed 数据 | 干净环境需先初始化 | README 已写明 |
| 5 | 后端测试出现 Mockito self-attach 警告 | 无功能影响，JDK 未来版本会禁用 | 升级 JDK 时再处理 |

### 7. 下一步建议

进入**阶段 4：科普知识库**：

1. 分类、文章、专题的只读接口（列表分页、详情、关键词搜索、按分类筛选、专题聚合）
2. 后台文章维护接口（创建、编辑、发布、下架），走 `ADMIN` 权限
3. 前端 `/knowledge`、`/knowledge/:id`、`/topics`、`/topics/:id`、`/admin/articles`、`/admin/categories`、`/admin/topics` 页面
4. 静态资源目录落地（seed 中的 `/images/articles/*.svg` 封面），统一处理图片缺失的占位
5. 补搜索与分页测试（T04），以及「游客可浏览已发布文章、看不到草稿」的权限测试
