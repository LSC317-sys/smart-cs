import request from './request'

// ============================================================
// API 端点常量（集中管理）
// ============================================================
const API = {
  // 文档
  DOC_UPLOAD:           '/knowledge/documents/upload',
  DOC_LIST:             '/knowledge/documents',
  DOC_DETAIL:           '/knowledge/documents',  // + /:id
  DOC_CONTENT:           '/knowledge/documents',  // + /:id/content
  DOC_DELETE:           '/knowledge/documents',  // + /:id
  DOC_BATCH_UPLOAD:     '/knowledge/documents/batch-upload',
  DOC_BATCH_DELETE:     '/knowledge/documents/batch-delete',

  // 统计
  STATS:                '/knowledge/stats',

  // 知识库
  KB_LIST:              '/knowledge/kb/list',
  KB_DETAIL:            '/knowledge/kb',       // + /:id
  KB_CREATE:             '/knowledge/kb',
  KB_UPDATE:             '/knowledge/kb',       // PUT + /:id
  KB_DELETE:            '/knowledge/kb',       // DELETE + /:id

  // 版本管理
  VERSION_LIST:          '/knowledge/documents',  // + /:id/versions
  VERSION_DETAIL:        '/knowledge/documents',  // + /:id/versions/:version
  VERSION_UPLOAD:        '/knowledge/documents',  // + /:id/update
  VERSION_ROLLBACK:      '/knowledge/documents',  // + /:id/rollback
}

// ============================================================
// 统一错误处理
// ============================================================
function handleApiError(error, defaultMsg = '操作失败') {
  const msg = error?.response?.data?.message || error?.message || defaultMsg
  console.error('[knowledgeApi]', msg, error)
  return Promise.reject(error)
}

// ============================================================
// 帮助函数：构建带 query 的 URL
// ============================================================
function buildUrl(base, params) {
  const usp = new URLSearchParams()
  for (const [k, v] of Object.entries(params || {})) {
    if (v !== null && v !== undefined) usp.append(k, v)
  }
  const qs = usp.toString()
  return qs ? `${base}?${qs}` : base
}

// ============================================================
// knowledgeApi 对象
// ============================================================
export const knowledgeApi = {

  // ---- 文档上传 ----
  upload(file, title, kbId = 1) {
    const formData = new FormData()
    formData.append('file', file)
    const url = buildUrl(API.DOC_UPLOAD, { title, kbId })
    return request.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).catch(e => handleApiError(e, '上传失败'))
  },

  // ---- 获取文档列表 ----
  getDocuments(kbId = null) {
    const params = kbId ? { kbId } : {}
    return request.get(API.DOC_LIST, { params })
      .catch(e => handleApiError(e, '获取文档列表失败'))
  },

  // ---- 获取文档详情 ----
  getDocument(id) {
    return request.get(`${API.DOC_DETAIL}/${id}`)
      .catch(e => handleApiError(e, '获取文档详情失败'))
  },

  // ---- 删除文档 ----
  deleteDocument(id) {
    return request.delete(`${API.DOC_DELETE}/${id}`)
      .catch(e => handleApiError(e, '删除文档失败'))
  },

  // ---- 获取统计 ----
  getStats(kbId = null) {
    const params = kbId ? { kbId } : {}
    return request.get(API.STATS, { params })
      .catch(e => handleApiError(e, '获取统计失败'))
  },

  // ---- 获取文档内容（预览）----
  getContent(id) {
    return request.get(`${API.DOC_CONTENT}/${id}/content`)
      .catch(e => handleApiError(e, '获取文档内容失败'))
  },

  // ---- 批量上传 ----
  batchUpload(files, titles = [], kbId = 1) {
    const formData = new FormData()
    for (const file of files) {
      formData.append('files', file)
    }
    if (titles.length > 0) {
      formData.append('titles', JSON.stringify(titles))
    }
    const url = buildUrl(API.DOC_BATCH_UPLOAD, { kbId })
    return request.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).catch(e => handleApiError(e, '批量上传失败'))
  },

  // ---- 批量删除 ----
  batchDelete(ids) {
    return request.post(API.DOC_BATCH_DELETE, { ids })
      .catch(e => handleApiError(e, '批量删除失败'))
  },

  // ========== 知识库管理 ==========

  listKnowledgeBases() {
    return request.get(API.KB_LIST)
      .catch(e => handleApiError(e, '获取知识库列表失败'))
  },

  getKnowledgeBase(id) {
    return request.get(`${API.KB_DETAIL}/${id}`)
      .catch(e => handleApiError(e, '获取知识库详情失败'))
  },

  createKnowledgeBase(name, description = '') {
    return request.post(API.KB_CREATE, { name, description })
      .catch(e => handleApiError(e, '创建知识库失败'))
  },

  updateKnowledgeBase(id, name, description) {
    return request.put(`${API.KB_UPDATE}/${id}`, { name, description })
      .catch(e => handleApiError(e, '更新知识库失败'))
  },

  deleteKnowledgeBase(id) {
    return request.delete(`${API.KB_DELETE}/${id}`)
      .catch(e => handleApiError(e, '删除知识库失败'))
  },

  // ========== 文档版本管理 ==========

  getVersions(docId) {
    return request.get(`${API.VERSION_LIST}/${docId}/versions`)
      .catch(e => handleApiError(e, '获取版本列表失败'))
  },

  getVersion(docId, version) {
    return request.get(`${API.VERSION_DETAIL}/${docId}/versions/${version}`)
      .catch(e => handleApiError(e, '获取版本详情失败'))
  },

  uploadNewVersion(docId, file, changeSummary = '', userId = 0) {
    const formData = new FormData()
    formData.append('file', file)
    if (changeSummary) formData.append('changeSummary', changeSummary)
    formData.append('userId', userId)
    return request.post(`${API.VERSION_UPLOAD}/${docId}/update`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).catch(e => handleApiError(e, '上传新版本失败'))
  },

  rollback(docId, version, changeSummary = '', userId = 0) {
    const params = { version }
    if (changeSummary) params.changeSummary = changeSummary
    params.userId = userId
    return request.post(`${API.VERSION_ROLLBACK}/${docId}/rollback`, null, { params })
      .catch(e => handleApiError(e, '版本回滚失败'))
  },
}
