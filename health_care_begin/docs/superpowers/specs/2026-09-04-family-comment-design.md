# 家属端发表评价设计文档(2026-09-04)

## 背景

颐养平台(居家养老服务预约)当前订单主链路已闭环(家属下单 → 商家接单/完成服务 → 家属查看/取消),但**写评价一环未做**:`service_comment` 表早已建好、评价展示(商家详情页评价列表)与商家级评分聚合(分类下商家列表/商家详情按 provider 实时算平均分)均已通,只差家属侧产出评价。本项目候选 #1「家属发表评价」落地后,评分体系闭环。

## 需求

- 家属对其**已完成(2)**的订单写评价,评价挂 `order_id`(一单一评)
- 评价由星级 + 内容构成;展示口径:一条评价一个 `score`,商家整体评分 = 该商家全部评价 `score` 的平均(既有实时聚合,写入即生效)
- 订单页(我的订单卡片 + 详情抽屉)提供「去评价」入口与「已评价」回显
- 个人中心「我的评价」宫格接真,新页含**待评价/已评价双区**:待评价区可直接发起评价

## 已定决策(用户拍板)

| 决策点 | 方案 |
| --- | --- |
| 评价约束 | 一单一评(应用层按 order_id 查重 + DB 唯一索引 `uk_order_id` 双保险) |
| 评价资格 | 仅本人订单且 `orderStatus==2 已完成`;其余状态/他人订单拒绝 |
| 评分模型 | 每条评价的 score 即该次评分;**不回填** `service_item.score/sales` 冗余列(维持现状不维护),商家分 = 评价平均,实时聚合 |
| 星级/内容 | 星级必选(默认 5 星可调,1~5);内容选填(去空,空串归一 null,限 500 字) |
| 入口形态 | 卡片/抽屉按钮 → 共用评价对话框组件;已评回显星级 + 原文 |
| 我的评价页 | 新页双区(待评价/已评价),待评价 = 已完成且未评订单(前端从订单全量派生,不开新接口) |
| 实现路线 | 方案 A:一个写评接口 + `UserOrderVO` 带已评态 + 一个我的评价接口 |
| 接口路径 | `/service-comment/**`,需登录(追加进 JWT 拦截) |

## 后端设计(改动均在 Elderly_care_Platfrom)

### ① `POST /service-comment/create`(新建,ServiceCommentController)

请求体仅信任 `orderId / score / content`,其余服务端定值。`IServiceCommentService`(现空壳)新增 `createComment`,校验顺序:

1. `orderId` 非空;订单不存在 → 「订单不存在」
2. 归属:`order.userId` ≠ token 家属 → 「无权操作」(防评他人订单)
3. 状态闸门:`orderStatus==2` 才可评;0/1 → 「服务完成后才能评价」,3 → 「订单已取消,无法评价」
4. 查重:该 order_id 已有评价 → 「该订单已评价」
5. 参数:`score` 空或不在 1~5 → 「评分不正确」;`content` 去空、空串归一 null(表可空)、>500 → 「评价内容过长」
6. 定值:`itemId/providerId` 从订单行取(防改评其他服务)、`userId` 取 token、时间戳自动填充 → save → 回查返回完整评价
7. 不改订单状态、不写任何冗余列

### ② `GET /service-comment/my`(新建)

我的全部评价(create_time 倒序),关联订单/商家/服务名**一次批量查出防 N+1**(模式同 `ServiceOrderServiceImpl` 的 toMap 批量),拼新 VO `dao/MyCommentVO`:commentId / orderId / orderNo / itemName / providerName / score / content / createTime。服务「我的评价」页已评价区。

### ③ `UserOrderVO` 扩展(listUserOrders 内回填)

- `commentScore`(Integer,已评 1~5,**null = 未评**,前端判定无需额外布尔)
- `commentContent`(String,已评原文,回显用)

`ServiceOrderServiceImpl.listUserOrders` 把全部订单 order_id 一次批量 IN 查 `service_comment`,map 分组逐行回填;注入 `ServiceCommentMapper`(风格与现直注 mapper 一致)。

### ④ 配置

- `WebMvcConfig` 拦截路径追加 `/service-comment/**`
- DB:`ALTER TABLE service_comment ADD UNIQUE uk_order_id (order_id)`(历史 5 条测试评价 order_id=NULL,MySQL 唯一索引允许多 NULL 不冲突;重复插入由 DB 拒绝兜底)。执行前 `SHOW INDEX` 确认,无新建表脚本需求(该表脚本不在 sql 目录)

## 前端设计(改动均在 health_care_begin)

### 接口层 `src/api/comment.js`(新建,一 Controller 一文件)

- `createComment({ orderId, score, content })`
- `listMyComments()`

### `src/components/OrderCommentDialog.vue`(新建,两页共用)

props:订单行;事件:提交成功后通知父级。中部 `el-rate`(默认 5)+ 内容 `el-input type=textarea`(maxlength 500 + 计数,选填);提交 loading 防重复;成功后 toast + 关闭。

### `src/views/UserOrders.vue`(改)

- 卡片脚:已完成未评(`commentScore==null`)→ 橙色「去评价」按钮;已评 → 「已评价 ★N」小 tag
- 详情抽屉:新增「我的评价」区块,**仅当 orderStatus==2 展示** —— 未评:提示 + 评价按钮;已评:只读星级 + 原文回显(0/1/3 状态订单不出现该区块)
- 成功后 `loadOrders()` 全量刷新(卡片/抽屉同步变已评态)

### `src/views/MyComments.vue`(新建,路由 `/user/comments` 挂 /user children,复用 UserLayout)

- 待评价/已评价双区胶囊(样式对齐 UserOrders 状态筛选)
- onMounted `Promise.all` 拉 `listUserOrders()` + `listMyComments()`
- 待评价区:已完成且 `commentScore==null` 的订单卡片(商家/服务×数量/金额/下单时间)+「去评价」→ OrderCommentDialog,评后重拉移出;空态 el-empty
- 已评价区:评价卡片(服务名·商家名 / 只读星级 / 原文(空不显示内容行)/ 评价时间 /「查看订单」跳 `/user/orders?status=2`);空态 el-empty

### `src/views/UserProfile.vue`(改)

「我的评价」工具格子由占位提示改跳 `/user/comments`。

## 错误处理与兜底

- 后端统一 `Result{success,errorMsg}`;错误原因透传:订单不存在 / 无权操作 / 服务完成后才能评价 / 订单已取消,无法评价 / 该订单已评价 / 评分不正确 / 评价内容过长
- 提交按钮 loading + 后端查重 + DB 唯一索引三重防双评
- 服务/商家名缺失沿用现有「服务已删除」「服务商家」兜底
- 商家详情页既有展示不受影响(provider-comments 按 provider 查,新评自动出现;商家级平均分实时变化)

## 验证方案

1. DB:`SHOW INDEX FROM service_comment`(无 uk)→ ALTER 加唯一索引 → 复查确认
2. 后端 `cd Elderly_care_Platfrom && ./mvnw -q compile`;前端 `npx vite build`
3. 冒烟链路:家属下单 → 商家接单/完成 → 家属端卡片+抽屉现「去评价」→ 提交(星级/内容)→ 变「已评价 ★N」原文回显 → 重复提交提示「该订单已评价」→ 个人中心「我的评价」:待评价区见另一已完成单、评完移走、已评价区见原文 → 商家详情页评价列表出现新评、评分随平均实时变化

## 不做清单(范围外)

- `service_item.score/sales` 冗余列回填(评分只走评价表商家级实时平均;商家端统计卡「平均分」口径问题属既有展示问题,本期不动)
- 商家端查看评价列表、管理员评价删除(预留模块,后续做)
- 评价修改/追评(一单一评定稿)
- UserProfile 其余占位格子(联系客服等)

## 改动纪律

代码/库/配置改动完成后同步记入 `health_care_begin/CHANGELOG.md`(2026-09-04 新节置顶,列涉及文件);DB 唯一索引 ALTER 亦记录;`PROJECT_PROGRESS.md` 完成地图同步勾选「发表评价」。
