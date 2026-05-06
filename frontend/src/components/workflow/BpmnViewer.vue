<template>
  <div ref="containerRef" class="bpmn-viewer" />
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import BpmnViewer from 'bpmn-js'

interface CanvasApi {
  zoom(mode: 'fit-viewport'): void
}

const props = defineProps<{ xml: string }>()
const emit = defineEmits<{ loaded: [] }>()

const containerRef = ref<HTMLElement | null>(null)
let viewer: BpmnViewer | null = null

onMounted(async () => {
  if (!containerRef.value) return
  viewer = new BpmnViewer({ container: containerRef.value })
  try {
    await viewer.importXML(props.xml)
    ;(viewer.get('canvas') as CanvasApi).zoom('fit-viewport')
    emit('loaded')
  } catch (err) {
    console.error('BPMN import failed:', err)
  }
})

watch(() => props.xml, async (newXml) => {
  if (viewer) {
    try {
      await viewer.importXML(newXml)
      ;(viewer.get('canvas') as CanvasApi).zoom('fit-viewport')
    } catch (err) {
      console.error('BPMN re-import failed:', err)
    }
  }
})

onBeforeUnmount(() => {
  if (viewer) {
    viewer.destroy()
    viewer = null
  }
})
</script>

<style scoped>
.bpmn-viewer {
  width: 100%;
  height: 500px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
</style>
