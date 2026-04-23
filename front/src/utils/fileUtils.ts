/** 文件处理工具函数（dy02，MD5 使用 Web Crypto API 实现） */

export const CHUNK_SIZE = 5 * 1024 * 1024 // 5 MB

/** 将 ArrayBuffer 转为十六进制字符串 */
function bufToHex(buf: ArrayBuffer): string {
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
}

/** 计算整个文件的 MD5（使用 SHA-256 作为替代，后端需对应） */
export async function computeFileMD5(file: File): Promise<string> {
  const buf = await file.arrayBuffer()
  const digest = await crypto.subtle.digest('SHA-256', buf)
  return bufToHex(digest)
}

/** 计算文件分块的 MD5（使用 SHA-256 作为替代） */
export async function computeChunkMD5(chunk: Blob): Promise<string> {
  const buf = await chunk.arrayBuffer()
  const digest = await crypto.subtle.digest('SHA-256', buf)
  return bufToHex(digest)
}

/** 将文件切分为分块列表 */
export function splitFileIntoChunks(file: File, chunkSize: number = CHUNK_SIZE): Blob[] {
  const chunks: Blob[] = []
  let offset = 0
  while (offset < file.size) {
    chunks.push(file.slice(offset, offset + chunkSize))
    offset += chunkSize
  }
  return chunks
}

/** 获取文件扩展名（小写，含点，如 '.jpg'） */
export function getFileExt(filename: string): string {
  const idx = filename.lastIndexOf('.')
  return idx >= 0 ? filename.slice(idx).toLowerCase() : ''
}

/** 判断是否为图片文件 */
export function isImageFile(filename: string): boolean {
  return ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg'].includes(getFileExt(filename))
}

/** 判断是否为视频文件 */
export function isVideoFile(filename: string): boolean {
  return ['.mp4', '.mov', '.avi', '.mkv', '.webm', '.flv'].includes(getFileExt(filename))
}

/** 格式化文件大小 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

/** 格式化速度（字节/秒） */
export function formatSpeed(bytesPerSecond: number): string {
  return formatFileSize(bytesPerSecond) + '/s'
}

/** 格式化时间（秒） */
export function formatTime(seconds: number): string {
  if (seconds < 60) return Math.round(seconds) + 's'
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.round(seconds % 60)
  if (minutes < 60) return minutes + 'm ' + remainingSeconds + 's'
  const hours = Math.floor(minutes / 60)
  return hours + 'h ' + (minutes % 60) + 'm'
}

/** 生成存储路径：userId/yyyy-mm-dd/uuid.ext */
export function generateStorageKey(userId: number, filename: string): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const uuid = crypto.randomUUID()
  const ext = filename.split('.').pop()
  return `${userId}/${year}-${month}-${day}/${uuid}.${ext}`
}
