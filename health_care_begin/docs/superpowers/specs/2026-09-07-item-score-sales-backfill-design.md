# service_item 冗余列接活设计文档:score=服务评价均值、sales=已完成订单数(2026-09-07)

## 背景

`service_item.score/sales` 自建表起是**注释自标「冗余」的死列**:score 恒 DEFAULT 5.0、sales 恒 0,`ServiceItemServiceImpl` 里 insert 置 null/0、update 置 null 跳过(防篡改),从未有任何路径聚合回填——而实体注释写明原设计意图「平均评分(冗余,评价后聚合回填)」「销量(冗余)」。

9-07 两轮决策:先拟删除死列(一评一星实时聚合模型下冗余),又反转**保留字段**;经再次拍板:**既然没删除,就用上**——
- `service_item.score` = 该服务(service_comment 按 item_id)收到评价的**均值**(1 位小数 HALF_UP),评价写入后同事务回填;无评价 → NULL(前端「暂无评分」)
- `service_item.sales` = 该服务**已完成(2)订单数**(用户拍板口径),订单 1→2 完成流转后同事务回填;下单/取消不计(已完成不可回退,单调无需回减)

保持既有的读路径不变(家属端商家列表/详情评分仍按 service_comment 实时聚合;商家端统计卡已接真)——score/sales 作为**写路径维护的冗余缓存**,服务于逐服务的行级展示(商家表格评分列、用户端商家卡片内嵌服务行 ★),查询零聚合成本。

## 已定决策

| 决策点 | 方案 |
| --- | --- |
| score 口径 | 按 item_id 聚合 service_comment.score 均值,1 位小数 HALF_UP;无评价 → NULL |
| score 写点 | `createComment` 评价落库成功后同事务回填(方法加 `@Transactional`,与评价写原子);删评功能落地时同法重算 |
| sales 口径 | 该服务 `order_status=2` 的订单数(已完成);用户拍板 |
| sales 写点 | `updateOrderStatus` 1→2 完成分支回填(同事务);0→3 取消不回填、下单不回填 |
| 列定义 | `ALTER service_item MODIFY score decimal(2,1) NULL DEFAULT NULL`(现 NOT NULL DEFAULT 5.0 是无评价也显 5.0 假象的根因;sales int NOT NULL DEFAULT 0 语义合理不动) |
| 存量 | 一次性 UPDATE 回填现有评价/已完成订单;此后走代码路径 |
| 防篡改 | ServiceItemServiceImpl 现有置空跳过逻辑保留(商家不得直写);前端零改动(展示端早已读列:商家表格 `row.score ?? '-'`、用户端服务行 ★) |

## 后端设计

### ServiceCommentServiceImpl(score 回填)

- `createComment` 方法加 `@Transactional`;`save(comment)` 成功后调用私有 `refreshItemScore(order.getItemId())`
- 私有 `refreshItemScore(itemId)`:按 item_id 全量查评价 → 空:`serviceItemMapper.update(null, wrapper.set("score", null))`(显式 set null,updateById 跳 null 无法置空);非空:均值 setScale(1, HALF_UP) 写入 `service_item.score`
- 文件已注入 serviceItemMapper;import 补 `Transactional`、`LambdaUpdateWrapper`

### ServiceOrderServiceImpl(sales 回填)

- `updateOrderStatus` 完成分支(`status == 2`)更新订单成功后调用私有 `refreshItemSales(order.getItemId())`;方法加 `@Transactional`
- 私有 `refreshItemSales(itemId)`:count 该 item `order_status=2` 订单 → `serviceItemMapper.update` set `sales`
- 文件已注入 serviceItemMapper、已用 LambdaUpdateWrapper;import 补 `Transactional`

### 改动文件
`service/impl/ServiceCommentServiceImpl.java`、`service/impl/ServiceOrderServiceImpl.java`(后端 2 文件)

## 验证

1. `./mvnw -q compile`;ALTER + 存量回填 SQL(两段 UPDATE,执行后 SELECT 核对:item1=5.0/已完成数、item2=4.5、item3(无评价)=NULL)
2. 起后端 curl 冒烟:
   - 家属对某已完成未评订单发表新评价 → 该 item.score 即时变(如 item2 5+4+新评 N → 均值断言)且响应与库一致
   - 商家把某 1→2 订单完成 → 该 item.sales = 库中已完成单数对账
   - 回归:商家 /service-item/list 行带新 score/sales;家属 /service-comment/my 正常;商家 /score 统计卡不受影响
3. 存量测试数据(评价/状态流转)恢复基线;CHANGELOG + PROJECT_PROGRESS(候选② score/sales 死列去留 → 已接活)同步
