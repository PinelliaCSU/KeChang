/**
 * 根据开始和结束日期计算状态。
 * @param {string|null} start
 * @param {string|null} end
 * @returns {'未开始'|'进行中'|'已结束'}
 */
export function computeStatus(start, end) {
  const now = new Date()
  const startDate = start ? new Date(start) : null
  const endDate = end ? new Date(end) : null

  if (!startDate && !endDate) return '进行中'
  if (startDate && now < startDate) return '未开始'
  if (endDate && now > endDate) return '已结束'
  return '进行中'
}

/**
 * 返回状态对应的 Element Plus 标签类型。
 * @param {string} status
 * @returns {'success'|'warning'|'info'|'danger'}
 */
export function statusTagType(status) {
  const map = {
    未开始: 'warning',
    进行中: 'success',
    已结束: 'info'
  }
  return map[status] || 'info'
}
