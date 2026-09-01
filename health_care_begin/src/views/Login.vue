<script setup>
// ============ 登录页：家属 / 商家 / 管理员统一入口 ============
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Iphone, Lock, User,
  Food, Brush, Umbrella, FirstAidKit,
} from '@element-plus/icons-vue'
import { loginService, registerService, providerLoginService, providerRegisterService, adminLoginService } from '@/api/user.js'
import { listCategory } from '@/api/category.js'

const router = useRouter()
// 当前角色：user 家属 / provider 商家 / admin 管理员
const role = ref('user')
// 家属面板：登录 / 注册 视图切换
const isRegister = ref(false)

/* ---------- 表单校验规则 ---------- */
const phoneRule = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
]
// 与后端 ValidationUtil.PASSWORD_REGEX 一致：至少 6 位，同时包含字母和数字
const passwordRule = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/, message: '密码至少 6 位，且需包含字母和数字', trigger: 'blur' },
]
// 确认密码一致性校验
const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== userRegisterForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/* ---------- 家属登录表单 ---------- */
const userLoginRef = ref()
const userLoginForm = reactive({
  phone: '',
  password: '',
  remember: false,
})
const userLoginRules = {
  phone: phoneRule,
  password: passwordRule,
}

/* ---------- 家属注册表单 ---------- */
const userRegisterRef = ref()
const userRegisterForm = reactive({
  phone: '',
  userName: '',
  password: '',
  confirmPassword: '',
})
const userRegisterRules = {
  phone: phoneRule,
  userName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: passwordRule,
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

/* ---------- 商家登录表单 ---------- */
const merchantLoginRef = ref()
const merchantLoginForm = reactive({
  phone: '',
  password: '',
  remember: false,
})
const merchantLoginRules = {
  phone: phoneRule,
  password: passwordRule,
}

/* ---------- 商家入驻表单 ---------- */
const merchantRegisterRef = ref()
const merchantRegisterForm = reactive({
  phone: '',
  providerName: '',
  categoryId: null,
  password: '',
  confirmPassword: '',
})
// 确认密码一致性校验（商家入驻）
const validateMerchantConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== merchantRegisterForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}
const merchantRegisterRules = {
  phone: phoneRule,
  providerName: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择主营服务分类', trigger: 'change' }],
  password: passwordRule,
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateMerchantConfirm, trigger: 'blur' },
  ],
}

/* ---------- 主营服务分类（来自 service_category 接口） ---------- */
const categoryOptions = ref([])
const categoryLoading = ref(false)
const loadCategories = async () => {
  categoryLoading.value = true
  try {
    const result = await listCategory()
    if (result.success && Array.isArray(result.data)) {
      categoryOptions.value = result.data.map((c) => ({ value: c.categoryId, label: c.categoryName }))
    }
  } catch (err) {
    // 接口失败不阻塞入驻页，分类选择框保持为空
  } finally {
    categoryLoading.value = false
  }
}

/* ---------- 管理员登录表单 ---------- */
const adminRef = ref()
const adminForm = reactive({
  username: '',
  password: '',
})
const adminRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: passwordRule,
}

/* ---------- 登录后需要跳转的页面 ---------- */
const homePath = () => {
  if (role.value === 'user') return '/user/home'
  if (role.value === 'provider') return '/provider/home'
  return '/admin/home'
}

// 切换角色：重置为登录视图，并清掉校验痕迹
const switchRole = (value) => {
  role.value = value
  isRegister.value = false
  userLoginRef.value?.clearValidate()
  userRegisterRef.value?.clearValidate()
  merchantLoginRef.value?.clearValidate()
  merchantRegisterRef.value?.clearValidate()
  adminRef.value?.clearValidate()
}

// 家属：切到注册视图时带上已填写的手机号，切回登录同理
const switchToRegister = () => {
  userRegisterForm.phone = userLoginForm.phone
  isRegister.value = true
}
const switchToLogin = () => {
  userLoginForm.phone = userRegisterForm.phone
  isRegister.value = false
}

// 商家：同上
const switchToMerchantRegister = () => {
  merchantRegisterForm.phone = merchantLoginForm.phone
  isRegister.value = true
}
const switchToMerchantLogin = () => {
  merchantLoginForm.phone = merchantRegisterForm.phone
  isRegister.value = false
}

/* ---------- 家属登录 ---------- */
const handleUserLogin = async () => {
  const valid = await userLoginRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    // 后端：POST /auth/family/login  { phone, password, role }
    // 返回 Result { success, errorMsg, data }（data 为 { user, token }）
    const result = await loginService({ phone: userLoginForm.phone, password: userLoginForm.password, role: '1' })
    if (result.success) {
      // 记住手机号（登录成功才记录）
      if (userLoginForm.remember) {
        localStorage.setItem('remember_phone', userLoginForm.phone)
      } else {
        localStorage.removeItem('remember_phone')
      }
      // 保存 token 和用户信息（剔除密码字段，不落本地）
      const { user, token } = result.data
      const { password, ...userInfo } = user
      localStorage.setItem('token', token)
      localStorage.setItem('user_info', JSON.stringify(userInfo))
      ElMessage.success('登录成功，正在进入用户端…')
      router.push(homePath())
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    // 网络异常时 request.js 拦截器已 alert('服务异常')
    ElMessage.error('登录失败，请稍后重试')
  }
}

/* ---------- 家属注册 ---------- */
const handleUserRegister = async () => {
  const valid = await userRegisterRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    // 后端：POST /auth/family/register  { phone, userName, password, role }
    // 返回 Result { success, errorMsg, data }
    const result = await registerService({
      phone: userRegisterForm.phone,
      userName: userRegisterForm.userName,
      password: userRegisterForm.password,
      role: '1',
    })
    if (result.success) {
      ElMessage.success('注册成功，请登录')
      switchToLogin()
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('注册失败，请稍后重试')
  }
}

/* ---------- 商家登录 ---------- */
const handleMerchantLogin = async () => {
  const valid = await merchantLoginRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    // 后端：POST /auth/provider/login  { phone, password, role }
    // 返回 Result { success, errorMsg, data }（data 为 { user, token }）
    const result = await providerLoginService({ phone: merchantLoginForm.phone, password: merchantLoginForm.password, role: '3' })
    if (result.success) {
      // 记住手机号（登录成功才记录）
      if (merchantLoginForm.remember) {
        localStorage.setItem('remember_phone_provider', merchantLoginForm.phone)
      } else {
        localStorage.removeItem('remember_phone_provider')
      }
      // 保存 token 和商家信息（剔除密码字段，不落本地）
      const { user, token } = result.data
      const { password, ...userInfo } = user
      localStorage.setItem('token', token)
      localStorage.setItem('user_info', JSON.stringify(userInfo))
      ElMessage.success('登录成功，正在进入商家端…')
      router.push(homePath())
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    // 网络异常时 request.js 拦截器已 alert('服务异常')
    ElMessage.error('登录失败，请稍后重试')
  }
}

/* ---------- 商家入驻 ---------- */
const handleMerchantRegister = async () => {
  const valid = await merchantRegisterRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    // 后端：POST /auth/provider/register  { phone, providerName, categoryId, password, role }
    // 返回 Result { success, errorMsg, data }
    const result = await providerRegisterService({
      phone: merchantRegisterForm.phone,
      providerName: merchantRegisterForm.providerName,
      categoryId: merchantRegisterForm.categoryId,
      password: merchantRegisterForm.password,
      role: '3',
    })
    if (result.success) {
      ElMessage.success('入驻申请已提交，审核通过后可登录')
      switchToMerchantLogin()
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    ElMessage.error('入驻失败，请稍后重试')
  }
}

/* ---------- 管理员登录 ---------- */
const handleAdminLogin = async () => {
  const valid = await adminRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    // 后端：POST /auth/admin/login  { username, password, role }
    // 返回的 user 对象后端已剔除密码字段，不会落本地
    const result = await adminLoginService({ username: adminForm.username, password: adminForm.password, role: '2' })
    if (result.success) {
      // 保存 token 和账号信息（剔除密码字段，不落本地）
      const { user, token } = result.data
      const { password, ...userInfo } = user
      localStorage.setItem('token', token)
      localStorage.setItem('user_info', JSON.stringify(userInfo))
      ElMessage.success('登录成功，正在进入管理后台…')
      router.push(homePath())
    } else {
      ElMessage.error(result.errorMsg)
    }
  } catch (err) {
    // 网络异常时 request.js 拦截器已 alert('服务异常')
    ElMessage.error('登录失败，请稍后重试')
  }
}

// 页面加载时加载服务分类、回填记住的手机号
onMounted(() => {
  loadCategories()
  const remembered = localStorage.getItem('remember_phone')
  if (remembered) {
    userLoginForm.phone = remembered
    userLoginForm.remember = true
  }
  const rememberedProvider = localStorage.getItem('remember_phone_provider')
  if (rememberedProvider) {
    merchantLoginForm.phone = rememberedProvider
    merchantLoginForm.remember = true
  }
})
</script>

<template>
  <div class="login-page">
    <!-- ======== 左侧品牌区 ======== -->
    <div class="brand-panel">
      <span class="deco deco-1"></span>
      <span class="deco deco-2"></span>

      <div class="brand-content">
        <div class="brand-logo">
          <span class="logo-badge">颐</span>
          <div class="logo-text">
            <h1>颐养平台</h1>
            <p>居家养老服务预约</p>
          </div>
        </div>

        <h2 class="slogan">专业照护 · 让爱到家</h2>
        <p class="slogan-sub">为家人预约专业护工上门服务，安心每一刻</p>

        <ul class="feature-list">
          <li><span class="feature-icon"><el-icon><Food /></el-icon></span>助餐服务</li>
          <li><span class="feature-icon"><el-icon><Brush /></el-icon></span>助洁服务</li>
          <li><span class="feature-icon"><el-icon><Umbrella /></el-icon></span>助浴服务</li>
          <li><span class="feature-icon"><el-icon><FirstAidKit /></el-icon></span>康复护理</li>
        </ul>
      </div>

      <p class="copyright">© 2026 颐养平台 · 居家养老服务预约</p>
    </div>

    <!-- ======== 右侧表单区 ======== -->
    <div class="form-panel">
      <div class="form-card">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">
          {{ role === 'user' ? '家属登录，为家人预约贴心照护' : (role === 'provider' ? '商家登录，管理您的服务店铺' : '管理员登录，进入平台管理后台') }}
        </p>

        <!-- 角色切换 -->
        <div class="role-tabs">
          <button
            type="button"
            class="role-tab"
            :class="{ active: role === 'user' }"
            @click="switchRole('user')"
          >家属登录</button>
          <button
            type="button"
            class="role-tab"
            :class="{ active: role === 'provider' }"
            @click="switchRole('provider')"
          >商家登录</button>
          <button
            type="button"
            class="role-tab"
            :class="{ active: role === 'admin' }"
            @click="switchRole('admin')"
          >管理员登录</button>
        </div>

        <!-- ====== 家属：登录 ====== -->
        <el-form
          v-if="role === 'user' && !isRegister"
          ref="userLoginRef"
          :model="userLoginForm"
          :rules="userLoginRules"
          size="large"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="userLoginForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Iphone"
              maxlength="11"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="userLoginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleUserLogin"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="userLoginForm.remember">记住手机号</el-checkbox>
            <a class="link" @click="ElMessage.info('请联系管理员重置密码')">忘记密码？</a>
          </div>

          <el-button class="submit-btn" type="primary" @click="handleUserLogin">登 录</el-button>

          <p class="switch-line">
            还没有账号？<a class="link" @click="switchToRegister">立即注册</a>
          </p>
        </el-form>

        <!-- ====== 家属：注册 ====== -->
        <el-form
          v-else-if="role === 'user'"
          ref="userRegisterRef"
          :model="userRegisterForm"
          :rules="userRegisterRules"
          size="large"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="userRegisterForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Iphone"
              maxlength="11"
            />
          </el-form-item>
          <el-form-item prop="userName">
            <el-input
              v-model="userRegisterForm.userName"
              placeholder="请输入昵称"
              :prefix-icon="User"
              maxlength="20"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="userRegisterForm.password"
              type="password"
              placeholder="请设置密码（至少 6 位，含字母和数字）"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="userRegisterForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleUserRegister"
            />
          </el-form-item>

          <el-button class="submit-btn" type="primary" @click="handleUserRegister">注 册</el-button>

          <p class="switch-line">
            已有账号？<a class="link" @click="switchToLogin">返回登录</a>
          </p>
        </el-form>

        <!-- ====== 商家：登录 ====== -->
        <el-form
          v-else-if="role === 'provider' && !isRegister"
          ref="merchantLoginRef"
          :model="merchantLoginForm"
          :rules="merchantLoginRules"
          size="large"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="merchantLoginForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Iphone"
              maxlength="11"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="merchantLoginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleMerchantLogin"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="merchantLoginForm.remember">记住手机号</el-checkbox>
            <a class="link" @click="ElMessage.info('请联系平台管理员重置密码')">忘记密码？</a>
          </div>

          <el-button class="submit-btn" type="primary" @click="handleMerchantLogin">登 录</el-button>

          <p class="switch-line">
            还没有店铺？<a class="link" @click="switchToMerchantRegister">立即入驻</a>
          </p>
        </el-form>

        <!-- ====== 管理员：登录 ====== -->
        <el-form
          v-else-if="role === 'admin'"
          ref="adminRef"
          :model="adminForm"
          :rules="adminRules"
          size="large"
        >
          <el-form-item prop="username">
            <el-input
              v-model="adminForm.username"
              placeholder="请输入管理员账号"
              :prefix-icon="User"
              maxlength="30"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="adminForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleAdminLogin"
            />
          </el-form-item>

          <el-button class="submit-btn" type="primary" @click="handleAdminLogin">登 录</el-button>

          <p class="admin-hint">管理员账号由平台统一创建，如需开通请联系超管</p>
        </el-form>

        <!-- ====== 商家：入驻注册 ====== -->
        <el-form
          v-else
          ref="merchantRegisterRef"
          :model="merchantRegisterForm"
          :rules="merchantRegisterRules"
          size="large"
        >
          <el-form-item prop="phone">
            <el-input
              v-model="merchantRegisterForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Iphone"
              maxlength="11"
            />
          </el-form-item>
          <el-form-item prop="providerName">
            <el-input
              v-model="merchantRegisterForm.providerName"
              placeholder="请输入商家名称"
              :prefix-icon="User"
              maxlength="30"
            />
          </el-form-item>
          <el-form-item prop="categoryId">
            <el-select
              v-model="merchantRegisterForm.categoryId"
              placeholder="请选择主营服务分类"
              style="width: 100%"
              :loading="categoryLoading"
            >
              <el-option
                v-for="c in categoryOptions"
                :key="c.value"
                :label="c.label"
                :value="c.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="merchantRegisterForm.password"
              type="password"
              placeholder="请设置密码（至少 6 位，含字母和数字）"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="merchantRegisterForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleMerchantRegister"
            />
          </el-form-item>

          <el-button class="submit-btn" type="primary" @click="handleMerchantRegister">提交入驻申请</el-button>

          <p class="switch-line">
            已有店铺？<a class="link" @click="switchToMerchantLogin">返回登录</a>
          </p>

          <p class="admin-hint">入驻申请提交后需平台审核，审核通过后方可登录</p>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

/* ============ 左侧品牌区 ============ */
.brand-panel {
  position: relative;
  flex: 0 0 44%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  overflow: hidden;
  background: linear-gradient(160deg, #ffb26b 0%, #ff8a4c 45%, #f96d3b 100%);
  color: #fff;
}

/* 装饰光斑 */
.deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  filter: blur(2px);
}
.deco-1 {
  width: 340px;
  height: 340px;
  top: -110px;
  right: -90px;
}
.deco-2 {
  width: 220px;
  height: 220px;
  bottom: -70px;
  left: -60px;
  background: rgba(255, 255, 255, 0.1);
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 420px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 64px;
}
.logo-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 28px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}
.logo-text h1 {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 2px;
}
.logo-text p {
  margin-top: 4px;
  font-size: 13px;
  opacity: 0.85;
  letter-spacing: 1px;
}

.slogan {
  font-size: 34px;
  font-weight: 600;
  letter-spacing: 2px;
  margin-bottom: 14px;
}
.slogan-sub {
  font-size: 15px;
  opacity: 0.88;
  margin-bottom: 48px;
}

.feature-list {
  list-style: none;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 32px;
}
.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  letter-spacing: 1px;
}
.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 20px;
}

.copyright {
  position: absolute;
  bottom: 28px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  opacity: 0.7;
}

/* ============ 右侧表单区 ============ */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fdf8f4;
  padding: 24px;
}

.form-card {
  width: 420px;
  padding: 44px 40px 36px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(249, 109, 59, 0.1);
}

.form-title {
  font-size: 26px;
  font-weight: 600;
  color: #2d2a26;
}
.form-sub {
  margin: 8px 0 26px;
  font-size: 13px;
  color: #a39c92;
}

/* 角色切换 Tab */
.role-tabs {
  display: flex;
  padding: 4px;
  margin-bottom: 26px;
  border-radius: 12px;
  background: #f4f0eb;
}
.role-tab {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 9px;
  background: transparent;
  font-size: 15px;
  color: #8a8378;
  cursor: pointer;
  transition: all 0.25s;
}
.role-tab.active {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(255, 122, 69, 0.35);
}

/* 表单细节 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -6px 0 22px;
  font-size: 13px;
}

.link {
  cursor: pointer;
}
.link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 8px;
  border: none;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  box-shadow: 0 6px 16px rgba(255, 122, 69, 0.35);
  transition: all 0.25s;
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(255, 122, 69, 0.45);
}

.switch-line {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #a39c92;
}

.admin-hint {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #c0b9ae;
}

/* ============ 响应式：窄屏隐藏左侧 ============ */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
