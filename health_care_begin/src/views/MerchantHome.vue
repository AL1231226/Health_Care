<script setup>
// ============ 商家工作台首页（服务项目已接 /service-item/*；店铺资料编辑接 /service-provider/self） ============
// 店铺信息卡：初始取登录 user_info 兜底展示（可失真），点「编辑资料」时拉 /service-provider/self 最新行并保存
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Shop, Phone, Calendar, Location, Star, WarningFilled } from '@element-plus/icons-vue'
import { listItems, addItem, updateItem, deleteItem, toggleItemStatus } from '@/api/item.js'
import { listCategory } from '@/api/category.js'
import { listMerchantOrders, updateOrderStatus } from '@/api/order.js'
import { getSelfProvider, updateSelfProvider } from '@/api/provider.js'

const router = useRouter()

/* ---------- 登录商家信息（优先 localStorage user_info，缺失时兜底假数据） ---------- */
const mockProvider = {
  providerId: 1,
  providerName: '阳光助洁家政',
  categoryId: 2,
  phone: '13812348888',
  status: 1,
  createTime: '2026-08-20 10:30:00',
  address: '广东省深圳市南山区科技园路 1 号',
  intro: '专业家政服务商，持证上岗，服务到家',
}
const provider = reactive({ ...mockProvider })

/* ---------- 服务分类（优先接口 GET /service-category/list，失败回退内置） ---------- */
const fallbackCategories = [
  { categoryId: 1, categoryName: '助餐' },
  { categoryId: 2, categoryName: '助洁' },
  { categoryId: 3, categoryName: '助浴' },
  { categoryId: 4, categoryName: '助医' },
  { categoryId: 5, categoryName: '康复护理' },
]
const categories = ref([])
const categoryName = (id) => categories.value.find((c) => c.categoryId === id)?.categoryName || '未分类'
const loadCategories = async () => {
  try {
    const res = await listCategory()
    if (res.success && Array.isArray(res.data) && res.data.length) {
      categories.value = res.data
      return
    }
  } catch (err) { /* 拦截器已统一提示，走兜底 */ }
  categories.value = fallbackCategories
}

// 状态徽章：0待审核 1正常 2停用（与 service_provider.status 一致）
const statusMap = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '正常', type: 'success' },
  2: { text: '停用', type: 'danger' },
}
const statusInfo = computed(() => statusMap[provider.status] || statusMap[1])

// 手机号脱敏
const maskPhone = (phone) => (phone || '').replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
// 时间兜底格式化（后端日期可能是时间戳或字符串）
const fmtDate = (v) => {
  if (!v) return '-'
  if (typeof v === 'number') return new Date(v).toLocaleDateString('zh-CN')
  return String(v).slice(0, 10)
}

/* ---------- 服务项目（GET /service-item/list 真实数据） ---------- */
const itemLoading = ref(false)
const items = ref([])
const loadItems = async () => {
  itemLoading.value = true
  try {
    const res = await listItems()
    if (res.success) {
      items.value = res.data || []
    } else {
      ElMessage.error(res.errorMsg || '加载服务列表失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ } finally {
    itemLoading.value = false
  }
}

// 统计卡片（由真实列表实时计算）
const stats = computed(() => {
  const total = items.value.length
  const onSale = items.value.filter((i) => i.status === 1).length
  const scored = items.value.filter((i) => i.score != null)
  const avg = scored.length ? (scored.reduce((s, i) => s + i.score, 0) / scored.length).toFixed(1) : '0.0'
  return { total, onSale, offSale: total - onSale, avg }
})

/* ---------- 服务项目：新增 / 编辑弹窗 ---------- */
const dialogVisible = ref(false)
const dialogTitle = ref('新增服务')
const editingId = ref(null) // null 为新增
const saving = ref(false)
const itemFormRef = ref()
const itemForm = reactive({
  itemName: '',
  categoryId: null,
  price: null,
  unit: '次',
  duration: '',
  detail: '',
})
const unitOptions = ['次', '餐', '小时', '半天', '天']
const itemRules = {
  itemName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择服务分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择计价单位', trigger: 'change' }],
  duration: [{ required: true, message: '请输入服务时长', trigger: 'blur' }],
}

const openAdd = () => {
  dialogTitle.value = '新增服务'
  editingId.value = null
  Object.assign(itemForm, { itemName: '', categoryId: null, price: null, unit: '次', duration: '', detail: '' })
  dialogVisible.value = true
}
const openEdit = (row) => {
  dialogTitle.value = '编辑服务'
  editingId.value = row.itemId
  Object.assign(itemForm, {
    itemName: row.itemName,
    categoryId: row.categoryId,
    price: row.price,
    unit: row.unit,
    duration: row.duration,
    detail: row.detail,
  })
  dialogVisible.value = true
}

// 保存：新增 POST /service-item/add，编辑 PUT /service-item/update
const saveItem = async () => {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value === null) {
      const res = await addItem({ ...itemForm })
      if (res.success) {
        ElMessage.success('服务已新增')
        dialogVisible.value = false
        loadItems()
      } else {
        ElMessage.error(res.errorMsg || '添加服务失败')
      }
    } else {
      const res = await updateItem({ itemId: editingId.value, ...itemForm })
      if (res.success) {
        ElMessage.success(res.data || '服务已保存')
        dialogVisible.value = false
        loadItems()
      } else {
        ElMessage.error(res.errorMsg || '修改服务失败')
      }
    }
  } finally {
    saving.value = false
  }
}

// 删除（DELETE /service-item/delete/{itemId}）
const removeItem = (row) => {
  ElMessageBox.confirm(`确定删除服务「${row.itemName}」吗？删除后不可恢复`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res = await deleteItem(row.itemId)
    if (res.success) {
      ElMessage.success(res.data || '已删除')
      loadItems()
    } else {
      ElMessage.error(res.errorMsg || '删除失败')
    }
  }).catch(() => {})
}

// 上架 / 下架（PUT /service-item/status/{itemId}；用 :model-value 单向绑定，
// 接口成功后才改行数据，失败界面不闪变）
const togglingId = ref(null)
const toggleStatus = async (row) => {
  const target = row.status === 0 ? 1 : 0
  togglingId.value = row.itemId
  try {
    const res = await toggleItemStatus(row.itemId, target)
    if (res.success) {
      row.status = target
      ElMessage.success(res.data || (target === 1 ? '已上架' : '已下架'))
    } else {
      ElMessage.error(res.errorMsg || '操作失败')
    }
  } finally {
    togglingId.value = null
  }
}

/* ---------- 店铺资料编辑（商家自助查改 GET/PUT /service-provider/self） ---------- */
const profileDialogVisible = ref(false)
const profileSaving = ref(false)
const profileFormRef = ref()
const profileForm = reactive({
  providerId: null,
  providerName: '',
  categoryId: null, // 入驻归属信息（同 phone），仅只读展示不可改，不随保存提交
  legalPerson: '',
  intro: '',
  phone: '', // 登录账号，仅只读展示不可改
  address: '',
})
const profileRules = {
  providerName: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
}
const openProfileEdit = async () => {
  // 弹窗前拉最新店铺资料填表单（不依赖登录时的本地快照，失败提示后不打开）
  try {
    const res = await getSelfProvider()
    if (res.success) {
      Object.assign(profileForm, {
        providerId: res.data?.providerId ?? null,
        providerName: res.data?.providerName ?? '',
        categoryId: res.data?.categoryId ?? null,
        legalPerson: res.data?.legalPerson ?? '',
        intro: res.data?.intro ?? '',
        phone: res.data?.phone ?? '',
        address: res.data?.address ?? '',
      })
      profileDialogVisible.value = true
    } else {
      ElMessage.error(res.errorMsg || '加载店铺资料失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ }
}
const saveProfile = async () => {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  profileSaving.value = true
  try {
    // 只提交可编辑字段（后端另有白名单兜底：主营分类/phone/status/role/providerId 等改了也无效）
    const res = await updateSelfProvider({
      providerName: profileForm.providerName,
      legalPerson: profileForm.legalPerson,
      intro: profileForm.intro,
      address: profileForm.address,
    })
    if (res.success) {
      ElMessage.success('店铺资料已保存')
      // 响应为最新商家行（密码已剔除），整对象同步店铺卡/顶栏与 localStorage（与登录存储形状一致），无需重登
      const updated = res.data
      if (updated) {
        Object.assign(provider, updated)
        localStorage.setItem('user_info', JSON.stringify(updated))
      }
      profileDialogVisible.value = false
    } else {
      ElMessage.error(res.errorMsg || '保存失败')
    }
  } finally {
    profileSaving.value = false
  }
}

/* ---------- 订单管理（GET /service-order/merchant/list 真实数据，接单/完成走状态流转） ---------- */
const activeTab = ref('items') // items 服务项目 / orders 订单管理
const orderLoading = ref(false)
const orders = ref([])
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
const loadOrders = async () => {
  orderLoading.value = true
  try {
    const res = await listMerchantOrders()
    if (res.success) {
      orders.value = res.data || []
    } else {
      ElMessage.error(res.errorMsg || '加载订单失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ } finally {
    orderLoading.value = false
  }
}

/* ---------- 订单详情抽屉（含服务老人健康信息，接单/服务前必看） ---------- */
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

// 接单(0→1) / 完成服务(1→2)：确认后调接口，成功后刷新列表
const orderAction = (row, nextStatus) => {
  const isAccept = nextStatus === 1
  ElMessageBox.confirm(
    isAccept ? '确认接单？接单后请按预约时间联系家属，安排上门服务。' : '确认服务已完成？完成后订单不可再变更状态。',
    isAccept ? '接单确认' : '完成服务确认',
    {
      confirmButtonText: isAccept ? '确认接单' : '确认完成',
      cancelButtonText: '取消',
      type: 'warning',
    },
  ).then(async () => {
    const res = await updateOrderStatus(row.orderId, nextStatus)
    if (res.success) {
      ElMessage.success(res.data || (isAccept ? '已接单' : '服务已完成'))
      drawerVisible.value = false
      loadOrders()
    } else {
      ElMessage.error(res.errorMsg || '操作失败')
    }
  }).catch(() => {})
}

/* ---------- 退出登录 ---------- */
const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '退出确认', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user_info')
    router.push('/login')
  }).catch(() => {})
}

onMounted(() => {
  // 登录成功后 user_info 已存 localStorage，这里合并兜底（只取商家相关字段）
  try {
    const saved = JSON.parse(localStorage.getItem('user_info') || '{}')
    Object.keys(provider).forEach((key) => {
      if (saved[key] !== undefined && saved[key] !== null) provider[key] = saved[key]
    })
  } catch (err) { /* 解析失败则全部用假数据 */ }
  loadCategories()
  loadItems()
  loadOrders()
})
</script>

<template>
  <div class="merchant-page">
    <!-- ======== 顶部栏 ======== -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <div class="brand">
          <span class="brand-badge">颐</span>
          <span class="brand-text">颐养平台 · 商家端</span>
        </div>
        <el-dropdown trigger="click">
          <span class="user-chip">
            <el-avatar :size="30" class="user-avatar">{{ provider.providerName?.charAt(0) }}</el-avatar>
            {{ provider.providerName }}
            <el-icon class="chip-arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>商家工作台</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="page-body">
      <!-- 待审核提示（仅 status=0 显示；实际待审核商家无法登录，此处防本地数据残留） -->
      <el-alert
        v-if="provider.status === 0"
        class="pending-alert"
        type="warning"
        show-icon
        :closable="false"
        title="店铺资料审核中"
        description="您的入驻申请正在平台审核，审核通过前商家功能暂不可用，请耐心等待。"
      />

      <!-- ======== 店铺信息卡 ======== -->
      <section class="card store-card">
        <div class="store-logo">{{ provider.providerName?.charAt(0) }}</div>
        <div class="store-main">
          <div class="store-name-row">
            <h2 class="store-name">{{ provider.providerName }}</h2>
            <span class="status-badge" :class="'status-' + provider.status">{{ statusInfo.text }}</span>
          </div>
          <p class="store-intro">{{ provider.intro || '暂无简介' }}</p>
          <div class="store-tags">
            <span class="tag"><el-icon><Shop /></el-icon>{{ categoryName(provider.categoryId) }}</span>
            <span class="tag"><el-icon><Phone /></el-icon>{{ maskPhone(provider.phone) }}</span>
            <span class="tag"><el-icon><Calendar /></el-icon>入驻于 {{ fmtDate(provider.createTime) }}</span>
            <span class="tag"><el-icon><Location /></el-icon>{{ provider.address || '地址待完善' }}</span>
          </div>
        </div>
        <el-button class="edit-btn" plain @click="openProfileEdit">编辑资料</el-button>
      </section>

      <!-- ======== 服务项目 / 订单管理 Tab ======== -->
      <el-tabs v-model="activeTab" class="work-tabs">
        <!-- ---------- Tab：服务项目 ---------- -->
        <el-tab-pane label="服务项目" name="items">
          <!-- ======== 数据统计 ======== -->
          <section class="stats-row">
            <div class="card stat-card">
              <p class="stat-num">{{ stats.total }}</p>
              <p class="stat-label">服务项目</p>
            </div>
            <div class="card stat-card">
              <p class="stat-num on">{{ stats.onSale }}</p>
              <p class="stat-label">已上架</p>
            </div>
            <div class="card stat-card">
              <p class="stat-num off">{{ stats.offSale }}</p>
              <p class="stat-label">已下架</p>
            </div>
            <div class="card stat-card">
              <p class="stat-num star">{{ stats.avg }}</p>
              <p class="stat-label">平均评分</p>
            </div>
          </section>

          <!-- ======== 服务项目管理 ======== -->
          <section class="card items-card">
            <div class="card-head">
              <h3>服务项目管理</h3>
              <el-button class="primary-btn" type="primary" @click="openAdd">＋ 新增服务</el-button>
            </div>

            <el-table :data="items" v-loading="itemLoading" stripe>
              <el-table-column prop="itemName" label="服务名称" min-width="180" show-overflow-tooltip />
              <el-table-column label="分类" width="100">
                <template #default="{ row }">
                  <span class="cat-chip">{{ categoryName(row.categoryId) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="价格" width="120">
                <template #default="{ row }">
                  <span class="price">¥{{ row.price }}</span>
                  <span class="price-unit">/{{ row.unit }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="服务时长" width="110" />
              <el-table-column label="销量" width="90">
                <template #default="{ row }">
                  {{ row.sales ?? 0 }}
                </template>
              </el-table-column>
              <el-table-column label="评分" width="100">
                <template #default="{ row }">
                  <span class="score"><el-icon class="score-icon"><Star /></el-icon>{{ row.score ?? '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    active-text="上架"
                    inline-prompt
                    :loading="togglingId === row.itemId"
                    @change="toggleStatus(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeItem(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!itemLoading && !items.length" description="还没有服务项目，点击右上角「新增服务」上架第一个服务" />
          </section>
        </el-tab-pane>

        <!-- ---------- Tab：订单管理 ---------- -->
        <el-tab-pane label="订单管理" name="orders">
          <section class="card orders-card">
            <div class="card-head orders-head">
              <h3>订单管理</h3>
              <el-radio-group v-model="orderStatusFilter" size="small">
                <el-radio-button v-for="t in orderStatusTabs" :key="t.value" :value="t.value">
                  {{ t.label }}
                  <span class="status-count">{{ t.value === ALL_STATUS ? orders.length : orderCounts[t.value] }}</span>
                </el-radio-button>
              </el-radio-group>
            </div>

            <el-table :data="filteredOrders" v-loading="orderLoading" stripe>
              <el-table-column label="订单号" width="180">
                <template #default="{ row }">
                  <span class="order-no-cell">{{ row.orderNo }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="itemName" label="服务项目" min-width="150" show-overflow-tooltip />
              <el-table-column label="服务老人" width="130">
                <template #default="{ row }">
                  <span v-if="row.elderName">{{ row.elderName }}<span class="elder-sex">（{{ genderText(row.gender) }}）</span></span>
                  <span v-else class="muted-text">档案已删</span>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="110">
                <template #default="{ row }">
                  <span class="price">¥{{ row.totalPrice }}</span>
                </template>
              </el-table-column>
              <el-table-column label="预约时间" width="165">
                <template #default="{ row }">
                  <span v-if="row.serviceTime">{{ fmtDateTime(row.serviceTime) }}</span>
                  <span v-else class="muted-text">未预约</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="orderStatusMap[row.orderStatus]?.type" size="small" effect="light">
                    {{ orderStatusMap[row.orderStatus]?.text }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openOrderDetail(row)">详情</el-button>
                  <el-button v-if="row.orderStatus === 0" link type="success" @click="orderAction(row, 1)">接单</el-button>
                  <el-button v-if="row.orderStatus === 1" link type="primary" @click="orderAction(row, 2)">完成服务</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!orderLoading && !filteredOrders.length" description="暂无相关订单" />
          </section>
        </el-tab-pane>
      </el-tabs>
    </main>

    <!-- ======== 新增 / 编辑服务弹窗 ======== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="服务名称" prop="itemName">
          <el-input v-model="itemForm.itemName" placeholder="如：家庭深度保洁" maxlength="30" />
        </el-form-item>
        <el-form-item label="服务分类" prop="categoryId">
          <el-select v-model="itemForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="itemForm.price" :min="0" :max="99999" :precision="2" :step="1" style="width: 200px" />
          <el-select v-model="itemForm.unit" placeholder="单位" style="width: 100px; margin-left: 12px">
            <el-option v-for="u in unitOptions" :key="u" :label="'/' + u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务时长" prop="duration">
          <el-input v-model="itemForm.duration" placeholder="如：2小时 / 45分钟" maxlength="20" />
        </el-form-item>
        <el-form-item label="项目详情" prop="detail">
          <el-input
            v-model="itemForm.detail"
            type="textarea"
            :rows="3"
            placeholder="介绍服务内容、适用人群等，便于家属了解"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button class="primary-btn" type="primary" :loading="saving" @click="saveItem">保 存</el-button>
      </template>
    </el-dialog>

    <!-- ======== 编辑店铺资料弹窗（白名单：名称/负责人/简介/地址；主营分类与联系电话为入驻归属信息只读不可改） ======== -->
    <el-dialog v-model="profileDialogVisible" title="编辑店铺资料" width="520px" :close-on-click-modal="false">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
        <el-form-item label="商家名称" prop="providerName">
          <el-input v-model="profileForm.providerName" placeholder="如：城志社区食堂" maxlength="30" />
        </el-form-item>
        <el-form-item label="主营分类" prop="categoryId">
          <el-select v-model="profileForm.categoryId" disabled style="width: 100%">
            <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
          <span class="muted-text" style="margin-left: 8px; font-size: 12px">入驻分类，暂不可修改</span>
        </el-form-item>
        <el-form-item label="负责人" prop="legalPerson">
          <el-input v-model="profileForm.legalPerson" placeholder="负责人姓名（选填）" maxlength="30" />
        </el-form-item>
        <el-form-item label="简介" prop="intro">
          <el-input
            v-model="profileForm.intro"
            type="textarea"
            :rows="3"
            placeholder="一句话介绍店铺服务，便于家属了解"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="profileForm.phone" disabled />
          <span class="muted-text" style="margin-left: 8px; font-size: 12px">登录手机号，暂不可修改</span>
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="profileForm.address" placeholder="详细经营地址（选填）" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取 消</el-button>
        <el-button class="primary-btn" type="primary" :loading="profileSaving" @click="saveProfile">保 存</el-button>
      </template>
    </el-dialog>

    <!-- ======== 订单详情抽屉（服务老人健康信息为接单/服务前必看项） ======== -->
    <el-drawer v-model="drawerVisible" title="订单详情" size="460px">
      <div v-if="currentOrder" class="order-detail">
        <!-- 服务信息 -->
        <div class="detail-section">
          <div class="section-title">服务信息</div>
          <div class="d-row"><span class="d-label">服务项目</span><span class="d-value">{{ currentOrder.itemName || '服务已删除' }}</span></div>
          <div class="d-row"><span class="d-label">单价 × 数量</span><span class="d-value">¥{{ currentOrder.unitPrice }} × {{ currentOrder.quantity }}</span></div>
          <div class="d-row"><span class="d-label">订单金额</span><span class="d-value amount">¥{{ currentOrder.totalPrice }}</span></div>
          <div class="d-row"><span class="d-label">订单号</span><span class="d-value order-no-cell">{{ currentOrder.orderNo }}</span></div>
          <div class="d-row"><span class="d-label">下单时间</span><span class="d-value">{{ fmtDateTime(currentOrder.createTime) }}</span></div>
        </div>

        <!-- 服务老人（健康信息给商家，服务前必看） -->
        <div class="detail-section elder-section">
          <div class="section-title">服务老人</div>
          <template v-if="currentOrder.elderName">
            <div class="elder-name">
              {{ currentOrder.elderName }}
              <span class="elder-tag">
                {{ genderText(currentOrder.gender) }} · {{ ageOf(currentOrder.birthDate) != null ? `${ageOf(currentOrder.birthDate)}岁` : '年龄未知' }}
              </span>
            </div>
            <div class="d-row"><span class="d-label">老人电话</span><span class="d-value">{{ currentOrder.elderPhone || '未填写' }}</span></div>
            <div class="health-box">
              <div class="health-title"><el-icon class="health-icon"><WarningFilled /></el-icon>健康备注 / 护理注意事项</div>
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

      <template #footer>
        <div class="drawer-actions">
          <el-button v-if="currentOrder?.orderStatus === 0" type="success" size="large" @click="orderAction(currentOrder, 1)">确认接单</el-button>
          <el-button v-if="currentOrder?.orderStatus === 1" type="primary" size="large" @click="orderAction(currentOrder, 2)">完成服务</el-button>
          <el-tag v-if="currentOrder?.orderStatus === 2" type="success" size="large">服务已完成</el-tag>
          <el-tag v-else-if="currentOrder?.orderStatus === 3" type="info" size="large">订单已取消</el-tag>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.merchant-page {
  min-height: 100vh;
  background: #fdf8f4;
}

/* ============ 服务项目 / 订单管理 Tab ============ */
.work-tabs {
  margin-bottom: 16px;
}
.work-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
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
.status-count {
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 8px;
  background: #f5efe9;
  color: #a39c92;
  font-size: 12px;
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
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
}

/* ============ 顶部栏 ============ */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
  border-bottom: 1px solid #f2ece5;
}
.top-bar-inner {
  max-width: 1100px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
  letter-spacing: 1px;
}
.user-chip {
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
.user-chip:hover {
  background: #fdf2ea;
}
.user-avatar {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 14px;
}
.chip-arrow {
  font-size: 12px;
  color: #b5aca1;
}

/* ============ 页面主体 ============ */
.page-body {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}
.pending-alert {
  margin-bottom: 16px;
  border-radius: 12px;
}

/* ============ 卡片基础 ============ */
.card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(249, 109, 59, 0.06);
  padding: 20px;
}

/* ============ 店铺信息卡 ============ */
.store-card {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 16px;
}
.store-logo {
  flex: none;
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffb26b, #ff7a45);
  color: #fff;
  font-size: 32px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 14px rgba(255, 122, 69, 0.3);
}
.store-main {
  flex: 1;
  min-width: 0;
}
.store-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.store-name {
  font-size: 20px;
  font-weight: 600;
  color: #2d2a26;
}
.status-badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
}
.status-1 {
  background: #e8f8ee;
  color: #34a853;
}
.status-0 {
  background: #fff4e0;
  color: #e6a23c;
}
.status-2 {
  background: #fdeeee;
  color: #f56c6c;
}
.store-intro {
  margin: 6px 0 10px;
  font-size: 13px;
  color: #a39c92;
}
.store-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 8px;
  background: #faf5ef;
  color: #8a8378;
  font-size: 12px;
}
.edit-btn {
  flex: none;
}

/* ============ 统计卡片 ============ */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
  padding: 18px 12px;
}
.stat-num {
  font-size: 30px;
  font-weight: 700;
  color: #2d2a26;
}
.stat-num.on { color: #34a853; }
.stat-num.off { color: #c0b9ae; }
.stat-num.star { color: #ff7a45; }
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #a39c92;
}

/* ============ 服务项目管理 ============ */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.card-head h3 {
  font-size: 17px;
  font-weight: 600;
  color: #2d2a26;
}
.primary-btn {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  border: none;
}
.primary-btn:hover {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  opacity: 0.9;
}
.cat-chip {
  padding: 2px 10px;
  border-radius: 8px;
  background: #fdf2ea;
  color: #ff7a45;
  font-size: 12px;
}
.price {
  font-size: 15px;
  font-weight: 600;
  color: #ff6b2c;
}
.price-unit {
  font-size: 12px;
  color: #b5aca1;
}
.score {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #ff9f43;
  font-weight: 600;
}
.score-icon {
  font-size: 14px;
}

/* ============ 响应式 ============ */
@media (max-width: 800px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .store-card {
    flex-wrap: wrap;
  }
  .edit-btn {
    width: 100%;
  }
}
</style>
