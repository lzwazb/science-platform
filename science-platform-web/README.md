# science-platform-web

科普知识共创与创意传播平台 · 前端（Vue 3 + Vite + Element Plus + Axios）

## 技术栈

| 组件 | 版本 |
| --- | --- |
| Vue | 3.5.44 |
| Vite | 8.3.5 |
| Vue Router | 5.4.0 |
| Pinia | 4.1.0 |
| Element Plus | 2.14.7 |
| Axios | 1.21.0 |

## 命令

```powershell
npm install       # 首次安装依赖
npm run dev       # 开发服务器 http://localhost:5173
npm run build     # 生产构建，产物在 dist/
npm run preview   # 预览构建产物
```

## 目录约定

```
src/
├── api/           # Axios 实例与接口封装（request.js 为全局实例）
├── assets/        # 静态资源
├── components/    # 通用组件
├── layouts/       # 页面布局
├── router/        # 路由表与守卫
├── stores/        # Pinia 状态（阶段 3 起添加 user store）
├── utils/         # 工具函数
└── views/         # 页面
    ├── public/    # 公开页面
    ├── knowledge/ # 知识库（阶段 4）
    ├── creator/   # 创作者中心（阶段 5）
    ├── contribution/ # 知识共建（阶段 6）
    ├── event/     # 主题活动（阶段 8）
    ├── user/      # 个人中心（阶段 9）
    └── admin/     # 后台管理（阶段 9 起）
```

## 后端联调

开发期 `/api` 请求由 Vite 代理转发到 `http://localhost:8080`（配置见 `vite.config.js`）。

**业务代码中不要硬编码后端地址**，统一写 `/api/v1/...` 相对路径，由 `src/api/request.js` 的 `baseURL` 补全。

后端未启动时，首页的「前后端连通性」区块会显示红色错误提示，属预期行为。
