# 管理员订单管理(纯只读监督)设计文档(2026-09-04)

## 背景

颐养平台订单主链路(家属下单→商家接单/完成→评价)与购物车已落地。AdminHome 左侧菜单「订单管理」为 building 占位(`订单表尚未设计` 提示语),订单数据仅有家属端/商家端按 token 归属的接口,**平台侧无法查看全平台订单**,投诉/协调无依据。本轮落地管理员订单只读监督 + 数据看板联动。

## 需求

- 管理员查看**全平台订单**:状态筛选、富字段表格、详情抽屉(含老人健康备注)
- 越权守卫:该接口仅管理员 token(role="2")可调,家属/商家 token 拒绝(否则拉走全平台订单=数据泄露)
- 数据看板「待办事项」新增**待接单新订单**卡,联动跳转订单管理并选中待接单筛选

## 已定决策(用户拍板)

| 决策点 | 方案 |
| --- | --- |
| 功能边界 | **纯只读监督**:订单流转仍由家属/商家各自操作,平台侧仅查看协调,无任何写操作接口 |
| 鉴权 | 接口内 role 守卫(最小必需防护);全局 checkAdmin 统一角色校验仍留候选专项,不在本轮扩散 |
| 看板联动 | 完整闭环:看板待办区「待接单新订单 N」卡 + 查看全部跳订单管理自动选待接单 |
| 抽屉深度 | 与商家端同深度,**含健康备注**+ 老人电话(管理员协调投诉需见老人健康与联系信息) |
| 数据获取 | 一次全量拉取,状态角标计数与关键词过滤均在**前端**做(无分页惯例;避免双接口双请求) |

## 后端设计(改动均在 Elderly_care_Platfrom)

### ① 接口

`GET /service-order/admin/list`(ServiceOrderController 新增):

1. **越权守卫**:`UserContext.get().role()` 非 `"2"` → `Result.fail("无权限")`(防家属/商家越权拉全平台单)
2. 全量订单 `create_time` 倒序(不走 status 参数,前端一次拿全量做角标/筛选)
3. 关联字段 **provider/item/elder/address/sys_user(下单家属)** 一次批量查出防 N+1(同 listMerchantOrders 手法,新增 sys_user 批量联查取 user_name)
4. 拼装新 VO `dao/AdminOrderVO`(见下),已删实体 null 透出由前端兜底文案(商家「商家已注销」/老人「档案已删」/家属「账号已删除」)

### ② VO `dao/AdminOrderVO`

订单基础:orderId / orderNo / orderStatus / quantity / unitPrice / totalPrice / serviceTime / createTime / contactPhone / remark
关联展示:providerId / providerName / itemId / itemName / **familyName(下单家属昵称,联 sys_user.user_name)** / elderId / elderName / gender / birthDate / elderPhone / **healthNote(健康备注,仅该接口供管理员监督协调)** / addressText(省市区+详细,逐段去空拼接)

### ③ 改动文件

- `dao/AdminOrderVO.java`(新)
- `service/IServiceOrderService.java` + `service/impl/ServiceOrderServiceImpl.java`:新增 `listAdminOrders()`(注入 SysUserMapper)
- `controller/ServiceOrderController.java`:新增 `GET /admin/list`
- `WebMvcConfig` 无需改(`/service-order/**` 已拦截,需登录)

## 前端设计(改动均在 health_care_begin)

### ① `src/api/order.js` 追加

`listAdminOrders()` → GET /service-order/admin/list

### ② `AdminHome.vue` 订单管理 Tab(菜单去 building)

- `menus` 中 order 项删 `building: true`;模板 comment section 后、建设中兜底 section 前插 `v-else-if="activeMenu === 'order'"` 区块
- 首行工具条:状态筛选胶囊(全部/待接单/服务中/已完成/已取消,**角标 = 全量中对应状态计数**,复用现有 badge 视觉)+ 关键词 el-input(placeholder「搜索商家/服务/老人/家属/订单号」,模糊匹配以下字段之一:providerName/itemName/elderName/familyName/orderNo,与状态筛选 AND)+「刷新」按钮
- 富字段 el-table(数据 = 过滤后行):订单号(等宽)/服务项目/商家/下单家属/服务老人(名+性别+年龄)/金额(单价×数量 + 总价)/状态 badge(0待接单橙 1服务中蓝 2已完成绿 3已取消灰)/下单时间/「详情」操作列
- 点详情开 el-drawer 对齐商家端详情抽屉三段:
  - 服务信息:订单号/状态 tag/下单家属/服务项目/单价×数量/金额/下单时间
  - 服务老人卡:姓名性别年龄/老人电话 + 高亮「健康备注 / 护理注意事项」信息块(档案已删显示「档案已删,无法查看」)
  - 预约安排:预约时间/服务地址全文/联系电话/备注
- 数据只读,全 Tab 共用 `orders`(全量)与 `orderLoading`;空表格 el-empty

### ③ 数据看板联动

- 看板「待办事项」区新增第二个 todo-card「待接单新订单」:N 角标 = orders 中 status 0 数;行 = 待接单订单(服务名+商家+¥金额+下单时间),「查看全部」点击 → `activeMenu='order'` 且 `orderFilter=0`;无待接单显示「🎉 没有待接单的新订单」;与 review 卡并排(v-loading=orderLoading)
- `onMounted` 与 dashboard 默认视图共享 `loadOrders()` 一次全量;切菜单不重拉,订单页「刷新」重新拉取同步看板

### ④ 改动文件

`src/api/order.js`、`src/views/AdminHome.vue`、`src/router/index.js` 不改(菜单即内容区)

## 错误处理与兜底

- 越权/未登录:未登录由拦截器 401 统一;登录但非管理员 → Result.fail「无权限」前端 ElMessage 展示(家属/商家拿不到 admin 数据)
- 商家/老人/家属档案被删:VO 对应关联字段 null → 表格/抽屉落「已注销/档案已删/账号已删除」兜底文案
- 时间兜底沿用 `fmtDate`;价格空兜 0;状态码 0~3 外值统一按「未知」灰 badge(理论上不发生)

## 验证方案

1. 后端 `./mvnw -q compile`
2. API 冒烟(起后端):admin 登录拉全量列表 → 与 DB `SELECT` 对账(order_no/数量/联查字段)→ **家属 token 打 `/service-order/admin/list` 被拒「无权限」**;商家 token 同拒
3. 前端 `npx vite build`
4. 前端冒烟:看板待接单卡计数 = 库中 0 态单数 → 查看全部 → 订单管理自动选待接单 → 状态胶囊计数与表内行一致 → 关键词过滤(商家/订单号)→ 详情抽屉健康备注正确 → 只读无写按钮

## 不做清单(范围外)

- 管理员写操作(强制取消/改状态/删单)——留待仲裁需求明确后单独立项
- 全局角色权限统一校验(checkAdmin 专项)、统计卡/平台概况静态数据替换(统计专项)
- 服务管理/分类管理/评价管理静态占位换真(各自模块立项)

## 改动纪律

代码/库改动同步记入 health_care_begin/CHANGELOG.md(2026-09-04 新节置顶,列涉及文件与原因);PROJECT_PROGRESS.md 完成地图管理员端「订单管理」勾选 ✅、待办联动说明、候选列表重排。
