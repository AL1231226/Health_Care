// 老人档案相关接口（后端 ElderProfileController；token 由 request.js 自动携带，归属校验走后端 UserContext）
import request from '@/utils/request'

// 档案列表：GET /elder-profile/gets（后端按 token 解析出的 userId 过滤，只返回自己的档案）
export const listElder = () => request.get('/elder-profile/gets')

// 新增档案：POST /elder-profile/add  { elderName, gender, birthDate, idCard, phone, healthNote, addrId }
// 归属由后端按 token 解析；返回实体（自增 elderId 已回填）
export const addElder = (data) => request.post('/elder-profile/add', data)

// 修改档案：PUT /elder-profile/update  { elderId, ... }（后端按 token 校验归属）
export const updateElder = (data) => request.put('/elder-profile/update', data)

// 删除档案：DELETE /elder-profile/delete/{elderId}（后端校验归属）
export const deleteElder = (elderId) => request.delete(`/elder-profile/delete/${elderId}`)
