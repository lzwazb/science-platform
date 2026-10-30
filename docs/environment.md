# 环境核验记录（阶段 0）

> 核验时间：2026-10-30 13:05（Asia/Shanghai）
> 核验方式：在本机实际执行命令并记录真实输出，未采用任何假设版本。

## 1. 已安装组件与版本

| 组件 | 实测版本 | 路径 / 备注 |
| --- | --- | --- |
| JDK | `21.0.6`（2025-01-21 LTS，Oracle） | `JAVA_HOME=C:\Program Files\java\jdk-21`，`javac 21.0.6` |
| Maven | `3.9.9` | `D:\javatools\apache-maven-3.9.9`，运行时 JDK 为 21.0.6 |
| Node.js | `v22.19.0` | nvm 安装于 `C:\Users\z1367\AppData\Local\nvm\installs\v22.19.0` |
| npm | `11.6.4` | 随 Node.js 提供 |
| MySQL | `8.0.42`（x86_64, Community Server） | `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` |
| Git | `2.47.1.windows.1` | 已安装，尚未初始化仓库 |

开发档案建议 JDK 17 或 21 兼容版本，本机 JDK 21 可用，**采用 JDK 21**。
Spring Boot 采用 **3.5.16**（3.5.x 稳定维护版本，Maven 中央仓库最新 3.5.x）。

## 2. 端口与连通性

| 检查项 | 结果 |
| --- | --- |
| `127.0.0.1:3306`（MySQL） | ✅ 已监听，MySQL 服务可用 |
| `127.0.0.1:8080`（后端预留） | ✅ 空闲，可占用 |
| `127.0.0.1:5173`（Vite 开发服务器） | ✅ 空闲，可占用 |

## 3. 网络与依赖源（重要）

| 源 | 可达性 | 处理方式 |
| --- | --- | --- |
| `repo1.maven.org` / `repo.maven.apache.org`（Maven 中央仓库） | ❌ 直接连接失败（TLS 连接被中断） | 改用镜像 |
| `maven.aliyun.com/repository/public` | ✅ 约 1.8s | **采用**，写入 `maven-settings.xml` |
| `mirrors.cloud.tencent.com` / `repo.huaweicloud.com` | ✅ 可用 | 备用 |
| npm registry | ✅ 可用 | 直接安装，无需镜像 |

因此 Maven 构建必须带 `-s maven-settings.xml` 参数：

```powershell
mvn -s ..\maven-settings.xml clean package
```

本地仓库被指向 `F:\毕业设计\.m2repo`（见 `maven-settings.xml`），原因：
1. 保持项目自包含，整体拷贝即可迁移；
2. 避免写入全局 `~/.m2` 影响其他项目。

> 说明：`.m2repo` 已加入 `.gitignore`，不进入版本库。

## 4. 版本锁定结果

| 组件 | 锁定版本 | 依据 |
| --- | --- | --- |
| Spring Boot | 3.5.16 | 3.5.x 稳定维护线最新版 |
| Java | 21 | 本机 JDK 版本 |
| Vue | 3.5.44 | `create-vite` 脚手架默认 |
| Vite | 8.3.5 | `create-vite` 脚手架默认 |
| Vue Router | 5.4.0 | 最新稳定版 |
| Pinia | 4.1.0 | 最新稳定版 |
| Element Plus | 2.14.7 | 与 Vue 3 配套稳定版 |
| Axios | 1.21.0 | 最新稳定版 |

## 5. 待确认事项（阻塞阶段 2，不阻塞阶段 1）

| 事项 | 现状 | 需要的信息 |
| --- | --- | --- |
| MySQL 连接凭据 | **未提供**；本机 3306 可连，但账号密码未知 | 需要本地 MySQL 的 `root` 密码或专用账号，用于创建 `science_platform` 库并执行 `schema.sql` |
| 数据库名 | 建议 `science_platform` | 如已有命名约定请告知 |
| 是否初始化 Git 仓库 | 尚未初始化 | 档案附录 A 要求「如用户允许」时初始化 |

## 6. 环境结论

阶段 0 完成条件已满足：所有命令可执行，版本与启动方式记录清楚。
阶段 1 的两个必要条件（后端可构建、前端可构建）已验证通过。
