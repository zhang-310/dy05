/**
 * ECharts 按需引入：仅注册项目使用的图表类型，减小打包体积；
 * LazyECharts 通过 React.lazy 延迟加载 echarts-for-react，进一步减小首屏依赖。
 */
import { lazy, Suspense, createElement, type CSSProperties } from 'react'
import type { EChartsReactProps } from 'echarts-for-react'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, FunnelChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  FunnelChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
  CanvasRenderer,
])

export { echarts }

export type LazyEChartsProps = Omit<EChartsReactProps, 'echarts'>

function fallbackHeight(style?: CSSProperties): number {
  const h = style?.height
  if (typeof h === 'number' && Number.isFinite(h)) return h
  if (typeof h === 'string') {
    const px = /^(\d+(?:\.\d+)?)px$/i.exec(h)?.[1]
    if (px) {
      const n = Number(px)
      if (Number.isFinite(n)) return n
    }
    const n = parseInt(h, 10)
    if (Number.isFinite(n)) return n
  }
  return 240
}

const LazyReactECharts = lazy(async () => {
  const { default: ReactECharts } = await import('echarts-for-react')
  function RegistryECharts(props: LazyEChartsProps) {
    return createElement(ReactECharts, { echarts, ...props })
  }
  return { default: RegistryECharts }
})

/** 延迟加载图表组件，内部绑定按需注册的 {@link echarts} 实例 */
export function LazyECharts(props: LazyEChartsProps) {
  const h = fallbackHeight(props.style as CSSProperties | undefined)
  return createElement(
    Suspense,
    {
      fallback: createElement('div', {
        style: {
          width: props.style?.width ?? '100%',
          height: h,
          borderRadius: 4,
          backgroundColor: 'rgba(0, 0, 0, 0.06)',
        },
        'aria-hidden': true,
      }),
    },
    createElement(LazyReactECharts, props),
  )
}
