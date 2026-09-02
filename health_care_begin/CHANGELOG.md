

# 改动记录

## 2026-09-02
- 家属端「我的订单」闭环（订单列表/详情/取消 + 个人中心状态条接真）：后端 `ServiceOrderController` 新增 `GET /service-order/user/list?status=`（本人订单，归属取 token 家属，create_time 倒序，关联字段 provider/item/elder/address 一次批量查出防 N+1，拼装新 VO `dao/UserOrderVO`——订单基础 + 商家名 + 服务名 + 老人姓名 + 地址全文；无单独详情接口，抽屉直接用列表行渲染与商家端一致）与 `PUT /service-order/cancel/{orderId}`（仅本人订单可取消、仅 0待接单→3已取消：待接单时取消无成本；接单后提示电话协商、已完成/已取消给对应提示）；前端 `api/order.js` 加 `listUserOrders`/`cancelOrder`，新建 `src/views/UserOrders.vue`（路由 `/user/orders`，可从 `?status=N` 定位筛选）：状态筛选胶囊（全部/待接单/服务中/已完成/已取消带角标，全量拉取前端聚合）+ 暖色订单卡片（商家名+状态 tag、服务项目×数量、服务老人、下单时间、金额、待接单「取消订单」确认弹窗防误触、点卡开详情抽屉）+ 详情 el-drawer（服务信息(状态/商家/项目/单价×数量/金额/订单号/下单时间)/服务老人/服务安排(预约/地址/电话/备注)，抽屉内同可取消）；`UserProfile.vue` 订单状态条接真：静态「待支付/待服务」改为 待接单/服务中/已完成/已取消 四格带角标（onMounted 拉全量订单按状态聚合），点击跳 `/user/orders?status=N`、「查看全部订单」跳列表；`OrderConfirm.vue` 下单成功卡补「查看我的订单」按钮并更新提示文案；另给老人档案删除加保护——`ElderProfileServiceImpl.deleteElder` 注入 ServiceOrderMapper，该老人存在进行中订单(0待接单/1服务中)时拒绝删除（提示先取消或等完成，避免删档后商家端进行中订单失去健康信息；已完成/已取消可删，详情已有「档案已删」兜底）；商家端零改动（已取消 tag/筛选/抽屉兜底本就就绪）；本轮未做 已完成订单评价入口（下轮候选）
后端 `ServiceOrderController` 新增 `GET /service-order/merchant/list?status=`（本店订单，归属取 token 商家，create_time 倒序，关联字段 item/elder/address **一次批量查出防 N+1**，拼装新 VO `dao/MerchantOrderVO`——订单基础 + itemName + 老人 elderName/gender/birthDate/elderPhone/**healthNote** + 服务地址全文）与 `PUT /service-order/status/{orderId}?status=`（状态流转：仅 0待接单→1服务中(接单)、1服务中→2已完成，非本店「无权操作」，其余流转拒绝）；`IServiceOrderService`/Impl 同步实现；前端 `api/order.js` 加 `listMerchantOrders`/`updateOrderStatus`；`MerchantHome.vue` 店铺信息卡下改 **el-tabs「服务项目 | 订单管理」**（服务项目原统计卡+表格搬入 Tab1），订单管理 Tab：状态筛选（全部/待接单/服务中/已完成/已取消带角标计数）+ 订单表格（订单号/服务项目/服务老人(性别)/金额/预约时间/状态/操作），操作行内「详情」或待接单「接单」、服务中「完成服务」（确认弹窗防误触）；点「详情」开 **el-drawer 抽屉**——服务信息（项目/单价×数量/金额/订单号/下单时间）→ **「服务老人」卡**（姓名性别年龄/老人电话/黄色高亮「健康备注 / 护理注意事项」信息块，档案已删则提示无法查看）→ 服务安排（预约时间/服务地址/联系电话/备注），底部按状态出接单/完成按钮或状态 tag；健康备注**实时联查 elder_profile**（家属更新后商家看到最新状态），仅该单商家订单详情可见不公开；本轮未做家属端订单列表/取消（status 3 入口下轮）

## 2026-09-02
- 家属提交订单打通（订单模型定稿：**不建订单明细表，1 订单 = 1 服务项目 × 数量**，状态/评价/老人信息挂订单）：后端新建 `ServiceOrderController`（`POST /service-order/create`）+ `IServiceOrderService`/Impl 实现 `createOrder`——下单只信任 elderId/itemId/quantity/serviceTime/addressId/contactPhone/remark，其余服务端定值：userId 取 token、providerId/单价从 `service_item` 快照、总价=单价×数量、status 固定 0 待接单、order_no 生成规则定为 `yyyyMMddHHmmss+4位随机数`（沿用待填的 order_no 列，不新增列）；校验：服务老人必填且**归属当前家属**（防跨档案串单）、服务须存在且上架中、数量 1~99、预约时间须晚于当前、地址可选但传了须归属本人（否则按未选）、联系电话空则默认取登录用户手机号否则校验 11 位手机号、备注去空限 200 字；`WebMvcConfig` 拦截路径追加 `/service-order/**`（下单需登录）；前端新建 `src/api/order.js`（createOrder），`OrderConfirm.vue`「提交订单」由占位改为真实提交——前端预检（老人必选/电话格式）、提交中 loading 防重复、成功整页切换**下单成功卡**（✓ 图标、订单号、服务项目/老人/金额/「待商家接单」状态、返回首页按钮、订单列表/详情建设中提示）、失败弹 errorMsg；「加入购物车」仍占位（购物车未建）

## 2026-09-02
- 下单确认页 `OrderConfirm.vue` 静态数据接通（服务订单设计含 elder_id，下单前需指明服务老人）：新增「服务老人」el-select（接 `GET /elder-profile/gets` 真实老人档案，标签含性别/年龄，默认选第一位，作为下单 elder_id）；服务地址由静态 radio 示例改为 el-select 下拉（选项接 `GET /address/gets` 真实地址簿），**默认取所选老人的常驻地址**（`elder_profile.addr_id` 在地址簿中命中则选中，老人切换时重新跟随，之后可手动改选/清空）；无老人档案时表单上方 el-alert 提示并提供「去添加老人档案」跳转，底栏按钮禁用；联系电话默认取登录 `user_info.phone` **完整显示**（原静态脱敏 138\*\*\*\*5678 移除）；吸底提交栏新增「加入购物车」按钮（橙描边，购物车表/接口未建先占位提示）与「提交订单」并列（提交仍占位，TODO 待接 POST /service-order/create）

## 2026-09-02
- 订单表结构调整：`service_order` 删除冗余列 `category_id`（服务项目 `service_item` 已归属分类，无需在订单冗余快照），改为新增 `elder_id`（FK → `elder_profile.elder_id`，NOT NULL + idx_elder 索引）——一个家属 `sys_user` 可添加多个老人档案，下单需指明「为哪位老人预约」；已通过 ALTER 直接改线上库（表 0 行数据无影响），建表脚本 `Elderly_care_Platfrom/src/main/resources/sql/service_order.sql` 的 CREATE TABLE 同步调整并注释记录迁移语句，实体 `ServiceOrder.java` 同步删 `categoryId` 加 `elderId`；前端 OrderConfirm 下单页（未接后端）后续接 POST /service-order/create 时需补「服务老人」选择

## 2026-09-02
- 商家详情页服务行改版 + 静态下单页参考稿：`ProviderDetail.vue` 服务行去掉 ★ 评分展示，新增服务描述行（`item.detail`，空显示「暂无描述」，单行截断），右侧价格下加「购买」按钮，服务行/按钮点击跳 `/user/order`（query 携带 itemId/itemName/price/unit/duration/providerName）；新建 `src/views/OrderConfirm.vue` 下单确认页（**静态版未接后端**，TODO 待接 POST /service-order/create）：服务信息卡 + 表单（数量 el-input-number 默认1/预约时间 datetime 可空/服务地址单选（静态示例，TODO 改拉 GET /address/gets）/联系电话（TODO 默认取登录手机号）/备注 textarea 限200字）+ 吸底提交栏（合计 = 单价×数量实时联动、提交按钮占位提示），无参直接访问兜底示例数据并告警提示；路由 `/user/order` 挂 /user children
- 商家详情页（用户端「商家列表 → 点进商家」）：后端 `ServiceCategoryController` 新增公开接口 `GET /service-category/provider-detail?providerId=`（商家基础信息 + 名下**全部**上架服务：主营分类名联字典、电话脱敏 138\*\*\*\*5678、评分/评价数聚合该商家全部评价、服务不限分类按 sort 升序）与 `GET /service-category/provider-comments?providerId=`（评价列表带评价人昵称（联 sys_user，查不到兜底「匿名用户」）/被评服务名（联 service_item），最新在前）；`IServiceCategoryService`/Impl 新增 `getProviderDetail`/`listProviderComments`（校验商家存在且 status=1，待审核/停用与不存在同等提示「商家不存在」不泄露状态；昵称/服务名一次批量查出避免 N+1），新建 VO `ProviderDetailVO`/`CommentWithUserVO`；前端新增 `src/views/ProviderDetail.vue` 商家详情页（页头返回 + 商家信息卡（logo 首字头像/名称/分类标签/★评分·评价数/服务区域/脱敏电话/负责人/简介）+「全部服务 (N)」行式列表 +「用户评价 (N)」卡片列表（首字头像/昵称/el-rate 只读星级/服务名/时间/内容），商家不存在与参数错误 el-empty 兜底），路由 `/user/merchant/:id` 挂 /user children，`api/provider.js` 新增 `getProviderDetail`/`getProviderComments`；`ServiceProviders.vue` 商家卡片点击由占位提示改为带参跳详情页（服务行预约下单仍占位）
- 订单表补齐：新建 `service_order` 服务订单表（health_data 库，脚本存 `src/main/resources/sql/service_order.sql`）：order_id / order_no（订单号，生成规则后续定）/ user_id(FK→sys_user.id) / provider_id(FK) / item_id(FK) / category_id(冗余) / quantity / unit_price / total_price / order_status(0待接单 1服务中 2已完成 3已取消) / service_time(可空) / address_id(可空) / **contact_phone（下单预留联系电话，可空，默认取用户手机号）** / remark / create_time / update_time；后端新建 `ServiceOrder` 实体/Mapper/`IServiceOrderService`/Impl（薄壳，订单接口待预约流程做）；`service_comment.order_id` 补外键 `fk_comment_order`（建表时订单表未建故悬空，现已闭合，现有 5 条测试评价 order_id 均为 NULL 不受影响）；预留电话列通过 ALTER 补入已建表，脚本含注释语句供新库参考
- 用户端「服务分类 → 商家列表页」功能（首页点分类进对应商家）：后端 `ServiceCategoryController` 新增公开接口 `GET /service-category/providers?categoryId=`（无需登录，路径天然避开 JWT 拦截，WebMvcConfig 未改），`IServiceCategoryService`/Impl 新增 `listCategoryProviders`——校验分类存在、查 status=1 商家（按入驻时间倒序）、该分类上架(1)服务项目一次查出按商家分组（避免 N+1）、组装 `ProviderWithItemsVO`（商家字段 + items）；前端新增 `src/views/ServiceProviders.vue` 商家列表页（页头返回/标题/共N家商家、商家卡片：logo 首字头像/名称/分类标签/服务区域/已上架数/简介/内嵌服务项目行（名称/¥价格/评分/销量/时长）、el-empty 空态与参数错误兜底、v-loading），路由 `/user/merchants` 挂 /user children（复用 UserLayout），`api/provider.js` 新增 `listProvidersByCategory`；`UserHome.vue` 分类宫格点击由占位提示改为带参跳转（loadCategories 映射补齐 `id: categoryId`，内置兜底分类补猜测 id 1~5）
- 商家列表页评分/评价数：后端新建 `ServiceComment` 实体/Mapper/`IServiceCommentService`/Impl（表 `service_comment` 已于 2026-08-31 建好，字段 comment_id/item_id/provider_id/order_id/user_id/score(1~5)/content，无 status 字段）；`listCategoryProviders` 聚合该分类下商家全部评价——`score` = 该商家所有评价均分（BigDecimal 1 位小数，无评价为 null）、`reviewCount` = 评价条数，`ProviderWithItemsVO` 新增 `score`/`reviewCount` 字段；前端 `ServiceProviders.vue` 卡片元信息行新增橙色「★ 4.7 · 3 条评价」（无评价显示「暂无评价」）
- 数据库 `service_comment` 表已存在无需建表（2026-08-31 三表已建），插入 5 条测试评价数据（商家1=4.7分/3条、商家2=4.5分/2条）供列表页展示验证

## 2026-09-01
- 商家端服务项目接通后端：前端新增 `api/item.js`（listItems/addItem/updateItem/deleteItem/getItem/toggleItemStatus）；`MerchantHome.vue` 服务项目由内存态改为真实接口——列表 v-loading + 空态 el-empty、新增/编辑/删除成功后刷新、上下架开关改单向 `:model-value` 绑定（接口成功才改行数据，失败不闪变）+ 行 loading、保存按钮防重复提交；统计卡（总数/上架/下架/平均分）随真实列表联动（评分为 null 不计入平均分）；分类下拉由内置静态改为接 `GET /service-category/list`（失败回退内置 5 分类）；移除 mockItems 与「本地演示」提示文案
- 商家服务项目后端逻辑补全（`service_item`）：`ServiceItemController` 对齐方法名并新增 PUT /service-item/status/{id}?status= 上下架、GET /service-item/list?status= 可选筛选；`IServiceItemService`/Impl 补全增删改查——归属强制取 token 商家 providerId（不信任请求体）、列表按 provider_id 过滤 + sort 升序 + create_time 降序、新增默认上架/销量0/评分空（评分销量为聚合字段，新增置空、修改置 null 跳过防篡改）、删改查均校验归属（「无权操作」）、参数校验（名称/分类/价格非负/单位必填）；店铺状态闸门（checkProviderAvailable）本已加上又按用户决定移除——登录时 providerLogin 已校验商家状态（0/2 登录不了），待审核/停用商家拿不到 token，此检查在登录后管理员中途停用店铺的窗口期有兜底价值，用户表示后续再考虑；`WebMvcConfig` 拦截路径追加 `/service-item/**`（商家服务接口从此需登录）；前端 MerchantHome 服务项目仍为内存态，待接入这些接口
- 用户管理（注册家属）接通后端：后端新增 `SysUserController`（GET /sys-user/list 家属列表 status 可选筛选、PUT /sys-user/status/{id}?status= 启用/禁用）+ `ISysUserService`/Impl 实现（列表返回前逐条 `setPassword(null)` 剔除密码、状态仅 0/1 切换），`WebMvcConfig` 拦截路径追加 `/sys-user/**`；前端 `api/user.js` 追加 `listSysUsers`/`updateUserStatus`，`AdminHome.vue` 用户管理模块接真实接口（v-loading、确认框、成功后刷新；移除静态「已添加老人」列），统计卡「注册家属」「入驻商家」改为实时计数（服务项目/累计评价仍静态）
- 商家审核/管理接通后端：后端 `ServiceProviderController` 新增 GET /service-provider/list（status 可选筛选）、PUT /service-provider/review/{id}?pass=（通过1/驳回2）、PUT /service-provider/status/{id}?status=（启停1/2）；`IServiceProviderService`/Impl 实现；审核仅限 status=0、启停仅限 1↔2（待审核必须走审核）；`WebMvcConfig` 拦截路径追加 `/service-provider/**`（商家接口从此需登录）；角色权限校验暂未做（checkAdmin 已撤销，用户后续统一处理）
- 前端新增 `api/provider.js`（listProviders/reviewProvider/toggleProviderStatus）；`AdminHome.vue` 商家审核/商家管理/看板待办三处改为真实接口（v-loading 加载、分类名经 listCategory 映射、时间戳兜底格式化、审核/启停带确认框成功后刷新列表；统计卡除待审核数实时联动外仍为静态）
- 管理后台首页 `AdminHome.vue` 整体页面（纯前端静态版，未接后端）：左侧深色菜单 + 顶栏（搜索/消息角标/管理员名退出登录）+ 内容区；模块头脑风暴定为 9 个：数据看板（5 统计卡 + 待办事项 + 平台概况）、商家审核（待审核列表 通过/驳回，角标联动）、商家管理（启用/停用）、用户管理（家属 禁用/启用）、服务管理（全平台项目 上架/下架）、分类管理（五个分类 增删改占位）、评价管理（删除）、订单管理（建设中占位，订单表未建）、系统设置（建设中占位）；菜单点击切换内容区，全部内存态假数据，TODO 标注待接后端
- 管理员登录接口：后端 `IAuthService`/`IAuthServiceImpl` 新增 `adminLogin`（校验 role=2、账号 username 非空、查 admin 表、密码比对、status=0 提示禁用，token 携带管理员 id/role/username），`AuthController` 新增 `POST /auth/admin/login`；前端 `api/user.js` 新增 `adminLoginService`，`Login.vue` 角色 Tab 恢复三入口「家属/商家/管理员」，管理员 Tab 为 账号+密码 登录（无注册），成功后存 token/user_info 跳 `/admin/home`；商家 Tab 文案「商家入驻」改「商家登录」（入驻入口保留在登录表单下方）
- 修复管理员登录报「管理员账号不能为空」：前端 JSON 字段是 `username` 而后端 `LoginRequest` 字段是 `userName`，Jackson 匹配不上导致后端拿到 null；`LoginRequest.userName` 加 `@JsonAlias("username")` 别名（原 `userName` 不受影响，家属注册照常）
- 密码加密方案整体撤销（用户决定，保持明文简单）：移除前端 js-md5 依赖与加密调用、后端 MD5 比对恢复明文 equals；改为**后端登录成功返回前统一置空密码字段**（familyLogin/providerLogin/adminLogin 的 `user` 对象均 `setPassword(null)`），密码不随响应体泄露，前端无需再剔除（原有剔除逻辑保留无害）
- 商家工作台首页 `MerchantHome.vue` 整体页面（纯前端版，未接后端）：顶部栏（品牌 + 商家名下拉退出登录）+ 待审核提示（status=0 显示 el-alert）+ 店铺信息卡（logo 首字/名称/主营分类/状态徽章/电话脱敏/入驻时间/地址，数据优先 localStorage user_info、缺失兜底假数据）+ 4 个统计卡（服务项目/已上架/已下架/平均评分，由内存列表实时计算）+ 服务项目管理表格（服务名称/分类/价格+单位/时长/销量/评分/上架下架开关/编辑/删除）+ 新增/编辑复用弹窗（名称/分类/价格/单位/时长/详情）；所有增删改上下架为前端内存态，TODO 标注待接 /service-item/* 接口；字段对齐 service_item 表
- 商家登录/注册接口（方案一：家属/商家各调各的接口，不做统一分流）：后端新增 `ProviderRegisterRequest` DTO（phone/password/providerName/categoryId），`IAuthService`/`IAuthServiceImpl` 新增 `providerLogin`（校验 role=3、查 service_provider、密码、状态机 0待审核/2停用 分别提示）、`providerRegister`（校验格式/商家名称/主营分类存在/手机号唯一，强制 role=3 + status=0 待审核），`AuthController` 新增 `POST /auth/provider/login`、`POST /auth/provider/register`；登录/注册成功均返回 `{ user, token }`，登录返回的商家信息与家属共用 `user_info` 存储
- 前端登录页 `Login.vue`：角色 Tab 由「家属/管理员」改为「家属/商家」（管理员登录入口暂移除，待后端 admin 接口就绪再补）；商家 Tab 含登录 + 入驻注册（手机号/商家名称/主营服务分类下拉（数据来自 GET /service-category/list，接口失败不阻塞）/密码/确认密码），登录调用 `/auth/provider/login`（role='3'）、入驻调用 `/auth/provider/register`；商家记住手机号用独立 key `remember_phone_provider`；登录成功跳 `/provider/home`
- `api/user.js` 新增 `providerLoginService`、`providerRegisterService`
- 新增商家端占位首页 `src/views/MerchantHome.vue`（商家工作台建设中），路由新增 `/provider/home`

## 2026-08-28
- 桌面生成项目设计文档（颐养平台-居家养老服务预约-项目设计文档.md）
- 登录页 `src/views/Login.vue`：左右分栏 + 温暖橙主题；家属登录/注册 + 管理员登录（Tab 切换）；校验齐全；未接 API（提交处留 TODO）
- 补全工程基础：`src/assets/main.scss`（全局样式 + Element Plus 主色覆盖）、`src/router/index.js`（路由：/login、/user/home、/admin/home）、占位首页 UserHome/AdminHome、index.html 标题
- 装依赖：element-plus、vue-router、@element-plus/icons-vue、sass
- 设计文档：`docs/superpowers/specs/2026-08-28-login-page-design.md`

## 2026-08-29
- 家属登录接入后端：`POST /auth/family/login`（loginService），按 Result{success, errorMsg, data} 处理；成功后存用户信息（剔除密码）并跳转
- 密码校验规则对齐后端 ValidationUtil（至少 6 位且含字母和数字）
- 工程修复：vite.config.js 配置 `@` → src 别名；安装 axios
- `Login.vue` 换用简化版登录/注册布局后重新接线：家属登录对接 `POST /auth/family/login`（前端预检与后端 ValidationUtil 一致，成功存 user_info 并跳转 /user/home）
- `Login.vue` 回滚为最初版本（品牌分栏 + 家属/管理员 Tab + 完整校验），后端对接暂时移除，提交处保留 TODO 假登录
- 家属登录/注册接入后端：`/auth/family/login`、`/auth/family/register`（按 Result{success, errorMsg, data} 处理；登录成功存 user_info 剔除密码）；密码校验对齐后端 PASSWORD_REGEX
- 工程修复：vite 增加 `/auth` 开发代理（后端无 CORS 配置）；request.js baseURL 改空（后端无 context-path）
- 前端移除 vite `/auth` 代理，baseURL 直连 `http://localhost:8080`；跨域改由后端 CORS 配置处理
- 家属端首页（方案A 电商式）：新增 `src/layout/UserLayout.vue` 公共布局（顶部导航/搜索/消息/用户下拉退出登录 + Footer），路由改为 `/user` 嵌套布局
- 首页 `src/views/UserHome.vue`：轮播 Banner + 服务分类宫格 + 热门服务卡片 + 推荐护工卡片 + 服务流程条；数据均为静态假数据（TODO 标注后端接口），子页面跳转统一占位提示
- 方案A 首页备份至 `src/views/backup/UserHome-variant-A.vue`；`UserHome.vue` 切换为方案C（问候条+订单状态徽章 替换轮播 Banner，其余区块不变）
- 方案C 备份至 `src/views/backup/UserHome-variant-C.vue`；`UserHome.vue` 恢复为方案A（轮播 Banner 版）
- 新增家属个人中心 `src/views/UserProfile.vue`（美团/饿了么风格）：顶部个人信息卡（昵称/手机号脱敏/编辑资料）+ 我的订单状态条（带角标）+ 工具宫格（老人管理/地址管理/我的评价/联系客服）+ 账号与设置列表 + 退出登录；数据静态假数据（TODO 标注）
- 路由新增 `/user/profile`；`UserLayout.vue` 导航项及头像下拉「个人中心」接通真实跳转
- `UserLayout.vue` 顶部导航「我的订单」项保留（曾尝试移除后又按用户要求恢复）；个人中心「我的订单」卡片作为订单状态入口（各状态角标 + 查看全部订单）
- `UserLayout.vue` 顶部导航移除「个人中心」项；个人中心入口保留在右上角头像下拉菜单
- 新增地址管理 `src/views/UserAddress.vue`（京东购物风格）：多地址卡片列表 + 默认标签 + 设为默认/编辑/删除 + 吸底「新增收货地址」按钮 + 新增/编辑弹窗（收货人/手机号/省市区级联/详细地址/设为默认开关）；数据为前端内存态（TODO 标注后端 user_address 接口）
- 路由新增 `/user/address`；个人中心「地址管理」格子接通真实跳转（其余格子仍占位提示）
- 新增老人管理 `src/views/UserElder.vue`：字段对齐数据库 elder_profile 表（elder_id/user_id/elder_name/gender/birth_date/id_card唯一/phone/emergency_phone必填/health_note/addr_id 关联地址）；卡片列表（性别色头像/年龄/身份证脱敏/紧急联系人/居住地址/健康备注）+ 添加编辑弹窗（身份证18位与手机号校验）+ 删除确认 + 吸底添加按钮；数据前端内存态（TODO 标注后端接口）
- 路由新增 `/user/elder`；个人中心「老人管理」格子接通真实跳转
- 地址管理对接后端增删改查：新增 `src/api/address.js`（GET /address/gets?userId=、POST /address/add、PUT /address/update、DELETE /address/delete/{addrId}）；`UserAddress.vue` 重写为真实接口（字段对齐表：detailAddr/isDefault 0|1/phone，去掉表不存在的收货人姓名；设为默认=先清其他默认再设当前；新增/编辑/删除成功后刷新列表；未登录跳登录页）
- 老人管理「居住地址」下拉改为从地址接口拉真实数据（与地址管理打通）
- 后端地址接口加固（UserAddressController/IUserAddressService/UserAddressServiceImpl）：列表按 userId 过滤（修越权）、update/delete/get 加归属校验、新增 PUT /address/set-default/{id} 事务设默认、add 返回实体（回填 addrId）、add 增加后端参数校验、清理无用 import
- 前端地址模块同步：api/address.js 删除接口带 userId、新增 setDefaultAddress；UserAddress.vue 设为默认改为单次调用后端事务接口（移除前端两次 update 补偿逻辑）
- 地址页省市区示例数据（广东/北京/上海）移除，换全国完整数据 element-china-area-data：级联选择器存行政区划编码，提交 codeToText 转名称入库，编辑时名称反查编码回显
- 角色字段：sys_user 表新增 role（tinyint 默认1=用户，已有数据自动补1）；admin 表原有 role（默认2=管理员）；SysUser/Admin 实体注释统一为「角色(1用户 2管理员)」

## 2026-08-31
- 登录/注册传递 role：前端 `Login.vue` 登录、注册请求体加 `role: '1'`（家属）；后端 `IAuthServiceImpl` 注册时显式 `setRole(1)`（不信任前端传值）、登录时校验请求 role 与库中角色一致（不一致返回「角色不匹配」），保证登录返回的用户对象（存入 user_info）带 role
- JWT 鉴权链路打通：后端 `IAuthServiceImpl` 登录/注册生成 token 并随用户信息返回（data 改为 `{ user, token }`）；`WebMvcConfig` 拦截路径修正为 `/address/**`、`/elder-profile/**`（原 `/api/**` 与实际接口对不上，拦截器此前不生效）；`UserAddressServiceImpl.addAddress` 归属强制取 token 解析出的 userId（防替他人加地址）、deleteAddress 增加地址不存在判断、familyLogin role 判断防空指针
- 前端 token 接入：`request.js` 请求拦截器自动携带 `Authorization: Bearer token`，401 清本地登录态回登录页；`Login.vue` 登录成功存 token；`UserLayout`/`UserProfile` 退出登录清 token；`api/address.js` 列表/删除/设默认去掉多余 userId 参数（归属由后端从 token 解析），`UserAddress.vue`/`UserElder.vue` 调用点同步
- 地址接口去掉请求体 userId：后端 `updateAddress` 改为先查库里地址的归属再与 token 用户比对（不信任请求体传 userId，归属字段强制以库里为准），前端 `UserAddress.vue` 新增/修改 payload 不再携带 userId（归属身份只来自 token）
- 老人档案增删改查（模仿地址模块）：后端 `ElderProfileController` 新增 GET /elder-profile/gets、POST /add、PUT /update、DELETE /delete/{elderId}；`ElderProfileServiceImpl` 归属校验同地址（add 强制 token userId、update/delete 按库里归属校验、list 按 userId 过滤），新增身份证 18 位校验与唯一冲突兜底；`ElderProfile.birthDate` 加 @JsonFormat(yyyy-MM-dd)，ValidationUtil 新增 ID_CARD_REGEX；前端新增 `src/api/elder.js`，`UserElder.vue` 由内存态改为真实接口（列表/新增/编辑/删除 + loading/saving 状态），字段改驼峰对齐实体，移除数据库不存在的 emergency_phone 字段
- 修复新增第二个无身份证老人报 500（Duplicate entry '' 撞唯一约束 uk_elder_id_card）：`ElderProfileServiceImpl` 增改时把空串身份证归一化为 null（库中 NULL 可重复，'' 不行）；编辑清空身份证用 LambdaUpdateWrapper 显式 set null（updateById 跳过 null 字段）
- 家属端首页新增 AI 助手小精灵（`UserHome.vue`）：右下角悬浮纯 CSS 小精灵（猫耳圆脸 + 眨眼/上下浮动/头顶星星动画 + 欢迎语气泡 4s 轮播文案 + 「AI 助手」标签），点击暂占位提示「建设中」，AI 对话功能待接入
- 小精灵支持拖拽：Pointer 事件（触屏/鼠标统一）按住拖动，位置限制在视口内，位移 < 5px 才算点击（拖动后补发的 click 会被拦截），触屏加 touch-action: none 防止拖动时页面滚动
- 商业模式调整：不再对接护工，改为各服务（助餐/助洁/助浴/助医/康复）直接对接对应服务商家；`UserHome.vue` 首页去掉「推荐护工」「服务流程」两个区块及相关样式/数据/响应式规则，保留：轮播 Banner、五个服务分类模块、推荐服务；Banner 第三条文案「护工实名认证」改「服务商家实名认证」
- `UserLayout.vue` 顶部导航调整为「首页 / 购物车」两项（移除「全部服务」「我的订单」，购物车页未建先占位提示），右侧搜索/消息/个人中心保持不变；搜索框 placeholder「搜索服务/护工」改「搜索服务/商家」
- `UserHome.vue`「推荐服务」改「热门推荐」，区块内加「服务 / 商家」胶囊 Tab 切换：服务 Tab 为原服务卡片，商家 Tab 新增商家卡片（logo 首字 + 名称 + 主营分类标签 + 评分/接单数/服务区域，静态假数据、字段对齐 service_provider 方案，TODO 待接 /provider/recommend）；商家卡片点击暂占位「商家详情」
- 数据库新建 `service_category` 服务分类字典表（health_data 库）：category_id PK / category_name / icon VARCHAR(255)（存图片 URL 或静态资源路径）/ sort / status / create_time / update_time，风格对齐现有表（utf8mb4_general_ci + 时间默认 CURRENT_TIMESTAMP）；已写入首页五个分类初始数据（助餐/助洁/助浴/助医/康复护理），icon 暂为 NULL 待填图片路径
- 五个分类图标就位：改用 Font Awesome 6.5.2 官方免费图标（jsdelivr CDN 下载，CC BY 4.0 可免费商用），按平台橙 #ff7a45 着色后存入后端 `src/main/resources/static/upload/`：meal=bowl-food(碗) / clean=broom(扫帚) / bath=bath(浴缸) / medical=stethoscope(听诊器) / rehab=hand-holding-medical(托举医疗)；`service_category.icon` 保持 `/upload/xxx.svg` 路径不变，启动后端后可直接 `http://localhost:8080/upload/xxx.svg` 访问（注：此前手绘 SVG 已被删，upload 目录重建）
- 分类图再次更换为真实照片（Pexels 免费授权，无需署名可商用；images.pexels.com CDN 国内可直连，网页 403 不受影响）：meal=烤肉时蔬餐(摄影师 Mohamed Olwy) / clean=客厅吸尘(Vitaly Gariev) / bath=泡泡浴(Polina Tankilevitch) / medical=医生听诊器(Daniil Kondrashin) / rehab=理疗按摩(Yan Krukau)，800px 宽 JPEG（37~160KB）；`service_category.icon` 更新为 `/upload/xxx.jpg`，旧 svg 已删除
- 图片方案整体放弃（用户决定）：`service_category.icon` 全部清回 NULL，前端首页分类保持原有 Element Plus 内置图标；upload 目录里的 5 张 jpg 暂留未删
- service_category 字典表接入前后端：后端新增 `ServiceCategory` 实体 / Mapper / `IServiceCategoryService`（listEnabledCategories 只查 status=1 按 sort 升序）/ `ServiceCategoryController` GET /service-category/list（公开接口，未加进 JWT 拦截路径，WebMvcConfig 无需改动）；前端新增 `src/api/category.js`，`UserHome.vue` 首页五个分类改为 onMounted 拉接口，按分类名匹配内置 Element Plus 图标兜底（icon 字段为 null 不阻塞），接口失败自动回退内置静态数据
- 商家模块三张表建好（health_data 库，风格对齐现有表）：
  - `service_provider` 商家表：provider_id / category_id(FK→service_category) / provider_name / logo / intro / phone(唯一,登录) / password / **role(tinyint 默认3，风格对齐 sys_user)** / legal_person / province / city / district / longitude decimal(10,7) / latitude / address / status(0待审核 1正常 2停用) / create_time / update_time；**无营业执照字段**、**无 score/order_count（以后从评价/订单聚合）**
  - `service_item` 服务项目表：item_id / provider_id(FK,CASCADE) / category_id(FK) / item_name / cover / price / unit / duration / detail / score(冗余平均分) / status(0下架1上架) / sales / sort / create_time / update_time
  - `service_comment` 评价表：comment_id / item_id(FK,CASCADE) / provider_id(冗余,FK) / order_id(可空,订单表未建故无外键) / user_id(FK→sys_user) / score(1~5) / content / create_time / update_time
  - 距离方案 A：省市区编码字段与 user_address 结构一致，longitude/latitude 支持精确距离排序（Haversine）
