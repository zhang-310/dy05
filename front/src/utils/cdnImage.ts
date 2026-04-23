/** 给 CDN 图片 URL 追加裁剪参数 @!{w}X{h}。支持规格：80X80（缩略图）、300X250（预览大图） */
export function cdnThumb(url: string | undefined | null, width: number, height: number): string {
  if (!url) return ''
  if (url.includes('@!')) return url
  return `${url}@!${width}X${height}`
}
