const FIELD_SELECTOR = [
  'input:not([type="hidden"]):not([disabled])',
  'textarea:not([disabled])',
  '.el-select__wrapper:not(.is-disabled)',
  '.el-radio:not(.is-disabled)',
  '.el-checkbox:not(.is-disabled)'
].join(',')

const TEXT_INPUT_TYPES = new Set(['', 'text', 'password', 'email', 'number', 'search', 'tel', 'url'])
let lastAutoFocusAt = 0

const isVisible = (el) => {
  if (!el) return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0 && window.getComputedStyle(el).visibility !== 'hidden'
}

const fieldElement = (target) => {
  if (!(target instanceof HTMLElement)) return null
  return target.closest('.el-select__wrapper, .el-radio, .el-checkbox') || target
}

const focusElement = (el) => {
  if (!el) return
  const target = el.matches('.el-select__wrapper, .el-radio, .el-checkbox')
    ? el
    : el.querySelector?.('input,textarea,[tabindex]:not([tabindex="-1"])') || el
  target.focus?.()
  if (target instanceof HTMLInputElement) {
    target.select?.()
  }
}

const editableFocused = () => {
  const active = document.activeElement
  return active instanceof HTMLInputElement
    || active instanceof HTMLTextAreaElement
    || active?.closest?.('.el-select__wrapper, .el-radio, .el-checkbox')
}

const fieldsIn = (root) => {
  const raw = [...root.querySelectorAll(FIELD_SELECTOR)]
    .filter(isVisible)
    .filter(el => !el.closest('.el-upload, .el-picker-panel, .el-select-dropdown, .el-popper'))
  const fields = []
  raw.forEach((el) => {
    const field = fieldElement(el)
    if (field && isVisible(field) && !fields.includes(field)) fields.push(field)
  })
  return fields
}

const rootFor = (target) => {
  if (!(target instanceof HTMLElement)) return null
  return target.closest('.el-dialog, .el-form, form, .login-card, .panel')
}

const isTextInput = (target) => {
  if (!(target instanceof HTMLInputElement)) return false
  return TEXT_INPUT_TYPES.has(target.type)
}

const shouldKeepNativeHorizontal = (event) => {
  const target = event.target
  if (!isTextInput(target)) return false
  const valueLength = target.value?.length || 0
  const start = target.selectionStart ?? 0
  const end = target.selectionEnd ?? start
  if (event.key === 'ArrowLeft') return start > 0 || end > 0
  if (event.key === 'ArrowRight') return start < valueLength || end < valueLength
  return false
}

const caretLineInfo = (textarea) => {
  const before = textarea.value.slice(0, textarea.selectionStart ?? 0)
  const after = textarea.value.slice(textarea.selectionEnd ?? textarea.selectionStart ?? 0)
  return {
    isFirstLine: !before.includes('\n'),
    isLastLine: !after.includes('\n'),
    atStart: (textarea.selectionStart ?? 0) === 0,
    atEnd: (textarea.selectionEnd ?? 0) === textarea.value.length
  }
}

const shouldKeepNativeTextarea = (event) => {
  const target = event.target
  if (!(target instanceof HTMLTextAreaElement)) return false
  if (event.key === 'Enter') return true
  const info = caretLineInfo(target)
  if (event.key === 'ArrowUp') return !info.isFirstLine
  if (event.key === 'ArrowDown') return !info.isLastLine
  if (event.key === 'ArrowLeft') return !info.atStart
  if (event.key === 'ArrowRight') return !info.atEnd
  return false
}

const shouldIgnore = (event) => {
  const target = event.target
  if (!(target instanceof HTMLElement)) return true
  if (event.altKey || event.ctrlKey || event.metaKey || event.isComposing) return true
  if (target.closest('.el-select-dropdown, .el-picker-panel, .el-popper')) return true
  if (event.key === 'Enter' && target.closest('[data-keyboard-nav-ignore-enter="true"]')) return true
  if (target.isContentEditable) return true
  if (target instanceof HTMLButtonElement) return true
  return false
}

const primaryActionIn = (root) => {
  return root.querySelector(
    '.el-button--primary:not([disabled]), .el-button--success:not([disabled]), button[type="submit"]:not([disabled])'
  )
}

const moveFocus = (event, direction) => {
  const root = rootFor(event.target)
  if (!root) return
  const fields = fieldsIn(root)
  const current = fieldElement(event.target)
  const index = fields.findIndex(field => field === current || field.contains(event.target))
  if (index < 0) return

  const next = fields[index + direction]
  if (!next && event.key === 'Enter' && direction > 0) {
    primaryActionIn(root)?.click()
    return
  }
  if (!next) return

  event.preventDefault()
  event.stopPropagation()
  focusElement(next)
}

const focusFirstField = (root) => {
  if (!root || !isVisible(root)) return
  if (editableFocused()) return
  const field = fieldsIn(root)[0]
  if (!field) return
  lastAutoFocusAt = Date.now()
  window.setTimeout(() => focusElement(field), 30)
}

const autoFocusFormContainers = (root = document) => {
  const now = Date.now()
  if (now - lastAutoFocusAt < 120) return
  const containers = [
    ...root.querySelectorAll?.('.el-dialog .el-form, .el-dialog form, .login-card .el-form, .page-shell > .el-form, .edit-form') || []
  ].filter(isVisible)
  const target = containers.find(container => fieldsIn(container).length)
  if (target) focusFirstField(target)
}

const installAutoFocusObserver = () => {
  window.setTimeout(() => autoFocusFormContainers(document), 200)
  let rafId = null
  const observer = new MutationObserver((mutations) => {
    // 使用 requestAnimationFrame 防抖：同一帧内的多次 DOM 变更合并为一次处理，
    // 避免布局销毁/重建时成百上千次昂贵的 querySelectorAll 查询
    if (rafId) return
    rafId = requestAnimationFrame(() => {
      rafId = null
      for (const mutation of mutations) {
        for (const node of mutation.addedNodes) {
          if (node instanceof HTMLElement) {
            autoFocusFormContainers(node.matches('.el-dialog, .login-card, .el-form, form, .page-shell')
              ? node.parentElement || document
              : node)
          }
        }
      }
    })
  })
  observer.observe(document.body, { childList: true, subtree: true })
}

export const installKeyboardNavigation = () => {
  document.addEventListener('keydown', (event) => {
    if (!['ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight', 'Enter'].includes(event.key)) return
    if (shouldIgnore(event)) return
    if (shouldKeepNativeHorizontal(event)) return
    if (shouldKeepNativeTextarea(event)) return

    const direction = event.key === 'ArrowUp' || event.key === 'ArrowLeft' ? -1 : 1
    moveFocus(event, direction)
  }, true)
  installAutoFocusObserver()
}
