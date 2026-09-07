# 商家端「平均评分」统计卡接真设计文档(2026-09-07)

## 背景

MerchantHome 服务项目管理区 4 张统计卡第 4 张「平均评分」现由前端基于 `service_item.score` 假算([MerchantHome.vue:87-88](/d/养老实践项目/health_care_begin/src/views/MerchantHome.vue#L87-L88):filter score!=null 后求均)——而 `service_item.score/sales` 是**建表注释自标「冗余」的死列**:score 恒 DEFAULT 5.0(库 3 行全 5.0 假象)、sales 恒 0,任何聚合从未写入(insert 置 null/0、update 置 null 跳过,见 ServiceItemServiceImpl.addItem/updateItem)。真实评分模型早已定稿 = `service_comment` 一评一星**实时聚合**:家属端商家列表/详情/评价列表均按 `service_comment` 聚合(ServiceCategoryServiceImpl L109-110/L173-174),`service_item.score` 从未参与任何读路径。商家端统计卡显示的是「5.0」假平均,与家属端看到的真实口碑(评价平均)脱节。

## 决策过程记录

1. 原候选②(评价延伸)第 3 件:「商家端统计卡平均分口径修正(现按从未维护的 item.score 算而空置,应改按商家评价平均或移除)」。
2. 用户一度拍板**删除 `service_item.score` 死列**(理由:一条评价对应一个评分,评分模型 = 每评一星实时聚合,该字段多余),并连带评估 sales。
3. 实施前**反转**:保留 `service_item.score/sales` 字段不动(死列清理另行),按「统计卡接真」方案执行——本轮仅把商家端「平均评分」卡改为 service_comment 实时聚合本人店铺评分,不再读死列。

## 已定决策

| 决策点 | 方案 |
| --- | --- |
| `service_item.score/sales` | **保留不动**(DB/实体/ServiceItemServiceImpl 零改动) |
| 统计卡口径 | 新增商家端轻接口实时聚合本人店铺全部评价;第 4 卡改真,文案维持「平均评分」(店铺级口径) |
| 接口落点 | `ServiceCommentController` 下 `GET /service-comment/provider/score`,**方法级 `@RequireRole(RoleType.PROVIDER)` 覆盖类级 USER**(仿 ServiceProviderController /self 先例:方法注解优先);家属/管理员打它拒「无权限」,家属 create/my 类级 USER 不受影响 |
| 聚合口径 | 与家属端同源:按 `service_comment.provider_id` 全量查出 AVG(score),BigDecimal 1 位小数 HALF_UP;无评价 → score=null、reviewCount=0 |
| 返回形状 | `{score, reviewCount}`(score 可能为 null,用 HashMap 不用 Map.of) |
| 前端 | onMounted 与拉 items 并发拉取一次;0 评价展示「—」,有评价展示均分 + 「N 条评价」小字 |
| 表格「销量/评分」列 | **不动**(字段保留,展示现状维持) |

## 后端设计(Elderly_care_Platfrom)

### ① `IServiceCommentService` + `Impl` 新增 1 方法

- `Result getProviderScore()`:providerId 取 `UserContext.get().userId()`(商家登录 userId 即 provider_id,先例 ServiceProviderServiceImpl.getSelfProfile)→ `list(new QueryWrapper<ServiceComment>().eq("provider_id", providerId))` 全量查本人评价 → 空则 `{score:null, reviewCount:0}`;非空 `mapToInt(ServiceComment::getScore).average()` → BigDecimal `setScale(1, RoundingMode.HALF_UP)`(照抄 ServiceCategoryServiceImpl 聚合写法)→ 返回 `{score, reviewCount}`

### ② `controller/ServiceCommentController` 新增 1 端点

```java
/** 商家端：本人店铺评分聚合（service_comment 实时平均，1 位小数；方法级 PROVIDER 覆盖类级 USER） */
@RequireRole(RoleType.PROVIDER)
@GetMapping("/provider/score")
public Result providerScore() { return serviceCommentService.getProviderScore(); }
```

改动文件:`IServiceCommentService.java`、`ServiceImpl`、`ServiceCommentController.java`(WebMvcConfig 无需改,`/service-comment/**` 已拦截)

## 前端设计(health_care_begin)

- `src/api/comment.js` 追加 `getProviderScore()` → GET /service-comment/provider/score
- `MerchantHome.vue`:
  - `stats` computed 删除 avg 分支(scored/filter/reduce),统计卡回总/上架/下架
  - 平均分改独立 state `providerAvg`(BigDecimal,初 null)+ `providerReviewCount`(初 0),onMounted 并发调 `getProviderScore()`(失败静默——卡显示「—」不阻塞整页)
  - 第 4 张卡:num 区 `providerAvg ?? '—'`,label「平均评分」;有评价时追加小字「N 条评价」(muted-text 风格)

改动文件:`src/api/comment.js`、`src/views/MerchantHome.vue`

## 验证

1. 后端 `./mvnw -q compile`;前端 `npx vite build`
2. curl 冒烟(商家 18239369455/123456ag = provider_id=1,库 1 条真实 5 星评价):
   - 商家 GET /provider/score → score=5.0、reviewCount=1(与库 SELECT AVG/COUNT 对账)
   - 越权:家属/管理员 token 打 /score → 「无权限」
   - 回归:家属 /my 正常(类级 USER 未破)、商家 /service-item/list 正常
3. 前端手测(可选):商家登录统计卡第 4 张显示真实均分
