<script setup>
// ============ 家属个人中心 ============
// 参考真实 App（美团/饿了么风格）：顶部个人信息卡 + 订单状态条 + 功能宫格 + 设置列表 + 退出登录
// 说明：后端目前仅有登录/注册接口，订单等数据均为静态假数据（TODO 标注后端接口）
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Location, Star, Service, Clock, Van, SuccessFilled, CircleClose,
  Lock, Bell, InfoFilled, ArrowRight,
} from '@element-plus/icons-vue'
import { listUserOrders } from '@/api/order.js'

const router = useRouter()

/* ---------- 当前用户信息（登录时写入 localStorage 的 user_info） ---------- */
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')
const userName = computed(() => userInfo.userName || '家属')
// 手机号脱敏：138****5678
const phone = computed(() => {
  const p = userInfo.phone || ''
  return p ? p.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '未绑定手机号'
})

/* ---------- 我的订单：状态角标接真（GET /service-order/user/list 全量拉取，前端按状态聚合） ---------- */
// 点击跳转我的订单页对应筛选：/user/orders?status=N
const orderCounts = ref({ 0: 0, 1: 0, 2: 0, 3: 0 })
const loadOrders = async () => {
  try {
    const res = await listUserOrders()
    if (res.success && Array.isArray(res.data)) {
      const counts = { 0: 0, 1: 0, 2: 0, 3: 0 }
      res.data.forEach((o) => { if (counts[o.orderStatus] !== undefined) counts[o.orderStatus] += 1 })
      orderCounts.value = counts
    }
  } catch (err) { /* 网络异常由拦截器统一提示，角标保持 0 */ }
}

const orderStats = computed(() => [
  { label: '待接单', count: orderCounts.value[0], icon: Clock, status: 0 },
  { label: '服务中', count: orderCounts.value[1], icon: Van, status: 1 },
  { label: '已完成', count: orderCounts.value[2], icon: SuccessFilled, status: 2 },
  { label: '已取消', count: orderCounts.value[3], icon: CircleClose, status: 3 },
])
const openOrders = (status) => router.push({ path: '/user/orders', query: status != null ? { status } : {} })

// TODO: 老人/评价数据替换为后端接口（当前静态假数据）
// path 非空 = 真实路由跳转；空 = 建设中占位提示
const tools = [
  { label: '老人管理', icon: User, desc: '家人健康档案', path: '/user/elder' },
  { label: '地址管理', icon: Location, desc: '常用服务地址', path: '/user/address' },
  { label: '我的评价', icon: Star, desc: '服务评分晒单', path: '/user/comments' },
  { label: '联系客服', icon: Service, desc: '在线/电话咨询', path: '' },
]

const handleTool = (t) => {
  if (t.path) {
    router.push(t.path)
  } else {
    todo(t.label)
  }
}

// TODO: 修改密码 需后端接口；消息/关于 暂为占位
const settings = [
  { label: '修改密码', icon: Lock },
  { label: '消息通知', icon: Bell },
  { label: '关于平台', icon: InfoFilled },
]

const todo = (name) => ElMessage.info(`${name}建设中，敬请期待`)

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user_info')
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(loadOrders)
</script>

<template>
  <div class="profile">
    <!-- ======== 顶部个人信息区 ======== -->
    <div class="profile-hero">
      <div class="hero-inner">
        <div class="avatar">{{ userName.slice(0, 1) }}</div>
        <div class="hero-info">
          <div class="name-row">
            <span class="name">{{ userName }}</span>
            <span class="vip-tag">家属</span>
          </div>
          <div class="phone">{{ phone }}</div>
        </div>
        <el-button class="edit-btn" round @click="todo('编辑资料')">
          编辑资料
        </el-button>
      </div>
    </div>

    <div class="container">
      <!-- ======== 我的订单 ======== -->
      <div class="card order-card">
        <div class="card-head">
          <span class="card-title">我的订单</span>
          <span class="card-more" @click="openOrders(null)">
            查看全部订单<el-icon class="arrow"><ArrowRight /></el-icon>
          </span>
        </div>
        <div class="order-stats">
          <div v-for="s in orderStats" :key="s.label" class="stat" @click="openOrders(s.status)">
            <div class="stat-icon">
              <el-icon :size="26"><component :is="s.icon" /></el-icon>
              <span v-if="s.count" class="stat-badge">{{ s.count }}</span>
            </div>
            <span class="stat-label">{{ s.label }}</span>
          </div>
        </div>
      </div>

      <!-- ======== 我的工具 ======== -->
      <div class="card">
        <div class="card-head">
          <span class="card-title">我的工具</span>
        </div>
        <div class="tools-grid">
          <div v-for="t in tools" :key="t.label" class="tool" @click="handleTool(t)">
            <span class="tool-icon">
              <el-icon :size="24"><component :is="t.icon" /></el-icon>
            </span>
            <span class="tool-label">{{ t.label }}</span>
            <span class="tool-desc">{{ t.desc }}</span>
          </div>
        </div>
      </div>

      <!-- ======== 账号与设置 ======== -->
      <div class="card">
        <div class="card-head">
          <span class="card-title">账号与设置</span>
        </div>
        <div class="setting-list">
          <div v-for="s in settings" :key="s.label" class="setting-item" @click="todo(s.label)">
            <el-icon class="setting-icon"><component :is="s.icon" /></el-icon>
            <span class="setting-label">{{ s.label }}</span>
            <el-icon class="setting-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- ======== 退出登录 ======== -->
      <el-button class="logout-btn" round @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<style scoped>
.profile {
  min-height: calc(100vh - 64px);
  background: #fdf8f4;
}

/* ============ 顶部个人信息区 ============ */
.profile-hero {
  background: linear-gradient(135deg, #ffb26b 0%, #ff7a45 100%);
}
.hero-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 24px 72px;
  color: #fff;
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.26);
  font-size: 30px;
  font-weight: 600;
  flex-shrink: 0;
}
.hero-info {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name {
  font-size: 24px;
  font-weight: 600;
}
.vip-tag {
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.26);
  font-size: 12px;
}
.phone {
  margin-top: 8px;
  font-size: 14px;
  opacity: 0.92;
}
.edit-btn {
  border: none;
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
}
.edit-btn:hover {
  background: rgba(255, 255, 255, 0.36);
  color: #fff;
}

/* ============ 内容卡片 ============ */
.container {
  max-width: 1200px;
  margin: -44px auto 0;
  padding: 0 24px 40px;
}

.card {
  padding: 22px 24px;
  background: #fff;
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(249, 109, 59, 0.06);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.card-title {
  position: relative;
  padding-left: 12px;
  font-size: 17px;
  font-weight: 600;
  color: #2d2a26;
}
.card-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  bottom: 3px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ffa05f, #ff7a45);
}
.card-more {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: #a39c92;
  cursor: pointer;
}
.card-more:hover {
  color: #ff7a45;
}
.arrow {
  font-size: 12px;
}

/* ============ 订单状态条 ============ */
.order-stats {
  display: flex;
}
.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 0;
}
.stat:hover .stat-label {
  color: #ff7a45;
}
.stat-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #fff2ec;
  color: #ff7a45;
}
.stat-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}
.stat-label {
  font-size: 13px;
  color: #5c564e;
}

/* ============ 工具宫格 ============ */
.tools-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.tool {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.tool:hover {
  background: #fff7f2;
}
.tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
}
.tool-label {
  font-size: 14px;
  font-weight: 600;
  color: #2d2a26;
}
.tool-desc {
  font-size: 12px;
  color: #a39c92;
}

/* ============ 设置列表 ============ */
.setting-list {
  display: flex;
  flex-direction: column;
}
.setting-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 4px;
  cursor: pointer;
}
.setting-item + .setting-item {
  border-top: 1px solid #f7f3ee;
}
.setting-item:hover .setting-label {
  color: #ff7a45;
}
.setting-icon {
  color: #ff8a4c;
  font-size: 20px;
}
.setting-label {
  flex: 1;
  font-size: 15px;
  color: #2d2a26;
}
.setting-arrow {
  color: #c0b9ae;
  font-size: 14px;
}

/* ============ 退出登录 ============ */
.logout-btn {
  width: 100%;
  height: 46px;
  border: none;
  background: #fff;
  color: #f56c6c;
  font-size: 15px;
}
.logout-btn:hover {
  background: #fdeaea;
  color: #f56c6c;
}

/* ============ 响应式 ============ */
@media (max-width: 700px) {
  .tools-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
