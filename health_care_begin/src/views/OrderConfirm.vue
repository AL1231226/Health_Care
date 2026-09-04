<script setup>
// ============ 下单确认页（双模式） ============
// 单服务模式：?itemId= 从商家详情页进入；服务数量自选，吸底「加入购物车 / 提交订单」
// 批量模式：?cartIds=1,2,3 从购物车页勾选进入；按商家分组只读清单，整批统一填老人/地址等，后端按行拆单一次生成多张订单
// 老人/地址已接真实接口；提交订单已接后端
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, SuccessFilled } from '@element-plus/icons-vue'
import { listElder } from '@/api/elder.js'
import { listAddress } from '@/api/address.js'
import { createOrder } from '@/api/order.js'
import { addToCart, checkoutCart, listCart } from '@/api/cart.js'

const route = useRoute()
const router = useRouter()

// 当前登录家属（登录时写入 localStorage 的 user_info，含完整手机号，剔除密码字段）
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')

// ============ 模式判定：route.query.cartIds 存在即批量结算模式，否则单服务模式（保留原逻辑） ============
const cartIdsStr = typeof route.query.cartIds === 'string' ? route.query.cartIds : ''
const isBatch = cartIdsStr.trim() !== ''

// ============ 单服务模式：从商家详情页带过来的服务展示字段（无参数直接访问时兜底示例数据） ============
const item = {
  itemId: route.query.itemId || 1,
  itemName: route.query.itemName || '午餐：三荤一素',
  price: Number(route.query.price || 20),
  unit: route.query.unit || '次',
  duration: route.query.duration || '无',
  providerName: route.query.providerName || '示例商家',
}
const fromQuery = !!route.query.itemId // 是否从详情页正常跳入（否则显示示例数据提示）

// ============ 批量模式状态：进入时拉购物车按 cartIds 过滤重建，失效/已被移除整批拦下 ============
const batchLoading = ref(false)
const batchError = ref('') // 非空则整页替换为错误空态（阻止提交）
const batchRows = ref([]) // 通过校验的勾选行（顺序 = URL cartIds 顺序）
const wantedCartIds = computed(() =>
  cartIdsStr.split(',').map((s) => Number(s.trim())).filter((n) => Number.isInteger(n) && n > 0))

const loadBatch = async () => {
  if (!isBatch) return
  batchLoading.value = true
  try {
    const result = await listCart()
    if (!result.success) {
      batchError.value = result.errorMsg || '获取购物车失败，请稍后重试'
      return
    }
    const all = result.data || []
    const wanted = wantedCartIds.value
    const matched = wanted.map((id) => all.find((r) => r.cartId === id)).filter(Boolean)
    // 任一勾选行已被移除/结算(cartId 未命中)或已下架失效 → 整批拦下，回购物车处理
    const invalidHit = matched.some((r) => r.itemStatus !== 1)
    if (!wanted.length || matched.length !== wanted.length || invalidHit) {
      batchError.value = '有服务已失效或已结算，请回购物车重新勾选'
      return
    }
    batchRows.value = matched
  } catch (err) {
    batchError.value = '获取购物车失败，请稍后重试'
  } finally {
    batchLoading.value = false
  }
}

// 批量清单按商家分组（保持勾选顺序；同商家多项目将生成多张订单，逐行标注）
const batchGroups = computed(() => {
  const list = []
  for (const r of batchRows.value) {
    const k = r.providerId != null ? String(r.providerId) : 'unknown'
    const g = list.find((m) => m.key === k)
    if (g) g.lines.push(r)
    else list.push({ key: k, providerName: r.providerName || '未知商家', lines: [r] })
  }
  return list
})

// ============ 老人档案（服务对象，下单 elder_id；服务地址以其常驻地址为默认） ============
const elders = ref([])
const elderId = ref(null) // 当前选中老人
const bootstrapDone = ref(false) // 老人/地址首次加载完成后才允许联动

const genderText = (g) => (Number(g) === 1 ? '男' : '女')
const ageOf = (d) => {
  if (!d) return null
  const birth = new Date(d)
  if (Number.isNaN(birth.getTime())) return null
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  if (now.getMonth() < birth.getMonth() || (now.getMonth() === birth.getMonth() && now.getDate() < birth.getDate())) age -= 1
  return age
}
const elderLabel = (e) => {
  const age = ageOf(e.birthDate)
  return `${e.elderName}（${genderText(e.gender)}${age != null ? ` · ${age}岁` : ''}）`
}

const loadElders = async () => {
  try {
    const result = await listElder()
    if (result.success) {
      elders.value = result.data || []
      // 默认选第一位老人
      if (elders.value.length) elderId.value = elders.value[0].elderId
    } else {
      ElMessage.error(result.errorMsg || '获取老人档案失败')
    }
  } catch (err) {
    ElMessage.error('获取老人档案失败，请稍后重试')
  }
}

// ============ 服务地址（下拉可改选，默认 = 所选老人的常驻地址） ============
const addressOptions = ref([]) // { addrId, text }
const addressId = ref(null)

const loadAddresses = async () => {
  try {
    const result = await listAddress()
    if (result.success) {
      addressOptions.value = (result.data || []).map((a) => ({
        addrId: a.addrId,
        text: [a.province, a.city, a.district, a.detailAddr].filter(Boolean).join(''),
      }))
    } else {
      ElMessage.error(result.errorMsg || '获取服务地址失败')
    }
  } catch (err) {
    ElMessage.error('获取服务地址失败，请稍后重试')
  }
}

// 服务地址跟随所选老人的常驻地址（老人常驻地址不在地址簿中则清空待选）
const applyElderDefaultAddress = () => {
  const elder = elders.value.find((e) => e.elderId === elderId.value)
  addressId.value =
    elder && addressOptions.value.some((a) => a.addrId === elder.addrId) ? elder.addrId : null
}

const onElderChange = () => {
  // 切换服务老人：服务地址默认重新跟随新老人的常驻地址（之后可手动改选）
  applyElderDefaultAddress()
}

onMounted(async () => {
  await Promise.all([loadElders(), loadAddresses()])
  await loadBatch()
  // 初次联动（老人列表/地址簿加载完成后，默认地址才生效）
  bootstrapDone.value = true
  if (elderId.value) applyElderDefaultAddress()
})

// ============ 表单其余项 ============
const quantity = ref(1) // 单服务模式专属
const serviceTime = ref(null) // 预约时间（可空，两种模式共用）
const contactPhone = ref(userInfo.phone || '') // 默认取登录用户手机号（完整显示，可改）
const remark = ref('')

// 价格合计：单服务 = 单价×数量；批量 = Σ(单价×数量)
const totalPrice = computed(() => {
  if (!isBatch) return (item.price * quantity.value).toFixed(2)
  return batchRows.value
    .reduce((sum, r) => sum + (Number(r.price) || 0) * (r.quantity || 0), 0)
    .toFixed(2)
})
const batchAmount = (r) => ((Number(r.price) || 0) * (r.quantity || 0)).toFixed(2)

// 无老人档案：无法指明服务对象与默认地址，阻止提交
const noElder = computed(() => bootstrapDone.value && elders.value.length === 0)

/* ============ 提交订单 ============ */
const submitting = ref(false)
const createdOrder = ref(null) // 单服务模式：下单成功后整页切换为成功卡
const createdOrders = ref(null) // 批量模式：结算成功后返回的多单展示 VO 列表
const currentElder = computed(() => elders.value.find((e) => e.elderId === elderId.value) || null)

// 提交前统一校验（老人必选、联系电话必填格式；批量模式整批共用一份）
const preSubmitCheck = () => {
  if (!elderId.value) {
    ElMessage.warning('请先选择服务老人')
    return false
  }
  const phone = contactPhone.value.trim()
  if (!phone) {
    ElMessage.warning('请填写联系电话')
    return false
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('联系电话格式不正确')
    return false
  }
  return true
}

const sharedPayload = () => ({
  elderId: elderId.value,
  serviceTime: serviceTime.value || null,
  addressId: addressId.value || null,
  contactPhone: contactPhone.value.trim(),
  remark: remark.value.trim(),
})

const onSubmit = async () => {
  if (submitting.value || createdOrder.value || createdOrders.value) return
  if (!preSubmitCheck()) return

  submitting.value = true
  try {
    if (isBatch) {
      const result = await checkoutCart({ cartIds: batchRows.value.map((r) => r.cartId), ...sharedPayload() })
      if (result.success) {
        createdOrders.value = result.data || []
      } else {
        ElMessage.error(result.errorMsg || '结算失败，请稍后重试')
      }
    } else {
      const result = await createOrder({
        itemId: Number(item.itemId),
        quantity: quantity.value,
        ...sharedPayload(),
      })
      if (result.success) {
        createdOrder.value = result.data
      } else {
        ElMessage.error(result.errorMsg || '下单失败，请稍后重试')
      }
    }
  } catch (err) {
    // 网络异常/401 由 request.js 统一处理
    ElMessage.error(isBatch ? '结算失败，请稍后重试' : '下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 加入购物车（单服务模式专属；已接后端：POST /service-cart/add，数量 = 当前所选）
const addingCart = ref(false)
const onAddCart = async () => {
  if (addingCart.value) return
  addingCart.value = true
  try {
    const result = await addToCart(Number(item.itemId), quantity.value)
    if (result.success) {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(result.errorMsg || '加入购物车失败')
    }
  } catch (err) {
    ElMessage.error('加入购物车失败，请稍后重试')
  } finally {
    addingCart.value = false
  }
}

/* ============ 成功卡数据：单服务/批量统一成“订单清单” ============ */
const successOrders = computed(() => {
  if (isBatch) {
    return (createdOrders.value || []).map((o) => ({ orderNo: o.orderNo, itemName: o.itemName, totalPrice: o.totalPrice }))
  }
  const o = createdOrder.value
  return o ? [{ orderNo: o.orderNo, itemName: item.itemName, totalPrice: o.totalPrice }] : []
})
const successTotal = computed(() =>
  successOrders.value.reduce((sum, o) => sum + (Number(o.totalPrice) || 0), 0).toFixed(2))
</script>

<template>
  <!-- ======== 下单成功卡（提交成功后整页切换；批量模式逐单列出各订单号） ======== -->
  <div v-if="createdOrder || createdOrders" class="container success-page">
    <div class="success-icon"><el-icon :size="64"><SuccessFilled /></el-icon></div>
    <h3 class="success-title">下单成功</h3>
    <p class="success-sub">
      <template v-if="isBatch">已为您按服务生成 {{ successOrders.length }} 张订单，各商家接单后将联系您安排上门服务</template>
      <template v-else>商家接单后将联系您安排上门服务，请保持电话畅通</template>
    </p>

    <div class="success-card">
      <div class="success-row" v-if="currentElder">
        <span class="label">服务老人</span>
        <span class="value">{{ elderLabel(currentElder) }}</span>
      </div>
      <div v-for="(o, idx) in successOrders" :key="o.orderNo" class="success-row success-line">
        <span class="label">{{ o.itemName }}<i v-if="isBatch" class="line-idx">{{ idx + 1 }} / {{ successOrders.length }}</i></span>
        <span class="value">
          <span class="order-no">{{ o.orderNo }}</span>
          <span class="amount">¥{{ o.totalPrice }}</span>
        </span>
      </div>
      <div class="success-row">
        <span class="label">订单金额</span>
        <span class="value amount">¥{{ successTotal }}</span>
      </div>
      <div class="success-row">
        <span class="label">订单状态</span>
        <span class="value status">待商家接单</span>
      </div>
    </div>

    <div class="success-actions">
      <el-button type="primary" round size="large" class="back-btn" @click="router.push('/user/home')">返回首页</el-button>
      <el-button round size="large" class="view-orders-btn" @click="router.push('/user/orders')">查看我的订单</el-button>
    </div>
    <p class="success-tip">订单待商家接单，可随时在「我的订单」查看进度或取消</p>
  </div>

  <!-- ======== 确认下单（正常表单） ======== -->
  <div v-else class="container">
    <!-- ======== 页头 ======== -->
    <div class="page-head">
      <el-button text :icon="ArrowLeft" @click="isBatch ? router.push('/user/cart') : router.back()">返回</el-button>
      <h3 class="page-title">确认订单</h3>
    </div>

    <!-- 批量模式：勾选行已失效/已结算 → 整页空态拦下，回购物车处理 -->
    <el-empty
      v-if="isBatch && batchError"
      :description="batchError"
      class="batch-empty"
    >
      <el-button type="primary" round @click="router.push('/user/cart')">返回购物车</el-button>
    </el-empty>

    <template v-else>
      <!-- ======== 服务信息卡（单服务：行内展示；批量：按商家分组只读清单） ======== -->
      <div v-if="isBatch" class="card" v-loading="batchLoading">
        <div class="card-head">
          <span class="card-title">服务清单（已选 {{ batchRows.length }} 项）</span>
          <span class="card-note">结算将按服务逐单生成订单</span>
        </div>
        <div v-for="g in batchGroups" :key="g.key" class="merchant-group">
          <div class="group-head">
            <span>{{ g.providerName }}</span>
            <span class="group-note">该商家将生成 {{ g.lines.length }} 张订单</span>
          </div>
          <div v-for="line in g.lines" :key="line.cartId" class="group-line">
            <span class="line-name">{{ line.itemName }}</span>
            <span class="line-meta">¥{{ line.price }} × {{ line.quantity }} {{ line.unit }}</span>
            <b class="line-amount">¥{{ batchAmount(line) }}</b>
          </div>
        </div>
        <div class="card-total">
          合计：<b>¥{{ totalPrice }}</b>
        </div>
      </div>
      <div v-else class="card">
        <div class="card-head">
          <span class="card-title">服务信息</span>
          <el-alert v-if="!fromQuery" title="当前为示例数据（请从商家详情页点购买进入）" type="warning" :closable="false" show-icon class="sample-tip" />
        </div>
        <div class="item-info">
          <div class="item-name">{{ item.itemName }}</div>
          <div class="item-meta">
            <span>{{ item.providerName }}</span>
            <i>·</i>
            <span>时长 {{ item.duration }}</span>
          </div>
          <div class="item-price">
            <b>¥{{ item.price }}</b>
            <i>起/{{ item.unit }}</i>
          </div>
        </div>
      </div>

      <!-- ======== 预约信息表单（两种模式共用：老人/地址/电话/预约时间/备注整批统一） ======== -->
      <div class="card">
        <div class="card-head">
          <span class="card-title">预约信息</span>
          <span v-if="isBatch" class="card-note">整批订单统一填写</span>
        </div>
        <!-- 无老人档案：引导先去老人管理添加 -->
        <el-alert
          v-if="noElder"
          title="暂无老人档案，请先在「老人管理」中添加服务老人"
          type="warning"
          :closable="false"
          show-icon
          class="no-elder-tip"
        >
          <el-button link type="warning" @click="router.push('/user/elder')">去添加老人档案</el-button>
        </el-alert>

        <el-form label-width="90px" label-position="left">
          <!-- 服务老人：多老人档案时需指明为谁服务（下单 elder_id） -->
          <el-form-item label="服务老人">
            <el-select
              v-model="elderId"
              placeholder="请选择服务老人"
              size="large"
              class="full-width"
              :disabled="noElder"
              @change="onElderChange"
            >
              <el-option v-for="e in elders" :key="e.elderId" :value="e.elderId" :label="elderLabel(e)" />
            </el-select>
          </el-form-item>

          <!-- 服务数量：仅单服务模式自选（批量模式份数已在购物车定好） -->
          <el-form-item v-if="!isBatch" label="服务数量">
            <el-input-number v-model="quantity" :min="1" :max="99" size="large" />
            <span class="form-hint">{{ item.unit }}为单位</span>
          </el-form-item>

          <el-form-item label="预约时间">
            <el-date-picker
              v-model="serviceTime"
              type="datetime"
              placeholder="选择预约服务时间（可选）"
              size="large"
              :disabled-date="(d) => d.getTime() < Date.now() - 86400000"
            />
          </el-form-item>

          <el-form-item label="服务地址">
            <el-select
              v-model="addressId"
              placeholder="请选择服务地址（不选则下单后电话沟通）"
              size="large"
              clearable
              class="full-width"
              :disabled="noElder"
            >
              <el-option v-for="a in addressOptions" :key="a.addrId" :value="a.addrId" :label="a.text" />
            </el-select>
            <span class="form-hint">默认取所选老人的常驻地址，可改选地址簿中其他地址</span>
          </el-form-item>

          <el-form-item label="联系电话">
            <el-input v-model="contactPhone" size="large" class="phone-input" placeholder="下单预留联系电话" />
          </el-form-item>

          <el-form-item label="订单备注">
            <el-input v-model="remark" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="给商家留言，如忌口、具体地址等（可选）" />
          </el-form-item>
        </el-form>
      </div>

      <!-- ======== 吸底提交栏 ======== -->
      <div class="submit-bar">
        <div class="total">
          合计：<b>¥{{ totalPrice }}</b>
          <span v-if="isBatch" class="total-note">共 {{ batchRows.length }} 张订单</span>
        </div>
        <div class="actions">
          <el-button
            v-if="!isBatch"
            class="cart-btn"
            round
            size="large"
            :disabled="noElder || !elderId"
            :loading="addingCart"
            @click="onAddCart"
          >加入购物车</el-button>
          <el-button
            type="primary"
            round
            size="large"
            class="submit-btn"
            :disabled="noElder || !elderId"
            :loading="submitting"
            @click="onSubmit"
          >{{ isBatch ? `提交订单（将生成 ${batchRows.length} 张订单）` : '提交订单' }}</el-button>
        </div>
      </div>
    </template>
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
.batch-empty {
  margin-top: 40px;
}

/* ============ 卡片通用 ============ */
.card {
  margin-top: 16px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
}
.card-note {
  font-size: 12px;
  color: #c0b9ae;
}
.sample-tip {
  padding: 4px 12px;
}
.no-elder-tip {
  margin-bottom: 16px;
}

/* ============ 批量服务清单（按商家分组） ============ */
.merchant-group + .merchant-group {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #f4f0eb;
}
.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #2d2a26;
}
.group-note {
  font-size: 12px;
  font-weight: 400;
  color: #ff8a4c;
}
.group-line {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0 8px 12px;
  font-size: 14px;
  color: #2d2a26;
}
.line-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.line-meta {
  flex-shrink: 0;
  font-size: 13px;
  color: #a39c92;
}
.line-amount {
  flex-shrink: 0;
  font-size: 14px;
  color: #ff6b3d;
}
.card-total {
  padding-top: 14px;
  margin-top: 10px;
  border-top: 1px dashed #f4f0eb;
  text-align: right;
  font-size: 14px;
  color: #5c564e;
}
.card-total b {
  font-size: 20px;
  color: #ff6b3d;
}

/* ============ 服务信息（单服务模式） ============ */
.item-name {
  font-size: 18px;
  font-weight: 600;
  color: #2d2a26;
}
.item-meta {
  margin-top: 8px;
  font-size: 13px;
  color: #a39c92;
}
.item-meta i {
  margin: 0 6px;
  font-style: normal;
}
.item-price {
  margin-top: 10px;
}
.item-price b {
  font-size: 24px;
  color: #ff6b3d;
}
.item-price i {
  margin-left: 2px;
  font-size: 13px;
  font-style: normal;
  color: #a39c92;
}

/* ============ 表单 ============ */
.full-width {
  width: 100%;
}
.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #c0b9ae;
}
.phone-input {
  width: 320px;
}

/* ============ 吸底提交栏 ============ */
.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
  padding: 14px max(24px, calc((100% - 1200px) / 2 + 24px));
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(6px);
  box-shadow: 0 -2px 12px rgba(249, 109, 59, 0.1);
}
.total {
  font-size: 15px;
  color: #5c564e;
}
.total b {
  font-size: 24px;
  color: #ff6b3d;
}
.total-note {
  margin-left: 10px;
  font-size: 12px;
  color: #a39c92;
}
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cart-btn {
  width: 150px;
  color: #ff6b3d;
  border-color: #ff6b3d;
  background: #fff;
}
.cart-btn:hover {
  color: #fff;
  background: #ff6b3d;
  border-color: #ff6b3d;
}
.submit-btn {
  width: 200px;
}

/* ============ 下单成功卡 ============ */
.success-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 60px;
}
.success-icon {
  color: #67c23a;
}
.success-title {
  margin-top: 16px;
  font-size: 26px;
  font-weight: 600;
  color: #2d2a26;
}
.success-sub {
  margin-top: 8px;
  font-size: 14px;
  color: #a39c92;
  text-align: center;
}
.success-card {
  width: 100%;
  max-width: 520px;
  margin-top: 28px;
  padding: 8px 24px;
  background: #fff;
  border-radius: 16px;
}
.success-row {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 0;
  border-bottom: 1px solid #f7f3ee;
}
.success-row:last-child {
  border-bottom: none;
}
.success-row .label {
  flex-shrink: 0;
  font-size: 14px;
  color: #a39c92;
  max-width: 55%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.success-row .value {
  font-size: 14px;
  color: #2d2a26;
  word-break: break-all;
  text-align: right;
}
.success-line .label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
}
.line-idx {
  font-style: normal;
  font-size: 11px;
  color: #c0b9ae;
}
.success-line .value {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
  justify-content: center;
}
.success-row .order-no {
  font-family: Consolas, monospace;
  letter-spacing: 0.5px;
}
.success-row .amount {
  font-weight: 600;
  color: #ff6b3d;
}
.success-row .status {
  color: #e6a23c;
  font-weight: 500;
}
.success-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 28px;
}
.success-actions .back-btn {
  margin-top: 0;
}
.back-btn {
  width: 180px;
}
.view-orders-btn {
  width: 180px;
  color: #ff6b3d;
  border-color: #ffb98a;
  background: #fff;
}
.view-orders-btn:hover {
  color: #fff;
  background: #ff7a45;
  border-color: #ff7a45;
}
.success-tip {
  margin-top: 14px;
  font-size: 12px;
  color: #c0b9ae;
}
</style>
