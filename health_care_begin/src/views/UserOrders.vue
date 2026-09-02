<script setup>
// ============ 家属端我的订单：状态筛选 + 订单卡片 + 详情抽屉 + 取消待接单订单(0→3) ============
// 数据源 GET /service-order/user/list（全量拉取、前端按状态筛选，与商家端订单 Tab 同模式）
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import { listUserOrders, cancelOrder } from '@/api/order.js'

const route = useRoute()
const router = useRouter()

/* ---------- 状态筛选（角标数字由全量列表实时聚合） ---------- */
const ALL_STATUS = -1 // 筛选哨兵：全部
const orderStatusFilter = ref(ALL_STATUS)
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
const statusText = (s) => orderStatusMap[s]?.text || '未知'

/* ---------- 订单列表 ---------- */
const loading = ref(false)
const orders = ref([])
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await listUserOrders()
    if (res.success) {
      orders.value = res.data || []
    } else {
      ElMessage.error(res.errorMsg || '加载订单失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ } finally {
    loading.value = false
  }
}

const filteredOrders = computed(() =>
  orderStatusFilter.value === ALL_STATUS
    ? orders.value
    : orders.value.filter((o) => o.orderStatus === orderStatusFilter.value),
)
const orderCounts = computed(() => {
  const counts = { 0: 0, 1: 0, 2: 0, 3: 0 }
  orders.value.forEach((o) => { if (counts[o.orderStatus] !== undefined) counts[o.orderStatus] += 1 })
  return counts
})

/* ---------- 时间/金额展示 ---------- */
const fmtDateTime = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/* ---------- 订单详情抽屉（列表行已含全部展示字段，无需单独详情接口） ---------- */
const drawerVisible = ref(false)
const currentOrder = ref(null)
const openOrderDetail = (row) => {
  currentOrder.value = row
  drawerVisible.value = true
}

/* ---------- 取消订单（仅 0待接单可取消，后端兜底） ---------- */
const cancelingId = ref(null)
const onCancel = (row) => {
  ElMessageBox.confirm(
    `确定取消订单「${row.itemName || '服务'}」吗？取消后如需服务请重新下单。`,
    '取消订单确认',
    { confirmButtonText: '取消订单', cancelButtonText: '再想想', type: 'warning' },
  ).then(async () => {
    cancelingId.value = row.orderId
    try {
      const res = await cancelOrder(row.orderId)
      if (res.success) {
        ElMessage.success(res.data || '订单已取消')
        drawerVisible.value = false
        loadOrders()
      } else {
        ElMessage.error(res.errorMsg || '取消失败')
      }
    } finally {
      cancelingId.value = null
    }
  }).catch(() => {})
}

onMounted(() => {
  // 从个人中心状态条跳入：/user/orders?status=N 定位到对应筛选
  const s = Number(route.query.status)
  if (orderStatusTabs.some((t) => t.value === s)) orderStatusFilter.value = s
  loadOrders()
})
</script>

<template>
  <div class="orders-page">
    <!-- ======== 页头 ======== -->
    <div class="page-head">
      <el-button text :icon="ArrowLeft" @click="router.push('/user/profile')">返回</el-button>
      <h3 class="page-title">我的订单</h3>
    </div>

    <!-- ======== 状态筛选 ======== -->
    <div class="filter-card">
      <el-radio-group v-model="orderStatusFilter" size="large">
        <el-radio-button v-for="t in orderStatusTabs" :key="t.value" :value="t.value">
          {{ t.label }}
          <span class="status-count">{{ t.value === ALL_STATUS ? orders.length : orderCounts[t.value] }}</span>
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- ======== 订单卡片列表 ======== -->
    <div v-loading="loading" class="order-list">
      <section
        v-for="o in filteredOrders"
        :key="o.orderId"
        class="order-card"
        @click="openOrderDetail(o)"
      >
        <!-- 卡片头：商家 + 状态 -->
        <div class="card-head">
          <span class="provider-name">{{ o.providerName || '服务商家' }}</span>
          <el-tag :type="orderStatusMap[o.orderStatus]?.type" size="small" effect="light" round>
            {{ statusText(o.orderStatus) }}
          </el-tag>
        </div>

        <!-- 卡片体：服务项目 + 老人 + 时间 -->
        <div class="card-body">
          <div class="item-line">
            <span class="item-name">{{ o.itemName || '服务已删除' }}</span>
            <span class="item-qty">× {{ o.quantity }}</span>
          </div>
          <div class="meta-line">
            <span v-if="o.elderName" class="meta-item">服务老人：{{ o.elderName }}</span>
            <span class="meta-item">下单时间：{{ fmtDateTime(o.createTime) }}</span>
          </div>
        </div>

        <!-- 卡片脚：金额 + 操作 -->
        <div class="card-foot">
          <span class="amount">¥{{ o.totalPrice }}</span>
          <div class="actions" @click.stop>
            <el-button v-if="o.orderStatus === 0" type="danger" plain size="small" :loading="cancelingId === o.orderId" @click="onCancel(o)">
              取消订单
            </el-button>
            <el-button type="primary" plain size="small" @click="openOrderDetail(o)">查看详情</el-button>
          </div>
        </div>
      </section>

      <el-empty v-if="!loading && !filteredOrders.length" description="暂无相关订单" />
    </div>

    <!-- ======== 订单详情抽屉 ======== -->
    <el-drawer v-model="drawerVisible" title="订单详情" size="460px">
      <div v-if="currentOrder" class="order-detail">
        <!-- 服务信息 -->
        <div class="detail-section">
          <div class="section-title">
            服务信息
            <el-tag :type="orderStatusMap[currentOrder.orderStatus]?.type" size="small" effect="light" round>
              {{ statusText(currentOrder.orderStatus) }}
            </el-tag>
          </div>
          <div class="d-row"><span class="d-label">服务商家</span><span class="d-value">{{ currentOrder.providerName || '服务商家' }}</span></div>
          <div class="d-row"><span class="d-label">服务项目</span><span class="d-value">{{ currentOrder.itemName || '服务已删除' }}</span></div>
          <div class="d-row"><span class="d-label">单价 × 数量</span><span class="d-value">¥{{ currentOrder.unitPrice }} × {{ currentOrder.quantity }}</span></div>
          <div class="d-row"><span class="d-label">订单金额</span><span class="d-value amount">¥{{ currentOrder.totalPrice }}</span></div>
          <div class="d-row"><span class="d-label">订单号</span><span class="d-value order-no-cell">{{ currentOrder.orderNo }}</span></div>
          <div class="d-row"><span class="d-label">下单时间</span><span class="d-value">{{ fmtDateTime(currentOrder.createTime) }}</span></div>
        </div>

        <!-- 服务老人 -->
        <div class="detail-section">
          <div class="section-title">服务老人</div>
          <div class="d-row">
            <span class="d-label">服务老人</span>
            <span class="d-value">{{ currentOrder.elderName || '档案已删除' }}</span>
          </div>
        </div>

        <!-- 服务安排 -->
        <div class="detail-section">
          <div class="section-title">服务安排</div>
          <div class="d-row"><span class="d-label">预约时间</span><span class="d-value">{{ currentOrder.serviceTime ? fmtDateTime(currentOrder.serviceTime) : '未预约（下单后沟通）' }}</span></div>
          <div class="d-row"><span class="d-label">服务地址</span><span class="d-value">{{ currentOrder.addressText || '未选择（下单后电话沟通）' }}</span></div>
          <div class="d-row"><span class="d-label">联系电话</span><span class="d-value">{{ currentOrder.contactPhone }}</span></div>
          <div class="d-row"><span class="d-label">订单备注</span><span class="d-value">{{ currentOrder.remark || '无' }}</span></div>
        </div>

        <!-- 待接单取消提示 -->
        <div v-if="currentOrder.orderStatus === 0" class="cancel-tip">
          <el-icon class="tip-icon"><WarningFilled /></el-icon>
          <span>商家尚未接单，如不再需要可取消订单</span>
        </div>
      </div>

      <template #footer>
        <div class="drawer-actions">
          <el-button v-if="currentOrder?.orderStatus === 0" type="danger" plain size="large" :loading="cancelingId === currentOrder?.orderId" @click="onCancel(currentOrder)">
            取消订单
          </el-button>
          <el-tag v-if="currentOrder?.orderStatus === 1" type="primary" size="large">服务进行中</el-tag>
          <el-tag v-else-if="currentOrder?.orderStatus === 2" type="success" size="large">服务已完成</el-tag>
          <el-tag v-else-if="currentOrder?.orderStatus === 3" type="info" size="large">订单已取消</el-tag>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 24px 60px;
}

/* ============ 页头 ============ */
.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 0 8px;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #2d2a26;
}

/* ============ 状态筛选 ============ */
.filter-card {
  margin-top: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(249, 109, 59, 0.06);
}
.filter-card :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.filter-card :deep(.el-radio-button__inner) {
  border-radius: 999px;
  border-color: #f0e6dc;
  box-shadow: none;
}
.filter-card :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  border-color: #ff7a45;
}
.status-count {
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

/* ============ 订单卡片 ============ */
.order-list {
  margin-top: 16px;
  min-height: 200px;
}
.order-card {
  padding: 16px 20px;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(249, 109, 59, 0.06);
  cursor: pointer;
  transition: all 0.2s;
}
.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(249, 109, 59, 0.12);
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px dashed #f5efe9;
}
.provider-name {
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
}
.card-body {
  padding: 12px 0 4px;
}
.item-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.item-name {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
}
.item-qty {
  font-size: 13px;
  color: #a39c92;
}
.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 8px;
}
.meta-item {
  font-size: 12px;
  color: #a39c92;
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
}
.amount {
  font-size: 20px;
  font-weight: 700;
  color: #ff6b3d;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ============ 订单详情抽屉 ============ */
.order-detail .detail-section {
  padding: 16px 18px;
  margin-bottom: 14px;
  background: #fdf9f5;
  border-radius: 12px;
}
.order-detail .section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
.order-detail .order-no-cell {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
.order-detail .cancel-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7e6;
  color: #e6a23c;
  font-size: 13px;
}
.order-detail .tip-icon {
  font-size: 14px;
}
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
}
</style>
