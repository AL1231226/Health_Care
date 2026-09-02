# 项目约定

## 改动记录(必须遵守)
- **所有代码/数据库/配置改动完成后,必须同步记录到本目录 `CHANGELOG.md`**(按日期分节,新增节放在文件顶部 `# 改动记录` 下方,风格对齐已有条目:一段描述改动内容、涉及文件、原因)。

## 项目结构
- **接手先读本目录 `PROJECT_PROGRESS.md`**(模块完成地图/订单模块现状/下一轮候选),进度细节见 `CHANGELOG.md`。
- 本目录 `health_care_begin` 是**前端**(Vue 3 `<script setup>` + Element Plus + vue-router 5 + Vite + axios,无 Pinia;`@` 别名 → `src`;api 层在 `src/api/`,按后端 Controller 一文件一模块)。
- **后端**在同级目录 `d:\养老实践项目\Elderly_care_Platfrom`(Spring Boot + MyBatis-Plus + MySQL):包 `com.example.Elderly_care_Platfrom`,统一返回 `dao/Result`(`{success,errorMsg,data,total}`,HTTP 恒 200),JWT 拦截路径在 `config/WebMvcConfig`(`/service-category/**`、`/auth/**` 公开,其余业务接口需登录),分页未启用(列表全量返回)。
- 数据库 `health_data`(root/123456,mysql 客户端在 `/d/MySQL/server/bin/mysql`)。已有表:admin、sys_user、elder_profile、user_address、service_category、service_provider、service_item、service_comment(评价表,评分由它聚合,order_id 外键关联订单)、service_order(订单表,2026-09-02 建)。建表脚本统一存后端 `src/main/resources/sql/`。**建表前先 `SHOW TABLES` 确认,勿重复建表**。
- 后端样式:Controller 薄壳 + Service 接口/Impl(QueryWrapper 查询)+ 实体 `@TableName`;商家列表/分类等用户端公开接口挂在 `ServiceCategoryController` 下。
