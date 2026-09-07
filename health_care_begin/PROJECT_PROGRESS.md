# 项目实时进度文档（颐养平台 · 居家养老服务预约）

> 本文档随开发持续更新,新会话先读它 + `CLAUDE.md` + `CHANGELOG.md`,即可接手。
> 最后更新:2026-09-07(商家端平均评分统计卡接真:GET /service-comment/provider/score 实时聚合,不读 service_item.score 死列)

## 一、项目全貌

| 端 | 位置 | 技术 |
|---|---|---|
| 前端 | `health_care_begin` | Vue 3 `<script setup>` + Element Plus + vue-router + Vite + axios(无 Pinia,`@`→`src`,api 按后端 Controller 一文件一模块) |
| 后端 | `Elderly_care_Platfrom`(同级目录) | Spring Boot + MyBatis-Plus + MySQL,包 `com.example.Elderly_care_Platfrom` |
| 数据库 | `health_data`(root/123456,客户端 `/d/MySQL/server/bin/mysql`) | 10 张表齐:admin / sys_user / elder_profile / user_address / service_category / service_provider / service_item / service_order / service_comment / service_cart |

后端约定:统一返回 `dao/Result`(`{success,errorMsg,data,total}`,HTTP 恒 200);JWT 拦截在 `config/WebMvcConfig`(公开:`/auth/**`、`/service-category/**`;其余业务接口需登录,含 `/service-order/**`、`/service-cart/**`);Controller 薄壳 + Service 接口/Impl(QueryWrapper);分页未启用(列表全量返回);建表脚本统一存 `Elderly_care_Platfrom/src/main/resources/sql/`。**角色权限统一校验(2026-09-07 起):** Controller 类/方法上标 `@RequireRole(RoleType.USER/ADMIN/PROVIDER)`,`config/RoleInterceptor`(注册在 AuthInterceptor 后)统一执行,新增业务接口只需标注解即受保护,**不要再手写 role 守卫**;登录门禁(IAuthServiceImpl 验请求体 role)与归属/范围校验(看本人/本店数据隔离)仍各司其职。**所有改动必须同步记入 `CHANGELOG.md`**(前端目录,按日期节,新节置顶)。

## 二、三角色 × 模块 完成地图

图例:✅ 已接后端 / 🧩 纯静态占位 / ⏳ 待做

### 登录与账号(✅)
- 家属登录/注册、商家登录/入驻(审核通过前不可登录)、管理员登录;三角色各自 `/auth/*` 接口
- 已定决策:密码**明文存储**(用户决定,登录返回前后端统一置空密码字段);**角色权限统一校验已落地(2026-09-07)**:`@RequireRole` 注解 + `RoleInterceptor` 在登录后统一强制三角色归属,管理员系接口(`/sys-user/**`、`/service-provider/**`)原「仅登录即可调」的裸奔口已堵;前端路由守卫(router 无 beforeEach)仍留候选
- 登录信息存 localStorage:`token` + `user_info`(剔除 password;家属含 id/phone/userName)

### 家属端(用户端)
| 模块 | 状态 | 说明 |
|---|---|---|
| 首页 UserHome | 🧩/✅ | 轮播/分类宫格(分类已接接口、宫格跳商家列表)、热门服务/护工仍静态 |
| 老人管理 | ✅ | `/elder-profile/*` 全通:增删改查/居住地址下拉;有进行中订单(0/1)的档案禁删保护已加 |
| 地址管理 | ✅ | `/address/*` 全通,含设为默认(事务) |
| 服务分类→商家列表 | ✅ | `/service-category/providers`(评分/评价数聚合展示) |
| 商家详情页 | ✅ | `/service-category/provider-detail`、`provider-comments`(评价展示) |
| 下单确认页 OrderConfirm | ✅ | 老人必选(默认第一位)+ 地址默认老人常驻地址可下拉改 + 电话完整显示;**提交订单已接后端**;成功页切换成功卡 |
| 我的订单/个人中心 | ✅ | 新页 `UserOrders.vue`(`/user/orders`):状态筛选(角标)+ 订单卡片 + 详情抽屉 + 取消(仅0待接单→3);UserProfile 状态条接真四格角标(待接单/服务中/已完成/已取消),点击跳对应筛选 |
| 购物车 | ✅ | 2026-09-04 落地:跨商家加购(商家详情行 +1 / 确认页带数量入车)→ `/user/cart` 商家分组页(勾选默认全选、改量、删行、失效标灰禁选)→ 勾选 `?cartIds=` 进确认页批量结算,后端按「商家×项目」拆单(一勾选行一订单,同商家多项目=多单),整批统一老人/地址/电话/备注,事务内清购物车行;UserLayout 导航入口接真 |
| 发表评价 | ✅ | 2026-09-04 落地:已完成(2)订单卡片/抽屉「去评价」+ 我的评价页双区(待评价/已评价),一单一评挂 order_id(唯一索引),商家分实时聚合闭环;不回填 item 冗余列 |

### 商家端(✅ 服务 + 订单 双 Tab 工作台 `MerchantHome.vue`)
- **服务项目 Tab**:增删改查/上下架/统计卡(总数/上架/下架实时,**平均评分 2026-09-07 接真 = `GET /service-comment/provider/score` 按 service_comment 实时聚合本人店铺评价,0 评价显示「—」;`service_item.score/sales` 冗余死列保留未动,待议**),接 `/service-item/*`
- **订单管理 Tab(2026-09-02 落地)**:状态筛选(角标)+ 表格 + **详情抽屉(服务老人健康备注必看)** + 接单(0→1)/完成服务(1→2),接 `/service-order/merchant/list`、`/service-order/status/{id}`
- **店铺资料编辑(2026-09-07 落地,主营分类即日修订为只读)**:店铺信息卡「编辑资料」接真,弹窗查改本人店铺资料——`GET/PUT /service-provider/self`(**方法级 @RequireRole(PROVIDER) 覆盖类级 ADMIN**,admin/family 打 /self 拒「无权限」,类级三管理员端点不受影响);白名单四字段(名称/负责人/简介/详细地址),**主营分类与 phone 同属入驻归属信息只读不可改**(后端不接收 categoryId,篡改不落库,弹窗分类 select disabled + 提示);保存成功整对象同步店铺卡 + localStorage user_info,无需重登;店铺状态/待审核提示已处理

### 管理员端 `AdminHome.vue`
- ✅ 用户管理(家属禁启)、商家审核(通过/驳回)、商家管理(启停)、待办看板(实时)、**订单管理(2026-09-04 落地:全平台只读监督)**、**角色权限统一校验(2026-09-07 落地:@RequireRole + RoleInterceptor,本组三接口全部类级 ADMIN 标注)**
- 订单管理只读监督: `GET /service-order/admin/list`(role=2 守卫,家属/商家越权「无权限」),状态筛选胶囊带角标 + 关键词搜索(商家/服务/老人/家属/订单号)+ 富表格 + 详情抽屉(含下单家属昵称/老人健康备注,纯查看);数据看板待办区「待接单新订单 N」卡联动(查看全部 → 订单管理自动选待接单),订单数据一次加载看板/订单页共享
- 🧩 数据看板统计卡部分静态;⏳ 服务管理、分类管理、评价管理(删除)、系统设置

## 三、订单模块现状(重点,2026-09-02 主链路)

**表结构(已定稿,勿再改)**
- `service_order`:1 订单 = 1 服务项目 × 数量,**无订单明细表**(已定决策;将来购物车按项目/商家拆单)
- 列:order_id / order_no(规则=`yyyyMMddHHmmss+4位随机`)/ user_id / **elder_id(服务老人,NOT NULL,一个家属多老人,下单必选)** / provider_id / item_id / quantity / unit_price / total_price(单价总价均为下单时服务端快照)/ order_status / service_time(可空,须未来时间)/ address_id(可空,须归属)/ contact_phone / remark / 时间戳
- 状态机:**0待接单 → 1服务中 → 2已完成**;3已取消(家属端可取消本人 0待接单 订单;1/2 只能电话协商,不进系统)
- `category_id` 已删(冗余,item 已归属分类);`service_comment.order_id` 外键已补;脚本 `src/main/resources/sql/service_order.sql` 含新库全量 CREATE + 旧库迁移注释

**已通流程**:家属下单(`POST /service-order/create`)→ 商家 Tab 看到待接单(详情含老人健康备注,实时联查 elder_profile)→ 接单 → 完成服务;家属端「我的订单」(`/service-order/user/list`)可随时查看进度/详情,待接单订单可取消(0→3,`/service-order/cancel/{id}`),商家端已取消 tab 正常展示。库内已有 1 条测试订单。

**评价闭环(2026-09-04)**:家属端对**已完成(2)**订单发表评价(`POST /service-comment/create`),一单一评(`service_comment.order_id` 唯一索引 `uk_order_id` 双保险;星级 1~5 必填、内容选填限 500);校验仅限本人订单、未评订单,item/provider 服务端从订单行定值不信任请求体;评价写入后商家详情/列表/商家详情评价列表的评分(**全部评价 score 实时平均**)自动更新,无需任何聚合写入;入口:我的订单卡片/抽屉「去评价」+已评回显(星级+原文),个人中心「我的评价」(`/user/comments`)双区页——待评价区 = 已完成未评订单直接发起,已评价区 = `GET /service-comment/my` 历史列表;**不回填** `service_item.score/sales` 冗余列(维持不维护)。

**购物车与按商家拆单结算(2026-09-04)**:表 `service_cart` 一行一项目(唯一 `uk_cart_user_item(user_id,item_id)`,重复加购数量累加上限 99,item 删除 FK 级联清行);`/service-cart/**` 五接口全挂 token 归属——add(须上架)/list(联查商家服务名并标 `itemStatus`)/quantity/remove/**checkout**;checkout 逐行归属校验 + **现查** item 任一失效整批拒绝点名,逐行**复用下单 createOrder 同源校验/快照**生成订单(@Transactional 失败整体回滚无孤儿单),成功清购物车行;拆单按「商家×项目」= 一勾选行一订单(含同商家多项目各成单),维持一单一项目模型不加明细表;入口:商家详情服务行「加入购物车」+ 确认页带量入车 + CartPage(`/user/cart`)勾选 → 确认页 `cartIds` 批量模式(整批统一老人/地址/电话/备注,按商家分组标注将生成 N 张订单);冒烟测试订单已清理。

**健康信息决策**:老人健康备注(**实时联查**,不做快照,家属更新商家即时可见);仅该单商家订单详情可见;老人档案删除后订单详情提示「档案已删」。

**管理员只读监督(2026-09-04)**:`GET /service-order/admin/list` 全平台订单(仅管理员可达,2026-09-07 起由 Controller 层 `@RequireRole(ADMIN)` + RoleInterceptor 统一强制,原接口内 role 守卫已删除,防全平台数据泄露),联 sys_user 取下单家属昵称,AdminHome「订单管理」状态筛选/关键词搜索/详情抽屉(含老人健康备注,协调投诉参考),纯查看无写操作;数据看板待办区待接单卡联动;订单写操作(强制取消等)留待仲裁需求单独立项。

## 四、下一轮候选(按惯例的小步推进,一次一个)

1. 前端路由守卫(router 无 beforeEach,建议补,防手输 URL 直达他端页面)——店铺资料编辑(前身候选)已完成
2. 评价延伸(不急):商家端查看「我的评价」列表(聚合接口已打底:provider/score)、管理员评价删除(AdminHome 预留)——「商家统计卡平均分口径」已接真完成(2026-09-07),`service_item.score/sales` 死列去留待议
3. 已定但未做:登录后管理员中途停用商家店铺的窗口期兜底(登录已拒 0/2 状态,token 有效期内被停用仍可用,30min 过期自然失效)

## 五、环境速查

```bash
# 前端
cd health_care_begin && npx vite build        # 构建验证(本项目用它代替 lint)
npm run dev                                   # 开发(端口见 vite.config.js,代理无,直连后端 8080 由 CORS 放行)

# 后端(仓库自带 mvnw,全局无 mvn)
cd Elderly_care_Platfrom && ./mvnw -q compile # 编译验证
# 运行:IDE 启动 ElderlyCarePlatfromApplication,端口 8080

# 数据库
/d/MySQL/server/bin/mysql -uroot -p123456 health_data
```

**改动纪律**:代码/库/配置改完 → 更新 `CHANGELOG.md`(新节置顶)→ 可顺手在本文档「完成地图/下一轮」同步勾选。

**当前 git 状态**:master 分支已按功能分批提交(最近:466daa0 商家端平均分统计卡接真设计文档 → 990b432 平均分接真落地(feat,后端聚合接口 + 前端统计卡 + CHANGELOG + 本文档同步),此前 679f45e 主营分类只读修订、e9df148/acfbe18 商家店铺资料编辑(设计+落地)、4e11e61/c35c52e 角色权限统一校验(设计+落地)、04e9469 管理员订单管理、19d18ee 购物车+拆单结算、01384a4 评价闭环),提交后工作树干净;`repomix-output.md` 为后端旧快照(2026-09-02 前),看代码以后端目录为准。
