-- =============================================================================
-- 科普知识共创与创意传播平台 · 数据库结构脚本
-- -----------------------------------------------------------------------------
-- 数据库：science_platform
-- 字符集：utf8mb4 / utf8mb4_general_ci
-- 引擎：InnoDB
-- 主键：BIGINT UNSIGNED 自增
-- 时间：DATETIME
-- 说明：
--   1. 本脚本可重复执行：先按依赖倒序 DROP，再重新创建
--   2. 逻辑删除统一使用 `deleted` 字段（0 = 正常，1 = 已删除）
--   3. 状态值统一使用大写英文枚举，中文含义见 docs/数据库设计/数据字典.md
--   4. 多态关联（评论/点赞/收藏/反馈）通过 target_type + target_id 表达，
--      不使用外键，由 Service 层校验目标存在性
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `science_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `science_platform`;

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_announcement`;
DROP TABLE IF EXISTS `sys_config`;
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `sys_notification`;
DROP TABLE IF EXISTS `event_review`;
DROP TABLE IF EXISTS `event_submission`;
DROP TABLE IF EXISTS `creative_event`;
DROP TABLE IF EXISTS `content_feedback`;
DROP TABLE IF EXISTS `interaction_favorite`;
DROP TABLE IF EXISTS `interaction_like`;
DROP TABLE IF EXISTS `interaction_comment`;
DROP TABLE IF EXISTS `work_article_relation`;
DROP TABLE IF EXISTS `creator_work`;
DROP TABLE IF EXISTS `knowledge_contribution`;
DROP TABLE IF EXISTS `article_version`;
DROP TABLE IF EXISTS `topic_article`;
DROP TABLE IF EXISTS `science_article`;
DROP TABLE IF EXISTS `science_topic`;
DROP TABLE IF EXISTS `science_category`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_role`;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 一、账号与权限
-- =============================================================================

-- 角色字典
CREATE TABLE `sys_role` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_code`   VARCHAR(32)     NOT NULL COMMENT '角色编码：USER/CREATOR/REVIEWER/ADMIN/SUPER_ADMIN',
    `role_name`   VARCHAR(50)     NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255)             DEFAULT NULL COMMENT '角色说明',
    `sort_order`  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色字典';

-- 用户账号
CREATE TABLE `sys_user` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(50)     NOT NULL COMMENT '登录名',
    `password_hash` VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname`      VARCHAR(50)     NOT NULL COMMENT '昵称',
    `avatar`        VARCHAR(255)             DEFAULT NULL COMMENT '头像地址',
    `email`         VARCHAR(100)             DEFAULT NULL COMMENT '邮箱（可选）',
    `bio`           VARCHAR(500)             DEFAULT NULL COMMENT '个人简介 / 创作者资料',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE/DISABLED',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户账号与资料';

-- 用户与角色关联（一个用户可有多个角色）
CREATE TABLE `sys_user_role` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `role_id`    BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

-- =============================================================================
-- 二、科普知识库与专题
-- =============================================================================

-- 分类（文章与作品共用，通过 type 区分）
CREATE TABLE `science_category` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50)     NOT NULL COMMENT '分类名称',
    `type`       VARCHAR(20)     NOT NULL COMMENT '分类类型：ARTICLE 文章 / WORK 作品',
    `description` VARCHAR(255)            DEFAULT NULL COMMENT '分类说明',
    `sort_order` INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `status`     VARCHAR(20)     NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_type` (`name`, `type`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章 / 作品分类';

-- 科普专题
CREATE TABLE `science_topic` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(100)    NOT NULL COMMENT '专题标题',
    `description` VARCHAR(500)             DEFAULT NULL COMMENT '专题简介',
    `cover_url`   VARCHAR(255)             DEFAULT NULL COMMENT '封面地址',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
    `sort_order`  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '科普专题';

-- 正式科普文章
CREATE TABLE `science_article` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`        VARCHAR(150)    NOT NULL COMMENT '标题',
    `summary`      VARCHAR(500)             DEFAULT NULL COMMENT '摘要',
    `content`      LONGTEXT        NOT NULL COMMENT '正文内容',
    `cover_url`    VARCHAR(255)             DEFAULT NULL COMMENT '封面地址',
    `category_id`  BIGINT UNSIGNED          DEFAULT NULL COMMENT '分类 ID',
    `author_id`    BIGINT UNSIGNED NOT NULL COMMENT '发布者 ID',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
    `published_at` DATETIME                 DEFAULT NULL COMMENT '发布时间',
    `view_count`   INT             NOT NULL DEFAULT 0 COMMENT '阅读量',
    `version_no`   INT             NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_published` (`status`, `published_at`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_title` (`title`),
    CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_article_category` FOREIGN KEY (`category_id`) REFERENCES `science_category` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '正式科普文章';

-- 专题与文章关联（多对多）
CREATE TABLE `topic_article` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_id`   BIGINT UNSIGNED NOT NULL COMMENT '专题 ID',
    `article_id` BIGINT UNSIGNED NOT NULL COMMENT '文章 ID',
    `sort_order` INT             NOT NULL DEFAULT 0 COMMENT '专题内排序',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_article` (`topic_id`, `article_id`),
    KEY `idx_article_id` (`article_id`),
    CONSTRAINT `fk_topic_article_topic` FOREIGN KEY (`topic_id`) REFERENCES `science_topic` (`id`),
    CONSTRAINT `fk_topic_article_article` FOREIGN KEY (`article_id`) REFERENCES `science_article` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '专题与文章关联';

-- 文章历史版本（内容可追溯）
-- 语义说明：version_no 对应的是「变更前的旧内容快照」。
--   审核通过时：先把当前文章内容以当前 version_no 存入本表，再把文章 version_no + 1 并更新内容。
--   因此 article.version_no = N 表示当前是第 N 版，本表中最多存在到 N-1 的历史快照。
CREATE TABLE `article_version` (
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`              BIGINT UNSIGNED NOT NULL COMMENT '文章 ID',
    `version_no`              INT             NOT NULL COMMENT '版本号',
    `title_snapshot`          VARCHAR(150)    NOT NULL COMMENT '版本标题快照',
    `summary_snapshot`        VARCHAR(500)             DEFAULT NULL COMMENT '版本摘要快照',
    `content_snapshot`        LONGTEXT        NOT NULL COMMENT '版本正文快照',
    `change_note`             VARCHAR(500)             DEFAULT NULL COMMENT '变更说明',
    `operator_id`             BIGINT UNSIGNED NOT NULL COMMENT '操作者 ID',
    `source_contribution_id`  BIGINT UNSIGNED          DEFAULT NULL COMMENT '来源知识贡献 ID（可追溯）',
    `created_at`              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_version` (`article_id`, `version_no`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_version_article` FOREIGN KEY (`article_id`) REFERENCES `science_article` (`id`),
    CONSTRAINT `fk_version_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章历史版本';

-- =============================================================================
-- 三、知识共建与纠错
-- =============================================================================

CREATE TABLE `knowledge_contribution` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `article_id`        BIGINT UNSIGNED NOT NULL COMMENT '目标文章 ID',
    `submitter_id`      BIGINT UNSIGNED NOT NULL COMMENT '提交人 ID',
    `contribution_type` VARCHAR(20)     NOT NULL COMMENT '类型：SUPPLEMENT 补充 / CORRECTION 纠错 / MODIFICATION 修改建议',
    `suggested_content` TEXT            NOT NULL COMMENT '建议内容',
    `reason`            VARCHAR(1000)   NOT NULL COMMENT '提交理由',
    `reference_source`  VARCHAR(500)    NOT NULL COMMENT '参考来源',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/WITHDRAWN',
    `reviewer_id`       BIGINT UNSIGNED          DEFAULT NULL COMMENT '审核人 ID',
    `review_comment`    VARCHAR(1000)            DEFAULT NULL COMMENT '审核意见',
    `final_content`     LONGTEXT                 DEFAULT NULL COMMENT '审核通过时最终采用的正文内容',
    `reviewed_at`       DATETIME                 DEFAULT NULL COMMENT '审核时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_submitter_id` (`submitter_id`),
    KEY `idx_reviewer_id` (`reviewer_id`),
    CONSTRAINT `fk_contribution_article` FOREIGN KEY (`article_id`) REFERENCES `science_article` (`id`),
    CONSTRAINT `fk_contribution_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_contribution_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识补充 / 纠错建议';

-- =============================================================================
-- 四、创意作品
-- =============================================================================

CREATE TABLE `creator_work` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `creator_id`     BIGINT UNSIGNED NOT NULL COMMENT '创作者 ID',
    `title`          VARCHAR(150)    NOT NULL COMMENT '作品标题',
    `description`    VARCHAR(500)             DEFAULT NULL COMMENT '简介',
    `content`        LONGTEXT        NOT NULL COMMENT '正文 / 作品说明',
    `cover_url`      VARCHAR(255)             DEFAULT NULL COMMENT '封面或图片地址',
    `category_id`    BIGINT UNSIGNED          DEFAULT NULL COMMENT '作品分类 ID',
    `status`         VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PENDING/PUBLISHED/REJECTED/OFFLINE',
    `reviewer_id`    BIGINT UNSIGNED          DEFAULT NULL COMMENT '审核人 ID',
    `review_comment` VARCHAR(1000)            DEFAULT NULL COMMENT '审核意见',
    `reviewed_at`    DATETIME                 DEFAULT NULL COMMENT '审核时间',
    `view_count`     INT             NOT NULL DEFAULT 0 COMMENT '浏览数',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_work_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_work_category` FOREIGN KEY (`category_id`) REFERENCES `science_category` (`id`),
    CONSTRAINT `fk_work_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '科普创意作品';

-- 作品与文章关联（多对多）
CREATE TABLE `work_article_relation` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_id`    BIGINT UNSIGNED NOT NULL COMMENT '作品 ID',
    `article_id` BIGINT UNSIGNED NOT NULL COMMENT '文章 ID',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_article` (`work_id`, `article_id`),
    KEY `idx_article_id` (`article_id`),
    CONSTRAINT `fk_war_work` FOREIGN KEY (`work_id`) REFERENCES `creator_work` (`id`),
    CONSTRAINT `fk_war_article` FOREIGN KEY (`article_id`) REFERENCES `science_article` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '作品与科普文章关联';

-- =============================================================================
-- 五、社区互动
-- =============================================================================

-- 评论（target_type + target_id 支持评论文章或作品）
CREATE TABLE `interaction_comment` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '评论人 ID',
    `target_type` VARCHAR(20)     NOT NULL COMMENT '目标类型：ARTICLE/WORK',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标 ID',
    `parent_id`   BIGINT UNSIGNED          DEFAULT NULL COMMENT '楼层所属的根评论 ID（支持回复，只保留一层）',
    `reply_to_comment_id` BIGINT UNSIGNED  DEFAULT NULL COMMENT '实际被回复的评论 ID（可能是根评论，也可能是同层回复）',
    `content`     VARCHAR(1000)   NOT NULL COMMENT '评论内容',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'VISIBLE' COMMENT '状态：VISIBLE/HIDDEN/DELETED',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`, `status`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_reply_to` (`reply_to_comment_id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_comment_reply_to` FOREIGN KEY (`reply_to_comment_id`) REFERENCES `interaction_comment` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评论';

-- 点赞（同一用户对同一目标只能有一条记录）
CREATE TABLE `interaction_like` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `target_type` VARCHAR(20)     NOT NULL COMMENT '目标类型：ARTICLE/WORK',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标 ID',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_like` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '点赞';

-- 收藏（同一用户对同一目标不能重复收藏）
CREATE TABLE `interaction_favorite` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `target_type` VARCHAR(20)     NOT NULL COMMENT '目标类型：ARTICLE/WORK',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标 ID',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收藏';

-- 内容反馈
CREATE TABLE `content_feedback` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '反馈人 ID',
    `target_type`    VARCHAR(20)     NOT NULL COMMENT '目标类型：ARTICLE/WORK/COMMENT',
    `target_id`      BIGINT UNSIGNED NOT NULL COMMENT '目标 ID',
    `feedback_type`  VARCHAR(30)     NOT NULL COMMENT '反馈类型：WRONG_CONTENT/COPYRIGHT/SPAM/SUGGESTION/OTHER',
    `description`    VARCHAR(1000)   NOT NULL COMMENT '反馈说明',
    `status`         VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/RESOLVED/REJECTED',
    `handler_id`     BIGINT UNSIGNED          DEFAULT NULL COMMENT '处理人 ID',
    `handle_comment` VARCHAR(500)             DEFAULT NULL COMMENT '处理说明',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `handled_at`     DATETIME                 DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_feedback_handler` FOREIGN KEY (`handler_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '内容反馈';

-- =============================================================================
-- 六、主题活动
-- =============================================================================

CREATE TABLE `creative_event` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(150)    NOT NULL COMMENT '活动标题',
    `description` TEXT                     DEFAULT NULL COMMENT '活动说明',
    `rules`       TEXT                     DEFAULT NULL COMMENT '参与规则',
    `cover_url`   VARCHAR(255)             DEFAULT NULL COMMENT '封面地址',
    `start_at`    DATETIME        NOT NULL COMMENT '开始时间',
    `end_at`      DATETIME        NOT NULL COMMENT '结束时间',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/CLOSED/CANCELLED',
    `creator_id`  BIGINT UNSIGNED NOT NULL COMMENT '创建人 ID',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `start_at`, `end_at`),
    KEY `idx_creator_id` (`creator_id`),
    CONSTRAINT `fk_event_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '主题创作活动';

-- 活动投稿（同一作品在同一活动只能提交一次）
CREATE TABLE `event_submission` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id`     BIGINT UNSIGNED NOT NULL COMMENT '活动 ID',
    `work_id`      BIGINT UNSIGNED NOT NULL COMMENT '作品 ID',
    `submitter_id` BIGINT UNSIGNED NOT NULL COMMENT '投稿人 ID',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/UNDER_REVIEW/REVIEWED/SELECTED/NOT_SELECTED/WITHDRAWN',
    `award_level`  VARCHAR(20)              DEFAULT NULL COMMENT '获奖等级：FIRST/SECOND/THIRD/EXCELLENT',
    `submitted_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投稿时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_work` (`event_id`, `work_id`),
    KEY `idx_event_status` (`event_id`, `status`),
    KEY `idx_submitter_id` (`submitter_id`),
    KEY `idx_work_id` (`work_id`),
    CONSTRAINT `fk_submission_event` FOREIGN KEY (`event_id`) REFERENCES `creative_event` (`id`),
    CONSTRAINT `fk_submission_work` FOREIGN KEY (`work_id`) REFERENCES `creator_work` (`id`),
    CONSTRAINT `fk_submission_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '活动作品投稿';

-- 活动评审（同一评审员对同一投稿只保留一份有效评审）
CREATE TABLE `event_review` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `submission_id` BIGINT UNSIGNED NOT NULL COMMENT '投稿 ID',
    `reviewer_id`   BIGINT UNSIGNED NOT NULL COMMENT '评审员 ID',
    `score`         TINYINT UNSIGNED NOT NULL COMMENT '评分（1–5）',
    `comment`       VARCHAR(1000)   NOT NULL COMMENT '评语',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_submission_reviewer` (`submission_id`, `reviewer_id`),
    KEY `idx_reviewer_id` (`reviewer_id`),
    CONSTRAINT `fk_review_submission` FOREIGN KEY (`submission_id`) REFERENCES `event_submission` (`id`),
    CONSTRAINT `fk_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '活动评审记录';

-- =============================================================================
-- 七、消息与系统
-- =============================================================================

CREATE TABLE `sys_notification` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `recipient_id` BIGINT UNSIGNED NOT NULL COMMENT '接收人 ID',
    `type`         VARCHAR(30)     NOT NULL COMMENT '类型：REVIEW_RESULT/COMMENT_REPLY/EVENT_NOTICE/SYSTEM',
    `title`        VARCHAR(100)    NOT NULL COMMENT '标题',
    `content`      VARCHAR(500)    NOT NULL COMMENT '内容',
    `related_type` VARCHAR(30)              DEFAULT NULL COMMENT '关联对象类型：ARTICLE/WORK/CONTRIBUTION/EVENT',
    `related_id`   BIGINT UNSIGNED          DEFAULT NULL COMMENT '关联对象 ID',
    `is_read`      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_at`      DATETIME                 DEFAULT NULL COMMENT '读取时间',
    PRIMARY KEY (`id`),
    KEY `idx_recipient_read` (`recipient_id`, `is_read`, `created_at`),
    CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内通知';

CREATE TABLE `sys_operation_log` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operator_id` BIGINT UNSIGNED          DEFAULT NULL COMMENT '操作人 ID（系统操作可为空）',
    `action`      VARCHAR(50)     NOT NULL COMMENT '操作动作，如 ARTICLE_PUBLISH / CONTRIBUTION_APPROVE',
    `target_type` VARCHAR(30)              DEFAULT NULL COMMENT '目标类型',
    `target_id`   BIGINT UNSIGNED          DEFAULT NULL COMMENT '目标 ID',
    `detail`      VARCHAR(1000)            DEFAULT NULL COMMENT '操作详情',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_action` (`action`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '重要操作日志';

-- =============================================================================
-- 八、系统基础设置（第二轮优化新增）
-- =============================================================================

-- 平台基础配置：键值对存储，便于以后扩展配置项而不用改表结构
CREATE TABLE `sys_config` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   VARCHAR(64)     NOT NULL COMMENT '配置键，如 site.name',
    `config_value` TEXT                     DEFAULT NULL COMMENT '配置值',
    `description`  VARCHAR(255)             DEFAULT NULL COMMENT '配置说明',
    `updated_by`   BIGINT UNSIGNED          DEFAULT NULL COMMENT '最后修改人',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    CONSTRAINT `fk_config_operator` FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台基础配置';

-- 系统公告：由超级管理员维护，仅 PUBLISHED 状态对前台可见
CREATE TABLE `sys_announcement` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`        VARCHAR(150)    NOT NULL COMMENT '公告标题',
    `content`      TEXT            NOT NULL COMMENT '公告正文',
    `status`       VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
    `sort_order`   INT             NOT NULL DEFAULT 0 COMMENT '排序，数值越大越靠前',
    `publisher_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人',
    `published_at` DATETIME                 DEFAULT NULL COMMENT '发布时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort_order`),
    KEY `idx_published_at` (`published_at`),
    CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统公告';