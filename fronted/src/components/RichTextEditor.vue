<template>
    <div ref="wrapperRef" class="rich-editor-wrapper" :class="{ 'is-disabled': disabled }">
        <!-- 工具栏 -->
        <div class="rich-editor-toolbar" v-if="editor && !disabled" @click.stop>
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
import { ref, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import { uploadApi } from '@/api'
import { resolveFileUrl } from '@/utils/fileUrl'
import { ElMessage } from 'element-plus'

const props = defineProps({
    modelValue: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    placeholder: { type: String, default: '开始编辑笔记...' }
})

const imageInput = ref(null)
const wrapperRef = ref(null)

// ============ 初始化编辑器 ============
const editor = useEditor({
    content: props.modelValue,
    extensions: [
        StarterKit.configure({ heading: { levels: [2, 3, 4] } }),
        Underline,
        Image.configure({ inline: false, allowBase64: false }),
        Link.configure({ openOnClick: false, HTMLAttributes: { target: '_blank', rel: 'noopener noreferrer' } })
    ],
    editorProps: {
        attributes: {
            class: 'prose-mirror-content',
            placeholder: props.placeholder
        }
    }
})

// 暴露方法：父组件通过 ref 读取内容
const getHTML = () => {
    if (!editor.value) return ''
    const html = editor.value.getHTML()
    return html === '<p></p>' ? '' : html
}
defineExpose({ getHTML })

// ============ 图片上传 ============
const triggerImageUpload = () => { imageInput.value?.click() }

const handleImageUpload = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''
    try {
        const formData = new FormData()
        formData.append('file', file)
        const res = await uploadApi.uploadFile(formData)
        if (res.data.code === 200) {
            editor.value.chain().focus().setImage({ src: resolveFileUrl(res.data.data?.url) }).run()
        } else {
            ElMessage.error('图片上传失败')
        }
    } catch { ElMessage.error('图片上传失败') }
}

// ============ 链接 ============
const setLink = () => {
    const prev = editor.value.getAttributes('link').href
    const url = window.prompt('请输入链接地址', prev || 'https://')
    if (url === null) return
    if (url === '') {
        editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
        return
    }
    editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

onBeforeUnmount(() => { editor.value?.destroy() })
</script>

<style>
/* ============ 容器 ============ */
.rich-editor-wrapper {
    border: 1px solid var(--bio-border, #dcdfe6);
    border-radius: var(--bio-radius-md, 8px);
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
