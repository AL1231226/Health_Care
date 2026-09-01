<script setup>
// ============ 家属端首页（方案C 混合式：问候条+订单状态 + 分类宫格 + 热门服务 + 推荐护工 + 服务流程） ============
// 说明：后端目前仅有登录/注册接口，本页数据均为静态假数据，
//       各区块已按后端接口形状组织，接口就绪后替换对应 TODO 即可。
// 方案A（电商式，带轮播 Banner）已备份至 src/views/backup/UserHome-variant-A.vue
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Food, Brush, Umbrella, FirstAidKit, Sunrise, Clock } from '@element-plus/icons-vue'

/* ---------- 问候条 ---------- */
const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 11) return '早上好'
  if (hour >= 11 && hour < 13) return '中午好'
  if (hour >= 13 && hour < 18) return '下午好'
  return '晚上好'
}
const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}')
const greeting = computed(() => `${getGreeting()}，${userInfo.userName || '家属'}`)

// TODO: 替换为后端 GET /order/status/count（当前静态假数据）
const orderStats = [
  { label: '待支付', count: 1 },
  { label: '待服务', count: 0 },
  { label: '服务中', count: 1 },
]

// TODO: 替换为后端 GET /service/category/list（当前静态假数据）
const categories = [
  { name: '助餐服务', desc: '营养三餐上门', icon: Food, bg: '#fff1e8', color: '#ff8a4c' },
  { name: '助洁服务', desc: '日常清洁打扫', icon: Brush, bg: '#e8f4ff', color: '#4c9fff' },
  { name: '助浴服务', desc: '专业安全洗浴', icon: Umbrella, bg: '#e8fff4', color: '#34c98e' },
  { name: '助医服务', desc: '陪诊取药挂号', icon: FirstAidKit, bg: '#fdeaea', color: '#f56c6c' },
  { name: '康复护理', desc: '理疗康复指导', icon: Sunrise, bg: '#f4edff', color: '#9b6cf5' },
]

// TODO: 替换为后端 GET /service/item/hot（当前静态假数据）
const hotServices = [
  { name: '上门助餐', icon: Food, bg: '#fff1e8', color: '#ff8a4c', desc: '营养师定制午/晚餐，三菜一汤', price: 35, unit: '/次', duration: '60分钟', sales: 1286, score: 4.9 },
  { name: '全屋日常保洁', icon: Brush, bg: '#e8f4ff', color: '#4c9fff', desc: '客厅卧室厨卫全方位清洁', price: 120, unit: '/2小时', duration: '120分钟', sales: 864, score: 4.8 },
  { name: '专业助浴服务', icon: Umbrella, bg: '#e8fff4', color: '#34c98e', desc: '便携助浴设备，恒温安全', price: 98, unit: '/次', duration: '90分钟', sales: 623, score: 4.9 },
  { name: '陪诊就医', icon: FirstAidKit, bg: '#fdeaea', color: '#f56c6c', desc: '挂号取药缴费全程陪同', price: 80, unit: '/次', duration: '半天', sales: 437, score: 4.7 },
]

// TODO: 替换为后端 GET /caregiver/recommend（当前静态假数据）
const caregivers = [
  { name: '李秀兰', skills: ['助餐', '助洁'], years: 8, orders: 1260, score: 4.9, color: '#ff8a4c' },
  { name: '王建国', skills: ['康复护理', '助医'], years: 12, orders: 2103, score: 4.9, color: '#4c9fff' },
  { name: '张桂芳', skills: ['助浴', '助洁'], years: 6, orders: 980, score: 4.8, color: '#34c98e' },
  { name: '陈立新', skills: ['助医', '助餐'], years: 5, orders: 765, score: 4.8, color: '#9b6cf5' },
]

const steps = [
  { title: '在线预约', desc: '选服务定时间' },
  { title: '系统派单', desc: '匹配附近护工' },
  { title: '护工上门', desc: '定位签到服务' },
  { title: '确认评价', desc: '完成后可评价' },
]

// 子页面均未建设，统一占位提示
const todo = (name) => ElMessage.info(`${name}建设中，敬请期待`)
</script>

<template>
  <div class="home">
    <div class="container">
      <!-- ======== 问候条 + 订单状态 ======== -->
      <section class="greeting-bar">
        <div class="greeting-text">
          <h2>{{ greeting }}</h2>
          <p>为家人预约专业照护，安心每一刻</p>
        </div>
        <div class="order-chips">
          <div v-for="s in orderStats" :key="s.label" class="chip" @click="todo('我的订单')">
            <span class="chip-count">{{ s.count }}</span>
            <span class="chip-label">{{ s.label }}</span>
          </div>
        </div>
      </section>

      <!-- ======== 服务分类 ======== -->
      <section class="block">
        <h3 class="block-title">服务分类</h3>
        <div class="category-grid">
          <div v-for="c in categories" :key="c.name" class="category-card" @click="todo(c.name)">
            <span class="category-icon" :style="{ background: c.bg, color: c.color }">
              <el-icon :size="26"><component :is="c.icon" /></el-icon>
            </span>
            <span class="category-name">{{ c.name }}</span>
            <span class="category-desc">{{ c.desc }}</span>
          </div>
        </div>
      </section>

      <!-- ======== 热门服务 ======== -->
      <section class="block">
        <div class="block-head">
          <h3 class="block-title">热门服务</h3>
          <a class="more" @click="todo('全部服务')">查看全部 ›</a>
        </div>
        <div class="service-grid">
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
      </section>

      <!-- ======== 推荐护工 ======== -->
      <section class="block">
        <div class="block-head">
          <h3 class="block-title">推荐护工</h3>
          <a class="more" @click="todo('护工列表')">查看全部 ›</a>
        </div>
        <div class="caregiver-grid">
          <div v-for="g in caregivers" :key="g.name" class="caregiver-card">
            <span class="caregiver-avatar" :style="{ background: g.color }">{{ g.name.slice(0, 1) }}</span>
            <div class="caregiver-info">
              <div class="caregiver-name">
                {{ g.name }}
                <span class="verified">已认证</span>
              </div>
              <div class="skill-tags">
                <span v-for="tag in g.skills" :key="tag" class="skill-tag">{{ tag }}</span>
              </div>
              <div class="caregiver-meta">从业 {{ g.years }} 年 · 完成 {{ g.orders }} 单 · ★ {{ g.score }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- ======== 服务流程 ======== -->
      <section class="block">
        <h3 class="block-title">服务流程</h3>
        <div class="steps">
          <template v-for="(step, i) in steps" :key="step.title">
            <div class="step">
              <span class="step-num">{{ i + 1 }}</span>
              <span class="step-title">{{ step.title }}</span>
              <span class="step-desc">{{ step.desc }}</span>
            </div>
            <span v-if="i < steps.length - 1" class="step-arrow">→</span>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ============ 问候条 + 订单状态 ============ */
.greeting-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding: 26px 32px;
  border-radius: 16px;
  background: linear-gradient(120deg, #ffb26b 0%, #ff7a45 100%);
  color: #fff;
}
.greeting-text h2 {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 1px;
}
.greeting-text p {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.9;
}
.order-chips {
  display: flex;
  gap: 12px;
}
.chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 78px;
  padding: 10px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.22);
  cursor: pointer;
  transition: all 0.2s;
}
.chip:hover {
  background: rgba(255, 255, 255, 0.34);
}
.chip-count {
  font-size: 20px;
  font-weight: 600;
}
.chip-label {
  font-size: 12px;
  opacity: 0.9;
}

/* ============ 区块通用 ============ */
.block {
  margin-top: 36px;
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

/* ============ 推荐护工 ============ */
.caregiver-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 20px;
}
.caregiver-card {
  display: flex;
  gap: 14px;
  padding: 20px;
  background: #fff;
  border-radius: 16px;
  transition: all 0.25s;
}
.caregiver-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(249, 109, 59, 0.14);
}
.caregiver-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}
.caregiver-info {
  min-width: 0;
}
.caregiver-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
}
.verified {
  padding: 1px 8px;
  border: 1px solid #ffbe9e;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 400;
  color: #ff7a45;
}
.skill-tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}
.skill-tag {
  padding: 2px 8px;
  border-radius: 4px;
  background: #fff2ec;
  font-size: 12px;
  color: #ff7a45;
}
.caregiver-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #a39c92;
}

/* ============ 服务流程 ============ */
.steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding: 32px 40px;
  background: #fff;
  border-radius: 16px;
}
.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffa05f, #ff7a45);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.step-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
}
.step-desc {
  font-size: 12px;
  color: #a39c92;
}
.step-arrow {
  font-size: 22px;
  color: #ffbe9e;
}

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .service-grid,
  .caregiver-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 700px) {
  .service-grid,
  .caregiver-grid,
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .greeting-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
