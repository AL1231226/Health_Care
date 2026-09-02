// 路由配置
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    // 用户端：公共布局（顶部导航 + Footer）包裹各子页面
    path: '/user',
    component: () => import('../layout/UserLayout.vue'),
    children: [
      { path: '', redirect: 'home' },
      { path: 'home', component: () => import('../views/UserHome.vue') },
      { path: 'profile', component: () => import('../views/UserProfile.vue') },
      { path: 'orders', component: () => import('../views/UserOrders.vue') },
      { path: 'address', component: () => import('../views/UserAddress.vue') },
      { path: 'elder', component: () => import('../views/UserElder.vue') },
      { path: 'merchants', component: () => import('../views/ServiceProviders.vue') },
      { path: 'merchant/:id', component: () => import('../views/ProviderDetail.vue') },
      { path: 'order', component: () => import('../views/OrderConfirm.vue') },
    ],
  },
  { path: '/admin/home', component: () => import('../views/AdminHome.vue') },
  { path: '/provider/home', component: () => import('../views/MerchantHome.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
