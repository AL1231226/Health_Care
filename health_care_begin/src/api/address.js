// 地址管理相关接口（后端 UserAddressController；token 由 request.js 自动携带，归属校验走后端 UserContext）
import request from '@/utils/request'

// 地址列表：GET /address/gets（后端按 token 解析出的 userId 过滤，只返回自己的地址）
export const listAddress = () => request.get('/address/gets')

// 新增地址：POST /address/add  { province, city, district, detailAddr, phone, isDefault }
// 归属由后端按 token 解析；返回实体（自增 addrId 已回填）
export const addAddress = (data) => request.post('/address/add', data)

// 修改地址：PUT /address/update  { addrId, ... }（后端按 token 校验归属，只更新非空字段）
export const updateAddress = (data) => request.put('/address/update', data)

// 删除地址：DELETE /address/delete/{addrId}（后端校验归属）
export const deleteAddress = (addrId) => request.delete(`/address/delete/${addrId}`)

// 设为默认地址：PUT /address/set-default/{addrId}（后端事务处理：先清其他默认再设当前）
export const setDefaultAddress = (addrId) => request.put(`/address/set-default/${addrId}`)
