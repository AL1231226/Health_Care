-- ============================================================
-- service_cart 家属端购物车表(2026-09-04 建)
-- 规则:一行一服务项目,唯一(user_id,item_id);重复加购数量累加(≤99)
-- 结算按「商家 × 项目」拆单(1 订单 = 1 项目 × 数量,维持 service_order 定稿模型)
-- 先 SHOW TABLES 确认无此表再执行(勿重复建表)
-- ============================================================
CREATE TABLE `service_cart` (
  `cart_id`     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '购物车行ID',
  `user_id`     BIGINT      NOT NULL                COMMENT '归属家属(FK -> sys_user.id)',
  `item_id`     BIGINT      NOT NULL                COMMENT '服务项目(FK -> service_item.item_id,ON DELETE CASCADE:项目删除自动清行)',
  `quantity`    INT         NOT NULL DEFAULT 1      COMMENT '份数(1~99)',
  `create_time` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '加购时间',
  `update_time` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cart_id`),
  UNIQUE KEY `uk_cart_user_item` (`user_id`, `item_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_item_id` (`item_id`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_cart_item` FOREIGN KEY (`item_id`) REFERENCES `service_item` (`item_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '购物车(家属)';
