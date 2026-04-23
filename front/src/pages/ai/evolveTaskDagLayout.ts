import dagre from '@dagrejs/dagre'
import { MarkerType, type Edge, type Node } from '@xyflow/react'

/** 由 dependsOnTaskNos 构建进化任务 DAG（F-4），无依赖边时 hasDeps=false */
export function layoutEvolveTaskDag(tasks: Record<string, unknown>[]): {
  nodes: Node[]
  edges: Edge[]
  hasDeps: boolean
} {
  const taskByNo = new Map<string, Record<string, unknown>>()
  for (const t of tasks) {
    const no = String(t.taskNo ?? '')
    if (no) taskByNo.set(no, t)
  }
  const depsEdges: { source: string; target: string }[] = []
  for (const t of tasks) {
    const target = String(t.taskNo ?? '')
    const depRaw = String((t as { dependsOnTaskNos?: string }).dependsOnTaskNos ?? '').trim()
    if (!target || !depRaw) continue
    for (const p of depRaw.split(/[,，]/).map((s) => s.trim()).filter(Boolean)) {
      depsEdges.push({ source: p, target })
      if (!taskByNo.has(p)) {
        taskByNo.set(p, { taskNo: p, status: '（未在列表）' } as Record<string, unknown>)
      }
    }
  }
  if (depsEdges.length === 0) {
    return { nodes: [], edges: [], hasDeps: false }
  }
  const allIds = [...taskByNo.keys()]
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: 'LR', ranksep: 72, nodesep: 28, marginx: 12, marginy: 12 })
  for (const id of allIds) {
    g.setNode(id, { width: 200, height: 52 })
  }
  depsEdges.forEach((e) => g.setEdge(e.source, e.target))
  dagre.layout(g)
  const nodes: Node[] = allIds.map((id) => {
    const n = g.node(id)
    const task = taskByNo.get(id)
    const st = String(task?.status ?? '?')
    return {
      id,
      position: { x: n.x - 100, y: n.y - 26 },
      data: { label: `${id}\n${st}` },
    }
  })
  const edges: Edge[] = depsEdges.map((e, i) => ({
    id: `e-${i}-${e.source}-${e.target}`,
    source: e.source,
    target: e.target,
    markerEnd: { type: MarkerType.ArrowClosed },
  }))
  return { nodes, edges, hasDeps: true }
}
