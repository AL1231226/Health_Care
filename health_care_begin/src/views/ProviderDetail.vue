<script setup>
// ============ 商家详情页：上方商家基础信息，下方服务项目，再下方用户评价 ============
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Location, Phone, User } from '@element-plus/icons-vue'
import { getProviderDetail, getProviderComments } from '@/api/provider.js'
import { addToCart } from '@/api/cart.js'

const route = useRoute()
const router = useRouter()

// 路由参数为字符串，转数字；NaN 视为无参（直接访问 URL 的情况）
const providerId = Number(route.params.id)
const valid = !Number.isNaN(providerId) && providerId > 0

const detail = ref(null) // 商家基础信息 + 服务
const comments = ref([]) // 评价列表
const loading = ref(false)
const commentsLoading = ref(false)
const notFound = ref(false) // 商家不存在/停业/参数有误

// 商家详情 + 名下全部上架服务
const loadDetail = async () => {
  if (!valid) {
    notFound.value = true
    return
  }
  loading.value = true
  try {
    const result = await getProviderDetail(providerId)
    if (result.success) {
      detail.value = result.data
    } else {
      // 商家不存在/已停业等，兜底展示
      notFound.value = true
    }
  } catch (err) {
    ElMessage.error('获取商家信息失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 评价列表（带评价人昵称/被评服务名）
const loadComments = async () => {
  if (!valid) return
  commentsLoading.value = true
  try {
    const result = await getProviderComments(providerId)
    if (result.success) {
      comments.value = result.data || []
    } else {
      comments.value = []
    }
  } catch (err) {
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
  loadComments()
})

// 点服务行或「购买」进入下单页（当前为静态参考页，把服务展示字段带过去）
const onItemClick = (item) => {
  router.push({
    path: '/user/order',
    query: {
      itemId: item.itemId,
      itemName: item.itemName,
      price: item.price,
      unit: item.unit,
      duration: item.duration,
      providerName: detail.value.providerName,
    },
  })
}

// 服务行「加入购物车」：+1 份直接加购（该页公开可逛，未登录先引导登录）
const addingItemId = ref(null)
const onAddToCart = async (item) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再加购')
    router.push('/login')
    return
  }
  if (addingItemId.value != null) return
  addingItemId.value = item.itemId
  try {
    const result = await addToCart(item.itemId, 1)
    if (result.success) {
      ElMessage.success(`已将「${item.itemName}」加入购物车`)
    } else {
      ElMessage.error(result.errorMsg || '加入购物车失败')
    }
  } catch (err) {
    ElMessage.error('加入购物车失败，请稍后重试')
  } finally {
    addingItemId.value = null
  }
}

// 地址拼接：省市区 + 详细地址，取有值的部分；全空兜底「—」
const fullAddress = () => {
  if (!detail.value) return '—'
  const parts = [detail.value.province, detail.value.city, detail.value.district, detail.value.address].filter(Boolean)
  return parts.length ? parts.join('') : '—'
}

// 时间格式化（后端 Date 序列化成字符串，兜底原样返回）
const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 无 logo/头像时按名称取首字 + 固定渐变头像
const avatarGradients = [
  'linear-gradient(135deg, #ffa05f, #ff7a45)',
  'linear-gradient(135deg, #5fc3ff, #4c9fff)',
  'linear-gradient(135deg, #52d6a8, #34c98e)',
  'linear-gradient(135deg, #b28cff, #9b6cf5)',
  'linear-gradient(135deg, #ff9a76, #f96d3b)',
]
const avatarStyle = (name) => {
  let hash = 0
  for (const ch of name || '') hash = (hash * 31 + ch.charCodeAt(0)) % 100000
  return { background: avatarGradients[hash % avatarGradients.length] }
}
</script>

<template>
  <div class="container">
    <!-- ======== 页头 ======== -->
    <div class="page-head">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h3 class="page-title">{{ detail ? detail.providerName : '商家详情' }}</h3>
    </div>

    <!-- ======== 商家不存在 / 参数有误 ======== -->
    <el-empty v-if="notFound" description="商家不存在或已停业">
      <el-button type="primary" round @click="router.push('/user/home')">返回首页</el-button>
    </el-empty>

    <template v-else>
      <!-- ======== 商家基础信息卡 ======== -->
      <div v-loading="loading" class="info-card" v-if="detail">
        <div class="info-head">
          <img v-if="detail.logo" :src="detail.logo" class="merchant-logo" alt="商家logo" />
          <span v-else class="merchant-avatar" :style="avatarStyle(detail.providerName)">{{ (detail.providerName || '商').slice(0, 1) }}</span>
          <div class="info-main">
            <div class="merchant-name">
              {{ detail.providerName }}
              <span v-if="detail.categoryName" class="merchant-tag">{{ detail.categoryName }}</span>
            </div>
            <div class="rating">
              <template v-if="detail.score != null">★ {{ detail.score }} · {{ detail.reviewCount }} 条评价</template>
              <template v-else>暂无评价</template>
            </div>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <el-icon class="info-icon"><Location /></el-icon>
            <span class="info-label">地址</span>
            <span class="info-value">{{ fullAddress() }}</span>
          </div>
          <div class="info-item">
            <el-icon class="info-icon"><Phone /></el-icon>
            <span class="info-label">联系电话</span>
            <span class="info-value">{{ detail.phone || '—' }}</span>
          </div>
          <div class="info-item">
            <el-icon class="info-icon"><User /></el-icon>
            <span class="info-label">负责人</span>
            <span class="info-value">{{ detail.legalPerson || '—' }}</span>
          </div>
        </div>
        <p v-if="detail.intro" class="intro">{{ detail.intro }}</p>
      </div>

      <!-- ======== 服务项目区 ======== -->
      <section v-if="detail" class="section">
        <h4 class="section-title">全部服务 ({{ detail.items.length }})</h4>
        <div class="item-list">
          <div v-for="item in detail.items" :key="item.itemId" class="item-row" @click="onItemClick(item)">
            <div class="item-info">
              <span class="item-name">{{ item.itemName }}</span>
              <span class="item-desc">{{ item.detail || '暂无描述' }}</span>
              <span class="item-tags">
                <span>已售 {{ item.sales ?? 0 }}</span>
                <span>{{ item.duration }}</span>
              </span>
            </div>
            <div class="price">
              <b>¥{{ item.price }}</b>
              <i>起/{{ item.unit }}</i>
              <div class="row-btns">
                <el-button
                  size="small"
                  round
                  class="add-cart-btn"
                  :loading="addingItemId === item.itemId"
                  @click.stop="onAddToCart(item)"
                >加入购物车</el-button>
                <el-button size="small" round class="buy-btn" @click.stop="onItemClick(item)">购买</el-button>
              </div>
            </div>
          </div>
          <div v-if="!detail.items.length" class="item-empty">该商家暂未上架服务</div>
        </div>
      </section>

      <!-- ======== 评价区 ======== -->
      <section class="section" v-loading="commentsLoading">
        <h4 class="section-title">用户评价 ({{ comments.length }})</h4>
        <div v-if="comments.length" class="comment-list">
          <div v-for="c in comments" :key="c.commentId" class="comment-card">
            <span class="comment-avatar" :style="avatarStyle(c.userName)">{{ (c.userName || '匿').slice(0, 1) }}</span>
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-user">{{ c.userName || '匿名用户' }}</span>
                <el-rate :model-value="c.score" disabled size="small" class="comment-rate" />
                <span v-if="c.itemName" class="comment-item">{{ c.itemName }}</span>
                <span class="comment-time">{{ formatTime(c.createTime) }}</span>
              </div>
              <p v-if="c.content" class="comment-content">{{ c.content }}</p>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无评价，快来第一个评价吧" :image-size="80" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 40px;
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

/* ============ 商家基础信息卡 ============ */
.info-card {
  margin-top: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
}
.info-head {
  display: flex;
  align-items: center;
  gap: 16px;
}
.merchant-logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  object-fit: cover;
  flex-shrink: 0;
}
.merchant-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 18px;
  color: #fff;
  font-size: 30px;
  flex-shrink: 0;
}
.info-main {
  min-width: 0;
}
.merchant-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 600;
  color: #2d2a26;
}
.merchant-tag {
  padding: 2px 10px;
  border-radius: 999px;
  background: #fff2ec;
  font-size: 12px;
  font-weight: 400;
  color: #ff7a45;
  white-space: nowrap;
}
.rating {
  margin-top: 8px;
  font-size: 14px;
  color: #ff8a4c;
  font-weight: 600;
}

/* 信息行：服务区域 / 电话 / 负责人 */
.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 40px;
  margin-top: 20px;
  padding: 16px 0;
  border-top: 1px dashed #f4f0eb;
  border-bottom: 1px dashed #f4f0eb;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #5c564e;
}
.info-icon {
  color: #ff8a4c;
}
.info-label {
  color: #a39c92;
}
.info-value {
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.intro {
  margin-top: 16px;
  font-size: 13px;
  color: #a39c92;
  line-height: 1.7;
}

/* ============ 区块通用 ============ */
.section {
  margin-top: 24px;
}
.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #2d2a26;
  margin-bottom: 12px;
}

/* ============ 服务项目行 ============ */
.item-list {
  background: #fff;
  border-radius: 16px;
  padding: 6px 16px;
}
.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}
.item-row + .item-row {
  border-top: 1px dashed #f4f0eb;
}
.item-row:hover {
  background: #fff8f3;
}
.item-info {
  min-width: 0;
}
.item-name {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #2d2a26;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-desc {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #c0b9ae;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 560px;
}
.item-tags {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  font-size: 12px;
  color: #c0b9ae;
}
.price {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.price b {
  font-size: 20px;
  color: #ff6b3d;
  line-height: 1;
}
.price i {
  margin-left: 2px;
  font-size: 12px;
  font-style: normal;
  color: #a39c92;
}
.row-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}
.buy-btn {
  margin: 0;
  background: #fff2ec;
  border-color: #ffd6bd;
  color: #ff6b3d;
}
.buy-btn:hover {
  background: #ff6b3d;
  border-color: #ff6b3d;
  color: #fff;
}
.add-cart-btn {
  margin: 0;
  color: #ff6b3d;
}
.add-cart-btn:hover {
  color: #fff;
  background: #ff6b3d;
  border-color: #ff6b3d;
}
.item-empty {
  padding: 20px 8px;
  font-size: 13px;
  color: #c0b9ae;
  text-align: center;
}

/* ============ 评价列表 ============ */
.comment-list {
  background: #fff;
  border-radius: 16px;
  padding: 6px 20px;
}
.comment-card {
  display: flex;
  gap: 14px;
  padding: 16px 0;
}
.comment-card + .comment-card {
  border-top: 1px dashed #f4f0eb;
}
.comment-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.comment-user {
  font-size: 14px;
  font-weight: 600;
  color: #2d2a26;
}
.comment-rate {
  transform: scale(0.9);
  transform-origin: left center;
}
.comment-item {
  padding: 1px 8px;
  border-radius: 999px;
  background: #f6f4f0;
  font-size: 11px;
  color: #8a8378;
  white-space: nowrap;
}
.comment-time {
  margin-left: auto;
  font-size: 12px;
  color: #c0b9ae;
}
.comment-content {
  margin-top: 8px;
  font-size: 13px;
  color: #5c564e;
  line-height: 1.7;
}
</style>
