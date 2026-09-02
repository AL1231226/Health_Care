-- ============================================================
-- 服务订单表（用户预约商家服务项目；service_comment.order_id 外键关联本表）
-- 在 MySQL 库 health_data 中执行：mysql -uroot -p health_data < service_order.sql
-- ============================================================
CREATE TABLE IF NOT EXISTS service_order (
    order_id     BIGINT AUTO_INCREMENT COMMENT '订单ID',
    order_no     VARCHAR(32)  DEFAULT NULL COMMENT '订单号（业务编号，生成规则后续定）',
    user_id      BIGINT       NOT NULL COMMENT '下单用户 (FK -> sys_user.id)',
    elder_id     BIGINT       NOT NULL COMMENT '服务老人 (FK -> elder_profile.elder_id，下单为哪位老人预约)',
    provider_id  BIGINT       NOT NULL COMMENT '服务商家 (FK -> service_provider.provider_id)',
    item_id      BIGINT       NOT NULL COMMENT '服务项目 (FK -> service_item.item_id)',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '数量',
    unit_price   DECIMAL(10, 2) NOT NULL COMMENT '单价快照（下单时项目价格）',
    total_price  DECIMAL(10, 2) NOT NULL COMMENT '总价 = 单价 * 数量',
    order_status TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待接单 1服务中 2已完成 3已取消',
    service_time DATETIME     DEFAULT NULL COMMENT '预约服务时间（可空）',
    address_id   BIGINT       DEFAULT NULL COMMENT '服务地址 (FK -> user_address.addr_id，可空)',
    contact_phone VARCHAR(20) DEFAULT NULL COMMENT '下单预留联系电话（可空，默认取用户手机号）',
    remark       VARCHAR(200) DEFAULT NULL COMMENT '订单备注',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (order_id),
    KEY idx_user (user_id),
    KEY idx_elder (elder_id),
    KEY idx_provider (provider_id),
    KEY idx_item (item_id),
    KEY idx_status (order_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '服务订单表';

-- service_comment.order_id 补外键（建表时订单表未建故悬空，现已补齐）
ALTER TABLE service_comment
    ADD CONSTRAINT fk_comment_order FOREIGN KEY (order_id) REFERENCES service_order (order_id);

-- 2026-09-02 前已建的表补预留电话列（新库由上面 CREATE TABLE 自带，无需执行）
-- ALTER TABLE service_order ADD COLUMN contact_phone VARCHAR(20) DEFAULT NULL
--     COMMENT '下单预留联系电话（可空，默认取用户手机号）' AFTER address_id;

-- 2026-09-02 已建表结构调整（新库由上面 CREATE TABLE 自带，无需执行）：
-- category_id 冗余删除（服务项目 service_item 已归属分类），改为关联老人档案 elder_profile
-- （一个家属可添加多个老人，下单需指明为哪位老人预约）
-- ALTER TABLE service_order
--     ADD COLUMN elder_id BIGINT NOT NULL COMMENT '服务老人 (FK -> elder_profile.elder_id，下单为哪位老人预约)' AFTER user_id,
--     DROP COLUMN category_id,
--     ADD KEY idx_elder (elder_id);
