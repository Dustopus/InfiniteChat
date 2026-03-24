CREATE DATABASE IF NOT EXISTS InfiniteChat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE InfiniteChat;

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
  `user_name`    VARCHAR(256) NOT NULL COMMENT '用户昵称',
  `password`     VARCHAR(512) DEFAULT NULL COMMENT '密码',
  `email`        VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone`        VARCHAR(256) NOT NULL COMMENT '手机号',
  `avatar`       VARCHAR(1024) NOT NULL DEFAULT 'http://118.25.77.201:9000/infinitec-chat/infinitechat_default_avatar.png' COMMENT '用户头像',
  `signature`    VARCHAR(512) DEFAULT NULL COMMENT '个性签名',
  `gender`       TINYINT      NOT NULL DEFAULT 2 COMMENT '性别 0男 1女 2保密',
  `status`       TINYINT      DEFAULT 1 COMMENT '用户状态 1正常 2封禁 3注销',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `idx_phone` (`phone`) USING BTREE,
  KEY `idx_userId` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 用户余额表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_balance` (
  `user_id`    BIGINT        NOT NULL COMMENT '用户ID',
  `balance`    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户余额表';

-- ============================================================
-- 好友关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS `contact` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `friend_id`  BIGINT       NOT NULL COMMENT '好友ID',
  `remark`     VARCHAR(256) DEFAULT NULL COMMENT '备注名',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ============================================================
-- 好友申请表
-- ============================================================
CREATE TABLE IF NOT EXISTS `friend_request` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_id`    BIGINT       NOT NULL COMMENT '申请人ID',
  `to_id`      BIGINT       NOT NULL COMMENT '被申请人ID',
  `message`    VARCHAR(512) DEFAULT NULL COMMENT '申请附言',
  `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待同意 1同意 2拒绝',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_from_id` (`from_id`),
  KEY `idx_to_id` (`to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- ============================================================
-- 群组表
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_info` (
  `group_id`   BIGINT        NOT NULL COMMENT '群组ID',
  `group_name` VARCHAR(256)  NOT NULL COMMENT '群组名称',
  `avatar`     VARCHAR(1024) DEFAULT NULL COMMENT '群头像',
  `owner_id`   BIGINT        NOT NULL COMMENT '群主ID',
  `notice`     VARCHAR(1024) DEFAULT NULL COMMENT '群公告',
  `member_num` INT           NOT NULL DEFAULT 0 COMMENT '成员数量',
  `status`     TINYINT       DEFAULT 1 COMMENT '状态 1正常 2解散',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ============================================================
-- 群成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_member` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id`   BIGINT   NOT NULL COMMENT '群组ID',
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `role`       TINYINT  NOT NULL DEFAULT 0 COMMENT '角色 0普通成员 1管理员 2群主',
  `nickname`   VARCHAR(256) DEFAULT NULL COMMENT '群内昵称',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ============================================================
-- 消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
  `message_id`  BIGINT        NOT NULL COMMENT '消息ID',
  `sender_id`   BIGINT        NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT        DEFAULT NULL COMMENT '接收者ID（单聊）',
  `group_id`    BIGINT        DEFAULT NULL COMMENT '群组ID（群聊）',
  `chat_type`   VARCHAR(16)   NOT NULL COMMENT '聊天类型 single/group',
  `msg_type`    TINYINT       NOT NULL DEFAULT 1 COMMENT '消息类型 1文本 2图片 3文件 4语音 5视频 6红包 7系统',
  `content`     TEXT          DEFAULT NULL COMMENT '消息内容',
  `extra`       JSON          DEFAULT NULL COMMENT '扩展信息（图片URL、文件URL等）',
  `status`      TINYINT       DEFAULT 1 COMMENT '状态 1正常 2撤回 3删除',
  `reply_to`    BIGINT        DEFAULT NULL COMMENT '回复的消息ID',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_receiver` (`receiver_id`),
  KEY `idx_group` (`group_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 离线消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `offline_message` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '接收用户ID',
  `message_id` BIGINT   NOT NULL COMMENT '消息ID',
  `chat_type`  VARCHAR(16) NOT NULL COMMENT '聊天类型',
  `sender_id`  BIGINT   NOT NULL COMMENT '发送者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='离线消息表';

-- ============================================================
-- 朋友圈表
-- ============================================================
CREATE TABLE IF NOT EXISTS `moment` (
  `moment_id`    BIGINT        NOT NULL COMMENT '朋友圈ID',
  `user_id`      BIGINT        NOT NULL COMMENT '发布者ID',
  `content`      TEXT          DEFAULT NULL COMMENT '文字内容',
  `images`       JSON          DEFAULT NULL COMMENT '图片列表',
  `like_count`   INT           NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count`INT           NOT NULL DEFAULT 0 COMMENT '评论数',
  `status`       TINYINT       DEFAULT 1 COMMENT '状态 1正常 2删除',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`moment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈表';

-- ============================================================
-- 朋友圈点赞表
-- ============================================================
CREATE TABLE IF NOT EXISTS `moment_like` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `moment_id`  BIGINT   NOT NULL COMMENT '朋友圈ID',
  `user_id`    BIGINT   NOT NULL COMMENT '点赞用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moment_user` (`moment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈点赞表';

-- ============================================================
-- 朋友圈评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS `moment_comment` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `moment_id`      BIGINT       NOT NULL COMMENT '朋友圈ID',
  `user_id`        BIGINT       NOT NULL COMMENT '评论者ID',
  `reply_to_id`    BIGINT       DEFAULT NULL COMMENT '回复评论ID',
  `content`        VARCHAR(512) NOT NULL COMMENT '评论内容',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朋友圈评论表';

-- ============================================================
-- 红包表
-- ============================================================
CREATE TABLE IF NOT EXISTS `red_packet` (
  `packet_id`    BIGINT        NOT NULL COMMENT '红包ID',
  `sender_id`    BIGINT        NOT NULL COMMENT '发送者ID',
  `group_id`     BIGINT        DEFAULT NULL COMMENT '群组ID（群红包）',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
  `total_count`  INT           NOT NULL COMMENT '总个数',
  `grabbed_count`INT           NOT NULL DEFAULT 0 COMMENT '已领取个数',
  `greeting`     VARCHAR(256)  DEFAULT NULL COMMENT '祝福语',
  `status`       TINYINT       DEFAULT 1 COMMENT '状态 1进行中 2已领完 3已过期',
  `expire_at`    DATETIME      NOT NULL COMMENT '过期时间',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`packet_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红包表';

-- ============================================================
-- 红包领取记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `red_packet_record` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `packet_id`  BIGINT        NOT NULL COMMENT '红包ID',
  `user_id`    BIGINT        NOT NULL COMMENT '领取用户ID',
  `amount`     DECIMAL(10,2) NOT NULL COMMENT '领取金额',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_packet_user` (`packet_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红包领取记录表';

-- ============================================================
-- 通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS `notification` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT       NOT NULL COMMENT '接收用户ID',
  `type`       VARCHAR(32)  NOT NULL COMMENT '通知类型 friend/moment/system',
  `content`    VARCHAR(512) NOT NULL COMMENT '通知内容',
  `ref_id`     BIGINT       DEFAULT NULL COMMENT '关联ID',
  `is_read`    TINYINT      DEFAULT 0 COMMENT '是否已读 0未读 1已读',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
