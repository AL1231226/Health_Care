<script setup>
// ============ 下单确认页（老人/地址已接真实接口；提交订单已接后端，加入购物车待购物车接口） ============
// TODO: 加入购物车：购物车表/接口未建，按钮先占位
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, SuccessFilled } from '@element-plus/icons-vue'
import { listElder } from '@/api/elder.js'
import { listAddress } from '@/api/address.js'
import { createOrder } from '@/api/order.js'

const route = useRoute()
const router = useRouter()

// 当前登录家属（登录时写入 localStorage 的 user_info，含完整手机号，剔除密码字段）
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')

// 从商家详情页带过来的服务展示字段（无参数直接访问时兜底示例数据）
const item = {
  itemId: route.query.itemId || 1,
  itemName: route.query.itemName || '午餐：三荤一素',
  price: Number(route.query.price || 20),
  unit: route.query.unit || '次',
  duration: route.query.duration || '无',
  providerName: route.query.providerName || '示例商家',
}
const fromQuery = !!route.query.itemId // 是否从详情页正常跳入（否则显示示例数据提示）

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
  // 初次联动（老人列表/地址簿加载完成后，默认地址才生效）
  bootstrapDone.value = true
  if (elderId.value) applyElderDefaultAddress()
})

// ============ 表单其余项 ============
const quantity = ref(1)
const serviceTime = ref(null) // 预约时间（可空）
const contactPhone = ref(userInfo.phone || '') // 默认取登录用户手机号（完整显示，可改）
const remark = ref('')

// 价格合计 = 单价 × 数量，实时联动
const totalPrice = computed(() => (item.price * quantity.value).toFixed(2))

// 无老人档案：无法指明服务对象与默认地址，阻止提交
const noElder = computed(() => bootstrapDone.value && elders.value.length === 0)

/* ============ 提交订单（POST /service-order/create） ============ */
const submitting = ref(false)
const createdOrder = ref(null) // 下单成功后整页切换为成功卡
const currentElder = computed(() => elders.value.find((e) => e.elderId === elderId.value) || null)

const onSubmit = async () => {
  if (submitting.value || createdOrder.value) return
  // 老人必选（无老人时按钮已禁用，双保险）
  if (!elderId.value) {
    ElMessage.warning('请先选择服务老人')
    return
  }
  const phone = contactPhone.value.trim()
  if (!phone) {
    ElMessage.warning('请填写联系电话')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('联系电话格式不正确')
    return
  }

  submitting.value = true
  try {
    const result = await createOrder({
      itemId: Number(item.itemId),
      elderId: elderId.value,
      quantity: quantity.value,
      serviceTime: serviceTime.value || null,
      addressId: addressId.value || null,
      contactPhone: phone,
      remark: remark.value.trim(),
    })
    if (result.success) {
      createdOrder.value = result.data
    } else {
      ElMessage.error(result.errorMsg || '下单失败，请稍后重试')
    }
  } catch (err) {
    // 网络异常/401 由 request.js 统一处理
    ElMessage.error('下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 加入购物车占位（购物车表/接口未建）
const onAddCart = () => ElMessage.info('购物车功能建设中，敬请期待')
</script>

<template>
  <!-- ======== 下单成功卡（提交成功后整页切换） ======== -->
  <div v-if="createdOrder" class="container success-page">
    <div class="success-icon"><el-icon :size="64"><SuccessFilled /></el-icon></div>
    <h3 class="success-title">下单成功</h3>
    <p class="success-sub">商家接单后将联系您安排上门服务，请保持电话畅通</p>

    <div class="success-card">
      <div class="success-row">
        <span class="label">订单号</span>
        <span class="value order-no">{{ createdOrder.orderNo }}</span>
      </div>
      <div class="success-row">
        <span class="label">服务项目</span>
        <span class="value">{{ item.itemName }}</span>
      </div>
      <div class="success-row">
        <span class="label">服务老人</span>
        <span class="value">{{ currentElder ? elderLabel(currentElder) : '—' }}</span>
      </div>
      <div class="success-row">
        <span class="label">订单金额</span>
        <span class="value amount">¥{{ createdOrder.totalPrice }}</span>
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
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h3 class="page-title">确认订单</h3>
    </div>

    <!-- ======== 服务信息卡 ======== -->
    <div class="card">
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

    <!-- ======== 下单表单 ======== -->
    <div class="card">
      <div class="card-head">
        <span class="card-title">预约信息</span>
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

        <el-form-item label="服务数量">
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
      </div>
      <div class="actions">
        <el-button class="cart-btn" round size="large" :disabled="noElder || !elderId" @click="onAddCart">加入购物车</el-button>
        <el-button
          type="primary"
          round
          size="large"
          class="submit-btn"
          :disabled="noElder || !elderId"
          :loading="submitting"
          @click="onSubmit"
        >提交订单</el-button>
      </div>
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
.sample-tip {
  padding: 4px 12px;
}
.no-elder-tip {
  margin-bottom: 16px;
}

/* ============ 服务信息 ============ */
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
}
.success-card {
  width: 100%;
  max-width: 480px;
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
}
.success-row .value {
  font-size: 14px;
  color: #2d2a26;
  word-break: break-all;
  text-align: right;
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
