# 家属端购物车与按商家拆单结算设计文档(2026-09-04)

## 背景

颐养平台(居家养老服务预约)订单主链路 + 评价闭环已落地。`UserLayout` 导航「购物车」与 `OrderConfirm` 吸底「加入购物车」按钮均为占位,`service_cart` 表未建。本轮落地购物车:**可跨商家加购,结算按「商家 × 项目」拆单(一个服务项目一张订单;同一商家多个不同项目 = 多张订单),不改变已定稿的"1 订单 = 1 服务项目 × 数量、无订单明细表"模型**。

## 需求

- 家属把不同商家的服务项目加入购物车(重复加购同一项目 = 数量累加)
- 购物车页按商家分组、行级勾选(默认全选有效行)、可改数量/删除
- 结算:勾选行 + 整批统一填写(服务老人/服务地址/联系电话/预约时间/备注)→ 后端一次事务生成多张订单(每勾选行一张,含同商家多项目各自成单)+ 清除已结算购物车行
- 失效服务(项目下架/删除)在购物车实时标灰禁选,结算二次校验整批拒绝并点名

## 已定决策(用户拍板)

| 决策点 | 方案 |
| --- | --- |
| 拆单模型 | 维持一单一项目(商家×项目维度);同商家多项目 = 多订单;购物车/确认页按商家分组展示并标注"该商家将生成 N 张订单" |
| 表模型 | 一行一项目;`service_cart` 唯一索引 `uk_cart_user_item(user_id, item_id)`;重复加购数量累加(上限 99 拒);item 删除 FK CASCADE 清行 |
| 加购入口 | 商家详情页服务行(直接 +1)+ 下单确认页吸底按钮(带当前所选数量) |
| 结算链路 | 购物车页勾选 → OrderConfirm 双模式(cartIds query 走批量结算;itemId 单服务模式保留)→ 一次生成 N 单 → 成功页列各订单号 |
| 失效处理 | 列表 itemStatus 联查标灰禁选可删;checkout 现查 item,任一失效整批拒绝点名;`@Transactional` 整体回滚 |
| 实现路线 | 后端主导:service_cart 表 + `/service-cart/**` 模块(CRUD + checkout);createOrder 校验/落库公共化,两入口共用 |
| 电话/备注规则 | 沿用 createOrder:电话空取登录手机号否则校验 11 位;备注去空限 200 字 |
| 接口鉴权 | `/service-cart/**` 追加进 JWT 拦截(需登录) |

## 后端设计(改动均在 Elderly_care_Platfrom)

### ① 建表 service_cart

脚本 `src/main/resources/sql/service_cart.sql`(建表前 SHOW TABLES 确认;当前库无该表):

- cart_id BIGINT PK AUTO_INCREMENT 购物车行 ID
- user_id BIGINT NOT NULL(归属家属;FK → sys_user.id;`idx_user_id`)
- item_id BIGINT NOT NULL(FK → service_item.item_id **ON DELETE CASCADE**,项目删除自动清行)
- quantity INT NOT NULL DEFAULT 1(1~99)
- create_time / update_time 时间戳惯例
- UNIQUE `uk_cart_user_item (user_id, item_id)`

### ② 实体/Mapper/服务/控制器

`ServiceCart` 实体 + `ServiceCartMapper` + `IServiceCartService`/Impl + `ServiceCartController`(`/service-cart/**`);`WebMvcConfig` 拦截路径追加 `/service-cart/**`。

### ③ 接口

| 接口 | 说明 |
| --- | --- |
| `POST /service-cart/add` | 请求 {itemId, quantity=本次新增份数 1~99};item 存在且上架(1)否则「服务不存在或已下架」;同 (user,item) 无行插新、有行累加,超 99 拒「已达数量上限 99」 |
| `GET /service-cart/list` | 本人全部行(归属 token,create_time 升序);item/provider **一次批量联查防 N+1**;VO `dao/CartItemVO`(cartId/itemId/itemName/price/unit/quantity/itemStatus/providerId/providerName/createTime);平铺返回,商家分组由前端做 |
| `PUT /service-cart/quantity/{cartId}?quantity=` | 归属校验 + 1~99 |
| `DELETE /service-cart/remove/{cartId}` | 归属校验 |
| `POST /service-cart/checkout` | 请求 {cartIds[], elderId, addressId?(可空), serviceTime?(可空), contactPhone, remark} —— 见下 |

### ④ checkout 逻辑(核心)

1. cartIds 非空(「请选择要结算的服务」),逐行**归属校验**(防伪造结算他人行)
2. 逐行**现查 item**(防竞态):不存在或 status≠1 → 整批拒绝「服务「X」已下架,请先在购物车移除」
3. 老人归属 / 地址归属(传了须归属,否则按未选)/ 电话默认规则 / 备注限长 —— 与 createOrder 完全同规
4. **公共化重构**:`ServiceOrderServiceImpl.createOrder` 的"校验 + 单价总价快照 + order_no 生成 + 落库"提炼为内部可复用方法,checkout 按"商家 × 项目"逐行调用,两入口共用一套逻辑
5. `@Transactional`:逐行生成订单(各自 order_no)→ 全部插入 + 按 cartIds 删除购物车行;任一失败整体回滚,无孤儿单
6. 返回订单展示 VO 数组 `dao/OrderCheckoutVO`(orderNo/itemName/quantity/totalPrice,落库后一次拼好),成功页逐单展示

## 前端设计(改动均在 health_care_begin)

### ① `src/api/cart.js`(新)

addToCart(itemId, quantity) / listCart() / updateCartQuantity(cartId, quantity) / removeCartItem(cartId) / checkoutCart(payload)

### ② 加购入口接真

- `ProviderDetail.vue`:服务行右侧「加入购物车」描边小按钮,直接 addToCart(itemId, 1),@click.stop 防触发行跳转,成功 toast
- `OrderConfirm.vue`:吸底「加入购物车」→ addToCart(itemId, 当前所选数量),成功 toast(移占位)

### ③ `UserLayout.vue` 导航

「购物车」由占位跳 `/user/cart`

### ④ 新页 `CartPage.vue`(路由 `/user/cart`,挂 /user children 复用 UserLayout)

- 商家分组卡片:商家名 → 行(勾选框 + 服务名 + ¥单价 × 数量 el-input-number 1~99,变更即 PUT 失败回滚 + 行小计 + 删除按钮)
- 失效行:itemStatus=0 → 整行标灰 +「已下架」tag + 勾选禁用 + 可删除;服务已删(null 兜底文案)同灰处理
- 行勾选默认全选(仅有效行);吸底「去结算(N)」= 已勾选行数,金额 = Σ;点击跳 `/user/order?cartIds=1,2,3`
- 空态 el-empty「购物车还是空的,去逛逛吧」

### ⑤ `OrderConfirm.vue` 双模式改造

- 模式判定:`route.query.cartIds` 存在 → 批量结算模式;否则原 itemId 单服务模式(保留)
- 批量模式进入流程:listCart() 按 cartIds 过滤重建;任一勾选行已失效/不存在 → 提示「有服务已失效,请回购物车处理」阻止提交;cartIds 全未命中(手改 URL 等)→ 空态提示 + 「返回购物车」按钮
- 老人/地址/电话/预约时间/备注 UI 与单服务模式**共用一份**(整批统一;老人默认第一位、地址跟随老人逻辑不变;无老人档案禁用提交)
- 服务清单区:按商家分组只读多行(商家名 → 服务名 × 数量 / 金额),合计 Σ(单价×数量)
- 吸底按钮「提交订单(将生成 N 张订单)」→ checkoutCart(...)→ 成功页列出各订单(服务名 + orderNo)清单,可返回首页/去我的订单;后端已清购物车行,前端无需补偿

## 错误处理与兜底

- 全链 Result{success,errorMsg} 透传:服务不存在或已下架 / 已达数量上限 99 / 数量不正确 / 无权操作 / 请选择要结算的服务 / 老人地址电话备注既有文案
- 列表 itemStatus 是提示性联查;结算裁决以 checkout 现查为准(竞态),整批拒绝点名,事务回滚
- 403 未登录由拦截器统一;失效行前端不可勾选双保险

## 验证方案

1. DB:执行 service_cart.sql,SHOW TABLES + SHOW INDEX 确认表与 uk_cart_user_item
2. 后端 `./mvnw -q compile`;前端 `npx vite build`
3. API 冒烟:商家A/B 各加购 → 同项目重复加购累加 → listCart 联查正确 → 改量/删行 → 勾选跨商家行 checkout → 每行一张订单(order_no 各异)+ 购物车清行 → 重复 checkout 同 cartId 报错 → 下架项目 checkout 整批拒绝点名
4. 前端冒烟:详情页 +1 → 购物车分组页改量/勾选 → 去结算 → 拆单展示 → 成功卡多订单号 → 回购物车仅剩未结算行

## 不做清单(范围外)

- 同商家多项目合并一张订单(需订单明细表,已定维持一单一项目模型)
- 购物车导航角标、收藏/关注、优惠券码、批量清失效行
- 结算后自动回到购物车的空态刷新体验细节留待 UI 打磨

## 改动纪律

代码/库改动同步记入 health_care_begin/CHANGELOG.md(2026-09-04 新节置顶,列涉及文件与原因);PROJECT_PROGRESS.md 完成地图「购物车」勾选 ✅、候选列表移除本项。
