# 角色权限统一校验设计文档(2026-09-07)

## 背景

后端鉴权目前只有一层:**JWT 登录校验**。AuthInterceptor 仅验 token 有效并解析出 userId/role/name,WebMvcConfig 声明 8 组业务前缀需登录,但**角色归属全靠各接口手写散落判断**,且覆盖面参差:

- 全后端业务层**仅 1 处** role 守卫(`ServiceOrderServiceImpl.listAdminOrders`,role=2)
- 管理员专属接口 **`/sys-user/**`(家属列表/启停)、`/service-provider/**`(商家列表/审核/启停)完全裸奔**——只要求登录,家属/商家 token 即可调:审核通过商家、禁用家属
- 商家专属 `/service-item/**`、家属专属 `/service-cart/**`、`/service-comment/**`、`/address/**`、`/elder-profile/**` 无角色闸门,仅靠归属/范围过滤兜底(如商家 token 打 `/service-cart/add`,userId 落在 provider_id 上仍可插入脏行)
- 角色编码定稿:`"1"`=家属(sys_user.id)/ `"2"`=管理员(admin.id)/ `"3"`=商家(service_provider.provider_id),登录侧门禁(IAuthServiceImpl 的 role 校验)已有且语义不同(校验登录请求体),不在本轮范围

**目标**:把「接口 → 所需角色」收拢成**统一机制**(一处定义、一处执行),堵住裸奔口子;今后新增接口标注即受保护,不再散落手写。

## 需求

- 一套统一角色校验:角色不符一律「无权限」拒绝,家属/商家/管理员三套接口互不可越
- 堵住现有裸奔:`/sys-user/**`、`/service-provider/**` 收紧为仅管理员
- 前端零代码改动(合法角色的既有行为零变化)

## 已定决策(用户拍板)

| 决策点 | 方案 |
| --- | --- |
| 生效机制 | **注解 `@RequireRole`(类级/方法级)+ 独立 `RoleInterceptor`**(第二个拦截器,挂在 AuthInterceptor 之后,先登录后角色) |
| 为何不用「路径→角色配置表」 | URL 除 `/service-order/{user,merchant,admin}` 子路径外无角色规律;配置表随接口增补易漏,漏一条即静默裸奔;注解贴 `@GetMapping` 写、加接口天然可见;单角色 Controller 类级标一次,新增方法自动继承 |
| 角色常量 | 新增 `RoleType`(USER="1"/ADMIN="2"/PROVIDER="3"),新代码用常量;历史魔法串(登录门禁等)不在本轮扩散 |
| 越权响应形态 | **HTTP 恒 200 + `{"success":false,"errorMsg":"无权限"}`**(对齐 CLAUDE.md 全站 HTTP 恒 200 惯例;success=false 直走前端既有错误处理,request.js/各页零改动;401「未登录」语义仍归 AuthInterceptor) |
| 散落守卫清理 | `listAdminOrders` 内联 role 守卫**删除**(职责上移拦截器,防双份漂移);其余归属/范围校验(商家看本店、家属看本人等)**全部保留**——与角色正交,属数据隔离 |
| 范围边界 | 纯后端专项;前端路由守卫(router 无 beforeEach)**明确不做**,留候选 |
| 注册方式 | 与 authInterceptor 同一份 8 前缀 addPathPatterns;`/auth/**`、`/service-category/**` 公开端点天然不检查 |

## 后端设计(改动均在 Elderly_care_Platfrom)

### ① 新注解 `annotation/RequireRole.java`

`@Target({TYPE, METHOD})` + `@Retention(RUNTIME)`,成员 `String[] value()`(支持多角色,当前无共用端点,预留)。

### ② 新常量 `utils/RoleType.java`

`USER="1"` / `ADMIN="2"` / `PROVIDER="3"`,javadoc 注明角色模型与对应 id 语义(provider 用 provider_id)。

### ③ 新拦截器 `config/RoleInterceptor.java`

preHandle:
1. 非 `HandlerMethod`(静态资源/预检等)→ 放行
2. 取 `@RequireRole`:方法注解优先,无则回退 Controller 类注解;两者皆无 → 放行
3. 命中 → `UserContext.get().role()` 须 ∈ 注解 value;否则 response 写 `{"success":false,"errorMsg":"无权限"}`(UTF-8 JSON)并返回 false(写法对齐 AuthInterceptor 的 response writer)

### ④ `config/WebMvcConfig.java`

`registry.addInterceptor(roleInterceptor)` 追加在 authInterceptor 之后,addPathPatterns 与 authInterceptor 同清单(8 前缀)。注册顺序即 preHandle 顺序 → 未登录到不了角色检查。

### ⑤ 注解覆盖清单(拦截 8 前缀内全部业务端点)

| Controller(前缀) | 注解位置 | 值 |
| --- | --- | --- |
| UserAddressController(`/address/**`) | 类 | USER |
| ElderProfileController(`/elder-profile/**`) | 类 | USER |
| ServiceCommentController(`/service-comment/**`) | 类 | USER |
| ServiceCartController(`/service-cart/**`) | 类 | USER |
| ServiceItemController(`/service-item/**`) | 类 | PROVIDER |
| ServiceProviderController(`/service-provider/**`) | 类 | **ADMIN**(堵裸奔) |
| SysUserController(`/sys-user/**`) | 类 | **ADMIN**(堵裸奔) |
| ServiceOrderController(`/service-order/**`) | 方法×6 | POST create→USER;GET merchant/list→PROVIDER;GET user/list→USER;PUT cancel/{id}→USER;PUT status/{id}→PROVIDER;GET admin/list→ADMIN |
| AuthController / ServiceCategoryController | 不标 | 公开(拦截器不覆盖) |

### ⑥ `service/impl/ServiceOrderServiceImpl.java`

`listAdminOrders` 开头内联 `role!=2 → Result.fail("无权限")` 删除,注释改为「仅管理员可达由 RoleInterceptor 统一强制」。

### 改动文件

新增:`annotation/RequireRole.java`、`utils/RoleType.java`、`config/RoleInterceptor.java`
修改:`config/WebMvcConfig.java`、8 个 Controller、`service/impl/ServiceOrderServiceImpl.java`

## 验证

1. `cd Elderly_care_Platfrom && ./mvnw -q compile`
2. 起后端(8080 旧实例先 taskkill)后 curl 冒烟矩阵(库内三角色账号登录取 token):
   - **堵口回归**:家属/商家 token 打 `/sys-user/list`、`/service-provider/list` → 改前返数据,改后必须 success=false「无权限」
   - **正例**:admin token 打 `/sys-user/list`、`/service-provider/list`、`/service-order/admin/list` → 正常
   - **互斥**:商家 token 打 `/service-cart/list`、`/elder-profile/gets` →「无权限」;家属 token 打 `/service-item/list`、`/service-order/merchant/list` →「无权限」;admin token 打 `/service-cart/list` →「无权限」
   - **合法主链路回归**:家属 `/address/gets` `/elder-profile/gets` `/service-order/user/list`;商家 `/service-order/merchant/list` `/service-item/list` → 正常
   - 公开端点 `/service-category/list` 无 token → 正常
3. 前端本轮零代码改动,不跑 build

## 提交

1. `docs: 统一角色权限校验设计文档(2026-09-07)`
2. `feat: 角色权限统一校验(RequireRole 注解+拦截器,堵 /sys-user /service-provider 裸奔)`(代码 + CHANGELOG + PROJECT_PROGRESS)
