// 服务分类字典接口（后端 ServiceCategoryController；公开接口，未加 JWT 拦截）
import request from '@/utils/request'

// 启用中的分类列表：GET /service-category/list（后端按 sort 升序返回 status=1 的分类）
export const listCategory = () => request.get('/service-category/list')
