<script setup>
// ============ 家属端评价对话框：星级 + 内容，提交 POST /service-comment/create ============
// 「我的订单」与「我的评价(待评价区)」两页共用：传订单行（需 orderId/itemName），父组件 ref.open() 弹出
// 提交成功 emit('success')，由父页面负责刷新列表
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createComment } from '@/api/comment.js'

const props = defineProps({
  order: { type: Object, default: null }, // 待评价订单行
})
const emit = defineEmits(['success'])

const visible = ref(false)
const open = () => { visible.value = true }
const close = () => { visible.value = false }
defineExpose({ open })

const score = ref(5) // 星级默认 5（可下调）
const content = ref('')
const submitting = ref(false)

// 每次打开重置（防上一次输入残留到下一单）
watch(visible, (v) => {
  if (v) {
    score.value = 5
    content.value = ''
  }
})

const submit = async () => {
  if (!props.order?.orderId) return
  submitting.value = true
  try {
    const res = await createComment({
      orderId: props.order.orderId,
      score: score.value,
      content: content.value.trim(),
    })
    if (res.success) {
      ElMessage.success('评价成功，感谢您的反馈')
      close()
      emit('success')
    } else {
      ElMessage.error(res.errorMsg || '评价提交失败')
    }
  } catch (err) { /* 拦截器已统一提示 */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="评价服务" width="440px" :close-on-click-modal="false">
    <div class="comment-dialog">
      <div class="item-name">{{ order?.itemName || '服务项目' }}</div>

      <div class="form-row">
        <span class="row-label">服务评分</span>
        <el-rate v-model="score" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text />
      </div>

      <div class="form-row content-row">
        <span class="row-label">评价内容</span>
        <el-input
          v-model="content"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="说说本次服务体验吧（选填）"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.comment-dialog .item-name {
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #2d2a26;
  margin-bottom: 18px;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.form-row .row-label {
  flex-shrink: 0;
  font-size: 13px;
  color: #a39c92;
}
.content-row {
  align-items: flex-start;
}
.content-row .row-label {
  padding-top: 8px;
}
</style>
