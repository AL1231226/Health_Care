<script setup>
// ============ 家属端 · 老人管理 ============
// 已对接后端：GET /elder-profile/gets、POST /elder-profile/add、PUT /elder-profile/update、DELETE /elder-profile/delete/{elderId}
// 字段对齐数据库 elder_profile 表：elderId / userId / elderName / gender(1男 0女) / birthDate / idCard(唯一,可空)
//   / phone(老人电话,可空) / healthNote / addrId(关联 user_address) / createTime / updateTime
// 请求自动携带 token（request.js），归属校验由后端按 token 解析出的 userId 完成
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, EditPen, Delete, Plus, Phone, Location } from '@element-plus/icons-vue'
import { listElder, addElder, updateElder, deleteElder } from '@/api/elder.js'
import { listAddress } from '@/api/address.js'

const router = useRouter()
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')

/* ---------- 居住地址选项（来自地址管理接口 GET /address/gets） ---------- */
const addressOptions = ref([])
const addressText = (addrId) => {
  const found = addressOptions.value.find((a) => a.addrId === addrId)
  return found ? found.text : ''
}
const loadAddressOptions = async () => {
  try {
    const result = await listAddress()
    if (result.success) {
      addressOptions.value = (result.data || []).map((a) => ({
        addrId: a.addrId,
        text: `${a.province}${a.city}${a.district}${a.detailAddr}`,
      }))
    }
  } catch (err) {
    // 地址列表加载失败不阻塞老人页
  }
}

/* ---------- 老人列表（来自后端接口） ---------- */
const loading = ref(false)
const elders = ref([])

const loadList = async () => {
  loading.value = true
  try {
    const result = await listElder()
    if (result.success) {
      elders.value = result.data || []
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('获取老人列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/* ---------- 添加 / 编辑弹窗 ---------- */
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null) // 编辑中的 elderId
const formRef = ref()
const form = reactive({
  elderName: '',
  gender: 0,
  birthDate: '',
  idCard: '',
  phone: '',
  healthNote: '',
  addrId: null,
})
const rules = {
  elderName: [{ required: true, message: '请输入老人姓名', trigger: 'blur' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  // 身份证选填，有值才校验（18 位，末位可为 X）
  idCard: [{ pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
}

/* 出生日期 → 年龄 */
const calcAge = (birth) => {
  const now = new Date()
  const b = new Date(birth)
  let age = now.getFullYear() - b.getFullYear()
  if (now.getMonth() < b.getMonth() || (now.getMonth() === b.getMonth() && now.getDate() < b.getDate())) age--
  return age
}
const ageOf = (elder) => (elder.birthDate ? calcAge(elder.birthDate) : '-')

// 身份证脱敏：440300********0000
const maskIdCard = (idCard) => (idCard ? idCard.replace(/^(\d{6})\d{8}(\d{4})$/, '$1********$2') : '')

const openAdd = () => {
  editingId.value = null
  Object.assign(form, {
    elderName: '', gender: 0, birthDate: '', idCard: '', phone: '',
    healthNote: '', addrId: null,
  })
  dialogVisible.value = true
}
const openEdit = (elder) => {
  editingId.value = elder.elderId
  Object.assign(form, {
    elderName: elder.elderName,
    gender: elder.gender,
    birthDate: elder.birthDate,
    idCard: elder.idCard || '',
    phone: elder.phone || '',
    healthNote: elder.healthNote || '',
    addrId: elder.addrId ?? null,
  })
  dialogVisible.value = true
}

const saveElder = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 归属由后端按 token 校验，无需前端传 userId
  const payload = { ...form }
  if (editingId.value) payload.elderId = editingId.value

  saving.value = true
  try {
    const result = editingId.value ? await updateElder(payload) : await addElder(payload)
    if (result.success) {
      dialogVisible.value = false
      editingId.value = null
      ElMessage.success('档案已保存')
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

const removeElder = (elder) => {
  ElMessageBox.confirm(
    `确定删除「${elder.elderName}」的档案吗？删除后该老人的服务记录将无法关联。`,
    '删除档案',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
    .then(async () => {
      try {
        const result = await deleteElder(elder.elderId)
        if (result.success) {
          ElMessage.success('档案已删除')
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
  if (!userInfo.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loadList()
  loadAddressOptions()
})
</script>

<template>
  <div class="elder-page">
    <!-- ======== 标题栏 ======== -->
    <div class="page-bar">
      <div class="bar-inner">
        <el-icon class="back" :size="20" @click="router.back()"><ArrowLeft /></el-icon>
        <span class="bar-title">老人管理</span>
        <span class="bar-count">已添加 {{ elders.length }} 位老人</span>
      </div>
    </div>

    <div v-loading="loading" class="container">
      <!-- ======== 老人卡片列表 ======== -->
      <template v-if="elders.length">
        <div v-for="elder in elders" :key="elder.elderId" class="elder-card">
          <div class="elder-main">
            <span
              class="elder-avatar"
              :style="{ background: elder.gender === 1 ? '#4c9fff' : '#ff8a4c' }"
            >{{ elder.elderName.slice(0, 1) }}</span>
            <div class="elder-info">
              <div class="elder-name-row">
                <span class="elder-name">{{ elder.elderName }}</span>
                <span class="elder-gender">{{ elder.gender === 1 ? '男' : '女' }} · {{ ageOf(elder) }}岁</span>
              </div>

              <div class="elder-meta">
                <span v-if="elder.phone" class="meta-item">
                  <el-icon><Phone /></el-icon>老人电话：{{ elder.phone }}
                </span>
                <span v-if="elder.addrId" class="meta-item">
                  <el-icon><Location /></el-icon>{{ addressText(elder.addrId) }}
                </span>
                <span v-if="elder.idCard" class="meta-item">身份证：{{ maskIdCard(elder.idCard) }}</span>
              </div>

              <p v-if="elder.healthNote" class="elder-note">健康备注：{{ elder.healthNote }}</p>
            </div>
          </div>

          <div class="elder-actions">
            <span class="action" @click="openEdit(elder)">
              <el-icon><EditPen /></el-icon>编辑
            </span>
            <span class="action danger" @click="removeElder(elder)">
              <el-icon><Delete /></el-icon>删除
            </span>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty v-else description="还没有添加老人，点击下方按钮添加" />

      <!-- ======== 添加按钮（吸底） ======== -->
      <div class="add-wrap">
        <el-button class="add-btn" type="primary" :icon="Plus" @click="openAdd">添加老人</el-button>
      </div>
    </div>

    <!-- ======== 添加 / 编辑弹窗 ======== -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑老人档案' : '添加老人'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="姓名" prop="elderName">
          <el-input v-model="form.elderName" placeholder="请输入老人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">女</el-radio>
            <el-radio :value="1">男</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择出生日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="选填，18 位" maxlength="18" />
        </el-form-item>
        <el-form-item label="老人电话" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" maxlength="11" />
        </el-form-item>
        <el-form-item label="居住地址">
          <el-select v-model="form.addrId" clearable placeholder="选填，从我的地址选择" style="width: 100%">
            <el-option
              v-for="a in addressOptions"
              :key="a.addrId"
              :label="a.text"
              :value="a.addrId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="健康备注">
          <el-input
            v-model="form.healthNote"
            type="textarea"
            :rows="2"
            placeholder="健康状况、照护注意事项等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveElder">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.elder-page {
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

/* ============ 老人卡片 ============ */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 90px;
}

.elder-card {
  padding: 18px 22px;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(249, 109, 59, 0.05);
  transition: all 0.2s;
}
.elder-card:hover {
  box-shadow: 0 6px 18px rgba(249, 109, 59, 0.1);
}

.elder-main {
  display: flex;
  gap: 14px;
}
.elder-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}
.elder-info {
  flex: 1;
  min-width: 0;
}
.elder-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.elder-name {
  font-size: 17px;
  font-weight: 600;
  color: #2d2a26;
}
.elder-gender {
  font-size: 13px;
  color: #8a8378;
}

.elder-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  margin-top: 10px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #5c564e;
}
.elder-note {
  margin-top: 8px;
  font-size: 13px;
  color: #a39c92;
  line-height: 1.6;
}

/* 操作行 */
.elder-actions {
  display: flex;
  justify-content: flex-end;
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

/* ============ 添加按钮（吸底） ============ */
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
