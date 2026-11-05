-- 用户库初始化脚本
-- 手动执行： mysql -u root -p < db/01_campus_user.sql
-- 可重复执行：会先 DROP 再 CREATE，并自带建库语句

CREATE DATABASE IF NOT EXISTS `campus_user` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;

USE `campus_user`;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`       int(11)      NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50)  NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码占位列：本示例不做密码校验，真实实现应存 BCrypt 摘要',
  `balance`  decimal(10,2) DEFAULT '0.00' COMMENT '账户余额',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 演示数据。password 为占位字符串，项目中没有任何接口读取或比对该列。
INSERT INTO `user` (`id`, `username`, `password`, `balance`) VALUES
  (1, 'seller_01', 'NOT_A_REAL_PASSWORD', 1000.00),
  (2, 'buyer_01',  'NOT_A_REAL_PASSWORD', 500.00);
