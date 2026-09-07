<script setup>
// ============ 服务详情页：单个服务信息卡（含商家入口）+ 该服务用户评价列表 ============
// 入口：首页热门服务卡 / 商家详情页服务行 / 分类商家页服务行 / 搜索结果服务行（点卡主体进来）
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getItemDetail, getItemComments } from '@/api/provider.js'

const route = useRoute()
const router = useRouter()

// 路由参数为字符串，转数字；NaN 视为无参（直接访问 URL 的情况）
const itemId = Number(route.params.itemId)
const valid = !Number.isNaN(itemId) && itemId > 0

const detail = ref(null) // 服务信息（itemName/price/unit/duration/detail/score/sales/providerId/providerName）
const comments = ref([]) // 该服务评价列表
const loading = ref(false)
const commentsLoading = ref(false)
const notFound = ref(false) // 服务不存在/已下架/参数有误

// 服务详情（后端校验：仅上架且归属商家正常的服务可见）
const loadDetail = async () => {
  if (!valid) {
    notFound.value = true
    return
  }
  loading.value = true
  try {
    const result = await getItemDetail(itemId)
    if (result.success) {
      detail.value = result.data
    } else {
      notFound.value = true
    }
  } catch (err) {
    ElMessage.error('获取服务信息失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 该服务的评价列表（带评价人昵称，最新在前）
const loadComments = async () => {
  if (!valid) return
  commentsLoading.value = true
  try {
    const result = await getItemComments(itemId)
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

// 点服务名旁的商家 → 商家详情页（可看该商家全部服务与评价）
const goProvider = () => {
  if (detail.value?.providerId) router.push(`/user/merchant/${detail.value.providerId}`)
}

// 「立即预约」→ 下单确认页（带齐字段，OrderConfirm 直读 query 不再拉接口；未登录先引导登录）
const goOrder = () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再预约服务')
    router.push('/login')
    return
  }
  const d = detail.value
  router.push({
    path: '/user/order',
    query: {
      itemId: d.itemId, itemName: d.itemName, price: d.price,
      unit: d.unit, duration: d.duration || '', providerName: d.providerName || '',
    },
  })
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
      <h3 class="page-title">{{ detail ? detail.itemName : '服务详情' }}</h3>
    </div>

    <!-- ======== 服务不存在 / 已下架 / 参数有误 ======== -->
    <el-empty v-if="notFound" description="服务不存在或已下架">
      <el-button type="primary" round @click="router.push('/user/home')">返回首页</el-button>
    </el-empty>

    <template v-else>
      <!-- ======== 服务信息卡 ======== -->
      <div v-if="detail" v-loading="loading" class="svc-card">
        <div class="svc-head">
          <span class="svc-avatar" :style="avatarStyle(detail.itemName)">{{ (detail.itemName || '服').slice(0, 1) }}</span>
          <div class="svc-main">
            <h2 class="svc-name">{{ detail.itemName }}</h2>
            <!-- 评分（后端实时口径）与销量/时长元信息 -->
            <div class="rate-row">
              <el-rate v-if="detail.score != null" :model-value="detail.score" disabled size="small" class="svc-rate" />
              <span v-if="detail.score != null" class="rate-text">★ {{ detail.score }}</span>
              <span v-else class="rate-none">暂无评分</span>
            </div>
            <div class="svc-tags">
              <span class="svc-tag">已售 {{ detail.sales ?? 0 }}</span>
              <span v-if="detail.duration" class="svc-tag">{{ detail.duration }}</span>
              <span class="svc-tag">按{{ detail.unit }}计费</span>
            </div>
            <!-- 商家入口：进店看该商家全部服务与评价 -->
            <div v-if="detail.providerId" class="provider-link" @click="goProvider">
              <span class="provider-avatar" :style="avatarStyle(detail.providerName)">{{ (detail.providerName || '商').slice(0, 1) }}</span>
              <span class="provider-name">{{ detail.providerName }}</span>
              <span class="enter">进店 ›</span>
            </div>
          </div>
          <div class="svc-side">
            <div class="price"><b>¥{{ detail.price }}</b><i>起/{{ detail.unit }}</i></div>
            <el-button type="primary" round size="large" class="order-btn" @click="goOrder">立即预约</el-button>
          </div>
        </div>

        <!-- 服务介绍 -->
        <div class="svc-desc">
          <h4>服务介绍</h4>
          <p>{{ detail.detail || '暂无详细介绍，可在预约下单后与服务人员沟通。' }}</p>
        </div>
      </div>

      <!-- ======== 该服务评价区 ======== -->
      <section v-if="detail" class="section" v-loading="commentsLoading">
        <h4 class="section-title">用户评价 ({{ comments.length }})</h4>
        <div v-if="comments.length" class="comment-list">
          <div v-for="c in comments" :key="c.commentId" class="comment-card">
            <span class="comment-avatar" :style="avatarStyle(c.userName)">{{ (c.userName || '匿').slice(0, 1) }}</span>
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-user">{{ c.userName || '匿名用户' }}</span>
                <el-rate :model-value="c.score" disabled size="small" class="comment-rate" />
                <span class="comment-time">{{ formatTime(c.createTime) }}</span>
              </div>
              <p v-if="c.content" class="comment-content">{{ c.content }}</p>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无评价，服务体验后欢迎评价" :image-size="80" />
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
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ============ 服务信息卡 ============ */
.svc-card {
  margin-top: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
}
.svc-head {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}
.svc-avatar {
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
.svc-main {
  flex: 1;
  min-width: 0;
}
.svc-name {
  font-size: 22px;
  font-weight: 600;
  color: #2d2a26;
}
.rate-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.svc-rate {
  transform: scale(0.95);
  transform-origin: left center;
}
.rate-text {
  font-size: 13px;
  color: #ff8a4c;
  font-weight: 600;
}
.rate-none {
  font-size: 13px;
  color: #c0b9ae;
}
.svc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.svc-tag {
  padding: 2px 10px;
  border-radius: 999px;
  background: #f6f4f0;
  font-size: 12px;
  color: #8a8378;
}
/* 商家入口行 */
.provider-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 6px 12px 6px 6px;
  border-radius: 999px;
  background: #fff8f3;
  cursor: pointer;
  transition: background 0.2s;
}
.provider-link:hover {
  background: #fff2ec;
}
.provider-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  flex-shrink: 0;
}
.provider-name {
  font-size: 13px;
  font-weight: 500;
  color: #5c564e;
}
.enter {
  font-size: 12px;
  color: #ff7a45;
}
/* 右侧价格与预约 */
.svc-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
  padding-top: 4px;
}
.price b {
  font-size: 26px;
  color: #ff6b3d;
}
.price i {
  margin-left: 2px;
  font-size: 12px;
  font-style: normal;
  color: #a39c92;
}
.order-btn {
  border: none;
  padding: 20px 34px;
  font-size: 15px;
}

/* ============ 服务介绍 ============ */
.svc-desc {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px dashed #f4f0eb;
}
.svc-desc h4 {
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
}
.svc-desc p {
  margin-top: 10px;
  font-size: 13px;
  color: #5c564e;
  line-height: 1.8;
  white-space: pre-wrap;
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

/* ============ 评价列表（与商家详情页同款） ============ */
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

/* ============ 响应式：窄屏右侧价格/按钮折行到底部 ============ */
@media (max-width: 760px) {
  .svc-head {
    flex-wrap: wrap;
  }
  .svc-side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding-top: 0;
  }
}
</style>
