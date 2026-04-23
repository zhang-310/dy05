/** 与后端 P1/P2/P3（1–3）及历史脏值收敛规则一致 */
export function displayTopicTier(priority: number | null | undefined): 1 | 2 | 3 {
  const p = priority
  if (p === 1 || p === 2 || p === 3) return p
  return 2
}
