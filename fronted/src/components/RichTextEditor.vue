<template>
    <div class="rich-editor-wrapper" :class="{ 'is-disabled': disabled }">
        <!-- 工具栏 -->
        <div class="rich-editor-toolbar" v-if="editor && !disabled">
            <!-- 撤销/重做 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" title="撤销 (Ctrl+Z)" @click="editor.chain().focus().undo().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z" />
                    </svg>
                </button>
                <button class="toolbar-btn" title="重做 (Ctrl+Y)" @click="editor.chain().focus().redo().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z" />
                    </svg>
                </button>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 文字样式 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" :class="{ active: editor.isActive('bold') }" title="加粗 (Ctrl+B)"
                    @click="editor.chain().focus().toggleBold().run()">
                    <strong>B</strong>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('italic') }" title="斜体 (Ctrl+I)"
                    @click="editor.chain().focus().toggleItalic().run()">
                    <em>I</em>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('underline') }" title="下划线 (Ctrl+U)"
                    @click="editor.chain().focus().toggleUnderline().run()">
                    <u>U</u>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('strike') }" title="删除线"
                    @click="editor.chain().focus().toggleStrike().run()">
                    <s>S</s>
                </button>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 标题 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" :class="{ active: editor.isActive('paragraph') }" title="正文"
                    @click="editor.chain().focus().setParagraph().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor" d="M3 4h18v2H3V4zm0 7h12v2H3v-2zm0 7h18v2H3v-2z" />
                    </svg>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('heading', { level: 2 }) }" title="标题1"
                    @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">
                    <span style="font-weight:700;font-size:14px">H1</span>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('heading', { level: 3 }) }" title="标题2"
                    @click="editor.chain().focus().toggleHeading({ level: 3 }).run()">
                    <span style="font-weight:700;font-size:12px">H2</span>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('heading', { level: 4 }) }" title="标题3"
                    @click="editor.chain().focus().toggleHeading({ level: 4 }).run()">
                    <span style="font-weight:600;font-size:12px">H3</span>
                </button>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 文字颜色 -->
            <div class="toolbar-group">
                <div class="color-picker-wrapper">
                    <button class="toolbar-btn" title="文字颜色" @click="showFontColor = !showFontColor">
                        <span class="color-indicator" :style="{ borderBottomColor: currentFontColor }">A</span>
                    </button>
                    <div class="color-picker-dropdown" v-if="showFontColor" @click.stop>
                        <div class="color-grid">
                            <span class="color-swatch" v-for="c in colors" :key="c" :style="{ backgroundColor: c }"
                                :class="{ active: currentFontColor === c }" @click="setFontColor(c)"></span>
                        </div>
                        <div class="color-reset" @click="setFontColor(null)">恢复默认</div>
                    </div>
                </div>
                <div class="color-picker-wrapper">
                    <button class="toolbar-btn" title="文字背景" @click="showHighlight = !showHighlight">
                        <svg viewBox="0 0 24 24" width="16" height="16">
                            <path fill="currentColor" d="M3 3h18v4l-2 2v10a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V9L3 7V3z" />
                        </svg>
                    </button>
                    <div class="color-picker-dropdown" v-if="showHighlight" @click.stop>
                        <div class="color-grid">
                            <span class="color-swatch" v-for="c in highlightColors" :key="c"
                                :style="{ backgroundColor: c }" :class="{ active: currentHighlight === c }"
                                @click="setHighlight(c)"></span>
                        </div>
                        <div class="color-reset" @click="setHighlight(null)">恢复默认</div>
                    </div>
                </div>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 字号 -->
            <div class="toolbar-group">
                <select class="toolbar-select" @change="setFontSize($event.target.value)" :value="currentFontSize">
                    <option value="">字号</option>
                    <option v-for="s in fontSizes" :key="s" :value="s">{{ s }}px</option>
                </select>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 对齐 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" :class="{ active: editor.isActive({ textAlign: 'left' }) }" title="左对齐"
                    @click="editor.chain().focus().setTextAlign('left').run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M3 3h18v2H3V3zm0 4h12v2H3V7zm0 4h18v2H3v-2zm0 4h12v2H3v-2zm0 4h18v2H3v-2z" />
                    </svg>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive({ textAlign: 'center' }) }" title="居中"
                    @click="editor.chain().focus().setTextAlign('center').run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M3 3h18v2H3V3zm2 4h14v2H5V7zm-2 4h18v2H3v-2zm2 4h14v2H5v-2zm-2 4h18v2H3v-2z" />
                    </svg>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive({ textAlign: 'right' }) }" title="右对齐"
                    @click="editor.chain().focus().setTextAlign('right').run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M3 3h18v2H3V3zm6 4h12v2H9V7zm-6 4h18v2H3v-2zm6 4h12v2H9v-2zm-6 4h18v2H3v-2z" />
                    </svg>
                </button>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 列表 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" :class="{ active: editor.isActive('bulletList') }" title="无序列表"
                    @click="editor.chain().focus().toggleBulletList().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M3 5h2v2H3V5zm0 6h2v2H3v-2zm0 6h2v2H3v-2zm5-6h14v2H8v-2zm0-6h14v2H8V5zm0 12h14v2H8v-2z" />
                    </svg>
                </button>
                <button class="toolbar-btn" :class="{ active: editor.isActive('orderedList') }" title="有序列表"
                    @click="editor.chain().focus().toggleOrderedList().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M3 17h2v.5H4v1h1V19H3v1h3v-4H3v1zm1-9h1V4H3v1h1v3zm-1 3h1.8L3 13.1v.9h3v-1H4.2L6 10.9V10H3v1zm5-6v2h14V5H8zm0 14h14v-2H8v2zm0-6h14v-2H8v2z" />
                    </svg>
                </button>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 插入 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" title="插入图片" @click="triggerImageUpload">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z" />
                    </svg>
                </button>
                <input ref="imageInput" type="file" accept="image/*" style="display:none" @change="handleImageUpload" />
                <button class="toolbar-btn" title="插入链接" @click="setLink">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z" />
                    </svg>
                </button>
                <button class="toolbar-btn" title="分割线" @click="editor.chain().focus().setHorizontalRule().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor" d="M3 11h18v2H3v-2z" />
                    </svg>
                </button>
            </div>

            <span class="toolbar-divider"></span>

            <!-- 清除格式 -->
            <div class="toolbar-group">
                <button class="toolbar-btn" title="清除格式"
                    @click="editor.chain().focus().clearNodes().unsetAllMarks().run()">
                    <svg viewBox="0 0 24 24" width="16" height="16">
                        <path fill="currentColor"
                            d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
                    </svg>
                </button>
            </div>
        </div>

        <!-- 编辑区 -->
        <editor-content :editor="editor" class="rich-editor-content" />
    </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, onMounted, nextTick } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import { TextStyle, FontSize } from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import TextAlign from '@tiptap/extension-text-align'
import DOMPurify from 'dompurify'
import { uploadApi } from '@/api'
import { resolveFileUrl } from '@/utils/fileUrl'
import { ElMessage } from 'element-plus'

// ============ Props & Emits ============
const props = defineProps({
    modelValue: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    placeholder: { type: String, default: '开始编辑笔记...' }
})

const emit = defineEmits(['update:modelValue'])

// ============ 颜色选项 ============
const colors = [
    '#000000', '#434343', '#666666', '#999999', '#b7b7b7', '#cccccc',
    '#980000', '#ff0000', '#ff9900', '#ffff00', '#00ff00', '#00ffff',
    '#4a86e8', '#0000ff', '#9900ff', '#ff00ff',
    '#e06666', '#f6b26b', '#ffd966', '#93c47d', '#76a5af', '#6fa8dc',
    '#8e7cc3', '#c27ba0'
]

const highlightColors = [
    '#ffff00', '#ff9900', '#00ff00', '#00ffff', '#4a86e8', '#ff00ff',
    '#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#d0e0e3', '#cfe2f3',
    '#d9d2e9', '#ead1dc'
]

const fontSizes = [12, 14, 16, 18, 20, 24, 28, 32]

// ============ 状态 ============
const imageInput = ref(null)
const showFontColor = ref(false)
const showHighlight = ref(false)
const currentFontColor = ref('#000000')
const currentHighlight = ref(null)
const currentFontSize = ref('')
const isUpdatingFromProps = ref(false)

// ============ 初始化编辑器 ============
const editor = useEditor({
    content: props.modelValue,
    extensions: [
        StarterKit.configure({
            heading: { levels: [2, 3, 4] }
        }),
        Underline,
        TextStyle,
        Color,
        Highlight.configure({ multicolor: true }),
        FontSize,
        Image.configure({ inline: false, allowBase64: false }),
        Link.configure({ openOnClick: false, HTMLAttributes: { target: '_blank', rel: 'noopener noreferrer' } }),
        TextAlign.configure({ types: ['heading', 'paragraph'] })
    ],
    editorProps: {
        attributes: {
            class: 'prose-mirror-content',
            placeholder: props.placeholder
        }
    },
    onUpdate: ({ editor }) => {
        if (isUpdatingFromProps.value) return
        const html = editor.getHTML()
        // DOMPurify 清洗
        const clean = DOMPurify.sanitize(html, {
            ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'del', 'h2', 'h3', 'h4',
                'ul', 'ol', 'li', 'a', 'img', 'span', 'div', 'hr', 'pre', 'code', 'blockquote'],
            ALLOWED_ATTR: ['href', 'target', 'rel', 'src', 'alt', 'title', 'style', 'class'],
            ALLOW_DATA_ATTR: false
        })
        emit('update:modelValue', clean === '<p></p>' ? '' : clean)
    }
})

// ============ v-model 双向同步 ============
watch(() => props.modelValue, (val) => {
    if (!editor.value) return
    const editorHTML = editor.value.getHTML()
    const cleanEditor = editorHTML === '<p></p>' ? '' : editorHTML
    if (val === cleanEditor) return
    isUpdatingFromProps.value = true
    editor.value.commands.setContent(val || '')
    nextTick(() => { isUpdatingFromProps.value = false })
})

// ============ 文字颜色 ============
const setFontColor = (color) => {
    showFontColor.value = false
    if (color) {
        editor.value.chain().focus().setColor(color).run()
        currentFontColor.value = color
    } else {
        editor.value.chain().focus().unsetColor().run()
        currentFontColor.value = '#000000'
    }
}

// ============ 高亮 ============
const setHighlight = (color) => {
    showHighlight.value = false
    if (color) {
        editor.value.chain().focus().toggleHighlight({ color }).run()
        currentHighlight.value = color
    } else {
        editor.value.chain().focus().unsetHighlight().run()
        currentHighlight.value = null
    }
}

// ============ 字号 ============
const setFontSize = (size) => {
    if (size) {
        editor.value.chain().focus().setFontSize(size + 'px').run()
        currentFontSize.value = size
    } else {
        editor.value.chain().focus().unsetFontSize().run()
        currentFontSize.value = ''
    }
}

// ============ 图片上传 ============
const triggerImageUpload = () => {
    imageInput.value?.click()
}

const handleImageUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    // 重置 input，允许重复选同一文件
    e.target.value = ''
    try {
        const formData = new FormData()
        formData.append('file', file)
        const res = await uploadApi.uploadFile(formData)
        if (res.data.code === 200) {
            const url = resolveFileUrl(res.data.data?.url)
            editor.value.chain().focus().setImage({ src: url }).run()
        } else {
            ElMessage.error('图片上传失败')
        }
    } catch (err) {
        ElMessage.error('图片上传失败')
    }
}

// ============ 链接 ============
const setLink = () => {
    const previousUrl = editor.value.getAttributes('link').href
    const url = window.prompt('请输入链接地址', previousUrl || 'https://')
    if (url === null) return
    if (url === '') {
        editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
        return
    }
    editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

// ============ 点击外部关闭颜色选择器 ============
const handleClickOutside = (e) => {
    const wrapper = document.querySelector('.rich-editor-wrapper')
    if (wrapper && !wrapper.contains(e.target)) {
        showFontColor.value = false
        showHighlight.value = false
    }
}

onMounted(() => {
    document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
    document.removeEventListener('click', handleClickOutside)
    editor.value?.destroy()
})
</script>

<style>
/* ============ 容器 ============ */
.rich-editor-wrapper {
    border: 1px solid var(--bio-border, #dcdfe6);
    border-radius: var(--bio-radius-md, 8px);
    overflow: hidden;
    background: #fff;
    transition: border-color 0.2s;
    display: flex;
    flex-direction: column;
}

.rich-editor-wrapper:focus-within {
    border-color: var(--bio-primary, #20c4cc);
}

.rich-editor-wrapper.is-disabled {
    background: #f5f7fa;
    pointer-events: none;
    opacity: 0.7;
}

/* ============ 工具栏 ============ */
.rich-editor-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 2px;
    padding: 6px 8px;
    background: #fafbfc;
    border-bottom: 1px solid var(--bio-border-soft, #e8edf3);
    user-select: none;
}

.toolbar-group {
    display: flex;
    align-items: center;
    gap: 1px;
}

.toolbar-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 28px;
    border: none;
    background: transparent;
    border-radius: 4px;
    cursor: pointer;
    color: var(--bio-ink-soft, #4b5563);
    font-size: 13px;
    transition: all 0.15s;
    padding: 0;
}

.toolbar-btn:hover {
    background: #e8edf3;
    color: var(--bio-ink, #1f2937);
}

.toolbar-btn.active {
    background: var(--bio-primary-soft, #e7fbfc);
    color: var(--bio-primary, #20c4cc);
}

.toolbar-divider {
    display: inline-block;
    width: 1px;
    height: 18px;
    background: var(--bio-border-soft, #e8edf3);
    margin: 0 4px;
}

/* 颜色指示器 */
.color-indicator {
    display: inline-block;
    border-bottom: 2px solid currentColor;
    font-weight: 700;
    font-size: 12px;
    line-height: 1;
}

/* 颜色选择器 */
.color-picker-wrapper {
    position: relative;
}

.color-picker-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    z-index: 100;
    background: #fff;
    border: 1px solid var(--bio-border, #dcdfe6);
    border-radius: 8px;
    padding: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    margin-top: 4px;
    min-width: 180px;
}

.color-grid {
    display: grid;
    grid-template-columns: repeat(8, 1fr);
    gap: 3px;
}

.color-swatch {
    display: block;
    width: 18px;
    height: 18px;
    border-radius: 3px;
    cursor: pointer;
    border: 1px solid rgba(0, 0, 0, 0.1);
    transition: transform 0.1s;
}

.color-swatch:hover {
    transform: scale(1.2);
    border-color: var(--bio-primary, #20c4cc);
}

.color-swatch.active {
    border: 2px solid var(--bio-primary, #20c4cc);
    transform: scale(1.15);
}

.color-reset {
    margin-top: 6px;
    padding: 4px 0;
    text-align: center;
    font-size: 12px;
    color: var(--bio-ink-muted, #8a94a6);
    cursor: pointer;
    border-top: 1px solid var(--bio-border-soft, #e8edf3);
}

.color-reset:hover {
    color: var(--bio-primary, #20c4cc);
}

/* 字号下拉 */
.toolbar-select {
    height: 28px;
    border: 1px solid var(--bio-border-soft, #e8edf3);
    border-radius: 4px;
    padding: 0 6px;
    font-size: 12px;
    color: var(--bio-ink-soft, #4b5563);
    background: #fff;
    cursor: pointer;
    outline: none;
    min-width: 62px;
}

.toolbar-select:focus {
    border-color: var(--bio-primary, #20c4cc);
}

/* ============ 编辑区 ============ */
.rich-editor-content {
    flex: 1;
    min-height: 200px;
}

.rich-editor-content .ProseMirror {
    padding: 12px 16px;
    min-height: 200px;
    max-height: 500px;
    overflow-y: auto;
    outline: none;
    font-size: 14px;
    line-height: 1.7;
    color: var(--bio-ink, #1f2937);
    word-break: break-word;
}

.rich-editor-content .ProseMirror p {
    margin: 0 0 8px;
}

.rich-editor-content .ProseMirror p:last-child {
    margin-bottom: 0;
}

/* Placeholder */
.rich-editor-content .ProseMirror p.is-editor-empty:first-child::before {
    content: attr(data-placeholder);
    float: left;
    color: var(--bio-ink-faint, #b7bfcc);
    pointer-events: none;
    height: 0;
}

/* 标题 */
.rich-editor-content .ProseMirror h2 {
    font-size: 22px;
    margin: 16px 0 10px;
    font-weight: 700;
}

.rich-editor-content .ProseMirror h3 {
    font-size: 18px;
    margin: 14px 0 8px;
    font-weight: 600;
}

.rich-editor-content .ProseMirror h4 {
    font-size: 16px;
    margin: 12px 0 6px;
    font-weight: 600;
}

/* 列表 */
.rich-editor-content .ProseMirror ul,
.rich-editor-content .ProseMirror ol {
    padding-left: 24px;
    margin: 8px 0;
}

.rich-editor-content .ProseMirror li {
    margin-bottom: 4px;
}

/* 图片 */
.rich-editor-content .ProseMirror img {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
    margin: 8px 0;
}

/* 分割线 */
.rich-editor-content .ProseMirror hr {
    border: none;
    border-top: 1px solid var(--bio-border-soft, #e8edf3);
    margin: 16px 0;
}

/* 链接 */
.rich-editor-content .ProseMirror a {
    color: var(--bio-primary, #20c4cc);
    text-decoration: underline;
}

/* 代码 */
.rich-editor-content .ProseMirror code {
    background: #f0f2f5;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'Consolas', 'Monaco', monospace;
}

.rich-editor-content .ProseMirror pre {
    background: #f5f7fa;
    padding: 12px 16px;
    border-radius: 8px;
    overflow-x: auto;
}

.rich-editor-content .ProseMirror pre code {
    background: none;
    padding: 0;
}

/* 引用 */
.rich-editor-content .ProseMirror blockquote {
    border-left: 3px solid var(--bio-primary, #20c4cc);
    padding: 4px 12px;
    margin: 8px 0;
    color: var(--bio-ink-muted, #8a94a6);
    background: var(--bio-primary-soft, #e7fbfc);
    border-radius: 0 6px 6px 0;
}
</style>
