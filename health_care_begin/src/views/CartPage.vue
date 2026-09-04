<script setup>
// ============ 购物车页：商家分组 + 行勾选/改量/删除，勾选去结算（跨商家按行拆单） ============
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import { listCart, removeCartItem, updateCartQuantity } from '@/api/cart.js'

const router = useRouter()

const loading = ref(false)
const rows = ref([]) // 购物车行（平铺，商家分组见 computed groups）
const checked = ref(new Set()) // 已勾选 cartId（仅有效行可勾）
let inited = false // 首次加载默认全选有效行，之后刷新保持用户勾选

const load = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const result = await listCart()
    if (result.success) {
      rows.value = result.data || []
      const set = new Set(checked.value) // 保留仍勾选且仍有效的行（删除/下架后自动摘除）
      rows.value.forEach((r) => {
        if (!isValid(r) || !set.has(r.cartId)) set.delete(r.cartId)
      })
      if (!inited) {
        // 首次加载默认全选（仅有效行可勾，全选集合由后端真值驱动）
        rows.value.forEach((r) => { if (isValid(r)) set.add(r.cartId) })
        inited = true
      }
      checked.value = set
    } else {
      ElMessage.error(result.errorMsg || '获取购物车失败')
    }
  } catch (err) {
    ElMessage.error('获取购物车失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => load())

// 失效行：下架(itemStatus=0)标灰禁选；服务已删(null)同灰（FK 级联一般不会残留，兜底）
const isValid = (row) => row.itemStatus === 1
const invalidTag = (row) => {
  if (row.itemStatus === 0) return { type: 'warning', text: '已下架' }
  if (row.itemName == null) return { type: 'info', text: '服务已删除' }
  return { type: 'warning', text: '已失效' }
}

// 商家分组（保持加购顺序）：providerId -> { providerName, lines }
const groups = computed(() => {
  const map = []
  const keyOf = (r) => (r.providerId != null ? String(r.providerId) : 'unknown')
  const nameOf = (r) => r.providerName || '未知商家'
  for (const r of rows.value) {
    const k = keyOf(r)
    const g = map.find((m) => m.key === k)
    if (g) g.lines.push(r)
    else map.push({ key: k, providerName: nameOf(r), lines: [r] })
  }
  return map
})

// 有效行（全选/默认勾选用）
const validRows = computed(() => rows.value.filter(isValid))
const validCount = computed(() => validRows.value.length)
const checkedValidIds = computed(() => [...checked.value].filter((id) => rows.value.some((r) => r.cartId === id && isValid(r))))
const allChecked = computed(() => validCount.value > 0 && checkedValidIds.value.length === validCount.value)

const toggle = (row) => {
  const set = new Set(checked.value)
  if (set.has(row.cartId)) set.delete(row.cartId)
  else if (isValid(row)) set.add(row.cartId)
  checked.value = set
}
const toggleAll = () => {
  const set = new Set(checked.value)
  if (allChecked.value) {
    validRows.value.forEach((r) => set.delete(r.cartId))
  } else {
    validRows.value.forEach((r) => set.add(r.cartId))
  }
  checked.value = set
}

// 金额：行小计 = 单价 × 数量；合计 = 勾选有效行小计
const lineTotal = (row) => ((Number(row.price) || 0) * (row.quantity || 0)).toFixed(2)
const totalAmount = computed(() =>
  rows.value
    .filter((r) => checked.value.has(r.cartId) && isValid(r))
    .reduce((sum, r) => sum + Number(lineTotal(r)), 0)
    .toFixed(2))
const checkoutCount = computed(() => checkedValidIds.value.length)

// 改数量：变更即 PUT；失败（含清空输入）回滚为后端真值
const changingId = ref(null)
const onQuantityChange = async (row, val) => {
  if (changingId.value != null) return
  if (!val || val < 1 || val > 99) {
    ElMessage.warning('数量需在 1~99 之间')
    await load(true)
    return
  }
  changingId.value = row.cartId
  try {
    const result = await updateCartQuantity(row.cartId, val)
    if (result.success) {
      row.quantity = val
    } else {
      ElMessage.error(result.errorMsg || '修改数量失败')
      await load(true)
    }
  } catch (err) {
    ElMessage.error('修改数量失败，请稍后重试')
    await load(true)
  } finally {
    changingId.value = null
  }
}

// 删行（确认后删除，删除可能影响勾选，统一回源）
const onRemove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定将「${row.itemName || '该服务'}」移出购物车吗？`, '移出购物车', {
      confirmButtonText: '移出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch (err) {
    return // 用户取消
  }
  try {
    const result = await removeCartItem(row.cartId)
    if (result.success) {
      ElMessage.success('已移出购物车')
      await load(true)
    } else {
      ElMessage.error(result.errorMsg || '删除失败')
    }
  } catch (err) {
    ElMessage.error('删除失败，请稍后重试')
  }
}

// 去结算：勾选行 cartId 透传确认页（cartIds 模式），跨商家按行拆单在后端一次完成
const goCheckout = () => {
  if (!checkoutCount.value) {
    ElMessage.warning('请先勾选要结算的服务')
    return
  }
  router.push({ path: '/user/order', query: { cartIds: checkedValidIds.value.join(',') } })
}
</script>

<template>
  <div class="container">
    <!-- ======== 页头 ======== -->
    <div class="page-head">
      <el-button text :icon="ArrowLeft" @click="router.push('/user/home')">返回</el-button>
      <h3 class="page-title">我的购物车</h3>
      <span v-if="rows.length" class="head-tip">跨商家可同时加购，结算时按服务逐单生成</span>
    </div>

    <!-- ======== 空态 ======== -->
    <el-empty v-if="!loading && !rows.length" description="购物车还是空的，去逛逛吧">
      <el-button type="primary" round @click="router.push('/user/home')">去逛逛</el-button>
    </el-empty>

    <!-- ======== 商家分组列表 ======== -->
    <div v-else v-loading="loading" class="list">
      <div v-for="g in groups" :key="g.key" class="merchant-card">
        <div class="merchant-head">
          <span class="merchant-logo">{{ (g.providerName || '商').slice(0, 1) }}</span>
          <span class="merchant-name">{{ g.providerName }}</span>
          <span class="merchant-count">{{ g.lines.length }} 项</span>
        </div>

        <div v-for="row in g.lines" :key="row.cartId" class="cart-row" :class="{ 'row-invalid': !isValid(row) }">
          <el-checkbox
            :model-value="checked.has(row.cartId)"
            :disabled="!isValid(row)"
            class="row-check"
            @change="toggle(row)"
          />
          <div class="row-main">
            <div class="row-name">
              {{ row.itemName || '服务已删除' }}
              <el-tag v-if="!isValid(row)" :type="invalidTag(row).type" size="small" class="row-tag">{{ invalidTag(row).text }}</el-tag>
            </div>
            <div class="row-meta">
              <span class="row-price">¥{{ row.price }}<i>/{{ row.unit }}</i></span>
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="99"
                size="small"
                class="row-qty"
                :disabled="!isValid(row) || changingId != null"
                @change="(val) => onQuantityChange(row, val)"
              />
              <span class="row-subtotal">小计 <b>¥{{ lineTotal(row) }}</b></span>
            </div>
          </div>
          <el-button
            text
            :icon="Delete"
            class="row-del"
            :disabled="changingId != null"
            @click="onRemove(row)"
          />
        </div>
      </div>
    </div>

    <!-- ======== 吸底结算栏 ======== -->
    <div v-if="rows.length" class="checkout-bar">
      <el-checkbox v-if="validCount > 0" :model-value="allChecked" class="check-all" @change="toggleAll">全选</el-checkbox>
      <span v-else class="check-all-placeholder" />
      <div class="bar-total">
        已选 <b class="total-num">{{ checkoutCount }}</b> 项 · 合计：<b class="total-amount">¥{{ totalAmount }}</b>
      </div>
      <el-button
        type="primary"
        round
        size="large"
        class="checkout-btn"
        :disabled="!checkoutCount"
        @click="goCheckout"
      >去结算({{ checkoutCount }})</el-button>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 110px;
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
.head-tip {
  font-size: 12px;
  color: #c0b9ae;
}

/* ============ 商家分组卡 ============ */
.list {
  margin-top: 16px;
  min-height: 200px;
}
.merchant-card {
  margin-bottom: 16px;
  padding: 0 20px 4px;
  background: #fff;
  border-radius: 16px;
}
.merchant-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 4px;
  border-bottom: 1px dashed #f4f0eb;
}
.merchant-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 14px;
}
.merchant-name {
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
}
.merchant-count {
  margin-left: auto;
  font-size: 12px;
  color: #c0b9ae;
}

/* ============ 购物车行 ============ */
.cart-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 4px;
}
.cart-row + .cart-row {
  border-top: 1px dashed #f4f0eb;
}
.row-check {
  flex-shrink: 0;
}
.row-main {
  flex: 1;
  min-width: 0;
}
.row-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #2d2a26;
}
.row-tag {
  flex-shrink: 0;
}
.row-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 8px;
  font-size: 13px;
  color: #a39c92;
}
.row-price b {
  color: #ff6b3d;
}
.row-price i {
  font-style: normal;
}
.row-qty {
  width: 120px;
}
.row-subtotal {
  margin-left: auto;
  font-size: 13px;
}
.row-subtotal b {
  font-size: 15px;
  color: #ff6b3d;
}
.row-del {
  color: #c0b9ae;
  flex-shrink: 0;
}
.row-del:hover {
  color: #f56c6c;
}

/* 失效行：整行标灰禁选 */
.row-invalid .row-name {
  color: #c0b9ae;
}
.row-invalid .row-price,
.row-invalid .row-subtotal b {
  color: #c0b9ae;
}

/* ============ 吸底结算栏 ============ */
.checkout-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px max(24px, calc((100% - 1200px) / 2 + 24px));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(6px);
  box-shadow: 0 -2px 12px rgba(249, 109, 59, 0.1);
}
.check-all {
  margin-right: auto;
}
.check-all-placeholder {
  flex: 1;
}
.bar-total {
  font-size: 14px;
  color: #5c564e;
}
.bar-total .total-num {
  color: #ff6b3d;
}
.bar-total .total-amount {
  font-size: 22px;
  color: #ff6b3d;
}
.checkout-btn {
  width: 180px;
  margin: 0;
}
</style>
