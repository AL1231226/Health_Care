<script setup>
// ============ 家属端首页（方案A 电商式：轮播 Banner + 分类宫格 + 热门服务 + 推荐护工 + 服务流程） ============
// 说明：后端目前仅有登录/注册接口，本页数据均为静态假数据，
//       各区块已按后端接口形状组织，接口就绪后替换对应 TODO 即可。
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Food, Brush, Umbrella, FirstAidKit, Sunrise, Clock } from '@element-plus/icons-vue'
import { listCategory } from '@/api/category.js'

const router = useRouter()

/* ---------- AI 助手小精灵（先展示，AI 对话后续接入） ---------- */
const aiTips = ['你好呀，我是小颐 👋', '需要帮你预约服务吗？', '点击我可以和我聊天哦～']

/* ---------- 小精灵拖拽：按住可拖动，拖完位置固定；位移 < 5px 视为点击 ---------- */
const spriteRef = ref(null)
const dragState = { dragging: false, moved: false, startX: 0, startY: 0, baseLeft: 0, baseTop: 0 }

const onSpriteDown = (e) => {
  const el = spriteRef.value
  dragState.dragging = true
  dragState.moved = false
  dragState.startX = e.clientX
  dragState.startY = e.clientY
  const rect = el.getBoundingClientRect()
  dragState.baseLeft = rect.left
  dragState.baseTop = rect.top
  el.classList.add('dragging')
  // 捕获指针：拖出元素范围也持续接收 move/up
  try { el.setPointerCapture(e.pointerId) } catch (err) {}
}

const onSpriteMove = (e) => {
  if (!dragState.dragging) return
  const el = spriteRef.value
  const dx = e.clientX - dragState.startX
  const dy = e.clientY - dragState.startY
  if (!dragState.moved && Math.abs(dx) + Math.abs(dy) > 5) {
    dragState.moved = true
  }
  if (!dragState.moved) return
  // 限制在视口内，防止拖出屏幕找不到
  const left = Math.min(Math.max(dragState.baseLeft + dx, 8), window.innerWidth - el.offsetWidth - 8)
  const top = Math.min(Math.max(dragState.baseTop + dy, 8), window.innerHeight - el.offsetHeight - 8)
  el.style.left = `${left}px`
  el.style.top = `${top}px`
  el.style.right = 'auto'
  el.style.bottom = 'auto'
}

const onSpriteUp = () => {
  dragState.dragging = false
  spriteRef.value?.classList.remove('dragging')
}

// 拖动结束后浏览器还会补发 click，这里拦截，只有「没拖过」才算真点击
const onSpriteClick = () => {
  if (dragState.moved) {
    dragState.moved = false
    return
  }
  todo('AI 助手对话')
}
const aiTip = ref(aiTips[0])
let aiTipTimer = null
let aiTipIndex = 0
onMounted(() => {
  aiTipTimer = setInterval(() => {
    aiTipIndex = (aiTipIndex + 1) % aiTips.length
    aiTip.value = aiTips[aiTipIndex]
  }, 4000)
  loadCategories()
})
onUnmounted(() => clearInterval(aiTipTimer))

// TODO: 轮播 Banner 可改为后台配置（当前静态假数据）
const banners = [
  {
    tag: '平台宣言',
    title: '专业照护 · 让爱到家',
    sub: '为家人预约专业护工上门服务，安心每一刻',
    btn: '立即预约',
    gradient: 'linear-gradient(120deg, #ffb26b 0%, #ff7a45 100%)',
  },
  {
    tag: '新人专享',
    title: '首次预约 立减 ¥20',
    sub: '注册即可领取新人礼券，五大服务任你选',
    btn: '去逛逛',
    gradient: 'linear-gradient(120deg, #ff9a76 0%, #f96d3b 100%)',
  },
  {
    tag: '服务保障',
    title: '资质审核 · 服务可查',
    sub: '服务商家实名认证、上门签到签退，服务轨迹可查',
    btn: '立即体验',
    gradient: 'linear-gradient(120deg, #ffc08a 0%, #ff8a4c 100%)',
  },
]

// 内置分类兜底：接口失败/未启用时用这份静态数据（图标为 Element Plus 内置组件）
// 注：id 仅在接口失败时用于跳转商家列表，为按 DB 种子顺序的最佳猜测
const builtinCategories = [
  { id: 1, name: '助餐服务', desc: '营养三餐上门', icon: Food, bg: '#fff1e8', color: '#ff8a4c' },
  { id: 2, name: '助洁服务', desc: '日常清洁打扫', icon: Brush, bg: '#e8f4ff', color: '#4c9fff' },
  { id: 3, name: '助浴服务', desc: '专业安全洗浴', icon: Umbrella, bg: '#e8fff4', color: '#34c98e' },
  { id: 4, name: '助医服务', desc: '陪诊取药挂号', icon: FirstAidKit, bg: '#fdeaea', color: '#f56c6c' },
  { id: 5, name: '康复护理', desc: '理疗康复指导', icon: Sunrise, bg: '#f4edff', color: '#9b6cf5' },
]

// 分类列表：来自后端 service_category 字典表（GET /service-category/list）
// 后端 icon 字段暂为 null，前端按分类名匹配内置图标兜底；等配图后再切 <img>
const categories = ref(builtinCategories)
const loadCategories = async () => {
  try {
    const result = await listCategory()
    if (result.success && (result.data || []).length) {
      categories.value = result.data.map((c) => {
        const builtin = builtinCategories.find((b) => b.name === c.categoryName) || {}
        return {
          id: c.categoryId,
          name: c.categoryName,
          desc: builtin.desc || '',
          icon: builtin.icon || Food,
          bg: builtin.bg || '#fff1e8',
          color: builtin.color || '#ff8a4c',
        }
      })
    }
  } catch (err) {
    // 接口异常时保持内置兜底数据，不阻塞首页
  }
}

// 热门推荐：Tab 切换「服务 / 商家」
const hotTab = ref('service')

// TODO: 替换为后端 GET /service/item/hot（当前静态假数据）
const hotServices = [
  { name: '上门助餐', icon: Food, bg: '#fff1e8', color: '#ff8a4c', desc: '营养师定制午/晚餐，三菜一汤', price: 35, unit: '/次', duration: '60分钟', sales: 1286, score: 4.9 },
  { name: '全屋日常保洁', icon: Brush, bg: '#e8f4ff', color: '#4c9fff', desc: '客厅卧室厨卫全方位清洁', price: 120, unit: '/2小时', duration: '120分钟', sales: 864, score: 4.8 },
  { name: '专业助浴服务', icon: Umbrella, bg: '#e8fff4', color: '#34c98e', desc: '便携助浴设备，恒温安全', price: 98, unit: '/次', duration: '90分钟', sales: 623, score: 4.9 },
  { name: '陪诊就医', icon: FirstAidKit, bg: '#fdeaea', color: '#f56c6c', desc: '挂号取药缴费全程陪同', price: 80, unit: '/次', duration: '半天', sales: 437, score: 4.7 },
]

// TODO: 替换为后端 GET /provider/recommend（当前静态假数据，字段对齐 service_provider 表）
const merchants = [
  { name: '幸福助餐中心', category: '助餐', area: '天河区', orders: 1286, score: 4.9, color: '#ff8a4c' },
  { name: '洁丽家政', category: '助洁', area: '越秀区', orders: 864, score: 4.8, color: '#4c9fff' },
  { name: '阳光助浴坊', category: '助浴', area: '海珠区', orders: 623, score: 4.9, color: '#34c98e' },
  { name: '康乐康复中心', category: '康复', area: '白云区', orders: 437, score: 4.7, color: '#9b6cf5' },
]

// 分类卡片 → 商家列表页（带分类 id 与名称，商家列表页按 id 拉取该分类下商家）
const goMerchants = (c) => {
  router.push({ path: '/user/merchants', query: { categoryId: c.id, categoryName: c.name } })
}

// 子页面均未建设，统一占位提示
const todo = (name) => ElMessage.info(`${name}建设中，敬请期待`)
</script>

<template>
  <div class="home">
    <!-- ======== 轮播 Banner ======== -->
    <section class="banner-wrap">
      <el-carousel height="300px" :interval="4000" arrow="always">
        <el-carousel-item v-for="b in banners" :key="b.title">
          <div class="banner-slide" :style="{ background: b.gradient }">
            <div class="banner-content">
              <span class="banner-tag">{{ b.tag }}</span>
              <h2 class="banner-title">{{ b.title }}</h2>
              <p class="banner-sub">{{ b.sub }}</p>
              <el-button round class="banner-btn" @click="todo(b.btn)">{{ b.btn }}</el-button>
            </div>
            <span class="banner-deco"></span>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="container">
      <!-- ======== 服务分类 ======== -->
      <section class="block">
        <h3 class="block-title">服务分类</h3>
        <div class="category-grid">
          <div v-for="c in categories" :key="c.name" class="category-card" @click="goMerchants(c)">
            <span class="category-icon" :style="{ background: c.bg, color: c.color }">
              <el-icon :size="26"><component :is="c.icon" /></el-icon>
            </span>
            <span class="category-name">{{ c.name }}</span>
            <span class="category-desc">{{ c.desc }}</span>
          </div>
        </div>
      </section>

      <!-- ======== 热门推荐（服务 / 商家 Tab 切换） ======== -->
      <section class="block">
        <div class="block-head">
          <h3 class="block-title">热门推荐</h3>
          <div class="hot-tabs">
            <span class="hot-tab" :class="{ active: hotTab === 'service' }" @click="hotTab = 'service'">服务</span>
            <span class="hot-tab" :class="{ active: hotTab === 'merchant' }" @click="hotTab = 'merchant'">商家</span>
          </div>
        </div>
        <div v-show="hotTab === 'service'" class="service-grid">
          <div v-for="s in hotServices" :key="s.name" class="service-card">
            <div class="service-head">
              <span class="service-icon" :style="{ background: s.bg, color: s.color }">
                <el-icon :size="22"><component :is="s.icon" /></el-icon>
              </span>
              <div class="service-title">
                <h4>{{ s.name }}</h4>
                <span class="score">★ {{ s.score }}</span>
              </div>
            </div>
            <p class="service-desc">{{ s.desc }}</p>
            <div class="service-meta">
              <span><el-icon><Clock /></el-icon>{{ s.duration }}</span>
              <span>已售 {{ s.sales }}</span>
            </div>
            <div class="service-bottom">
              <div class="price"><b>¥{{ s.price }}</b><i>起{{ s.unit }}</i></div>
              <el-button type="primary" round size="small" @click="todo('预约下单')">立即预约</el-button>
            </div>
          </div>
        </div>

        <!-- 热门商家（字段对齐 service_provider 表，归属状态等后端接入后替换） -->
        <div v-show="hotTab === 'merchant'" class="merchant-grid">
          <div v-for="m in merchants" :key="m.name" class="merchant-card" @click="todo('商家详情')">
            <span class="merchant-avatar" :style="{ background: m.color }">{{ m.name.slice(0, 1) }}</span>
            <div class="merchant-info">
              <div class="merchant-name">
                {{ m.name }}
                <span class="merchant-tag">{{ m.category }}</span>
              </div>
              <div class="merchant-meta">★ {{ m.score }} · 已接单 {{ m.orders }} · 服务 {{ m.area }}</div>
            </div>
          </div>
        </div>
      </section>

    </div>

    <!-- ======== AI 助手小精灵（可拖动；点击进入 AI 对话待接入） ======== -->
    <div
      ref="spriteRef"
      class="ai-sprite"
      @pointerdown="onSpriteDown"
      @pointermove="onSpriteMove"
      @pointerup="onSpriteUp"
      @pointercancel="onSpriteUp"
      @click="onSpriteClick"
    >
      <div class="sprite-bubble">{{ aiTip }}</div>
      <div class="sprite">
        <span class="ear ear-l"></span>
        <span class="ear ear-r"></span>
        <span class="eye eye-l"></span>
        <span class="eye eye-r"></span>
        <span class="cheek cheek-l"></span>
        <span class="cheek cheek-r"></span>
        <span class="mouth"></span>
        <span class="spark">✨</span>
      </div>
      <span class="sprite-tag">AI 助手</span>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ============ 轮播 Banner ============ */
.banner-wrap {
  max-width: 1200px;
  margin: 24px auto 0;
  padding: 0 24px;
}
.banner-slide {
  position: relative;
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
}
.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 56px 64px;
  color: #fff;
}
.banner-tag {
  padding: 4px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  font-size: 13px;
  letter-spacing: 2px;
}
.banner-title {
  margin-top: 16px;
  font-size: 38px;
  font-weight: 600;
  letter-spacing: 2px;
}
.banner-sub {
  margin-top: 12px;
  font-size: 15px;
  opacity: 0.9;
}
.banner-btn {
  margin-top: 28px;
  padding: 0 32px;
  height: 42px;
  border: none;
  color: #ff7a45;
  font-size: 15px;
  font-weight: 600;
}
.banner-deco {
  position: absolute;
  right: -60px;
  top: -80px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
}

/* ============ 区块通用 ============ */
.block {
  margin-top: 40px;
}
.block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.block-title {
  position: relative;
  padding-left: 14px;
  font-size: 22px;
  font-weight: 600;
  color: #2d2a26;
}
.block-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ffa05f, #ff7a45);
}
.more {
  font-size: 13px;
  color: #a39c92;
  cursor: pointer;
}
.more:hover {
  color: #ff7a45;
}

/* ============ 服务分类 ============ */
.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-top: 20px;
}
.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 26px 12px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s;
}
.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(249, 109, 59, 0.14);
}
.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
}
.category-name {
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
}
.category-desc {
  font-size: 12px;
  color: #a39c92;
}

/* ============ 热门服务 ============ */
.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 20px;
}
.service-card {
  padding: 20px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s;
}
.service-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(249, 109, 59, 0.14);
}
.service-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  flex-shrink: 0;
}
.service-title h4 {
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
}
.score {
  font-size: 13px;
  color: #ff8a4c;
}
.service-desc {
  margin-top: 12px;
  font-size: 13px;
  color: #a39c92;
  line-height: 1.6;
}
.service-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  font-size: 12px;
  color: #c0b9ae;
}
.service-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.service-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #f4f0eb;
}
.price b {
  font-size: 22px;
  color: #ff6b3d;
}
.price i {
  margin-left: 2px;
  font-size: 12px;
  font-style: normal;
  color: #a39c92;
}

/* ============ 热门推荐 Tab ============ */
.hot-tabs {
  display: flex;
  gap: 4px;
  padding: 3px;
  background: #fff4ed;
  border-radius: 999px;
}
.hot-tab {
  padding: 5px 20px;
  border-radius: 999px;
  font-size: 13px;
  color: #8a8378;
  cursor: pointer;
  transition: all 0.2s;
}
.hot-tab.active {
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 3px 8px rgba(255, 122, 69, 0.3);
}

/* ============ 热门商家 ============ */
.merchant-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 20px;
}
.merchant-card {
  display: flex;
  gap: 14px;
  padding: 20px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s;
}
.merchant-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(249, 109, 59, 0.14);
}
.merchant-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}
.merchant-info {
  min-width: 0;
}
.merchant-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
}
.merchant-tag {
  padding: 1px 8px;
  border-radius: 999px;
  background: #fff2ec;
  font-size: 11px;
  font-weight: 400;
  color: #ff7a45;
}
.merchant-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #a39c92;
}

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .service-grid,
  .merchant-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 700px) {
  .service-grid,
  .merchant-grid,
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .banner-content {
    padding: 36px 28px;
  }
  .banner-title {
    font-size: 26px;
  }
}

/* ============ AI 助手小精灵（悬浮右下角） ============ */
.ai-sprite {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: grab;
  user-select: none;
  /* 触屏拖动时不让页面跟着滚动 */
  touch-action: none;
}
.ai-sprite.dragging {
  cursor: grabbing;
}
.ai-sprite:hover .sprite {
  transform: scale(1.06);
}

/* 欢迎语气泡（箭头指向精灵，随文案 4s 一轮淡入淡出） */
.sprite-bubble {
  position: relative;
  margin-bottom: 10px;
  padding: 8px 14px;
  background: #fff;
  border-radius: 10px;
  font-size: 13px;
  color: #5c564e;
  white-space: nowrap;
  box-shadow: 0 4px 14px rgba(249, 109, 59, 0.16);
  animation: bubble-fade 4s ease-in-out infinite;
}
.sprite-bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -8px;
  transform: translateX(-50%);
  border: 8px solid transparent;
  border-top-color: #fff;
  border-bottom: none;
}

/* 精灵本体：圆润身体 + 猫耳 + 眨眼 / 悬浮动画 */
.sprite {
  position: relative;
  width: 74px;
  height: 66px;
  background: linear-gradient(145deg, #ffb26b, #ff7a45);
  border-radius: 46% 46% 50% 50% / 42% 42% 58% 58%;
  box-shadow: 0 8px 20px rgba(255, 122, 69, 0.38), inset 0 -8px 14px rgba(255, 255, 255, 0.18);
  animation: sprite-bob 3s ease-in-out infinite;
  transition: transform 0.2s;
}
.ear {
  position: absolute;
  top: -14px;
  width: 22px;
  height: 22px;
  background: linear-gradient(145deg, #ffa05f, #ff7a45);
  border-radius: 6px 6px 0 0;
}
.ear-l {
  left: 8px;
  transform: rotate(-18deg);
}
.ear-r {
  right: 8px;
  transform: rotate(18deg);
}
.eye {
  position: absolute;
  top: 26px;
  width: 8px;
  height: 10px;
  border-radius: 50%;
  background: #4a3526;
  animation: sprite-blink 4.2s infinite;
}
.eye-l {
  left: 22px;
}
.eye-r {
  right: 22px;
}
.cheek {
  position: absolute;
  top: 38px;
  width: 10px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
}
.cheek-l {
  left: 12px;
}
.cheek-r {
  right: 12px;
}
.mouth {
  position: absolute;
  left: 50%;
  top: 42px;
  transform: translateX(-50%);
  width: 14px;
  height: 7px;
  border-bottom: 2px solid rgba(74, 53, 38, 0.7);
  border-radius: 0 0 100% 100%;
}
/* 头顶小星星 */
.spark {
  position: absolute;
  top: -18px;
  right: -8px;
  font-size: 18px;
  animation: spark-twinkle 2.5s ease-in-out infinite;
}

/* 底部 AI 标签 */
.sprite-tag {
  margin-top: 8px;
  padding: 2px 12px;
  border-radius: 999px;
  background: #fff;
  font-size: 12px;
  color: #ff7a45;
  box-shadow: 0 3px 10px rgba(249, 109, 59, 0.2);
}

/* 动画：上下悬浮 / 眨眼 / 气泡淡入淡出 / 星星闪烁 */
@keyframes sprite-bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
@keyframes sprite-blink {
  0%, 90%, 100% { transform: scaleY(1); }
  93%, 95% { transform: scaleY(0.1); }
}
@keyframes bubble-fade {
  0%, 8% { opacity: 0; transform: translateY(4px); }
  15%, 82% { opacity: 1; transform: translateY(0); }
  96%, 100% { opacity: 0; transform: translateY(4px); }
}
@keyframes spark-twinkle {
  0%, 100% { opacity: 0.4; transform: rotate(0deg) scale(0.9); }
  50% { opacity: 1; transform: rotate(20deg) scale(1.15); }
}

/* 移动端缩小一点，别挡内容 */
@media (max-width: 700px) {
  .ai-sprite {
    right: 16px;
    bottom: 16px;
    transform: scale(0.9);
  }
}
</style>
