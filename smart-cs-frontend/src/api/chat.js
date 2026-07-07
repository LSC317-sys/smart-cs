import request from './request'

// ============================================================
// 服务端基础 URL（必须与 request.js 中的 baseURL 保持一致）
// ============================================================


// ============================================================
// API 端点常量（集中管理，避免硬编码）
// ============================================================
const API = {
  // 聊天
  CHAT_ASK:            '/chat/ask',
  CHAT_CHAT:           '/chat/chat',
  CHAT_STREAM:         '/chat/chat/stream',
  CHAT_SESSION:        '/chat/session',

  // RAG 配置
  CONFIG:              '/chat/config',
  CONFIG_RESET:        '/chat/config/reset',

  // 测试
  TEST_CHINESE:       '/chat/test/chinese',

  // 历史记录
  HISTORY_USER:        '/chat/history/user',
  HISTORY_SESSION:     '/chat/history/session',
  HISTORY_SEARCH:      '/chat/history/search',
  HISTORY_STATS:       '/chat/history/stats/rating',
  HISTORY_RATING:      '/chat/history',
  HISTORY_DELETE:      '/chat/history',
}

// ============================================================
// 统一错误处理
// ============================================================
function handleApiError(error, defaultMsg = '操作失败') {
  const msg = error?.response?.data?.message || error?.message || defaultMsg
  console.error('[chatApi]', msg, error)
  return Promise.reject(error)
}

// ============================================================
// SSE data 反转义
// 后端发送格式: data: "转义后的内容"
// 转义规则: " → \" | \n → \\n | \r → \\r | \ → \\
// 需要还原为原始内容
// ============================================================
function unescapeSSEData(raw) {
  // 去掉首尾的双引号（如果有）
  let s = raw
  if (s.startsWith('"') && s.endsWith('"')) {
    s = s.slice(1, -1)
  }
  // 反转义顺序: 先还原反斜杠自身，再还原其他
  return s
    .replace(/\\\\/g, '\\')   // \\ → \
    .replace(/\\"/g, '"')     // \" → "
    .replace(/\\n/g, '\n')    // \n → 换行
    .replace(/\\r/g, '\r')    // \r → 回车
    .replace(/\\t/g, '\t')    // \t → 制表符
}

// ============================================================
// SSE 流解析器（可复用）
// 返回 { event, data } 的生成器
// ============================================================
async function* parseSSEStream(reader, decoder) {
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    let currentEvent = null
    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.substring(6).trim()
      } else if (line.startsWith('data:')) {
        const data = unescapeSSEData(line.substring(5).trim())
        yield { event: currentEvent || 'message', data }
        currentEvent = null
      }
    }
  }
  // 处理剩余 buffer
  if (buffer.trim().startsWith('data:')) {
    yield { event: 'message', data: unescapeSSEData(buffer.substring(5).trim()) }
  }
}

// ============================================================
// chatApi 对象
// ============================================================
export const chatApi = {

  // ---- 单轮问答（同步）----
  ask(question) {
    return request.post(API.CHAT_ASK, { question }).catch(e => handleApiError(e, '提问失败'))
  },

  // ---- 多轮对话（同步）----
  chat(question, sessionId) {
    return request.post(API.CHAT_CHAT, { question, sessionId }).catch(e => handleApiError(e, '对话失败'))
  },

  // ---- 流式对话 (SSE) ----
  // 使用 fetch + ReadableStream（POST 接口无法用 EventSource）
  async chatStream(question, sessionId, callbacks) {
    const { onMessage, onError, onComplete, onSources } = callbacks || {}

    try {
      const response = await fetch(`/api${API.CHAT_STREAM}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify({ question, sessionId }),
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let sessionIdReceived = null

      for await (const { event, data } of parseSSEStream(reader, decoder)) {
        switch (event) {
          case 'sources':
            try {
              const sources = JSON.parse(data)
              if (onSources) onSources(sources)
            } catch (e) {
              console.error('解析 sources 失败:', e)
            }
            break

          case 'sessionId':
            sessionIdReceived = data
            console.log('[SSE] sessionId:', data)
            break

          case 'done':
            if (onComplete) onComplete(sessionIdReceived)
            return

          case 'error':
            if (onError) onError(new Error(data))
            return

          case 'message':
          default:
            if (data && data !== '[DONE]') {
              if (onMessage) onMessage(data)
            }
            if (data === '[DONE]') {
              if (onComplete) onComplete(sessionIdReceived)
              return
            }
            break
        }
      }

      // 流正常结束（未收到 done 事件）
      if (onComplete) onComplete(sessionIdReceived)

    } catch (error) {
      console.error('[chatStream] 失败:', error)
      if (onError) onError(error)
    }
  },

  // ---- 会话历史 ----
  getSession(sessionId) {
    return request.get(`${API.CHAT_SESSION}/${sessionId}`).catch(e => handleApiError(e, '获取会话失败'))
  },

  deleteSession(sessionId) {
    return request.delete(`${API.CHAT_SESSION}/${sessionId}`).catch(e => handleApiError(e, '删除会话失败'))
  },

  // ========== RAG 配置接口 ==========

  getRAGConfig() {
    return request.get(API.CONFIG).catch(e => handleApiError(e, '获取配置失败'))
  },

  updateRAGConfig(config) {
    return request.put(API.CONFIG, config).catch(e => handleApiError(e, '更新配置失败'))
  },

  resetRAGConfig() {
    return request.post(API.CONFIG_RESET).catch(e => handleApiError(e, '重置配置失败'))
  },

  testChinese() {
    return request.get(API.TEST_CHINESE).catch(e => handleApiError(e, '测试失败'))
  },

  // ===== 兼容别名 =====
  getConfig()          { return this.getRAGConfig() },
  updateConfig(topK, minSimilarity) { return this.updateRAGConfig({ topK, minSimilarity }) },
  resetConfig()        { return this.resetRAGConfig() },

  // ========== 搜索历史接口 ==========

  historyList(params) {
    return request.get(`${API.HISTORY_USER}/${params.userId}`, {
      params: { limit: params.limit || 50 }
    }).catch(e => handleApiError(e, '获取历史失败'))
  },

  historyBySession(sessionId) {
    return request.get(`${API.HISTORY_SESSION}/${sessionId}`).catch(e => handleApiError(e, '获取历史失败'))
  },

  historySearch(params) {
    return request.get(API.HISTORY_SEARCH, { params }).catch(e => handleApiError(e, '搜索失败'))
  },

  historyRate(id, rating) {
    return request.patch(`${API.HISTORY_RATING}/${id}/rating`, null, {
      params: { rating }
    }).catch(e => handleApiError(e, '评分失败'))
  },

  getRatingStats(userId) {
    return request.get(`${API.HISTORY_STATS}`, {
      params: { userId }
    }).catch(e => handleApiError(e, '获取统计失败'))
  },

  historyDelete(id) {
    return request.delete(`${API.HISTORY_DELETE}/${id}`).catch(e => handleApiError(e, '删除历史失败'))
  },
}
