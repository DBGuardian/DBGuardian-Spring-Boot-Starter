-- ===============================================
-- DBGuardian 读写分离测试数据库初始化脚本
-- 需要在主库和从库都执行
-- ===============================================

-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS dbguardian_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dbguardian_test;

-- ===============================================
-- 用户表
-- ===============================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入测试数据
INSERT INTO t_user (username, email, phone, status) VALUES
('admin', 'admin@test.com', '13800138000', 1),
('user1', 'user1@test.com', '13800138001', 1),
('user2', 'user2@test.com', '13800138002', 1);

-- ===============================================
-- 订单表
-- ===============================================
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理，PAID-已支付，COMPLETED-已完成，CANCELLED-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 插入测试数据
INSERT INTO t_order (order_no, user_id, amount, status) VALUES
('ORD20260509001', 1, 100.00, 'COMPLETED'),
('ORD20260509002', 1, 200.00, 'PAID'),
('ORD20260509003', 2, 150.00, 'PENDING');

-- ===============================================
-- 验证复制状态
-- ===============================================
-- 在主库执行：SHOW MASTER STATUS;
-- 在从库执行：SHOW SLAVE STATUS;
-- 确保 GTID 复制正常
