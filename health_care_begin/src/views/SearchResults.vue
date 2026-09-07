<script setup>
// ============ 搜索结果页：顶栏搜索框带 keyword 进来，服务 / 商家 双 Tab 展示 ============
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { searchAll } from '@/api/provider.js'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const items = ref([])
const providers = ref([])
const loading = ref(false)
const activeTab = ref('items')
const noKeyword = computed(() => !keyword.value)

// query 无关键词（直接访问 URL）：空态提示，不请求
const load = async () => {
  const kw = (route.query.keyword || '').toString().trim()
  keyword.value = kw
  if (!kw) {
    items.value = []
    providers.value = []
    return
  }
  loading.value = true
  try {
    const result = await searchAll(kw)
    if (result.success) {
      items.value = result.data?.items || []
      providers.value = result.data?.providers || []
      // 默认落在有结果的 Tab（服务优先）
      activeTab.value = items.value.length ? 'items' : providers.value.length ? 'providers' : 'items'
    } else {
      ElMessage.error(result.errorMsg || '搜索失败，请稍后重试')
      items.value = []
      providers.value = []
    }
  } catch (err) {
    ElMessage.error('搜索失败，请稍后重试')
    items.value = []
    providers.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
// 顶栏搜索框再次搜索：同路由仅 query 变化，组件复用需重拉
watch(() => route.query.keyword, load)

// 服务结果行 → 服务详情页（看介绍与该服务评价，可预约下单）；商家卡 → 商家详情页
const goService = (it) => router.push(`/user/item/${it.itemId}`)
const goMerchant = (providerId) => router.push(`/user/merchant/${providerId}`)

// 无 logo 时按商家名取首字 + 固定渐变头像（与商家列表页同款）
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
      <h3 class="page-title" v-if="noKeyword">搜索</h3>
      <h3 class="page-title" v-else>“{{ keyword }}”的搜索结果</h3>
    </div>

    <!-- ======== 无关键词：直接访问 ======== -->
    <el-empty v-if="noKeyword" description="请输入搜索关键词，搜索服务或商家">
      <el-button type="primary" round @click="router.push('/user/home')">返回首页</el-button>
    </el-empty>

    <!-- ======== 结果区 ======== -->
    <template v-else>
      <div v-loading="loading" class="result-area" :min-height="120">
        <!-- 有结果：服务 / 商家 双 Tab -->
        <template v-if="!loading && (items.length || providers.length)">
          <el-tabs v-model="activeTab">
            <el-tab-pane :label="`相关服务 (${items.length})`" name="items">
              <div v-for="it in items" :key="it.itemId" class="svc-row" @click="goService(it)">
                <div class="svc-main">
                  <span class="svc-name">{{ it.itemName }}</span>
                  <span class="svc-sub">
                    <span class="svc-provider">{{ it.providerName }}</span>
                    <i>·</i>
                    <span class="rating">★ {{ it.score != null ? it.score : '暂无评分' }}</span>
                    <i>·</i>
                    <span>已售 {{ it.sales ?? 0 }}</span>
                    <template v-if="it.duration"><i>·</i><span>{{ it.duration }}</span></template>
                  </span>
                </div>
                <div class="svc-price">
                  <b>¥{{ it.price }}</b>
                  <i>起/{{ it.unit }}</i>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="`相关商家 (${providers.length})`" name="providers">
              <div v-if="providers.length" class="merchant-list">
                <div v-for="p in providers" :key="p.providerId" class="merchant-card" @click="goMerchant(p.providerId)">
                  <img v-if="p.logo" :src="p.logo" class="merchant-avatar-img" alt="商家logo" />
                  <span v-else class="merchant-avatar" :style="avatarStyle(p.providerName)">{{ (p.providerName || '商').slice(0, 1) }}</span>
                  <div class="merchant-info">
                    <div class="merchant-name">
                      {{ p.providerName }}
                      <span v-if="p.categoryName" class="merchant-tag">{{ p.categoryName }}</span>
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
                    <p v-if="p.intro" class="merchant-intro">{{ p.intro }}</p>
                  </div>
                  <span class="go-detail">进入商家 ›</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>

        <!-- 空态：无任何命中 -->
        <el-empty v-if="!loading && !items.length && !providers.length" :description="`未找到与“${keyword}”相关的内容，换个关键词试试`">
          <el-button type="primary" round @click="router.push('/user/home')">返回首页</el-button>
        </el-empty>
      </div>
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
  font-size: 20px;
  font-weight: 600;
  color: #2d2a26;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.result-area {
  margin-top: 8px;
  min-height: 240px;
}

/* ============ 服务结果行（可点击进商家详情） ============ */
.svc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.svc-row:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(249, 109, 59, 0.12);
}
.svc-main {
  min-width: 0;
}
.svc-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #2d2a26;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.svc-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: #a39c92;
}
.svc-sub i {
  font-style: normal;
  color: #d8d1c8;
}
.svc-provider {
  color: #7a7267;
}
.rating {
  color: #ff8a4c;
  font-weight: 600;
}
.svc-price {
  flex-shrink: 0;
  text-align: right;
}
.svc-price b {
  font-size: 18px;
  color: #ff6b3d;
}
.svc-price i {
  margin-left: 2px;
  font-size: 12px;
  font-style: normal;
  color: #a39c92;
}

/* ============ 商家结果卡（可点击进商家详情） ============ */
.merchant-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: #fff;
  border-radius: 14px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.merchant-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(249, 109, 59, 0.12);
}
.merchant-avatar,
.merchant-avatar-img {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  object-fit: cover;
  flex-shrink: 0;
}
.merchant-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
}
.merchant-info {
  flex: 1;
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
  margin-top: 6px;
  font-size: 12px;
  color: #a39c92;
}
.merchant-meta i {
  margin: 0 4px;
  font-style: normal;
}
.merchant-intro {
  margin: 8px 0 0;
  font-size: 13px;
  color: #a39c92;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.go-detail {
  flex-shrink: 0;
  font-size: 13px;
  color: #ff7a45;
}
</style>
