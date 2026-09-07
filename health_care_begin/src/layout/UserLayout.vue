<script setup>
// ============ 用户端公共布局：顶部导航 + 内容区 + 底部版权 ============
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Bell, SwitchButton, CaretBottom } from '@element-plus/icons-vue'

const router = useRouter()

// 登录后存入 localStorage 的用户信息（Login.vue 中已剔除密码字段）
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')
const userName = computed(() => userInfo.userName || '家属')

// 导航项：path 为空的子页面未建设，点击提示
// 右侧：搜索 / 消息铃铛 / 个人中心（头像下拉）保持不变
const navItems = [
  { label: '首页', path: '/user/home' },
  { label: '购物车', path: '/user/cart' },
]
const handleNav = (item) => {
  if (item.path) {
    router.push(item.path)
  } else {
    ElMessage.info(`${item.label}建设中，敬请期待`)
  }
}

// 消息通知未接入
const keyword = ref('')
// 顶栏搜索：带关键词跳搜索结果页（页面内再次搜索会经 query 变化触发重拉）
const handleSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  router.push({ path: '/user/search', query: { keyword: kw } })
}
const handleBell = () => ElMessage.info('消息中心建设中，敬请期待')

// 退出登录：清除本地 token 和用户信息，回到登录页
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user_info')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div class="user-layout">
    <header class="header">
      <div class="header-inner">
        <div class="logo">
          <span class="logo-badge">颐</span>
          <span class="logo-text">颐养平台</span>
        </div>

        <nav class="nav">
          <a
            v-for="item in navItems"
            :key="item.label"
            class="nav-item"
            :class="{ active: $route.path === item.path }"
            @click="handleNav(item)"
          >{{ item.label }}</a>
        </nav>

        <div class="header-right">
          <el-input
            v-model="keyword"
            class="search"
            placeholder="搜索服务/商家"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
          <el-badge is-dot class="bell">
            <el-icon :size="20" class="bell-icon" @click="handleBell"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click">
            <span class="user-entry">
              <span class="avatar">{{ userName.slice(0, 1) }}</span>
              <span class="user-name">{{ userName }}</span>
              <el-icon class="caret"><CaretBottom /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">© 2026 颐养平台 · 居家养老服务预约 · 专业照护 让爱到家</footer>
  </div>
</template>

<style scoped>
.user-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #fdf8f4;
}

/* ============ 顶部导航 ============ */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 2px 12px rgba(249, 109, 59, 0.08);
}
.header-inner {
  display: flex;
  align-items: center;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #2d2a26;
  letter-spacing: 2px;
}

.nav {
  display: flex;
  gap: 8px;
  flex: 1;
}
.nav-item {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 15px;
  color: #5c564e;
  cursor: pointer;
  transition: all 0.2s;
}
.nav-item:hover {
  color: #ff7a45;
  background: #fff2ec;
}
.nav-item.active {
  color: #ff7a45;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.search {
  width: 200px;
}
.bell {
  cursor: pointer;
}
.bell-icon {
  color: #8a8378;
}
.bell-icon:hover {
  color: #ff7a45;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 15px;
}
.user-name {
  font-size: 14px;
  color: #2d2a26;
}
.caret {
  color: #a39c92;
  font-size: 12px;
}

/* ============ 内容区 ============ */
.main {
  flex: 1;
}

/* ============ 底部 ============ */
.footer {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #c0b9ae;
  background: #fff;
  border-top: 1px solid #f4f0eb;
}

/* ============ 响应式 ============ */
@media (max-width: 900px) {
  .nav {
    display: none;
  }
}
</style>
