<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import uCharts from '@qiun/ucharts'

type ChartSeries = { name?: string; data: number[] | { name?: string; value: number }[] }

interface ChartData {
  categories?: string[]
  series: ChartSeries[]
}

interface Props {
  type: string
  chartData: ChartData | null
  opts?: Record<string, unknown>
  canvasId?: string
  height?: number
}

const props = defineProps<Props>()
const resolvedCanvasId = ref(props.canvasId || `chart-${Math.random().toString(36).slice(2)}`)
const width = ref(0)
const height = computed(() => props.height ?? 260)

function resolveWidth(): void {
  const system = uni.getSystemInfoSync()
  width.value = Math.max(300, system.windowWidth - 40)
}

function renderChart(): void {
  if (!props.chartData) return
  if (!width.value) resolveWidth()
  new uCharts({
    canvasId: resolvedCanvasId.value,
    type: props.type,
    width: width.value,
    height: height.value,
    categories: props.chartData.categories || [],
    series: props.chartData.series || [],
    ...(props.opts || {})
  })
}

onMounted(() => {
  resolveWidth()
  void nextTick(renderChart)
})

watch(
  () => [props.type, props.chartData, props.opts],
  () => {
    void nextTick(renderChart)
  },
  { deep: true }
)
</script>

<template>
  <canvas
    class="chart-canvas"
    :canvas-id="resolvedCanvasId"
    :id="resolvedCanvasId"
    :style="{ width: `${width}px`, height: `${height}px` }"
  />
</template>

<style scoped>
.chart-canvas {
  display: block;
}
</style>
