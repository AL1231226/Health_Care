<script setup>
// ============ 商家工作台首页（服务项目已接后端 /service-item/*） ============
// 仍为本地态：店铺资料卡（user_info 兜底，待接 /service-provider 资料接口）
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Shop, Phone, Calendar, Location, Star } from '@element-plus/icons-vue'
import { listItems, addItem, updateItem, deleteItem, toggleItemStatus } from '@/api/item.js'
import { listCategory } from '@/api/category.js'

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
        <el-button class="edit-btn" plain disabled title="店铺资料编辑功能建设中">编辑资料</el-button>
      </section>

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
  </div>
</template>

<style scoped>
.merchant-page {
  min-height: 100vh;
  background: #fdf8f4;
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
