<script setup>
// ============ 商家列表页：首页「服务分类」点进来，展示该分类下的商家（含该分类上架的服务项目） ============
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { listProvidersByCategory } from '@/api/provider.js'
import { listCategory } from '@/api/category.js'

const route = useRoute()
const router = useRouter()

// query 均为字符串，转数字；NaN 视为无参（直接访问 URL 的情况）
const categoryId = Number(route.query.categoryId)
const valid = !Number.isNaN(categoryId) && categoryId > 0

const pageTitle = ref(route.query.categoryName || '服务分类')
const providers = ref([])
const loading = ref(false)

// query 没带分类名称时，按 id 从分类字典反查标题（兜底）
const resolveTitle = async () => {
  if (route.query.categoryName || !valid) return
  try {
    const result = await listCategory()
    const match = (result.data || []).find((c) => c.categoryId === categoryId)
    if (match) pageTitle.value = match.categoryName
  } catch (err) {
    // 反查失败保持默认标题
  }
}

// 该分类下的商家列表（商家 + 该分类上架的服务项目）
const loadProviders = async () => {
  if (!valid) return
  loading.value = true
  try {
    const result = await listProvidersByCategory(categoryId)
    if (result.success) {
      providers.value = result.data || []
    } else {
      ElMessage.error(result.errorMsg || '获取商家列表失败，请稍后重试')
      providers.value = []
    }
  } catch (err) {
    ElMessage.error('获取商家列表失败，请稍后重试')
    providers.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  resolveTitle()
  loadProviders()
})

// 点商家卡片进商家详情页；预约下单仍占位
const onMerchantClick = (p) => router.push(`/user/merchant/${p.providerId}`)
const onItemClick = () => ElMessage.info('预约下单建设中，敬请期待')

// 无 logo 时按商家名取首字 + 固定渐变头像
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
      <h3 class="page-title">{{ pageTitle }}</h3>
      <span class="page-count">共 {{ providers.length }} 家商家</span>
    </div>

    <!-- ======== 参数有误：直接从 URL 进入且无 categoryId ======== -->
    <el-empty v-if="!valid" description="参数有误，请从首页服务分类进入">
      <el-button type="primary" round @click="router.push('/user/home')">返回首页</el-button>
    </el-empty>

    <!-- ======== 商家列表 ======== -->
    <div v-else v-loading="loading" class="merchant-grid">
      <div v-for="p in providers" :key="p.providerId" class="merchant-card" @click="onMerchantClick(p)">
        <div class="merchant-head">
          <img v-if="p.logo" :src="p.logo" class="merchant-avatar-img" alt="商家logo" />
          <span v-else class="merchant-avatar" :style="avatarStyle(p.providerName)">{{ (p.providerName || '商').slice(0, 1) }}</span>
          <div class="merchant-info">
            <div class="merchant-name">
              {{ p.providerName }}
              <span class="merchant-tag">{{ pageTitle }}</span>
            </div>
            <div class="merchant-meta">
              <span class="rating">
                <template v-if="p.score != null">★ {{ p.score }} · {{ p.reviewCount }} 条评价</template>
                <template v-else>暂无评价</template>
              </span>
              <i>·</i>
              服务区域 {{ p.district || p.address || '—' }}
              <i>·</i>
              已上架 {{ p.items.length }} 项服务
            </div>
          </div>
        </div>
        <p v-if="p.intro" class="merchant-intro">{{ p.intro }}</p>

        <!-- 该分类下上架的服务项目 -->
        <div class="item-list">
          <div v-for="item in p.items" :key="item.itemId" class="item-row" @click.stop="onItemClick">
            <div class="item-info">
              <span class="item-name">{{ item.itemName }}</span>
              <span class="item-tags">
                <span>★ {{ item.score != null ? item.score : '暂无评分' }}</span>
                <span>已售 {{ item.sales ?? 0 }}</span>
                <span>{{ item.duration }}</span>
              </span>
            </div>
            <div class="price">
              <b>¥{{ item.price }}</b>
              <i>起/{{ item.unit }}</i>
            </div>
          </div>
          <div v-if="!p.items.length" class="item-empty">该分类下暂未上架服务</div>
        </div>
      </div>
    </div>

    <!-- ======== 空态：分类下暂无商家 ======== -->
    <el-empty v-if="valid && !loading && !providers.length" description="该分类下暂无商家，敬请期待">
      <el-button type="primary" round @click="router.push('/user/home')">返回首页</el-button>
    </el-empty>
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
.page-count {
  font-size: 13px;
  color: #a39c92;
}

/* ============ 商家卡片 ============ */
.merchant-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;
  min-height: 200px;
}
.merchant-card {
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
.merchant-head {
  display: flex;
  align-items: center;
  gap: 14px;
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
.merchant-avatar-img {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  object-fit: cover;
  flex-shrink: 0;
}
.merchant-info {
  min-width: 0;
}
.merchant-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
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
  white-space: nowrap;
}
.merchant-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #a39c92;
}
.rating {
  color: #ff8a4c;
  font-weight: 600;
}
.merchant-meta i {
  margin: 0 4px;
  font-style: normal;
}
.merchant-intro {
  margin-top: 12px;
  font-size: 13px;
  color: #a39c92;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ============ 服务项目行 ============ */
.item-list {
  margin-top: 14px;
  padding-top: 4px;
  border-top: 1px dashed #f4f0eb;
}
.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 10px;
  transition: background 0.2s;
}
.item-row:hover {
  background: #fff8f3;
}
.item-info {
  min-width: 0;
}
.item-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #2d2a26;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-tags {
  display: flex;
  gap: 10px;
  margin-top: 4px;
  font-size: 12px;
  color: #c0b9ae;
}
.price {
  flex-shrink: 0;
  text-align: right;
}
.price b {
  font-size: 18px;
  color: #ff6b3d;
}
.price i {
  margin-left: 2px;
  font-size: 12px;
  font-style: normal;
  color: #a39c92;
}
.item-empty {
  padding: 14px 8px;
  font-size: 12px;
  color: #c0b9ae;
  text-align: center;
}

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .merchant-grid {
    grid-template-columns: 1fr;
  }
}
</style>
