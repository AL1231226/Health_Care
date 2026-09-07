// 商家相关接口（后端 ServiceProviderController；token 由 request.js 自动携带）
import request from '@/utils/request'

// 商家列表（管理员）：GET /service-provider/list，status 可选 0待审核/1正常/2停用，不传查全部
export const listProviders = (status) => {
  return request.get('/service-provider/list', { params: status != null ? { status } : {} })
}

// 入驻审核（管理员）：PUT /service-provider/review/{providerId}?pass=true 通过(1) / false 驳回(2)
export const reviewProvider = (providerId, pass) => {
  return request.put(`/service-provider/review/${providerId}`, null, { params: { pass } })
}

// 启用/停用（管理员）：PUT /service-provider/status/{providerId}?status=1 或 2
export const toggleProviderStatus = (providerId, status) => {
  return request.put(`/service-provider/status/${providerId}`, null, { params: { status } })
}

// 分类下商家列表（用户端公开接口，后端 ServiceCategoryController）：GET /service-category/providers?categoryId=
export const listProvidersByCategory = (categoryId) => {
  return request.get('/service-category/providers', { params: { categoryId } })
}

// 商家详情（用户端公开接口）：GET /service-category/provider-detail?providerId=，返回基础信息 + 全部上架服务
export const getProviderDetail = (providerId) => {
  return request.get('/service-category/provider-detail', { params: { providerId } })
}

// 商家评价列表（用户端公开接口）：GET /service-category/provider-comments?providerId=，最新在前
export const getProviderComments = (providerId) => {
  return request.get('/service-category/provider-comments', { params: { providerId } })
}

// 商家本人店铺资料（商家端）：GET /service-provider/self，返回最新行（密码剔除）
export const getSelfProvider = () => {
  return request.get('/service-provider/self')
}

// 修改本人店铺资料（商家端）：PUT /service-provider/self，白名单字段（名称/负责人/简介/地址），主营分类与 phone 为入驻归属信息只读不可改
export const updateSelfProvider = (data) => {
  return request.put('/service-provider/self', data)
}
