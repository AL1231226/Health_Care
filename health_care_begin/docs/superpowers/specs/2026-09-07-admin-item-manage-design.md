# 管理员端「服务管理(全平台项目监督)」接真设计文档(2026-09-07)

## 背景

AdminHome「服务管理」节目前是**内存态假数据**:`items` 数组 4 条写死服务([AdminHome.vue:181-191](/d/养老实践项目/health_care_begin/src/views/AdminHome.vue#L181-L191))、`toggleItem` 只翻本地状态不发请求;模板节在 [AdminHome.vue:544-570](/d/养老实践项目/health_care_begin/src/views/AdminHome.vue#L544-L570)。后端 `service_item` 侧**没有任何管理员/全平台视角入口**:`ServiceItemServiceImpl.listItems` 恒强制 `provider_id = 当前 token 商家`([ServiceItemServiceImpl.java:51-60](/d/养老实践项目/Elderly_care_Platfrom/src/main/java/com/example/Elderly_care_Platfrom/service/impl/ServiceItemServiceImpl.java#L51-L60)),全后端无带 providerName/categoryName 联查的项目列表方法。

评分/销量取数背景(重要):`service_item.score/sales` 原为**死列**(恒 5.0/0,从未写入),2026-09-07 已「**接活**」(docs 提交 6ccb3e8 + 工作树 `ServiceCommentServiceImpl`/`ServiceOrderServiceImpl` 改动 + `src/main/resources/sql/service_item_score_sales.sql` 存量回填):score = 按 item_id 的 service_comment 均值(1 位小数 HALF_UP,无评价 NULL)、sales = order_status=2 已完成订单数,分别由写路径(发表评价/完成服务)同事务回填。**本模块直接读这两个接活列**,与写路径同源,不另做聚合。

## 决策过程记录

1. 用户拍板本轮范围:**仅「服务管理」一个模块**(其余分类/评价/系统设置留待后续)。
2. 定位:与订单管理「只读监督」一致的**监督定位**——全平台服务项目列表 + 上下架(违规可下架);服务的新增/编辑/删除由商家端自管(已有完整 CRUD),管理员不做代管增删改,避免职责重叠。
3. 列表列:mock 同款全列(名称/所属商家/分类/价格×单位/评分/销量/状态),另加创建时间;评分销量按「A:读接活列」方案(用户选定),非实时聚合、非省略。
4. 筛选:状态胶囊(全部/上架/下架,带计数)+ 关键词搜索(服务名/商家名),对齐订单管理节交互风格(该节 2026-09-04 落地样式可直接复用 CSS)。

## 已定决策

| 决策点 | 方案 |
| --- | --- |
| 接口落点 | `ServiceItemController` 下新增 2 端点,**方法级 `@RequireRole(RoleType.ADMIN)` 覆盖类级 PROVIDER**(仿 `ServiceProviderController /self`、`ServiceCommentController /provider/score` 先例:方法注解优先);WebMvcConfig 无需改(`/service-item/**` 已在 PROTECTED_PATTERNS) |
| 列表接口 | `GET /service-item/admin/list?status=`(status 可选 0下架/1上架,不传全量),全量返回、create_time 倒序;provider/category 名**防 N+1 批量联查**(仿 `ServiceOrderServiceImpl.listAdminOrders` L368-429 的批量 in 拼 Map 写法);score/sales 直读列 |
| 上下架接口 | `PUT /service-item/admin/status/{itemId}?status=0/1` — 仅存在性 + status∈{0,1} 校验,**无归属校验**(监督任意商家),复用商家端 toggle 的更新写法去掉归属段 |
| 下架语义 | 复用现有读时校验,无需新联动:商家/家属端加购与下单 `status=1` 校验拦截(ServiceCartServiceImpl L63-64、ServiceOrderServiceImpl L85-92),购物车 checkout 现查失效整批拒绝点名(L185-199);**进行中(0/1 状态)订单不受影响**,完成服务照旧——监督动作不伤存量履约 |
| 前后端职责 | 列表接口可选 status 参数(为将来留),但 AdminHome **每次拉全量本地聚合计数 + 状态/关键词过滤**(与订单节同模式,胶囊计数需全量) |
| VO | 新 `dao/AdminItemVO`(仿 dao 包 AdminOrderVO 先例):itemId/itemName/providerId/providerName/categoryId/categoryName/price/unit/score/sales/status/createTime(与展示列严格对应) |
| 列口径 | score 可能 null → 前端展示「—」,非 null 显示 1 位小数;sales 直读整数 |
| 前端形态 | AdminHome 服务管理节原位替换 mock(script 181-191 + template 544-570),不新开路由页;onMounted 加载 + 卡片头「刷新」按钮;复用既有胶囊/搜索 CSS(filter-group/filter-capsule/order-search 等) |
| 越权兜底 | 前端无路由守卫(已知候选不扩范围),管理员专属页面由后端 `@RequireRole(ADMIN)` 强校验兜底(商家/家属 token 打 /admin/list → 「无权限」) |

## 后端设计(Elderly_care_Platfrom)

### ① `IServiceItemService` + `Impl` 新增 2 方法

- `Result listAdminItems(Integer status)`:
  - `list(new QueryWrapper<ServiceItem>().eq(status != null, "status", status).orderByDesc("create_time"))` 全量
  - 收集去重 providerId 集、categoryId 集 → 各自一次 `in` 批量查 `service_provider`/`service_category` 拼 Map(防 N+1)
  - 组装 `AdminItemVO` 列表返回;score/sales 直读列不加工
- `Result toggleItemStatusByAdmin(Long itemId, Integer status)`:
  - status 仅 0/1、itemId 存在性校验(不存在的服务与商家端同文案)
  - LambdaUpdateWrapper 仅 set status(**不带归属条件**)

### ② `controller/ServiceItemController` 新增 2 端点

```java
/** 管理员端:全平台服务项目列表(status 可选 0下架/1上架,含商家名/分类名;方法级 ADMIN 覆盖类级 PROVIDER) */
@RequireRole(RoleType.ADMIN)
@GetMapping("/admin/list")
public Result adminList(@RequestParam(required = false) Integer status) {
    return serviceItemService.listAdminItems(status);
}

/** 管理员端:任意服务上下架监督(status=0/1;方法级 ADMIN 覆盖类级 PROVIDER) */
@RequireRole(RoleType.ADMIN)
@PutMapping("/admin/status/{itemId}")
public Result adminToggleStatus(@PathVariable Long itemId, @RequestParam Integer status) {
    return serviceItemService.toggleItemStatusByAdmin(itemId, status);
}
```

改动文件:`IServiceItemService.java`、`ServiceImpl`、`dao/AdminItemVO.java`(新建)、`ServiceItemController.java`

## 前端设计(health_care_begin)

### `src/api/item.js` 追加 2 个函数

```js
// 管理员端:全平台服务项目列表:GET /service-item/admin/list,status 可选(0下架/1上架)
export const listAdminItems = (status) => {
    return request.get('/service-item/admin/list', { params: status != null ? { status } : {} })
}
// 管理员端:任意服务上下架:PUT /service-item/admin/status/{id}?status=0/1
export const toggleAdminItemStatus = (id, status) =>
    request.put(`/service-item/admin/status/${id}`, null, { params: { status } })
```

### `AdminHome.vue`

- **script**:删除 `items` 数组与 `toggleItem`(181-191);新增:
  - `adminItems`/`itemLoading`/`itemFilter`(哨兵 -1 全部)/`itemKeyword`;`itemCounts` computed(0/1 计数);`filteredItems` computed = 状态 AND 关键词(匹配 `itemName`/`providerName`,仿订单节 filteredOrders 写法)
  - `loadAdminItems()`(失败 ElMessage 提示),加入 `onMounted` 与现有各 load 并行;卡片头刷新按钮复用
  - `handleAdminToggle(row)`:ElMessageBox.confirm(下架文案注明「下架后不可再加购/下单,购物车中该服务将失效」;上架则普通确认)→ 成功调 `toggleAdminItemStatus` → 刷新列表 + ElMessage.success;取消空 catch
- **template**(替换 544-570 节):
  - 卡片头:「全平台服务项目(N)」+ 刷新按钮(对齐订单节)
  - 筛选条:复用 `.order-bar/.filter-group/.filter-capsule/.filter-count` 胶囊(全部/上架/下架带计数)+ `.order-search` 关键词输入(placeholder「搜索服务名称 / 商家名称」)
  - 表格列:服务名称(show-overflow-tooltip)/ 所属商家(show-overflow-tooltip,`providerName || '商家已注销'`)/ 分类(`cat-chip`,`categoryName || '未分类'`)/ 价格(`¥price/unit`)/ 评分(`score != null ? Number(score).toFixed(1) : '—'`)/ 销量(`sales`)/ 状态(`status-badge` ok=上架/off=下架)/ 创建时间(fmtDate)/ 操作(状态 1 → 下架 warning 按钮;0 → 上架 success 按钮,plain)
  - 空态 `<el-empty description="暂无相关服务">`
- import 不变(Search 等图标已有);若 `Number(score).toFixed(1)` 出现 5.00 之类后端 BigDecimal 序列化差异,统一 `Number()` 转数值再格式化

## 验证

1. **前置**:score/sales 接活改动(工作树 `ServiceCommentServiceImpl`/`ServiceOrderServiceImpl` + `service_item_score_sales.sql`)先行 `./mvnw -q compile` 验证通过、sql 已执行,本模块读列才有意义
2. 后端 `./mvnw -q compile`;前端 `npx vite build`
3. curl 冒烟(管理员 token):
   - `GET /service-item/admin/list` 全量 → 与 `SELECT * FROM service_item` 对账行数、itemName/providerName/categoryName 非空、score null 与有值并存展示
   - `?status=0` / `?status=1` 过滤正确
   - `PUT /admin/status/{itemId}?status=0` → 再查列表该行 status=0;商家/家属端加购该服务被拒(回归 ServiceCart add)
   - **越权**:商家 token 打 `/admin/list` → 「无权限」(RoleInterceptor 方法级先判定);商家本人 `/service-item/list` 回归正常(类级 PROVIDER 未破)
4. 前端手测:管理员登录 → 服务管理:列表真实、胶囊计数对账、关键词筛选、下架确认弹窗后状态翻转且商家端该服务显示下架

## 文档纪律

落地后同步:本项目 `CHANGELOG.md` 新节置顶(描述改动/涉及文件/原因);`PROJECT_PROGRESS.md` 完成地图管理员端「服务管理」勾 ✅(原 ⏳ 服务管理);本设计文档随代码同批次或先行 git 提交(docs: 前缀)。
