/**
 * 与 BaseLayout AppBar 中 Toolbar 的 minHeight 一致。
 * 若调整顶栏高度，请同步修改 BaseLayout.tsx 中 Toolbar 的 minHeight 与本常量。
 */
export const LAYOUT_APPBAR_HEIGHT_PX = 56

/**
 * 若某页仍用写死的 calc(100vh - N)，且 N 按「旧顶栏更高」估算，可整体减少该像素以收回底部留白。
 * 优先改为 flex 填满（见 ProductPage、BaseLayout main）。
 */
export const LAYOUT_APPBAR_HEIGHT_DELTA_FROM_LEGACY = 12
