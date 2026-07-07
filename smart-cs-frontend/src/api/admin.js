import request from './request'

// ============================================================
// API 端点常量
// ============================================================
const API = {
  STATS_OVERVIEW:        '/admin/stats/overview',
  STATS_CONVERSATIONS: '/admin/stats/conversations',
  STATS_CONVERSATION_CLEAR: '/admin/stats/conversations',  // + /:sessionId/clear
  STATS_KNOWLEDGE:      '/admin/stats/knowledge',
  CONFIG_MODELS:        '/admin/config/models',
  FEEDBACK_STATS:       '/admin/stats/feedback',
  FEEDBACK_LIST:        '/admin/stats/feedback/list',
}

// ============================================================
// 统一错误处理
// ============================================================
function handleApiError(error, defaultMsg = '操作失败') {
  const msg = error?.response?.data?.message || error?.message || defaultMsg
  console.error('[adminApi]', msg, error)
  return Promise.reject(error)
}

// ============================================================
// adminApi 对象
// ============================================================
export const adminApi = {

  // ---- 统计概览 ----
  getOverview() {
    return request.get(API.STATS_OVERVIEW)
      .catch(e => handleApiError(e, '获取统计概览失败'))
  },

  // ---- 会话统计 ----
  getConversations() {
    return request.get(API.STATS_CONVERSATIONS)
      .catch(e => handleApiError(e, '获取会话统计失败'))
  },

  // ---- 清除指定会话 ----
  clearSession(sessionId) {
    return request.get(`${API.STATS_CONVERSATION_CLEAR}/${sessionId}/clear`)
      .catch(e => handleApiError(e, '清除会话失败'))
  },

  // ---- 知识库统计 ----
  getKnowledgeStats() {
    return request.get(API.STATS_KNOWLEDGE)
      .catch(e => handleApiError(e, '获取知识库统计失败'))
  },

  // ---- 模型配置 ----
  getModelConfig() {
    return request.get(API.CONFIG_MODELS)
      .catch(e => handleApiError(e, '获取模型配置失败'))
  },

  // ---- 用户反馈统计 ----
  getFeedbackStats() {
    return request.get(API.FEEDBACK_STATS)
      .catch(e => handleApiError(e, '获取反馈统计失败'))
  },

  // ---- 用户反馈列表 ----
  getFeedbackList() {
    return request.get(API.FEEDBACK_LIST)
      .catch(e => handleApiError(e, '获取反馈列表失败'))
  },
}