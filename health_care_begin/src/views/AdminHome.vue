<script setup>
// ============ 管理后台首页（纯前端静态版，未接后端） ============
// TODO 后端接口就绪后逐块替换：审核/商家/用户/服务/分类/评价均接对应 controller
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataBoard, User, Shop, List, Menu, Star, Tickets,
  Setting, ArrowDown, Search, Bell, Warning,
} from '@element-plus/icons-vue'
import { listProviders, reviewProvider, toggleProviderStatus } from '@/api/provider.js'
import { listCategory } from '@/api/category.js'
import { listSysUsers, updateUserStatus } from '@/api/user.js'
import { listAdminOrders } from '@/api/order.js'

const router = useRouter()

/* ---------- 当前登录管理员（localStorage user_info 兜底） ---------- */
const adminName = ref('超级管理员')

/* ---------- 左侧菜单 ---------- */
const menus = [
  { key: 'dashboard', label: '数据看板', icon: DataBoard },
  { key: 'review', label: '商家审核', icon: Warning, badge: 3 },
  { key: 'provider', label: '商家管理', icon: Shop },
  { key: 'user', label: '用户管理', icon: User },
  { key: 'item', label: '服务管理', icon: List },
  { key: 'category', label: '分类管理', icon: Menu },
  { key: 'comment', label: '评价管理', icon: Star },
  { key: 'order', label: '订单管理', icon: Tickets },
  { key: 'setting', label: '系统设置', icon: Setting, building: true },
]
const activeMenu = ref('dashboard')
const currentMenu = computed(() => menus.find((m) => m.key === activeMenu.value))

/* ---------- 统计卡（家属/商家/待审核实时来自接口，其余仍为静态 TODO 接统计接口） ---------- */
const stats = computed(() => [
  { label: '注册家属', value: users.value.length, color: '#ff7a45' },
  { label: '入驻商家', value: providers.value.length, color: '#34a853' },
  { label: '服务项目', value: 186, color: '#4a90d9' },
  { label: '待审核商家', value: pendingProviders.value.length, color: '#e6a23c' },
  { label: '累计评价', value: 3592, color: '#9b59b6' },
])

/* ---------- 分类映射（用于表格显示分类名，来自 service_category 接口） ---------- */
const categoryMap = ref({})
const loadCategories = async () => {
  try {
    const result = await listCategory()
    if (result.success && Array.isArray(result.data)) {
      const map = {}
      result.data.forEach((c) => { map[c.categoryId] = c.categoryName })
      categoryMap.value = map
    }
  } catch (err) { /* 失败不阻塞，分类名显示「未分类」 */ }
}

/* ---------- 商家审核（status=0 待审核，真实接口） ---------- */
const reviewLoading = ref(false)
const pendingProviders = ref([])
const loadPending = async () => {
  reviewLoading.value = true
  try {
    const result = await listProviders(0)
    if (result.success) {
      pendingProviders.value = result.data || []
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('获取待审核商家失败，请稍后重试')
  } finally {
    reviewLoading.value = false
  }
}

/* ---------- 商家管理（全部商家，真实接口） ---------- */
const providerLoading = ref(false)
const providers = ref([])
const loadProviders = async () => {
  providerLoading.value = true
  try {
    const result = await listProviders()
    if (result.success) {
      providers.value = result.data || []
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('获取商家列表失败，请稍后重试')
  } finally {
    providerLoading.value = false
  }
}

// 审核通过/驳回：调接口成功后刷新两个列表
const handleReview = (row, pass) => {
  ElMessageBox.confirm(
    pass ? `审核通过「${row.providerName}」？通过后即可登录商家端` : `驳回「${row.providerName}」的入驻申请？`,
    pass ? '审核通过' : '驳回申请',
    { type: pass ? 'success' : 'warning', confirmButtonText: pass ? '通过' : '驳回', cancelButtonText: '取消' },
  ).then(async () => {
    try {
      const result = await reviewProvider(row.providerId, pass)
      if (result.success) {
        ElMessage.success(result.data || (pass ? '审核通过' : '已驳回'))
        loadPending()
        loadProviders()
      } else {
        ElMessage.error(result.errorMsg)
      }
    } catch (err) {
      ElMessage.error('操作失败，请稍后重试')
    }
  }).catch(() => {})
}

// 启用/停用：调接口成功后刷新列表
const handleToggleProvider = (row) => {
  const target = row.status === 1 ? 2 : 1
  ElMessageBox.confirm(
    target === 2 ? `确定停用「${row.providerName}」？停用后商家无法登录` : `确定启用「${row.providerName}」？`,
    target === 2 ? '停用商家' : '启用商家',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
  ).then(async () => {
    try {
      const result = await toggleProviderStatus(row.providerId, target)
      if (result.success) {
        ElMessage.success(result.data || '操作成功')
        loadProviders()
      } else {
        ElMessage.error(result.errorMsg)
      }
    } catch (err) {
      ElMessage.error('操作失败，请稍后重试')
    }
  }).catch(() => {})
}

/* ---------- 用户管理（家属 status 0禁用 1正常，真实接口） ---------- */
const userLoading = ref(false)
const users = ref([])
const loadUsers = async () => {
  userLoading.value = true
  try {
    const result = await listSysUsers()
    if (result.success) {
      users.value = result.data || []
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('获取用户列表失败，请稍后重试')
  } finally {
    userLoading.value = false
  }
}

// 启用/禁用：调接口成功后刷新列表
const handleToggleUser = (row) => {
  const target = row.status === 1 ? 0 : 1
  ElMessageBox.confirm(
    target === 0 ? `确定禁用「${row.userName}」？禁用后该账号无法登录` : `确定启用「${row.userName}」？`,
    target === 0 ? '禁用用户' : '启用用户',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
  ).then(async () => {
    try {
      const result = await updateUserStatus(row.id, target)
      if (result.success) {
        ElMessage.success(result.data || '操作成功')
        loadUsers()
      } else {
        ElMessage.error(result.errorMsg)
      }
    } catch (err) {
      ElMessage.error('操作失败，请稍后重试')
    }
  }).catch(() => {})
}

/* ---------- 服务管理（全平台服务项目） ---------- */
const items = ref([
  { id: 1, name: '长者营养午餐', provider: '幸福食堂', category: '助餐', price: 28, unit: '餐', sales: 320, status: 1 },
  { id: 2, name: '家庭深度保洁', provider: '阳光助洁家政', category: '助洁', price: 89, unit: '次', sales: 156, status: 1 },
  { id: 3, name: '上门助浴服务', provider: '爱心助浴', category: '助浴', price: 128, unit: '次', sales: 89, status: 0 },
  { id: 4, name: '康复理疗按摩', provider: '安康康复中心', category: '康复护理', price: 99, unit: '次', sales: 134, status: 1 },
])
const toggleItem = (row) => {
  row.status = row.status === 1 ? 0 : 1
  ElMessage.success(row.status === 1 ? `「${row.name}」已恢复上架` : `「${row.name}」已下架`)
}

/* ---------- 分类管理（service_category 五个分类） ---------- */
const categories = ref([
  { id: 1, name: '助餐服务', sort: 1, status: 1, itemCount: 38 },
  { id: 2, name: '助洁服务', sort: 2, status: 1, itemCount: 45 },
  { id: 3, name: '助浴服务', sort: 3, status: 1, itemCount: 22 },
  { id: 4, name: '助医服务', sort: 4, status: 1, itemCount: 31 },
  { id: 5, name: '康复护理', sort: 5, status: 1, itemCount: 50 },
])

/* ---------- 评价管理 ---------- */
const comments = ref([
  { id: 1, userName: '陈阿姨', itemName: '长者营养午餐', provider: '幸福食堂', score: 5, content: '饭菜很合老人胃口，服务及时，谢谢！', createTime: '2026-09-01 11:20' },
  { id: 2, userName: '刘叔', itemName: '家庭深度保洁', provider: '阳光助洁家政', score: 4, content: '打扫得挺干净，时间也准时。', createTime: '2026-09-01 10:05' },
  { id: 3, userName: '赵女士', itemName: '上门助浴服务', provider: '爱心助浴', score: 2, content: '预约时间临时改了好几次，体验一般。', createTime: '2026-08-31 19:42' },
])
const removeComment = (row) => {
  ElMessageBox.confirm(`确定删除「${row.userName}」的这条评价吗？`, '删除评价', {
    type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
  }).then(() => {
    comments.value = comments.value.filter((c) => c.id !== row.id)
    ElMessage.success('评价已删除')
  }).catch(() => {})
}

/* ---------- 订单管理（全平台只读监督，GET /service-order/admin/list 真实数据；状态筛选/关键词过滤在前端做） ---------- */
const orderLoading = ref(false)
const orders = ref([])
const ALL_STATUS = -1 // 筛选哨兵：全部
const orderFilter = ref(ALL_STATUS)
const orderKeyword = ref('')
const orderStatusTabs = [
  { value: ALL_STATUS, label: '全部' },
  { value: 0, label: '待接单' },
  { value: 1, label: '服务中' },
  { value: 2, label: '已完成' },
  { value: 3, label: '已取消' },
]
const orderStatusMap = {
  0: { text: '待接单', type: 'warning' },
  1: { text: '服务中', type: 'primary' },
  2: { text: '已完成', type: 'success' },
  3: { text: '已取消', type: 'info' },
}
const orderCounts = computed(() => {
  const counts = { 0: 0, 1: 0, 2: 0, 3: 0 }
  orders.value.forEach((o) => { if (counts[o.orderStatus] !== undefined) counts[o.orderStatus] += 1 })
  return counts
})
// 状态 + 关键词双重过滤（关键词匹配 商家/服务/老人/家属/订单号 任一字段，与状态筛选 AND）
const filteredOrders = computed(() => {
  const kw = orderKeyword.value.trim().toLowerCase()
  return orders.value.filter((o) => {
    if (orderFilter.value !== ALL_STATUS && o.orderStatus !== orderFilter.value) return false
    if (!kw) return true
    return [o.providerName, o.itemName, o.elderName, o.familyName, o.orderNo]
      .some((f) => f != null && String(f).toLowerCase().includes(kw))
  })
})
// 看板「待接单新订单」卡数据（全量订单已按下单时间倒序）
const pendingOrders = computed(() => orders.value.filter((o) => o.orderStatus === 0))
const loadOrders = async () => {
  orderLoading.value = true
  try {
    const res = await listAdminOrders()
    if (res.success) {
      orders.value = res.data || []
    } else {
      ElMessage.error(res.errorMsg || '加载订单失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ } finally {
    orderLoading.value = false
  }
}
// 看板/订单页跳转：切菜单并选中状态筛选
const goOrderTab = (status) => {
  orderFilter.value = status
  activeMenu.value = 'order'
}

/* ---------- 订单详情抽屉（只读监督） ---------- */
const drawerVisible = ref(false)
const currentOrder = ref(null)
const genderText = (g) => (Number(g) === 1 ? '男' : Number(g) === 0 ? '女' : '—')
const ageOf = (d) => {
  if (!d) return null
  const birth = new Date(d)
  if (Number.isNaN(birth.getTime())) return null
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  if (now.getMonth() < birth.getMonth() || (now.getMonth() === birth.getMonth() && now.getDate() < birth.getDate())) age -= 1
  return age
}
const ageText = (row) => {
  const age = ageOf(row.birthDate)
  return age != null ? `${age}岁` : '年龄未知'
}
const fmtDateTime = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
const openOrderDetail = (row) => {
  currentOrder.value = row
  drawerVisible.value = true
}

/* ---------- 顶栏：搜索/消息占位 + 退出登录 ---------- */
const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '退出确认', {
    confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning',
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user_info')
    router.push('/login')
  }).catch(() => {})
}

// 时间兜底格式化（后端 Date 序列化可能是时间戳或字符串）
const fmtDate = (v) => {
  if (!v) return '-'
  if (typeof v === 'number') return new Date(v).toLocaleString('zh-CN')
  return String(v)
}

onMounted(() => {
  // 管理员名取登录存的 user_info.username，缺失兜底
  try {
    const saved = JSON.parse(localStorage.getItem('user_info') || '{}')
    if (saved.username) adminName.value = saved.username
  } catch (err) { /* 兜底默认值 */ }
  // 加载数据（分类映射 + 待审核商家 + 全部商家 + 家属列表 + 全平台订单；订单数据看板/订单页共用一份）
  loadCategories()
  loadPending()
  loadProviders()
  loadUsers()
  loadOrders()
})
</script>

<template>
  <div class="admin-page">
    <!-- ======== 左侧菜单 ======== -->
    <aside class="sidebar">
      <div class="side-brand">
        <span class="brand-badge">颐</span>
        <span class="brand-text">颐养平台</span>
      </div>
      <nav class="side-nav">
        <button
          v-for="m in menus"
          :key="m.key"
          class="nav-item"
          :class="{ active: activeMenu === m.key }"
          @click="activeMenu = m.key"
        >
          <el-icon class="nav-icon"><component :is="m.icon" /></el-icon>
          <span class="nav-label">{{ m.label }}</span>
          <span v-if="m.badge" class="nav-badge">{{ m.badge }}</span>
          <span v-if="m.building" class="nav-tag">建设中</span>
        </button>
      </nav>
      <p class="side-foot">© 2026 颐养平台管理后台</p>
    </aside>

    <!-- ======== 右侧主区 ======== -->
    <div class="main">
      <!-- 顶栏 -->
      <header class="top-bar">
        <div class="top-left">
          <h2 class="page-title">{{ currentMenu.label }}</h2>
          <el-input class="top-search" placeholder="搜索商家 / 用户 / 服务（建设中）" :prefix-icon="Search" disabled />
        </div>
        <div class="top-right">
          <el-badge :value="pendingProviders.length" :offset="[-2, 2]">
            <el-icon class="bell" :size="18"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click">
            <span class="admin-chip">
              <el-avatar :size="28" class="admin-avatar">{{ adminName.charAt(0) }}</el-avatar>
              {{ adminName }}
              <el-icon class="chip-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>系统管理员</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <!-- ====== 数据看板 ====== -->
        <template v-if="activeMenu === 'dashboard'">
          <section class="stats-row">
            <div v-for="s in stats" :key="s.label" class="card stat-card">
              <p class="stat-num" :style="{ color: s.color }">{{ s.value }}</p>
              <p class="stat-label">{{ s.label }}</p>
            </div>
          </section>

          <section class="card todo-card">
            <div class="card-head">
              <h3>待办事项</h3>
              <el-button link type="primary" @click="activeMenu = 'review'">查看全部 →</el-button>
            </div>
            <div class="todo-list" v-loading="reviewLoading">
              <div class="todo-empty" v-if="!pendingProviders.length && !reviewLoading">🎉 没有待审核的商家，全部已处理</div>
              <div v-for="p in pendingProviders" :key="p.providerId" class="todo-item">
                <div class="todo-info">
                  <span class="todo-name">{{ p.providerName }}</span>
                  <span class="tag">{{ categoryMap[p.categoryId] || '未分类' }}</span>
                  <span class="todo-meta">联系人 {{ p.legalPerson || '-' }} · {{ fmtDate(p.createTime) }}</span>
                </div>
                <div class="todo-actions">
                  <el-button size="small" type="success" plain @click="handleReview(p, true)">通过</el-button>
                  <el-button size="small" type="danger" plain @click="handleReview(p, false)">驳回</el-button>
                </div>
              </div>
            </div>
          </section>

          <section class="card todo-card">
            <div class="card-head">
              <h3>待接单新订单</h3>
              <el-button link type="primary" @click="goOrderTab(0)">查看全部 →</el-button>
            </div>
            <div class="todo-list" v-loading="orderLoading">
              <div class="todo-empty" v-if="!pendingOrders.length && !orderLoading">🎉 没有待接单的新订单</div>
              <div v-for="o in pendingOrders" :key="o.orderId" class="todo-item todo-order" @click="openOrderDetail(o)">
                <div class="todo-info">
                  <span class="todo-name">{{ o.itemName || '服务已删除' }}</span>
                  <span class="tag">{{ o.providerName || '商家已注销' }}</span>
                  <span class="todo-meta">¥{{ o.totalPrice }} · {{ fmtDateTime(o.createTime) }}</span>
                </div>
                <el-tag :type="orderStatusMap[o.orderStatus]?.type" size="small">{{ orderStatusMap[o.orderStatus]?.text }}</el-tag>
              </div>
            </div>
          </section>

          <section class="card todo-card">
            <div class="card-head">
              <h3>平台概况</h3>
            </div>
            <div class="overview-grid">
              <div class="overview-item"><p class="ov-num">5</p><p class="ov-label">服务分类</p></div>
              <div class="overview-item"><p class="ov-num">1286</p><p class="ov-label">家属档案</p></div>
              <div class="overview-item"><p class="ov-num">42</p><p class="ov-label">入驻商家</p></div>
              <div class="overview-item"><p class="ov-num">186</p><p class="ov-label">服务项目</p></div>
              <div class="overview-item"><p class="ov-num">3592</p><p class="ov-label">累计评价</p></div>
            </div>
            <p class="table-hint">以上为静态演示数据，后续接入统计接口（TODO）</p>
          </section>
        </template>

        <!-- ====== 商家审核 ====== -->
        <section v-else-if="activeMenu === 'review'" class="card">
          <div class="card-head">
            <h3>待审核商家（{{ pendingProviders.length }}）</h3>
          </div>
          <el-table v-loading="reviewLoading" :data="pendingProviders" stripe>
            <el-table-column prop="providerName" label="商家名称" min-width="160" />
            <el-table-column label="主营分类" width="120">
              <template #default="{ row }"><span class="cat-chip">{{ categoryMap[row.categoryId] || '未分类' }}</span></template>
            </el-table-column>
            <el-table-column prop="legalPerson" label="负责人" width="110">
              <template #default="{ row }">{{ row.legalPerson || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="联系电话" width="130" />
            <el-table-column label="申请时间" width="160">
              <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" plain @click="handleReview(row, true)">通过</el-button>
                <el-button size="small" type="danger" plain @click="handleReview(row, false)">驳回</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无待审核商家 🎉" /></template>
          </el-table>
        </section>

        <!-- ====== 商家管理 ====== -->
        <section v-else-if="activeMenu === 'provider'" class="card">
          <div class="card-head"><h3>全部商家（{{ providers.length }}）</h3></div>
          <el-table v-loading="providerLoading" :data="providers" stripe>
            <el-table-column prop="providerName" label="商家名称" min-width="180" />
            <el-table-column label="主营分类" width="120">
              <template #default="{ row }"><span class="cat-chip">{{ categoryMap[row.categoryId] || '未分类' }}</span></template>
            </el-table-column>
            <el-table-column prop="phone" label="联系电话" width="140" />
            <el-table-column prop="legalPerson" label="负责人" width="110">
              <template #default="{ row }">{{ row.legalPerson || '-' }}</template>
            </el-table-column>
            <el-table-column label="入驻时间" width="160">
              <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span class="status-badge" :class="row.status === 1 ? 'ok' : (row.status === 2 ? 'off' : 'pending')">
                  {{ row.status === 0 ? '待审核' : (row.status === 1 ? '正常' : '停用') }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status !== 0"
                  size="small"
                  :type="row.status === 1 ? 'danger' : 'success'"
                  plain
                  @click="handleToggleProvider(row)"
                >{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                <span v-else class="status-badge pending">待审核</span>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <!-- ====== 用户管理 ====== -->
        <section v-else-if="activeMenu === 'user'" class="card">
          <div class="card-head"><h3>注册家属（{{ users.length }}）</h3></div>
          <el-table v-loading="userLoading" :data="users" stripe>
            <el-table-column prop="userName" label="昵称" min-width="140">
              <template #default="{ row }">{{ row.userName || '未设置' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" width="150" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">{{ row.role === 1 ? '家属' : '其他' }}</template>
            </el-table-column>
            <el-table-column label="注册时间" width="170">
              <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span class="status-badge" :class="row.status === 1 ? 'ok' : 'off'">{{ row.status === 1 ? '正常' : '禁用' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain @click="handleToggleUser(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <!-- ====== 服务管理 ====== -->
        <section v-else-if="activeMenu === 'item'" class="card">
          <div class="card-head"><h3>全平台服务项目（{{ items.length }}）</h3></div>
          <el-table :data="items" stripe>
            <el-table-column prop="name" label="服务名称" min-width="180" />
            <el-table-column prop="provider" label="所属商家" min-width="160" />
            <el-table-column label="分类" width="110">
              <template #default="{ row }"><span class="cat-chip">{{ row.category }}</span></template>
            </el-table-column>
            <el-table-column label="价格" width="110">
              <template #default="{ row }"><span class="price">¥{{ row.price }}</span><span class="price-unit">/{{ row.unit }}</span></template>
            </el-table-column>
            <el-table-column prop="sales" label="销量" width="90" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span class="status-badge" :class="row.status === 1 ? 'ok' : 'off'">{{ row.status === 1 ? '上架' : '下架' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleItem(row)">
                  {{ row.status === 1 ? '下架' : '上架' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <!-- ====== 分类管理 ====== -->
        <section v-else-if="activeMenu === 'category'" class="card">
          <div class="card-head">
            <h3>服务分类（{{ categories.length }}）</h3>
            <el-button class="primary-btn" type="primary" @click="ElMessage.info('分类新增/编辑功能建设中（TODO）')">＋ 新增分类</el-button>
          </div>
          <el-table :data="categories" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="分类名称" min-width="180" />
            <el-table-column prop="sort" label="排序" width="100" />
            <el-table-column prop="itemCount" label="服务项目数" width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span class="status-badge" :class="row.status === 1 ? 'ok' : 'off'">{{ row.status === 1 ? '启用' : '停用' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="ElMessage.info('编辑建设中（TODO）')">编辑</el-button>
                <el-button size="small" type="danger" link @click="ElMessage.info('删除建设中（TODO）')">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <!-- ====== 评价管理 ====== -->
        <section v-else-if="activeMenu === 'comment'" class="card">
          <div class="card-head"><h3>全平台评价（{{ comments.length }}）</h3></div>
          <el-table :data="comments" stripe>
            <el-table-column prop="userName" label="评价人" width="120" />
            <el-table-column prop="itemName" label="服务" min-width="160" />
            <el-table-column prop="provider" label="商家" min-width="150" />
            <el-table-column label="评分" width="100">
              <template #default="{ row }">
                <span class="score">{{ '★'.repeat(row.score) }}<span class="score-empty">{{ '★'.repeat(5 - row.score) }}</span></span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="150" />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="danger" link @click="removeComment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <!-- ====== 订单管理（全平台只读监督） ====== -->
        <section v-else-if="activeMenu === 'order'" class="card">
          <div class="card-head">
            <h3>全平台订单（{{ orders.length }}）</h3>
            <el-button class="primary-btn" type="primary" :loading="orderLoading" @click="loadOrders">刷新</el-button>
          </div>
          <div class="order-bar">
            <div class="filter-group">
              <span
                v-for="t in orderStatusTabs"
                :key="t.value"
                class="filter-capsule"
                :class="{ active: orderFilter === t.value }"
                @click="orderFilter = t.value"
              >
                {{ t.label }}
                <span v-if="t.value !== ALL_STATUS" class="filter-count">{{ orderCounts[t.value] || 0 }}</span>
              </span>
            </div>
            <el-input
              v-model="orderKeyword"
              class="order-search"
              placeholder="搜索商家 / 服务 / 老人 / 家属 / 订单号"
              :prefix-icon="Search"
              clearable
            />
          </div>
          <el-table v-loading="orderLoading" :data="filteredOrders" stripe>
            <el-table-column label="订单号" width="185">
              <template #default="{ row }"><span class="order-no-cell">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column label="服务项目" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.itemName || '服务已删除' }}</template>
            </el-table-column>
            <el-table-column label="商家" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.providerName || '商家已注销' }}</template>
            </el-table-column>
            <el-table-column label="下单家属" width="105" show-overflow-tooltip>
              <template #default="{ row }">{{ row.familyName || '账号已删除' }}</template>
            </el-table-column>
            <el-table-column label="服务老人" width="160">
              <template #default="{ row }">
                <span v-if="row.elderName">{{ row.elderName }}<span class="elder-sex">（{{ genderText(row.gender) }} · {{ ageText(row) }}）</span></span>
                <span v-else class="muted-text">档案已删</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="150">
              <template #default="{ row }">
                <div class="amount-cell">
                  <span class="price">¥{{ row.totalPrice }}</span>
                  <span class="amount-sub">¥{{ row.unitPrice }} × {{ row.quantity }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="95">
              <template #default="{ row }">
                <el-tag :type="orderStatusMap[row.orderStatus]?.type" size="small" effect="light">
                  {{ orderStatusMap[row.orderStatus]?.text || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" width="150">
              <template #default="{ row }">{{ fmtDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openOrderDetail(row)">详情</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无相关订单" /></template>
          </el-table>
        </section>

        <!-- ====== 建设中模块 ====== -->
        <section v-else class="card building-card">
          <h3>🛠️ {{ currentMenu.label }}建设中</h3>
          <p>该模块待后续版本实现</p>
        </section>
      </main>

      <!-- ======== 订单详情抽屉（管理员只读监督：含下单家属/服务老人健康备注） ======== -->
      <el-drawer v-model="drawerVisible" title="订单详情" size="460px">
        <div v-if="currentOrder" class="order-detail">
          <!-- 服务信息 -->
          <div class="detail-section">
            <div class="section-title">服务信息</div>
            <div class="d-row"><span class="d-label">服务项目</span><span class="d-value">{{ currentOrder.itemName || '服务已删除' }}</span></div>
            <div class="d-row"><span class="d-label">下单家属</span><span class="d-value">{{ currentOrder.familyName || '账号已删除' }}</span></div>
            <div class="d-row"><span class="d-label">服务商家</span><span class="d-value">{{ currentOrder.providerName || '商家已注销' }}</span></div>
            <div class="d-row"><span class="d-label">单价 × 数量</span><span class="d-value">¥{{ currentOrder.unitPrice }} × {{ currentOrder.quantity }}</span></div>
            <div class="d-row"><span class="d-label">订单金额</span><span class="d-value amount">¥{{ currentOrder.totalPrice }}</span></div>
            <div class="d-row"><span class="d-label">订单号</span><span class="d-value order-no-cell">{{ currentOrder.orderNo }}</span></div>
            <div class="d-row"><span class="d-label">下单时间</span><span class="d-value">{{ fmtDateTime(currentOrder.createTime) }}</span></div>
          </div>

          <!-- 服务老人（健康备注供管理员协调投诉参考） -->
          <div class="detail-section elder-section">
            <div class="section-title">服务老人</div>
            <template v-if="currentOrder.elderName">
              <div class="elder-name">
                {{ currentOrder.elderName }}
                <span class="elder-tag">
                  {{ genderText(currentOrder.gender) }} · {{ ageText(currentOrder) }}
                </span>
              </div>
              <div class="d-row"><span class="d-label">老人电话</span><span class="d-value">{{ currentOrder.elderPhone || '未填写' }}</span></div>
              <div class="health-box">
                <div class="health-title"><el-icon class="health-icon"><Warning /></el-icon>健康备注 / 护理注意事项</div>
                <p class="health-text">{{ currentOrder.healthNote || '家属未填写健康备注' }}</p>
              </div>
            </template>
            <div v-else class="muted-text">该老人档案已被删除，无法查看健康信息</div>
          </div>

          <!-- 服务安排 -->
          <div class="detail-section">
            <div class="section-title">服务安排</div>
            <div class="d-row"><span class="d-label">预约时间</span><span class="d-value">{{ currentOrder.serviceTime ? fmtDateTime(currentOrder.serviceTime) : '未预约（下单后沟通）' }}</span></div>
            <div class="d-row"><span class="d-label">服务地址</span><span class="d-value">{{ currentOrder.addressText || '未选择（下单后电话沟通）' }}</span></div>
            <div class="d-row"><span class="d-label">联系电话</span><span class="d-value">{{ currentOrder.contactPhone }}</span></div>
            <div class="d-row"><span class="d-label">订单备注</span><span class="d-value">{{ currentOrder.remark || '无' }}</span></div>
          </div>
        </div>
      </el-drawer>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  min-height: 100vh;
  background: #f6f2ed;
}

/* ============ 左侧菜单 ============ */
.sidebar {
  flex: none;
  width: 210px;
  display: flex;
  flex-direction: column;
  background: #2d2a26;
  color: #cfc8bd;
}
.side-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}
.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}
.side-nav {
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 42px;
  padding: 0 12px;
  margin-bottom: 4px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #cfc8bd;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.nav-item.active {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 122, 69, 0.3);
}
.nav-icon {
  font-size: 17px;
}
.nav-label {
  flex: 1;
  text-align: left;
}
.nav-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}
.nav-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: #b5aca1;
}
.side-foot {
  padding: 14px;
  font-size: 11px;
  color: #6f6860;
  text-align: center;
}

/* ============ 主区 ============ */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.top-bar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f2ece5;
}
.top-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #2d2a26;
}
.top-search {
  width: 260px;
}
.top-right {
  display: flex;
  align-items: center;
  gap: 18px;
}
.bell {
  color: #8a8378;
  cursor: pointer;
}
.admin-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: 20px;
  cursor: pointer;
  color: #4a443c;
  font-size: 14px;
  outline: none;
}
.admin-chip:hover {
  background: #fdf2ea;
}
.admin-avatar {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 13px;
}
.chip-arrow {
  font-size: 12px;
  color: #b5aca1;
}

/* ============ 内容区 ============ */
.content {
  flex: 1;
  padding: 20px 24px 40px;
  overflow-y: auto;
}
.card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(249, 109, 59, 0.05);
  padding: 18px 20px;
  margin-bottom: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.card-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
}

/* 统计卡 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
  padding: 18px 10px;
  margin-bottom: 0;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
}
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #a39c92;
}

/* 待办 */
.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 4px;
  border-bottom: 1px dashed #f2ece5;
}
.todo-item:last-child {
  border-bottom: none;
}
.todo-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.todo-name {
  font-weight: 600;
  color: #2d2a26;
}
.todo-meta {
  font-size: 12px;
  color: #b5aca1;
}
.todo-empty {
  padding: 20px 0;
  text-align: center;
  color: #a39c92;
  font-size: 13px;
}
.tag {
  padding: 2px 10px;
  border-radius: 8px;
  background: #fdf2ea;
  color: #ff7a45;
  font-size: 12px;
}

/* 平台概况 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.overview-item {
  text-align: center;
  padding: 14px 8px;
  border-radius: 12px;
  background: #faf5ef;
}
.ov-num {
  font-size: 22px;
  font-weight: 700;
  color: #ff7a45;
}
.ov-label {
  margin-top: 4px;
  font-size: 12px;
  color: #a39c92;
}
.table-hint {
  margin-top: 14px;
  text-align: center;
  font-size: 12px;
  color: #c0b9ae;
}

/* 表格通用 */
.cat-chip {
  padding: 2px 10px;
  border-radius: 8px;
  background: #fdf2ea;
  color: #ff7a45;
  font-size: 12px;
}
.status-badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
}
.status-badge.ok {
  background: #e8f8ee;
  color: #34a853;
}
.status-badge.off {
  background: #fdeeee;
  color: #f56c6c;
}
.status-badge.pending {
  background: #fff4e0;
  color: #e6a23c;
}
.price {
  font-size: 14px;
  font-weight: 600;
  color: #ff6b2c;
}
.price-unit {
  font-size: 12px;
  color: #b5aca1;
}
.score {
  color: #ff9f43;
  letter-spacing: 1px;
}
.score-empty {
  color: #e8e2da;
}
.primary-btn {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  border: none;
}
.primary-btn:hover {
  opacity: 0.9;
}

/* 建设中 */
.building-card {
  text-align: center;
  padding: 60px 20px;
}
.building-card h3 {
  font-size: 18px;
  color: #2d2a26;
  margin-bottom: 10px;
}
.building-card p {
  color: #a39c92;
  font-size: 13px;
}

/* ============ 订单管理 ============ */
.order-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-capsule {
  padding: 5px 14px;
  border-radius: 20px;
  background: #f6f2ed;
  color: #8a8378;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.filter-capsule:hover {
  background: #fdf2ea;
  color: #ff7a45;
}
.filter-capsule.active {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  box-shadow: 0 4px 10px rgba(255, 122, 69, 0.25);
}
.filter-count {
  margin-left: 5px;
  padding: 0 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.85);
  color: #ff7a45;
  font-size: 11px;
}
.filter-capsule.active .filter-count {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}
.order-search {
  width: 300px;
}
.order-no-cell {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
.muted-text {
  color: #c0b9ae;
}
.elder-sex {
  color: #a39c92;
  font-size: 12px;
}
.amount-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
}
.amount-sub {
  font-size: 11px;
  color: #b5aca1;
}
/* 看板待接单行：整行可点看详情 */
.todo-order {
  cursor: pointer;
  border-radius: 8px;
}
.todo-order:hover {
  background: #fdf8f3;
}

/* ============ 订单详情抽屉 ============ */
.order-detail .detail-section {
  padding: 16px 18px;
  margin-bottom: 14px;
  background: #fdf9f5;
  border-radius: 12px;
}
.order-detail .section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
  margin-bottom: 10px;
}
.order-detail .d-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0;
}
.order-detail .d-label {
  flex-shrink: 0;
  font-size: 13px;
  color: #a39c92;
}
.order-detail .d-value {
  font-size: 13px;
  color: #2d2a26;
  text-align: right;
  word-break: break-all;
}
.order-detail .d-value.amount {
  font-weight: 600;
  color: #ff6b3d;
}
.order-detail .elder-section {
  border: 1px solid #ffd8bd;
}
.order-detail .elder-name {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
  margin-bottom: 4px;
}
.order-detail .elder-tag {
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 400;
  color: #ff6b3d;
  background: #fff1e6;
}
.order-detail .health-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff;
  border-left: 3px solid #ffb26b;
}
.order-detail .health-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #e6a23c;
}
.order-detail .health-icon {
  font-size: 14px;
}
.order-detail .health-text {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #5c564e;
  white-space: pre-wrap;
  word-break: break-all;
}

/* ============ 响应式 ============ */
@media (max-width: 1000px) {
  .sidebar {
    width: 64px;
  }
  .brand-text, .nav-label, .nav-tag, .side-foot {
    display: none;
  }
  .nav-item {
    justify-content: center;
    padding: 0;
  }
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
