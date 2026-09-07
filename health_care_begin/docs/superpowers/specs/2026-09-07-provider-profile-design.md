# 商家店铺资料编辑设计文档(2026-09-07)

## 背景

MerchantHome 店铺信息卡「编辑资料」按钮自建页起为禁用占位(「店铺资料编辑功能建设中」,TODO 待接资料接口);全项目**不存在商家自助取/改自己资料的接口**——`/service-provider/**` 现有三端点均为管理员专用,且 2026-09-07 角色权限轮起类级 `@RequireRole(RoleType.ADMIN)`;商家店铺卡数据 = 登录时存的 localStorage user_info 本地快照(非实时,会失真),编辑能力为零。本轮把按钮做真:商家自助**查询 + 修改自己店铺资料**。

## 需求

- 商家(登录后状态必为 1 正常)可查看/修改自己店铺资料:名称、主营分类、负责人、简介、详细地址
- 商家不可动的:登录手机号、状态、角色、密码、平台侧字段;请求体白名单化防篡改
- 改动实时同步店铺卡/顶栏与 localStorage,无需重登

## 已定决策(用户拍板)

| 决策点 | 方案 |
| --- | --- |
| 联系电话 phone | **只读不可改**(登录账号);后端不接收该字段,弹窗禁用展示 + 提示 |
| 编辑字段集 | 精简集:providerName / categoryId / legalPerson / intro / address;logo(全项目无上传组件)、省市区编码、经纬度(地图预留无城市选择器基建)不纳入 |
| 自助接口落点 | `ServiceProviderController` 下 `/service-provider/self`(GET 查 / PUT 改),**方法级 `@RequireRole(RoleType.PROVIDER)` 覆盖类级 ADMIN**——RoleInterceptor 方法注解优先(先例 ServiceOrderController 六方法);admin/family 打 self 同样拒,类级 ADMIN 三端点不受影响 |
| 服务端信任面 | 取回本人行后**白名单复制** 5 个可编辑字段;请求体里 providerId/role/status/password/phone/logo/geo/省市区/时间戳全忽略(不信任前端传值,同注册/下单惯例) |
| 校验口径 | providerName 非空 ≤30;categoryId 真实存在(selectById,同 providerRegister);legalPerson ≤30 / intro ≤200 / address ≤100:trim、空串归一 null |
| 更新后返回 | 回返最新实体(password 置空),前端整对象同步 provider reactive + localStorage user_info(形状与登录返回一致) |
| 拒绝形态 | HTTP 恒 200 + `{"success":false,"errorMsg":...}`(全站惯例) |

## 后端设计(改动均在 Elderly_care_Platfrom)

### ① `IServiceProviderService` / `service/impl/ServiceProviderServiceImpl`

- `Result getSelfProfile()`:商家身份 = `UserContext.get().userId()`(provider_id,先例 ServiceItemServiceImpl.currentProviderId)→ getById → 行不存在 `Result.fail("商家不存在")`;`setPassword(null)`(先例 SysUserServiceImpl.listUsers 置空后返回);`Result.ok(provider)`
- `Result updateSelfProfile(ServiceProvider request)`:getById 取本人行 → 白名单复制(request 的 providerName/categoryId/legalPerson/intro/address 到行对象)→ 校验(名称非空限长;categoryId selectById 存在否则「主营服务分类不存在」;长字段 trim + 空串归一 null + 限长)→ `updateById`(update_time 由 MyMetaObjectHandler 自动填充)→ 再 getById 置空 password 返回

### ② `controller/ServiceProviderController`

```java
/** 商家端：查询本人店铺资料（登录后自用；方法级 PROVIDER 覆盖类级 ADMIN，管理员/家属不可调） */
@RequireRole(RoleType.PROVIDER)
@GetMapping("/self")

/** 商家端：修改本人店铺资料（白名单字段；phone/status/role/password 等一律忽略，不改登录账号） */
@RequireRole(RoleType.PROVIDER)
@PutMapping("/self")
```

现有 list/review/status 与类级 `@RequireRole(RoleType.ADMIN)` 不动;`WebMvcConfig` 无需改(`/service-provider/**` 已拦截,公开除外)。

### 改动文件

`service/IServiceProviderService.java`、`service/impl/ServiceProviderServiceImpl.java`、`controller/ServiceProviderController.java`

## 前端设计(改动均在 health_care_begin)

### ① `src/api/provider.js` 追加

`getSelfProvider()` → GET /service-provider/self;`updateSelfProvider(data)` → PUT /service-provider/self

### ② `MerchantHome.vue`(弹窗对齐服务项目新增/编辑模式)

- 店铺卡编辑按钮去 `disabled`/建设中 title,`@click="openProfileEdit"`
- 新弹窗 state:`profileDialogVisible / profileSaving / profileFormRef / profileForm / profileRules`
- `openProfileEdit()`:先调 `getSelfProvider()` 拉**最新行**填表单(失败 ElMessage.error 并关闭;不依赖本地快照)
- 表单:商家名称 el-input(必填,≤30)/ 主营分类 el-select(必填,复用已有 categories)/ 负责人 el-input(≤30)/ 简介 el-textarea(≤200 show-word-limit)/ 联系电话 el-input **disabled** 只读展示 + 提示「登录手机号,暂不可修改」/ 详细地址 el-input(≤100)
- 保存:`profileFormRef.validate()` → `updateSelfProvider` → 成功:`ElMessage.success`、`Object.assign(provider, res.data)` 即时刷新店铺卡与顶栏、localStorage user_info 整对象替换、关弹窗;失败弹 errorMsg;按钮 :loading 防重复
- 保存后无需重登(phone 未变;JWT name claim 无服务端消费,前端展示即时由 provider 更新)

### 改动文件

`src/api/provider.js`、`src/views/MerchantHome.vue`

## 验证

1. 后端 `./mvnw -q compile`;前端 `npx vite build`
2. 起后端 curl 冒烟(商家 18239369455/123456ag、管理员 root/123456ag、家属 19943931069/123456ag):
   - 商家 GET /self → 自身行(password null)
   - 商家 PUT /self 正常改 intro/address/providerName → 响应新实体 + SELECT 行核对,role/status/password/phone 未被改动
   - 白名单防篡改:body 塞 {status:2, role:2, password:"hack", phone:"13900000000", providerId:99} → 行内对应列全不变
   - 越权双测:admin/family token 打 /self → 均「无权限」;admin 打 /service-provider/list 仍正常(类级 ADMIN 未破)
   - 校验:空 providerName 拒;categoryId=99999 拒「主营服务分类不存在」
   - 回归:商家 /service-item/list 正常;原手机号仍可登录

## 提交

1. `docs: 商家店铺资料编辑设计文档(2026-09-07)`
2. `feat: 商家店铺资料编辑(自助查改 /service-provider/self)`(代码 + CHANGELOG + PROJECT_PROGRESS)
