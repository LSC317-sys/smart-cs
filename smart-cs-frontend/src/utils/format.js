/**
 * 统计格式化工具
 * 千位分隔符、百分比、文件大小
 */

// 千位分隔符（1,234,567）
export function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  return Number(num).toLocaleString('zh-CN')
}

// 百分比（保留 1 位小数）
export function formatPercent(value, total) {
  if (!total) return '0%'
  const pct = ((value / total) * 100).toFixed(1)
  return `${pct}%`
}

// 文件大小（Bytes → KB/MB/GB）
export function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}
