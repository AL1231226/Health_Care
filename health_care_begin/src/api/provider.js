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
