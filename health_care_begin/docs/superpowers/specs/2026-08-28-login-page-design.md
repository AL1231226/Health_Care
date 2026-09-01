# 登录页设计文档（2026-08-28）

## 背景

颐养平台（居家养老服务预约）用户端前端项目。当前实现用户（家属）与管理员的登录 / 注册页面，两角色共用同一个登录页。

## 需求

- 家属与管理员登录共用一页，通过角色切换（Tab）区分
- 家属：手机号 + 密码登录，可自助注册（手机号 + 昵称 + 密码 + 确认密码）
- 管理员：账号 + 密码登录，无注册入口（由超管在后台创建账号）
- 纯前端页面，暂不接后端接口（提交处留 TODO 占位）

## 设计决策

| 决策点 | 方案 |
| --- | --- |
| 布局 | 左右分栏：左侧品牌区（渐变暖橙背景 + 宣传语 + 服务亮点），右侧白色表单区 |
| 主题色 | 温暖橙 `#ff7a45`（覆盖 Element Plus primary 变量） |
| 角色切换 | 分割式 Tab：家属登录 / 管理员登录 |
| 注册 | 家属表单内切换为注册视图（切回登录时保留已填手机号） |
| 校验 | 手机号正则 `/^1[3-9]\d{9}$/`，密码 ≥ 6 位，确认密码一致性 |
| 登录后 | `router.push` 至 `/user/home` 或 `/admin/home` 占位页 |
| 响应式 | < 900px 时隐藏左侧品牌区，表单区居中 |

## 文件清单

- `src/views/Login.vue` — 登录 / 注册页（核心）
- `src/views/UserHome.vue` / `src/views/AdminHome.vue` — 占位首页
- `src/router/index.js` — 路由配置（原为空）
- `src/assets/main.scss` — 全局样式 + Element Plus 主色覆盖（原缺失，main.js 已引用）
- `index.html` — 标题改为「颐养平台」，lang=zh-CN

## 依赖

新增 `element-plus`、`vue-router`、`@element-plus/icons-vue`（main.js 原本就引用了前两者但未安装，项目此前无法启动）。

## 后续对接点

- 家属登录：`POST /sys-user/login`（phone, password）
- 家属注册：`POST /sys-user/register`（phone, user_name, password）
- 管理员登录：`POST /admin/login`（username, password）
- 响应约定 `{ code, msg, data }`，前端 axios 实例已存在于 `src/utils/request.js`
