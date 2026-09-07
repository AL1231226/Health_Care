-- service_item 冗余列接活(2026-09-07):score=服务评价均值、sales=已完成订单数
-- 背景:两列建表起为死列(score 恒 DEFAULT 5.0 假象、sales 恒 0),现由代码路径维护
-- (createComment / updateOrderStatus 1->2 同事务回填,见 ServiceCommentServiceImpl.refreshItemScore / ServiceOrderServiceImpl.refreshItemSales)。
-- 本脚本 = 一次性 DDL + 存量回填,此后走代码路径。

-- 1. score 列允许 NULL(无评价的服务不再显假 5.0);sales NOT NULL DEFAULT 0 语义合理不动
ALTER TABLE service_item MODIFY score decimal(2,1) NULL DEFAULT NULL;

-- 2. 存量回填 score:按 item_id 取 service_comment 评分均值(1 位小数,ROUND 四舍五入同代码 HALF_UP);无评价保持 NULL
UPDATE service_item si
LEFT JOIN (
    SELECT item_id, ROUND(AVG(score), 1) AS avg_score
    FROM service_comment
    GROUP BY item_id
) c ON si.item_id = c.item_id
SET si.score = c.avg_score;

-- 3. 存量回填 sales:各服务 order_status=2 已完成订单数(口径同代码)
UPDATE service_item si
LEFT JOIN (
    SELECT item_id, COUNT(*) AS done_cnt
    FROM service_order
    WHERE order_status = 2
    GROUP BY item_id
) o ON si.item_id = o.item_id
SET si.sales = IFNULL(o.done_cnt, 0);
