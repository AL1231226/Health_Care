<script setup>
// ============ 家属端 · 地址管理（京东购物风格） ============
// 已对接后端：GET /address/gets、POST /address/add、PUT /address/update、DELETE /address/delete/{addrId}
// 请求自动携带 token（request.js），归属校验由后端按 token 解析出的 userId 完成
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Location, EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { listAddress, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address.js'
import { regionData, codeToText } from 'element-china-area-data'

const router = useRouter()

// 当前登录家属（登录时写入 localStorage 的 user_info，剔除密码字段）
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')
const userId = userInfo.id

/* ---------- 地址列表 ---------- */
const loading = ref(false)
const addresses = ref([])

const loadList = async () => {
  loading.value = true
  try {
    const result = await listAddress()
    if (result.success) {
      addresses.value = result.data || []
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('获取地址列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/* ---------- 省市区选项（全国完整数据 element-china-area-data） ---------- */
// 级联选择器存的是行政区划编码，提交时通过 codeToText 转名称入库
const regionOptions = regionData

// 后端存的是省市区名称，编辑回显时反查名称对应的编码
const findRegionCodes = (province, city, district) => {
  for (const p of regionData) {
    if (codeToText[p.value] !== province) continue
    for (const c of p.children || []) {
      if (codeToText[c.value] !== city) continue
      for (const d of c.children || []) {
        if (codeToText[d.value] === district) return [p.value, c.value, d.value]
      }
      return [p.value, c.value]
    }
    return [p.value]
  }
  return []
}

/* ---------- 新增 / 编辑弹窗 ---------- */
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null) // null=新增，否则=编辑的 addrId
const formRef = ref()
const form = reactive({
  phone: '',
  region: [], // ['广东省', '深圳市', '南山区']
  detail: '',
  isDefault: false,
})
const rules = {
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  region: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

const openAdd = () => {
  editingId.value = null
  Object.assign(form, { phone: '', region: [], detail: '', isDefault: false })
  dialogVisible.value = true
}
const openEdit = (addr) => {
  editingId.value = addr.addrId
  Object.assign(form, {
    phone: addr.phone,
    // 名称反查编码回显；查不到（数据异常）时留空让用户重选
    region: findRegionCodes(addr.province, addr.city, addr.district),
    detail: addr.detailAddr,
    isDefault: addr.isDefault === 1,
  })
  dialogVisible.value = true
}

const saveAddress = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 归属由后端按 token 校验，无需前端传 userId
  const payload = {
    // 编码转名称入库（与表字段 province/city/district 对应）
    province: codeToText[form.region[0]] || '',
    city: codeToText[form.region[1]] || '',
    district: codeToText[form.region[2]] || '',
    detailAddr: form.detail,
    phone: form.phone,
    isDefault: form.isDefault ? 1 : 0,
  }
  if (editingId.value) payload.addrId = editingId.value

  saving.value = true
  try {
    const result = editingId.value ? await updateAddress(payload) : await addAddress(payload)
    if (result.success) {
      dialogVisible.value = false
      // 新增时后端返回实体，自增 addrId 在 result.data 里
      const savedId = editingId.value || result.data?.addrId
      editingId.value = null
      ElMessage.success('地址已保存')
      // 勾选了设为默认：走后端事务接口（先清其他默认再设当前）
      if (form.isDefault && savedId) {
        const defResult = await setDefaultAddress(savedId)
        if (!defResult.success) ElMessage.error(defResult.errorMsg)
      }
      loadList()
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const setDefault = async (addr) => {
  try {
    const result = await setDefaultAddress(addr.addrId)
    if (result.success) {
      ElMessage.success('已设为默认地址')
      loadList()
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const removeAddress = (addr) => {
  ElMessageBox.confirm(
    `确定删除地址「${addr.province}${addr.city}${addr.district} ${addr.detailAddr}」吗？`,
    '删除地址',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
    .then(async () => {
      try {
        const result = await deleteAddress(addr.addrId)
        if (result.success) {
          ElMessage.success('地址已删除')
          loadList()
        } else {
          ElMessage.error(result.errorMsg)
        }
      } catch (err) {
        ElMessage.error('删除失败，请稍后重试')
      }
    })
    .catch(() => {})
}

onMounted(() => {
  if (!userId) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loadList()
})
</script>

<template>
  <div class="address-page">
    <!-- ======== 标题栏 ======== -->
    <div class="page-bar">
      <div class="bar-inner">
        <el-icon class="back" :size="20" @click="router.back()"><ArrowLeft /></el-icon>
        <span class="bar-title">地址管理</span>
        <span class="bar-count">共 {{ addresses.length }} 条</span>
      </div>
    </div>

    <div v-loading="loading" class="container">
      <!-- ======== 地址列表 ======== -->
      <template v-if="addresses.length">
        <div v-for="addr in addresses" :key="addr.addrId" class="addr-card">
          <div class="addr-main">
            <span class="addr-icon"><el-icon :size="22"><Location /></el-icon></span>
            <div class="addr-info">
              <div class="addr-name-row">
                <span class="addr-phone">{{ addr.phone }}</span>
                <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
              </div>
              <p class="addr-detail">
                {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detailAddr }}
              </p>
            </div>
          </div>
          <div class="addr-actions">
            <span
              v-if="addr.isDefault !== 1"
              class="action"
              @click="setDefault(addr)"
            >设为默认</span>
            <span v-else class="action default-done">默认地址</span>
            <span class="action" @click="openEdit(addr)">
              <el-icon><EditPen /></el-icon>编辑
            </span>
            <span class="action danger" @click="removeAddress(addr)">
              <el-icon><Delete /></el-icon>删除
            </span>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty v-else description="还没有收货地址，点击下方按钮添加" />

      <!-- ======== 新增按钮（吸底） ======== -->
      <div class="add-wrap">
        <el-button class="add-btn" type="primary" :icon="Plus" @click="openAdd">新增收货地址</el-button>
      </div>
    </div>

    <!-- ======== 新增 / 编辑弹窗 ======== -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑地址' : '新增收货地址'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="form.region"
            :options="regionOptions"
            placeholder="请选择省/市/区"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="2"
            placeholder="街道、楼牌号等"
            maxlength="60"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.address-page {
  min-height: calc(100vh - 64px);
  background: #fdf8f4;
}

/* ============ 标题栏 ============ */
.page-bar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(249, 109, 59, 0.06);
}
.bar-inner {
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
}
.back {
  color: #5c564e;
  cursor: pointer;
}
.back:hover {
  color: #ff7a45;
}
.bar-title {
  font-size: 17px;
  font-weight: 600;
  color: #2d2a26;
}
.bar-count {
  font-size: 12px;
  color: #a39c92;
}

/* ============ 地址列表 ============ */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 90px;
}

.addr-card {
  padding: 18px 22px;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(249, 109, 59, 0.05);
  transition: all 0.2s;
}
.addr-card:hover {
  box-shadow: 0 6px 18px rgba(249, 109, 59, 0.1);
}

.addr-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.addr-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fff2ec;
  color: #ff7a45;
  flex-shrink: 0;
}
.addr-info {
  flex: 1;
  min-width: 0;
}
.addr-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.addr-phone {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
}
.default-tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 11px;
}
.addr-detail {
  margin-top: 8px;
  font-size: 14px;
  color: #8a8378;
  line-height: 1.6;
}

/* 操作行 */
.addr-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 18px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #f7f3ee;
}
.action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #8a8378;
  cursor: pointer;
}
.action:hover {
  color: #ff7a45;
}
.action.danger:hover {
  color: #f56c6c;
}
.action.default-done {
  color: #c0b9ae;
  cursor: default;
}
.action.default-done:hover {
  color: #c0b9ae;
}

/* ============ 新增按钮（吸底） ============ */
.add-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 14px 24px calc(14px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(253, 248, 244, 0), #fdf8f4 40%);
  text-align: center;
}
.add-btn {
  width: 100%;
  max-width: 1200px;
  height: 48px;
  font-size: 16px;
  letter-spacing: 2px;
  border: none;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  box-shadow: 0 6px 16px rgba(255, 122, 69, 0.35);
}
.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(255, 122, 69, 0.45);
}
</style>
