<script setup>
// ============ 家属端我的评价：待评价 / 已评价 双区 ============
// 数据源：待评价 = GET /service-order/user/list 全量订单中「已完成(2) 且 commentScore 为 null」派生；
// 已评价 = GET /service-comment/my（后端已最新在前）
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { listUserOrders } from '@/api/order.js'
import { listMyComments } from '@/api/comment.js'
import OrderCommentDialog from '@/components/OrderCommentDialog.vue'

const router = useRouter()

/* ---------- 双区切换 ---------- */
const TAB_PENDING = 'pending'
const TAB_DONE = 'done'
const tab = ref(TAB_PENDING)

/* ---------- 数据加载（两接口一次并行） ---------- */
const loading = ref(false)
const orders = ref([])
const myComments = ref([])
const loadAll = async () => {
  loading.value = true
  try {
    const [o, c] = await Promise.all([listUserOrders(), listMyComments()])
    if (o.success) {
      orders.value = o.data || []
    } else {
      ElMessage.error(o.errorMsg || '加载订单失败')
    }
    if (c.success) {
      myComments.value = c.data || []
    } else {
      ElMessage.error(c.errorMsg || '加载评价失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ } finally {
    loading.value = false
  }
}

const pendingOrders = computed(() => orders.value.filter((o) => o.orderStatus === 2 && o.commentScore == null))

/* ---------- 评价对话框 ---------- */
const commentDialogRef = ref(null)
const commentTarget = ref(null)
const openComment = (row) => {
  commentTarget.value = row
  commentDialogRef.value?.open()
}
// 评完重拉：待评价区该单移走，已评价区出现新评
const onCommentSuccess = () => loadAll()

/* ---------- 时间/金额展示 ---------- */
const fmtDateTime = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(loadAll)
</script>

<template>
  <div class="comments-page">
    <!-- ======== 页头 ======== -->
    <div class="page-head">
      <el-button text :icon="ArrowLeft" @click="router.push('/user/profile')">返回</el-button>
      <h3 class="page-title">我的评价</h3>
    </div>

    <!-- ======== 双区切换 ======== -->
    <div class="filter-card">
      <el-radio-group v-model="tab" size="large">
        <el-radio-button :value="TAB_PENDING">
          待评价
          <span v-if="pendingOrders.length" class="status-count">{{ pendingOrders.length }}</span>
        </el-radio-button>
        <el-radio-button :value="TAB_DONE">
          已评价
          <span class="status-count">{{ myComments.length }}</span>
        </el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="content-list">
      <!-- ======== 待评价：已完成未评订单，可直接发起评价 ======== -->
      <template v-if="tab === TAB_PENDING">
        <section v-for="o in pendingOrders" :key="o.orderId" class="card-item">
          <div class="card-head">
            <span class="provider-name">{{ o.providerName || '服务商家' }}</span>
            <el-tag type="success" size="small" effect="light" round>已完成</el-tag>
          </div>
          <div class="card-body">
            <div class="item-line">
              <span class="item-name">{{ o.itemName || '服务已删除' }}</span>
              <span class="item-qty">× {{ o.quantity }}</span>
            </div>
            <div class="meta-line">
              <span class="meta-item">下单时间：{{ fmtDateTime(o.createTime) }}</span>
              <span v-if="o.elderName" class="meta-item">服务老人：{{ o.elderName }}</span>
            </div>
          </div>
          <div class="card-foot">
            <span class="amount">¥{{ o.totalPrice }}</span>
            <div class="actions">
              <el-button type="primary" plain size="small" @click="openComment(o)">去评价</el-button>
            </div>
          </div>
        </section>
        <el-empty v-if="!pendingOrders.length" description="暂无待评价订单" />
      </template>

      <!-- ======== 已评价：我的历史评价列表 ======== -->
      <template v-else>
        <section v-for="c in myComments" :key="c.commentId" class="card-item rated-card">
          <div class="card-head">
            <span class="provider-name">{{ c.providerName || '服务商家' }}</span>
            <span class="rated-time">{{ fmtDateTime(c.createTime) }}</span>
          </div>
          <div class="card-body">
            <div class="item-line">
              <span class="item-name">{{ c.itemName || '服务已删除' }}</span>
              <el-rate :model-value="c.score" disabled class="rated-stars" />
            </div>
            <div v-if="c.content" class="rated-content">{{ c.content }}</div>
            <div class="rated-empty-line">订单号：{{ c.orderNo || '—' }}</div>
          </div>
          <div class="card-foot">
            <el-button text type="primary" size="small" @click="router.push({ path: '/user/orders', query: { status: 2 } })">
              查看订单
            </el-button>
          </div>
        </section>
        <el-empty v-if="!myComments.length" description="暂无已发表的评价" />
      </template>
    </div>

    <!-- 评价对话框（待评价区共用） -->
    <OrderCommentDialog ref="commentDialogRef" :order="commentTarget" @success="onCommentSuccess" />
  </div>
</template>

<style scoped>
.comments-page {
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

/* ============ 双区切换 ============ */
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

/* ============ 卡片列表 ============ */
.content-list {
  margin-top: 16px;
  min-height: 200px;
}
.card-item {
  padding: 16px 20px;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(249, 109, 59, 0.06);
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
  align-items: center;
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

/* ============ 已评价卡 ============ */
.rated-stars {
  margin-left: auto;
  height: auto;
}
.rated-content {
  margin-top: 10px;
  font-size: 13px;
  color: #2d2a26;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}
.rated-empty-line {
  margin-top: 8px;
  font-size: 12px;
  color: #b9b2a8;
  font-family: Consolas, Monaco, monospace;
}
.rated-time {
  font-size: 12px;
  color: #a39c92;
}
</style>
