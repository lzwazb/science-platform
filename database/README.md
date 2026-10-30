# 数据库脚本目录

阶段 1 尚未接入数据库，本目录暂为空。

## 阶段 2 计划产出

| 文件 | 内容 |
| --- | --- |
| `schema.sql` | 建库建表语句（17 张表，含主键、外键、唯一约束、索引） |
| `seed.sql` | 演示数据：角色字典、演示账号（BCrypt 哈希）、分类、示例科普文章、专题、活动 |

## 执行方式（阶段 2 起）

```powershell
mysql -u root -p < schema.sql
mysql -u root -p < seed.sql
```

脚本要求可重复执行（`CREATE TABLE IF NOT EXISTS` / 先 `DROP` 后建，二者择一并注明）。

## 约定

- 库名：`science_platform`
- 字符集：`utf8mb4`，排序规则 `utf8mb4_general_ci`
- 表名：小写下划线；主键：`BIGINT` 自增；时间字段：`DATETIME`
- 正文类字段使用 `TEXT` 或 `LONGTEXT`
- 密码只存 BCrypt 哈希，禁止明文
- 演示账号密码写在 `README.md`，并注明首次登录后应修改
