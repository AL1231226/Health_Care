# 项目实时进度文档（颐养平台 · 居家养老服务预约）

> 本文档随开发持续更新,新会话先读它 + `CLAUDE.md` + `CHANGELOG.md`,即可接手。
> 最后更新:2026-09-02(家属端「我的订单」列表/详情/取消闭环落地)

## 一、项目全貌

| 端 | 位置 | 技术 |
|---|---|---|
| 前端 | `health_care_begin` | Vue 3 `<script setup>` + Element Plus + vue-router + Vite + axios(无 Pinia,`@`→`src`,api 按后端 Controller 一文件一模块) |
| 后端 | `Elderly_care_Platfrom`(同级目录) | Spring Boot + MyBatis-Plus + MySQL,包 `com.example.Elderly_care_Platfrom` |
| 数据库 | `health_data`(root/123456,客户端 `/d/MySQL/server/bin/mysql`) | 9 张表齐:admin / sys_user / elder_profile / user_address / service_category / service_provider / service_item / service_order / service_comment |

后端约定:统一返回 `dao/Result`(`{success,errorMsg,data,total}`,HTTP 恒 200);JWT 拦截在 `config/WebMvcConfig`(公开:`/auth/**`、`/service-category/**`;其余业务接口需登录,含 `/service-order/**`);Controller 薄壳 + Service 接口/Impl(QueryWrapper);分页未启用(列表全量返回);建表脚本统一存 `Elderly_care_Platfrom/src/main/resources/sql/`。**所有改动必须同步记入 `CHANGELOG.md`**(前端目录,按日期节,新节置顶)。

## 二、三角色 × 模块 完成地图

图例:✅ 已接后端 / 🧩 纯静态占位 / ⏳ 待做

### 登录与账号(✅)
- 家属登录/注册、商家登录/入驻(审核通过前不可登录)、管理员登录;三角色各自 `/auth/*` 接口
- 已定决策:密码**明文存储**(用户决定,登录返回前后端统一置空密码字段);角色权限统一校验(如 admin 接口 checkAdmin)**暂未做,后续统一处理**
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
| 购物车 | ⏳ | UserLayout 导航有入口;OrderConfirm 有「加入购物车」按钮(占位) |
| 发表评价 | ⏳ | 评价表已建、展示已通,家属写评价未做(评挂 order_id) |

### 商家端(✅ 服务 + 订单 双 Tab 工作台 `MerchantHome.vue`)
- **服务项目 Tab**:增删改查/上下架/统计卡,接 `/service-item/*`
- **订单管理 Tab(2026-09-02 最新落地)**:状态筛选(角标)+ 表格 + **详情抽屉(服务老人健康备注必看)** + 接单(0→1)/完成服务(1→2),接 `/service-order/merchant/list`、`/service-order/status/{id}`
- 店铺资料编辑:按钮禁用占位 ⏳;店铺状态/待审核提示已处理

### 管理员端 `AdminHome.vue`
- ✅ 用户管理(家属禁启)、商家审核(通过/驳回)、商家管理(启停)、待办看板(实时)
- 🧩 数据看板统计卡部分静态;⏳ 服务管理、分类管理、**订单管理(预留位,订单模块落地后填充)**、评价管理(删除)、系统设置

## 三、订单模块现状(重点,2026-09-02 主链路)

**表结构(已定稿,勿再改)**
- `service_order`:1 订单 = 1 服务项目 × 数量,**无订单明细表**(已定决策;将来购物车按项目/商家拆单)
- 列:order_id / order_no(规则=`yyyyMMddHHmmss+4位随机`)/ user_id / **elder_id(服务老人,NOT NULL,一个家属多老人,下单必选)** / provider_id / item_id / quantity / unit_price / total_price(单价总价均为下单时服务端快照)/ order_status / service_time(可空,须未来时间)/ address_id(可空,须归属)/ contact_phone / remark / 时间戳
- 状态机:**0待接单 → 1服务中 → 2已完成**;3已取消(家属端可取消本人 0待接单 订单;1/2 只能电话协商,不进系统)
- `category_id` 已删(冗余,item 已归属分类);`service_comment.order_id` 外键已补;脚本 `src/main/resources/sql/service_order.sql` 含新库全量 CREATE + 旧库迁移注释

**已通流程**:家属下单(`POST /service-order/create`)→ 商家 Tab 看到待接单(详情含老人健康备注,实时联查 elder_profile)→ 接单 → 完成服务;家属端「我的订单」(`/service-order/user/list`)可随时查看进度/详情,待接单订单可取消(0→3,`/service-order/cancel/{id}`),商家端已取消 tab 正常展示。库内已有 1 条测试订单。

**健康信息决策**:老人健康备注(**实时联查**,不做快照,家属更新商家即时可见);仅该单商家订单详情可见;老人档案删除后订单详情提示「档案已删」。

## 四、下一轮候选(按惯例的小步推进,一次一个)

1. **家属发表评价**(评挂 order_id,已完成(2)订单详情内入口;评价后回填 item 评分/销量聚合——商家端/列表页评分已按评价实时聚合,补写评价即可闭环)
2. 购物车(加购→结算按项目/商家拆单;OrderConfirm「加入购物车」按钮接真)
3. 管理员订单管理;角色权限统一校验;商家店铺资料编辑

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

**当前 git 状态**:master 分支仅 1 个 init commit,大量改动(前后端订单功能等)未提交;`repomix-output.md` 为后端旧快照(2026-09-02 前),看代码以后端目录为准。
