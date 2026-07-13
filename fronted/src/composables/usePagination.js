import { ref, computed } from 'vue'

/**
 * 统一分页 composable
 * 封装 page/size/total 状态和常用方法，消除各页面重复代码
 *
 * @param {Object} options
 * @param {number} [options.defaultSize=10]   默认每页条数
 * @param {number[]} [options.pageSizes]      可选的每页条数
 * @returns {{ page, size, total, pageSizes, resetPage, paginationProps, handleSizeChange, handleCurrentChange }}
 */
export function usePagination(options = {}) {
  const defaultSize = options.defaultSize || 10
  const pageSizes = options.pageSizes || [10, 20, 50]

  const page = ref(1)
  const size = ref(defaultSize)
  const total = ref(0)

  /** 重置到第 1 页（搜索/筛选时调用） */
  const resetPage = () => {
    page.value = 1
  }

  /** 直接传给 PaginationWrapper 的 props */
  const paginationProps = computed(() => ({
    page: page.value,
    size: size.value,
    total: total.value,
    pageSizes
  }))

  const handleSizeChange = (val) => {
    size.value = val
    resetPage()
  }

  const handleCurrentChange = (val) => {
    page.value = val
  }

  return {
    page,
    size,
    total,
    pageSizes,
    resetPage,
    paginationProps,
    handleSizeChange,
    handleCurrentChange
  }
}
