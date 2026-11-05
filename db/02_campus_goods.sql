-- 商品库初始化脚本
-- 手动执行： mysql -u root -p < db/02_campus_goods.sql
-- 可重复执行：会先 DROP 再 CREATE，并自带建库语句

CREATE DATABASE IF NOT EXISTS `campus_goods` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;

USE `campus_goods`;

DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `id`        int(11)       NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `title`     varchar(100)  NOT NULL COMMENT '商品标题',
  `price`     decimal(10,2) NOT NULL COMMENT '商品价格',
  `seller_id` int(11)       NOT NULL COMMENT '卖家ID，逻辑关联 campus_user.user.id',
  `stock`     int(11)       DEFAULT '1' COMMENT '库存数量',
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

INSERT INTO `goods` (`id`, `title`, `price`, `seller_id`, `stock`) VALUES
  (1, '二手Java微服务教材', 35.00,  1, 5),
  (2, '九成新山地自行车',    200.00, 1, 2),
  (3, '机械键盘',            80.00,  2, 10);

-- 订单表为预留结构：当前示例只把下单消息投递到 RabbitMQ，尚未实现消费端落库。
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`          int(11)       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `goods_id`    int(11)       NOT NULL COMMENT '商品ID',
  `buyer_id`    int(11)       NOT NULL COMMENT '买家ID',
  `amount`      decimal(10,2) NOT NULL COMMENT '订单金额',
  `status`      varchar(20)   DEFAULT 'SUCCESS' COMMENT '订单状态',
  `create_time` timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表（预留，暂无消费端写入）';
