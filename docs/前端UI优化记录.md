# 前端 UI 优化记录

> 依据《科普共创平台前端UI优化修改文档》实施。
> 本次改造只动前端表现层：**不改后端、不改接口契约、不改权限规则、不改路由名称、不改业务状态流转**。

## 一、改造范围与结果

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 一 | 框架统一：设计变量、导航重组、双布局 | ✅ |
| 二 | 内容页：首页、知识库、作品、活动、详情页 | ✅ |
| 三 | 状态字典统一、个人中心与后台页面规范 | ✅ |
| 四 | 响应式补全、交互回归、修改记录整理 | ✅ |

## 二、设计变量（`src/style.css`）

所有颜色、圆角、间距、容器宽度集中定义一次，页面只引用变量。

| 类别 | 变量 | 值 |
| --- | --- | --- |
| 品牌主色 | `--sp-primary` | `#347fd1` |
| 深色态 | `--sp-primary-dark` | `#2a66a7` |
| 浅色底 | `--sp-primary-soft` | `#edf3fb` |
| 正文文字 | `--sp-text-strong` | `#25364a` |
| 页面底色 | `--sp-bg` | `#f5f8fc` |
| 卡片圆角 | `--sp-radius` | `12px` |
| 卡片间距 | `--sp-gap` | `16px` |
| 内容容器 | `--sp-container` | `1200px` |
| 后台容器 | `--sp-container-wide` | `1240px` |
| 阅读容器 | `--sp-container-reading` | `900px` |

品牌色同时覆盖了 Element Plus 的 `--el-color-primary` 与全部深浅变体（light-3/5/7/8/9、dark-2），
保证按钮、链接、选中态、Tag 属于同一色系。

### 透明色档位

新增 10 个 `--sp-primary-aNN` 变量（a08 ~ a92），替代页面里写死的 rgba 数值。
本次清理了 **42 处** 硬编码的旧主题色 `rgba(64,158,255,X)`，覆盖 13 个页面。
换主题色从此只需改 `style.css` 一处。

## 三、导航重组（`layouts/DefaultLayout.vue`）

### 改前

顶栏平铺 13 个入口，随角色增长而拥挤，窄屏直接换行堆叠：

```
首页 知识库 专题 创意作品 主题活动 我的贡献 我的收藏 消息
创作者中心 贡献审核 作品审核 活动评审 后台管理
```

### 改后

```
[Logo]  首页 知识库 专题 创意作品 主题活动 …  [工作台▾]  [消息①]  [头像▾]
```

| 区域 | 内容 |
| --- | --- |
| 主导航 | 5 个公共入口，桌面端单行不换行 |
| 工作台下拉 | 创作者中心 / 审核工作台（三项）/ 后台管理，按角色出现 |
| 消息入口 | 带未读数角标（本轮补上第二阶段遗留） |
| 头像下拉 | 个人中心、我的贡献、我的收藏、消息、退出登录 |
| ≤860px | 收起为汉堡按钮 + 抽屉菜单，按「浏览 / 工作台 / 我的」分组 |

导航逻辑抽到 `src/config/navigation.js`，桌面端与抽屉共用同一份定义。

### 各角色可见性（已实测）

| 角色 | 工作台菜单 |
| --- | --- |
| 游客 | 无（只显示登录/注册） |
| 普通用户 | 创作者中心 |
| 创作 / 评审员 | 创作者中心 + 知识贡献审核、作品审核、活动审核 |
| 管理员 / 超管 | 上述 + 后台管理 |

## 四、状态字典（`src/constants/dict.js`）

### 为什么合并

改造前 11 个页面各写一份 `statusMeta`，已经出现分歧：

```
「已下架」OFFLINE
  AdminArticleView / AdminTopicView / AdminSettingsView / ProfileView → warning（黄）
  AdminWorkView / CreatorWorksView / ReviewWorksView               → info（灰）
```

同一个状态，作品页显示灰色、文章页显示黄色。

### 颜色语义约定

| 颜色 | 含义 | 典型状态 |
| --- | --- | --- |
| success | 正常、通过、公开 | 已发布、已通过、已处理 |
| warning | 需要关注 | 待审核、待处理、已下架、已结束 |
| danger | 失败、被拒、异常 | 已驳回、已禁用、已获奖 |
| primary | 进行中 | 处理中、已评审 |
| info | 中性、归档 | 草稿、已撤回、已取消 |

### 字典内容

内容状态、投稿状态、反馈状态、账号状态、活动时间状态、贡献类型、反馈类型、获奖等级、目标类型，
外加 `statusOf(status, dict)` 安全取值函数（未知状态原样显示，不会渲染成空白）。

> 反馈的 `PENDING` 是「待处理」，内容审核的 `PENDING` 是「待审核」——
> 为了文案准确，这两个分开定义，没有强行合并。

11 个页面改为引用共享字典，本地定义全部删除。

## 五、布局与卡片规范

### 卡片网格

新增 `.sp-card-grid`：桌面三列 / 中屏两列 / 手机一列。

原来是 `auto-fill + minmax(300px, 1fr)`，在 1200px 下会排出 **4 列**，卡片被压窄，
与文档要求的「三列阅读节奏」不符。改用显式断点后行为可预期。

### 封面

`.sp-cover` 统一 16:9。改造前活动卡片是 16:7，与文章、作品卡片混排时高度参差。

### 列表页统一

知识库、作品广场、活动列表、专题列表、收藏页：
容器宽度改用变量、卡片网格加断点、分页间距统一。

## 六、首页内容顺序（文档 4.2 节）

```
改前：Hero → 公告 → 共建流程 → 文章 → 作品 → 活动 → 模块
改后：Hero → 文章 → 作品 → 活动 → 共建流程 → 公告 → 模块
```

即「先给内容，再讲机制，最后放通知」。原来访客还没看到一篇文章，就先读了一屏公告和四步流程。

## 七、响应式补全

### 断点

| 断点 | 主要变化 |
| --- | --- |
| ≥1025px | 三列网格、单行导航 |
| ≤1024px | 两列网格、导航收窄 |
| ≤860px | 导航收隐形、汉堡菜单 |
| ≤768px | 页面内边距收窄为 16px、页面头部纵向排列 |
| ≤640px | 单列网格 |
| ≤375px | 内边距进一步收窄为 12px |

### 补充媒体查询的页面（9 个）

`LoginView`、`RegisterView`、`ProfileView`、`NotificationsView`、
`MyContributionsView`、`MyFavoritesView`、`TopicListView`、
`ReviewEventsView`、`ReviewWorksView`。

### 溢出风险排查

扫描全站固定像素宽度，共 4 处：

| 页面 | 宽度 | 是否处理 |
| --- | --- | --- |
| `EventDetailView` 活动封面 | 220px | ✅ 760px 断点下改为整宽 |
| `TopicDetailView` 专题封面 | 180px | ✅ 720px 断点下改为整宽 |
| `HomeView` Hero 统计栏 | 200px | ✅ 860px 断点下改为横向排列 |
| `WorkDetailView` 关联文章封面 | 120px | ✅ 720px 断点下已有处理 |

登录/注册页的 `max-width: 420px/440px` 配合 `width: 100%`，在 375px 下实际渲染约 335px，不会溢出。

后台页面本身不设内边距，由 `AdminLayout` 统一控制，该布局已有 900px 断点。

### 无障碍

`style.css` 增加 `prefers-reduced-motion` 支持：系统开启「减少动态效果」时，
过渡与动画时长压到 0.01ms。

## 八、交互回归结果

走前端代理验证，确认 UI 改造没有破坏任何接口契约：

### 游客

| 操作 | 结果 |
| --- | --- |
| 文章列表 | 200，total=8，返回 3 条 |
| 关键词搜索「DNA」 | 200，命中 1 条 |
| 分页第 2 页 | 200，返回 3 条 |
| 文章详情 | 200，显示「第 2 版」 |
| 作品列表 | 200，total=3 |
| 活动列表 | 200，2 个 |
| 专题列表 | 200，3 个 |

### 普通用户（user01）

| 操作 | 结果 |
| --- | --- |
| 收藏文章 | 200；重复收藏同样 200（幂等） |
| 点赞 | 200 |
| 发表评论 / 评论列表 | 200 |
| 提交纠错建议 | 200 |
| 我的贡献 | 200，3 条 |
| 我的消息 | 200 |
| 创建作品 → 提交审核 | 200，状态 PENDING |

### 评审员（reviewer01）

| 操作 | 结果 |
| --- | --- |
| 待审贡献列表 | 200，2 条 |
| 审核作品通过 | 200，状态 PUBLISHED |

### 管理员（admin01）

| 操作 | 结果 |
| --- | --- |
| 后台文章 / 作品 / 活动 / 反馈 | 均 200 |

### 权限边界未被 UI 改造破坏

| 场景 | 结果 |
| --- | --- |
| 普通用户访问 `/admin/**` | 403 |
| 普通用户访问 `/review/**` | 403 |
| 未登录访问消息接口 | 401 |

回归完成后已用 `seed.sql` 重置演示数据。

## 九、构建与页面检查

```
前端构建   ✓ built
页面渲染   27 个页面全部 200
CSS 变量   40 个已定义、35 个被引用、无未定义引用
后端测试   140 个通过（本轮未改后端，仅确认无影响）
```

## 十、修改文件清单

### 新增

```
src/constants/dict.js          状态与类型字典
src/config/navigation.js       导航项与可见性逻辑
```

### 布局

```
src/layouts/DefaultLayout.vue  导航重组、移动端抽屉、未读数
src/layouts/AdminLayout.vue    菜单分组、窄屏折叠
```

### 全局

```
src/style.css                  设计变量、公共类、响应式、无障碍
src/stores/user.js             新增 isSuperAdmin
```

### 页面（按改动类型）

```
容器与网格统一：
  views/knowledge/KnowledgeListView.vue
  views/knowledge/TopicListView.vue
  views/work/WorkListView.vue
  views/event/EventListView.vue
  views/interaction/MyFavoritesView.vue
  views/public/HomeView.vue

状态字典替换：
  views/admin/AdminArticleView.vue
  views/admin/AdminCategoryView.vue
  views/admin/AdminEventView.vue
  views/admin/AdminFeedbackView.vue
  views/admin/AdminSettingsView.vue
  views/admin/AdminTopicView.vue
  views/admin/AdminUserView.vue
  views/admin/AdminWorkView.vue
  views/contribution/MyContributionsView.vue
  views/contribution/ReviewContributionsView.vue
  views/creator/CreatorWorksView.vue
  views/review/ReviewWorksView.vue
  views/user/ProfileView.vue

时间状态字典：
  views/event/EventListView.vue
  views/event/EventDetailView.vue
  views/public/HomeView.vue

响应式补全：
  views/auth/LoginView.vue
  views/auth/RegisterView.vue
  views/user/NotificationsView.vue
  views/review/ReviewEventsView.vue
```

## 十一、浏览器实测（Playwright）

用真实浏览器逐档验证，截图存于 [docs/screenshots/ui](screenshots/ui/)。

### 断点实测结果

| 视口 | 导航 | 卡片网格 | 横向溢出 | 备注 |
| --- | --- | --- | --- | --- |
| 1440px | 单行 5 项 | 3 列 | 无 | 顶栏、Hero、列表、详情均正常 |
| 1024px | 单行（收窄） | 2 列 | 无 | — |
| 768px | 收起为汉堡 | 2 列 | 无 | Hero 转为纵向 |
| 375px | 汉堡 + 抽屉 | 1 列 | 无 | 抽屉分组完整、可滚动 |

### 实测发现并修复的 3 个问题

**1. 首页统计栏在 375px 下排成 4 + 1**

原用 `flex-wrap`，五个数字在窄屏挤成「8 3 2 1 / 2」，最后一个「创作者」孤零零挂在第二行。
改为 `grid-template-columns: repeat(3, 1fr)`，排成 3 + 2 两行，整齐且不孤立。

**2. 移动端抽屉的头像显示固定字「科」**

抽屉头部原本写死 `<span>科</span>` 作为装饰，但那个位置是用户信息区，
显示品牌字很怪。改为 `el-avatar` + 昵称首字，与顶栏头像一致。

**3. 抽屉内容超出视口时无法滚动**

菜单项多时，「退出登录」被挤出可视区且无法滚动到——功能上等同于丢失。
修复两处：

```css
.drawer { overflow-y: auto; }        /* 内容可滚动 */
.drawer__head { position: sticky; top: 0; }  /* 头部固定，关闭按钮始终可见 */
```

实测：抽屉 `scrollHeight 818 > clientHeight 760`，滚动到底部后「退出登录」可见（top=737），
头部保持 sticky 未被滚走。

### 其他确认项

| 检查项 | 结果 |
| --- | --- |
| 工作台下拉内容 | 创作者中心 / 审核工作台（三项）/ 后台管理，超管视角正确 |
| 后台侧栏分组 | 内容 / 运营 / 系统三组，8 项 |
| 后台表格窄屏 | 在容器内横向滚动，页面本身不溢出 |
| 后台窄屏折叠 | 侧栏隐藏，「后台菜单」按钮出现 |
| 文章详情阅读宽度 | 1440px 下正文约 730px，单行约 40 字 |
| 控制台错误 | 仅 2 条，均来自卡巴斯基浏览器扩展注入脚本，与项目无关 |

## 十二、需要人工确认的点

以下几项依赖真实浏览器，建议在演示前过一遍：

以下项已用 Playwright 实测通过，其余需要人工在真机上看：

| 检查项 | 状态 | 建议宽度 |
| --- | --- | --- |
| 顶栏导航不换行、工作台下拉位置正确 | ✅ 已实测 | 1440px |
| 卡片网格三列、间距均匀 | ✅ 已实测 | 1440px |
| 卡片网格变两列 | ✅ 已实测 | 1024px |
| 导航收起为汉堡、抽屉分组完整 | ✅ 已实测 | 860px 以下 |
| 页面无横向滚动条 | ✅ 已实测 | 375px |
| 后台表格可横向滚动、不被截断 | ✅ 已实测 | 375px |
| 文章详情阅读宽度适中 | ✅ 已实测 | 1440px |
| 登录卡片不贴边、按钮可点击 | ⬜ 待人工 | 375px |
| 弹窗不超出视口（新建/编辑对话框） | ⬜ 待人工 | 375px |
| 真机触摸操作手感 | ⬜ 待人工 | 手机 |

## 十三、未处理项

以下属于文档「本轮优化范围边界」之外或留待后续迭代：

| 项 | 说明 |
| --- | --- |
| 文章正文结构化 | 当前正文为纯文本段落，标题/引用等富文本能力属于后续迭代 |
| 图片上传 | 封面仍填写 URL |
| 前端单测 | 未引入 ESLint / Prettier / Vitest |
| 状态字典后端对称 | 后端仍有 5 处独立的状态文案方法，未与前端字典统一 |

---

# 第二轮：页面布局建议落实

> 来源：《页面布局优化建议》（2026-10-31 走查）。逐条在真实浏览器里核实后处理。

## 一、核实结果

| 编号 | 建议 | 核实结论 | 处理 |
| --- | --- | --- | --- |
| P1-1 | 导航高亮在详情页失效 | ✅ 属实，**且比建议写的更严重** | 已修 |
| P1-2 | 平台名称三处硬编码 | ⚠️ 部分属实 | 已修 |
| P2-1 | 860–1024px 主导航可能被裁剪 | ❌ **不属实** | 未改断点，仅加防御 |
| P2-2 | 后台缺面包屑 | ✅ 属实 | 已加 |
| P2-3 | 阅读宽度体系不统一 | ✅ 属实 | 已统一 |
| P2-4 | HomeView 过大应拆分 | ✅ 属实 | **本轮未做**，见下文 |
| P3-1 | 抽屉宽度上限 | ✅ 属实 | 已加 |
| P3-2 | 暗色模式 | 建议类 | 未做（超出本轮范围） |
| P3-3 | 键盘焦点样式 | ✅ 属实 | 已加 |
| P3-4 | 页脚信息丰富化 | 可选 | 未做 |

## 二、P1-1 导航高亮（属实，比建议更严重）

**实测证据**：在 `/knowledge/3` 上读取导航项的 class——

```
首页    router-link-active（无 app-nav__item--active）  背景透明
知识库  无任何 active 类                                背景透明
```

结果不是"详情页不高亮"，而是**所有导航项都不高亮**，用户完全失去位置感。

更需要注意的是：建议里的**方案 A（改用 router-link-active）不能直接用**——
`/` 是所有路径的前缀，换成非 exact 版本会让「首页」在每一页都高亮，比现在更糟。

所以采用方案 B 显式判断，并对根路径单独处理：

```js
function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(path + '/')
}
```

顺带把移动端抽屉的菜单项也加上当前位置高亮（原来没有任何指示）。

**实测验证**：

| 路径 | 高亮项 |
| --- | --- |
| `/knowledge/3` | 知识库（首页不再误高亮） |
| `/works/3` | 创意作品 |
| `/` | 首页 |

## 三、P1-2 平台名称接入后台配置（部分属实）

建议说"三处硬编码"，实际核实：

| 位置 | 核实 |
| --- | --- |
| 顶栏品牌文字 | ✅ 硬编码，未接配置 |
| 页脚说明 | ✅ 硬编码，未接配置 |
| 首页 hero 标题 | ❌ **早已接入** `/site/info`（上一轮做首页时接的） |
| 浏览器标题 | ✅ 硬编码 |

所以真正要补的是三处，不是四处。

**实现**：新增 `stores/site.js`，统一提供 `name / subtitle / description / banner / contact / announcements`，
接口失败时用内置默认值兜底。顶栏品牌、页脚、router 标题都改读它；首页也改读 store，
避免与布局重复请求 `/site/info`。

**实测闭环**：

```
超管改名 → 保存
  brand      = 科普知识共创平台（改名验证）    ✅
  footer     = 本科毕业设计 · 基于 … 的科普知识共创平台（改名验证）的设计与实现  ✅
  heroTitle  = 科普知识共创平台（改名验证）    ✅
  heroBanner = 改名验证横幅                    ✅
  documentTitle = 首页 - 科普知识共创平台（改名验证）  ✅
```

**过程中发现一个时序问题**：`router.afterEach` 在导航时写标题，
那一刻 store 还没加载完，标签页会停在默认名（第一次测就是品牌和 hero 都变了、标题没变）。
补了一个 `watch(siteStore.name)`，名称到位后重写一次标题。

**平台名称变长后的防御**：名称由后台维护，长度不可控。顶栏品牌加了
`max-width: 260px` + 省略号（1024px 以下收窄到 180px），避免把导航挤出去。

## 四、P2-1 导航裁剪（不属实）

在 1024 / 900 / 870px 三档实测：

```
900px  navClientWidth 385 = navScrollWidth 385  未被裁剪
870px  navClientWidth 355 = navScrollWidth 355  未被裁剪
       五个导航项 right 全部 ≤ nav 右边界
```

所以当前不存在裁剪问题，断点保持 860px 不变。

不过建议指出的风险是真的：`.app-nav` 有 `overflow: hidden`，一旦内容超宽会被静默裁掉。
接入平台配置后品牌名变长，这个风险变大了——处理方式是上面说的品牌名截断，
而不是提前收导航（过早收进抽屉会让 860–1024px 区间损失横向空间）。

## 五、P2-2 后台面包屑（属实，已加）

`AdminLayout` 的 `.admin-main` 顶部加面包屑，当前页取 `route.meta.title`：

```
后台管理 / 作品管理
```

窄屏侧栏收起后，面包屑仍能说明当前位置。

## 六、P2-3 阅读宽度统一（属实，已改）

原来三个详情页各写一个值：

| 页面 | 改前 | 改后 |
| --- | --- | --- |
| 文章详情 | 860px | `var(--sp-container-reading)` |
| 作品详情 | 900px | 同上 |
| 活动详情 | 960px | 同上 |

`--sp-container-reading` 保持 900px（约 55 字/行，中文长文舒适区间），三处引用同一变量。

## 七、P3-1 抽屉宽度（属实，已改）

`size="78%"` → `size="min(78%, 320px)"`。

实测 430px 视口：抽屉宽度 **320px**（改前会是 335px），不再偏宽。
375px 视口下仍是 78%（292px），不受影响。

## 八、P3-3 键盘焦点（属实，已加）

`style.css` 加全局兜底：

```css
a:focus-visible,
button:focus-visible,
[tabindex]:focus-visible,
.el-tag:focus-visible,
.el-button:focus-visible {
  outline: 2px solid var(--sp-primary);
  outline-offset: 2px;
}
```

用 `:focus-visible` 而非 `:focus`——鼠标点击不触发，只有键盘导航才显示。

实测：Tab 到导航项时 `outlineWidth: 2px`、`outlineColor: rgb(52,127,209)`、匹配 `:focus-visible`。

## 九、实测中额外发现并修复的问题

**后台作品表格的「点赞」列整列空白。**

`AdminWorkView` 绑定了 `prop="likeCount"`，但后端 `WorkListItemVO` 没有这个字段
（只有详情 VO 有）。列表接口返回的字段核对：

```
id, title, description, coverUrl, categoryId, categoryName, creatorId,
creatorName, status, viewCount, reviewComment, reviewedAt, createdAt,
updatedAt, articleCount, articleTitles
```

改成显示「关联文章 N 篇」，用的是 VO 里真实存在的 `articleCount`。

顺手把其余后台表格的列字段与接口返回逐个核对了一遍，均能对应。

## 十、P2-4 HomeView 拆分（本轮未做）

建议合理（1025 行确实偏大），但本轮不做，原因：

1. 纯文件结构调整，**不影响任何视觉与交互**，改造收益只在可维护性
2. 拆成 4 个子组件要动模板与样式作用域，回归成本高于收益
3. 当前有更值得做的事（真实功能修复）

论文里如果要写"代码组织"，可以在「不足与展望」提一句即可。
若后续要做，建议顺序：先抽 `HomeLatestSection`（三个列表区块结构相近，复用价值最高），
再抽 Hero 与流程区块，最后才动数据编排。

## 十一、本轮验证

```
前端构建    ✓ built
27 个页面   全部 200
后端测试    140 个通过（本轮未改后端）
```

重点回归：

| 项 | 结果 |
| --- | --- |
| 导航高亮 | `/knowledge/3`→知识库、`/works/3`→创意作品、`/`→首页 |
| 后台改名闭环 | 品牌/页脚/hero/横幅/标题全部跟随 |
| 900 / 870px 导航 | 五项完整，无裁剪 |
| 430px 抽屉 | 320px（受上限约束） |
| Tab 焦点 | 2px 品牌蓝焦点环可见 |
| 后台表格 | 列字段与接口返回一一对应 |
