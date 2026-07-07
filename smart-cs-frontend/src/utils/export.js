/**
 * Export utility functions
 */

/**
 * Download a string as a file
 */
export function downloadFile(content, filename, mimeType) {
  const blob = new Blob(['\uFEFF' + content], { type: mimeType + ';charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * Escape CSV field
 */
function escapeCsv(val) {
  if (val === null || val === undefined) return ''
  const s = String(val)
  if (s.includes(',') || s.includes('"') || s.includes('\n')) {
    return '"' + s.replace(/"/g, '""') + '"'
  }
  return s
}

/**
 * Export data as CSV
 */
export function exportCsv(rows, headers, filename) {
  const headerLine = headers.map(h => escapeCsv(h.label)).join(',')
  const dataLines = rows.map(row =>
    headers.map(h => escapeCsv(h.key ? row[h.key] : (h.value ? h.value(row) : ''))).join(',')
  )
  const csv = [headerLine, ...dataLines].join('\n')
  downloadFile(csv, filename, 'text/csv')
}

/**
 * Export chat history as Markdown
 */
export function exportChatHistoryMarkdown(records, filename) {
  const lines = ['# SmartCS 会话历史导出', '', '_导出时间: ' + new Date().toLocaleString() + '_', '']

  // Group by session
  const sessions = {}
  records.forEach(r => {
    if (!sessions[r.sessionId]) sessions[r.sessionId] = []
    sessions[r.sessionId].push(r)
  })

  let sessionIndex = 1
  for (const [sessionId, items] of Object.entries(sessions)) {
    lines.push('---')
    lines.push('')
    lines.push('## 会话 ' + sessionIndex + ' (' + sessionId.substring(0, 8) + '...)')
    lines.push('')

    items.forEach((item, i) => {
      const time = item.createdAt ? new Date(item.createdAt).toLocaleString() : '未知时间'
      lines.push('### Q' + (i + 1) + ' - ' + time)
      lines.push('')
      lines.push('**问题:** ' + (item.question || '(空)'))
      lines.push('')
      lines.push('**回答:** ' + (item.answer || '(空)'))
      lines.push('')
      if (item.rating) {
        const stars = '⭐'.repeat(item.rating)
        lines.push('**评分:** ' + stars)
        lines.push('')
      }
      if (item.model) {
        lines.push('*模型: ' + item.model + ' | Token数: ' + (item.tokensUsed || 'N/A') + ' | 延迟: ' + (item.latencyMs || 'N/A') + 'ms*')
        lines.push('')
      }
    })
    sessionIndex++
  }

  lines.push('---')
  lines.push('')
  lines.push('_共 ' + records.length + ' 条记录，' + (sessionIndex - 1) + ' 个会话_')

  downloadFile(lines.join('\n'), filename, 'text/markdown')
}
