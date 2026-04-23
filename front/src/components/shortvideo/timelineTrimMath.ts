/**
 * V-1：时间轴裁切数值与 {@link VideoTimeline} Dialog 一致，便于单测与「非 NLE」边界说明。
 */
export function effectiveEndSec(clipDuration: number | undefined, sourceLen: number): number {
  return clipDuration != null && clipDuration > 0 ? Math.min(clipDuration, sourceLen) : sourceLen
}

export function clampEndDurationTrim(trimDraft: number, sourceLen: number): number {
  return Math.min(Math.max(trimDraft, 0.05), sourceLen)
}

/** 片头裁切：不超过 endSec-0.1s，且 ≥0 */
export function clampStartTrim(trimDraft: number, endSec: number): number {
  const clamped = Math.min(Math.max(trimDraft, 0), Math.max(0, endSec - 0.1))
  return clamped <= 0 ? 0 : clamped
}
