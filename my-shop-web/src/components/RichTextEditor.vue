<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'

const props = withDefaults(
  defineProps<{
    modelValue: string
    editable?: boolean
    minHeight?: number | string
  }>(),
  {
    modelValue: '',
    editable: true,
    minHeight: 140
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const editor = shallowRef<Editor | null>(null)
const stateVersion = ref(0)
const editorInstance = computed(() => editor.value ?? undefined)

const minHeightStyle = computed(() => {
  const value = props.minHeight
  if (value == null) {
    return undefined
  }
  return typeof value === 'number' ? `${value}px` : value
})

function bumpState(): void {
  stateVersion.value += 1
}

onMounted(() => {
  editor.value = new Editor({
    extensions: [
      StarterKit.configure({
        bulletList: { keepMarks: true, keepAttributes: false },
        orderedList: { keepMarks: true, keepAttributes: false }
      })
    ],
    content: props.modelValue || '',
    editable: props.editable ?? true,
    onUpdate: ({ editor }) => {
      emit('update:modelValue', editor.isEmpty ? '' : editor.getHTML())
      bumpState()
    },
    onSelectionUpdate: bumpState,
    onTransaction: bumpState
  })
})

watch(
  () => props.modelValue,
  (value) => {
    const instance = editor.value
    if (!instance) {
      return
    }
    const normalizedIncoming = value || ''
    const normalizedCurrent = instance.isEmpty ? '' : instance.getHTML()
    if (normalizedIncoming !== normalizedCurrent) {
      instance.commands.setContent(normalizedIncoming)
    }
  }
)

watch(
  () => props.editable,
  (value) => {
    editor.value?.setEditable(value ?? true)
  }
)

onBeforeUnmount(() => {
  editor.value?.destroy()
  editor.value = null
})

const isBold = computed(() => {
  stateVersion.value
  return editor.value?.isActive('bold') ?? false
})

const isItalic = computed(() => {
  stateVersion.value
  return editor.value?.isActive('italic') ?? false
})

const isStrike = computed(() => {
  stateVersion.value
  return editor.value?.isActive('strike') ?? false
})

const isBulletList = computed(() => {
  stateVersion.value
  return editor.value?.isActive('bulletList') ?? false
})

const isOrderedList = computed(() => {
  stateVersion.value
  return editor.value?.isActive('orderedList') ?? false
})

const canBold = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().toggleBold().run() ?? false
})

const canItalic = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().toggleItalic().run() ?? false
})

const canStrike = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().toggleStrike().run() ?? false
})

const canBulletList = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().toggleBulletList().run() ?? false
})

const canOrderedList = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().toggleOrderedList().run() ?? false
})

const canUndo = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().undo().run() ?? false
})

const canRedo = computed(() => {
  stateVersion.value
  return editor.value?.can().chain().focus().redo().run() ?? false
})
</script>

<template>
  <div class="rich-text" :style="{ '--rte-min-height': minHeightStyle }">
    <div class="rich-text__toolbar">
      <button
        type="button"
        class="rich-text__btn"
        :class="{ 'is-active': isBold }"
        :disabled="!canBold"
        @click="editor?.chain().focus().toggleBold().run()"
      >
        B
      </button>
      <button
        type="button"
        class="rich-text__btn"
        :class="{ 'is-active': isItalic }"
        :disabled="!canItalic"
        @click="editor?.chain().focus().toggleItalic().run()"
      >
        I
      </button>
      <button
        type="button"
        class="rich-text__btn"
        :class="{ 'is-active': isStrike }"
        :disabled="!canStrike"
        @click="editor?.chain().focus().toggleStrike().run()"
      >
        S
      </button>
      <span class="rich-text__divider" />
      <button
        type="button"
        class="rich-text__btn"
        :class="{ 'is-active': isBulletList }"
        :disabled="!canBulletList"
        @click="editor?.chain().focus().toggleBulletList().run()"
      >
        ? List
      </button>
      <button
        type="button"
        class="rich-text__btn"
        :class="{ 'is-active': isOrderedList }"
        :disabled="!canOrderedList"
        @click="editor?.chain().focus().toggleOrderedList().run()"
      >
        1. List
      </button>
      <span class="rich-text__divider" />
      <button
        type="button"
        class="rich-text__btn"
        :disabled="!canUndo"
        @click="editor?.chain().focus().undo().run()"
      >
        Undo
      </button>
      <button
        type="button"
        class="rich-text__btn"
        :disabled="!canRedo"
        @click="editor?.chain().focus().redo().run()"
      >
        Redo
      </button>
    </div>

    <div class="rich-text__content">
      <EditorContent :editor="editorInstance" />
    </div>
  </div>
</template>

<style scoped>
.rich-text {
  display: grid;
  gap: 8px;
  width: 100%;
}

.rich-text__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.rich-text__btn {
  border: 1px solid transparent;
  background: #fff;
  color: #111827;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 0.86rem;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.rich-text__btn:hover:not(:disabled) {
  border-color: #d1d5db;
  background: #f3f4f6;
}

.rich-text__btn.is-active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.rich-text__btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.rich-text__divider {
  width: 1px;
  height: 20px;
  background: #e5e7eb;
  margin: 0 4px;
}

.rich-text__content {
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 10px 12px;
  min-height: var(--rte-min-height, 140px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
}

.rich-text__content :deep(.ProseMirror) {
  outline: none;
  min-height: var(--rte-min-height, 140px);
  font-size: 0.94rem;
  line-height: 1.6;
  color: #0f172a;
}

.rich-text__content :deep(.ProseMirror p) {
  margin: 0 0 0.7rem;
}

.rich-text__content :deep(.ProseMirror p:last-child) {
  margin-bottom: 0;
}

.rich-text__content :deep(.ProseMirror ul),
.rich-text__content :deep(.ProseMirror ol) {
  padding-left: 1.4rem;
  margin: 0 0 0.7rem;
}
</style>
