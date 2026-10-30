# 数据库脚本目录

数据库：`science_platform`｜字符集 `utf8mb4 / utf8mb4_general_ci`｜引擎 InnoDB｜共 **20 张表**

## 文件

| 文件 | 内容 |
| --- | --- |
| `schema.sql` | 建库建表：20 张表、29 个外键、72 个索引，字段均带中文注释。脚本可重复执行 |
| `seed.sql` | 演示数据：6 个账号、5 类角色、10 个分类、3 个专题、8 篇文章、4 件作品、3 条知识贡献、1 个历史版本、2 个活动、评论点赞收藏与通知若干 |

## 执行方式

```powershell
cd database
mysql -u root -p --default-character-set=utf8mb4 -e "source schema.sql"
mysql -u root -p --default-character-set=utf8mb4 -e "source seed.sql"
```

> 注意：`mysql.exe` 无法打开含中文的路径，必须在 `database` 目录内用相对文件名执行，
> 或者先把脚本复制到纯英文路径。`<` 重定向在 PowerShell 下也不可用，请使用上面的 `source` 写法。

## 演示账号

统一密码 **`123456`**（BCrypt cost=10，仅用于本地演示，首次登录后应修改）

| 账号 | 昵称 | 角色 |
| --- | --- | --- |
| `superadmin` | 超级管理员 | SUPER_ADMIN + ADMIN |
| `admin01` | 内容管理员 | ADMIN |
| `reviewer01` | 评审员小林 | REVIEWER + USER |
| `creator01` | 星野科普 | CREATOR + USER |
| `creator02` | 青柠手绘 | CREATOR + USER |
| `user01` | 好奇的小周 | USER |

## 演示数据的用途

数据不是随机填充的，每一条都对应某个阶段的验证场景：

| 数据 | 用途 |
| --- | --- |
| 文章 3 的 `version_no = 2` + 1 条历史版本 + 贡献 #2（APPROVED） | 验证「审核通过 → 版本快照 → 文章更新」的完整链路，阶段 6 直接复用 |
| 贡献 #1（PENDING） | 评审工作台的待办样本 |
| 贡献 #3（REJECTED） | 验证驳回意见与通知 |
| 作品 4（PENDING） | 验证公开页面不展示待审作品 |
| 活动 1（进行中）与活动 2（已结束 + 优秀作品） | 验证活动状态判定与优秀作品展示 |
| 评论 6（HIDDEN）+ 反馈 #1（RESOLVED） | 验证内容治理与反馈处理回链 |
| creator01 与 creator02 各有作品 | 验证「用户 A 不能修改用户 B 的作品」 |

## 约定

- 表名小写下划线；主键 `BIGINT UNSIGNED` 自增；时间字段 `DATETIME`
- 正文类字段 `TEXT` / `LONGTEXT`
- 密码只存 BCrypt 哈希，禁止明文
- 逻辑删除字段统一命名为 `deleted`（0 正常 / 1 已删除），仅用于用户、文章、专题、作品、活动
- 状态字段统一存大写英文枚举，取值见 [数据字典](../docs/数据库设计/数据字典.md#附录状态枚举速查)

## 相关文档

- [数据库设计说明（含 ER 图与设计决策）](../docs/数据库设计/README.md)
- [数据字典](../docs/数据库设计/数据字典.md)
