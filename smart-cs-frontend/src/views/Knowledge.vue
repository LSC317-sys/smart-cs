<template>
  <div class="knowledge-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon blue">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalDocs }}</div>
          <div class="stat-label">文档总数</div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon green">
          <el-icon :size="24"><Grid /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalChunks }}</div>
          <div class="stat-label">切片总数</div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon purple">
          <el-icon :size="24"><Folder /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ formatSize(stats.totalContentLen) }}</div>
          <div class="stat-label">内容大小</div>
        </div>
      </el-card>
    </div>
    
    <!-- 文档列表 -->
    <el-card class="document-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20"><Collection /></el-icon>
            <span>知识库文档</span>
            <el-divider direction="vertical" />
            <el-select 
              v-model="currentKbId" 
              placeholder="选择知识库" 
              size="default"
              style="width: 180px;"
              @change="handleKbChange"
            >
              <el-option 
                v-for="kb in knowledgeBases" 
                :key="kb.id" 
                :label="kb.name" 
                :value="kb.id"
              />
            </el-select>
          </div>
          <div class="header-actions">
            <el-button 
              type="danger" 
              :disabled="selectedDocs.length === 0" 
              @click="handleBatchDelete"
              v-if="selectedDocs.length > 0"
            >
              <el-icon><Delete /></el-icon>
              批量删除 ({{ selectedDocs.length }})
            </el-button>
            <el-button type="success" @click="batchDialogVisible = true">
              <el-icon><Upload /></el-icon>
              批量上传
            </el-button>
            <el-upload
              :show-file-list="false"
              :before-upload="handleUpload"
              accept=".txt,.md,.pdf,.doc,.docx,.xls,.xlsx"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传文档
              </el-button>
            </el-upload>
          </div>
        </div>
      </template>
      
      <el-table 
        :data="documents" 
        v-loading="loading" 
        stripe 
        @selection-change="handleSelectionChange"
        class="doc-table"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="title" label="标题" min-width="150">
          <template #default="{ row }">
            <div class="doc-title">
              <el-icon><Document /></el-icon>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="150">
          <template #default="{ row }">
            <span class="filename-text">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getFileTypeTag(row.fileType)">
              {{ getFileType(row.fileType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            <span class="size-text">{{ formatSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small" effect="light">
              <el-icon v-if="row.status !== 'completed'" class="is-loading"><Loading /></el-icon>
              {{ row.status === 'completed' ? '已完成' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              text
              @click="handlePreview(row)"
            >
              预览
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              text
              @click="handleShowVersions(row)"
            >
              版本
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              text
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 批量上传对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量上传文档"
      width="650px"
      class="batch-dialog"
    >
      <el-upload
        ref="batchUploadRef"
        :auto-upload="false"
        :on-change="handleBatchFilesChange"
        :on-remove="handleBatchFilesRemove"
        :file-list="batchFileList"
        multiple
        accept=".txt,.md,.pdf,.doc,.docx,.xls,.xlsx"
        drag
        class="batch-upload"
      >
        <div class="upload-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将文件拖到此处，或<em>点击选择</em></div>
          <div class="upload-tip">支持 TXT, MD, PDF, DOC, DOCX, XLS, XLSX 文件</div>
        </div>
      </el-upload>

      <div v-if="batchFileList.length > 0" class="batch-file-list">
        <div class="batch-file-header">
          已选择 {{ batchFileList.length }} 个文件
        </div>
        <div class="batch-file-items">
          <div class="batch-file-item" v-for="(file, index) in batchFileList" :key="index">
            <el-icon class="file-icon"><Document /></el-icon>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ formatSize(file.size) }}</span>
            <el-tag v-if="batchResults[file.name]" :type="batchResults[file.name].success ? 'success' : 'danger'" size="small" effect="dark">
              {{ batchResults[file.name].success ? '成功' : '失败' }}
            </el-tag>
            <el-icon class="remove-icon" @click="removeBatchFile(index)"><Close /></el-icon>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <span v-if="batchSummary" class="batch-summary">
            <el-icon v-if="batchSummary.failed === 0" color="#67c23a"><CircleCheck /></el-icon>
            <el-icon v-else color="#E6A23C"><Warning /></el-icon>
            成功: <span class="success">{{ batchSummary.success }}</span> / 
            失败: <span class="failed">{{ batchSummary.failed }}</span>
          </span>
          <div class="footer-actions">
            <el-button @click="resetBatchUpload">重置</el-button>
            <el-button type="primary" :loading="batchUploading" :disabled="batchFileList.length === 0" @click="submitBatchUpload">
              <el-icon v-if="!batchUploading"><Upload /></el-icon>
              上传 {{ batchFileList.length > 0 ? `(${batchFileList.length})` : '' }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 文档预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewDoc?.title || '文档预览'"
      width="75%"
      top="5vh"
      class="preview-dialog"
    >
      <div v-if="previewLoading" class="preview-loading">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>正在加载文档内容...</p>
      </div>
      <div v-else class="preview-content">
        <div class="preview-meta">
          <el-tag type="info">文件名: {{ previewDoc?.fileName }}</el-tag>
          <el-tag type="info">切片数: {{ previewDoc?.chunkCount }}</el-tag>
          <el-tag type="success">{{ getFileType(previewDoc?.fileType) }}</el-tag>
        </div>
        <el-divider />
        <div class="preview-text">
          <div v-for="(chunk, index) in previewChunks" :key="index" class="chunk-block">
            <div class="chunk-header">
              <el-icon><Grid /></el-icon>
              切片 {{ index + 1 }}
            </div>
            <div class="chunk-content">{{ chunk }}</div>
          </div>
          <el-empty v-if="previewChunks.length === 0" description="暂无内容" />
        </div>
      </div>
    </el-dialog>
    
    <!-- 版本历史对话框 -->
    <el-dialog
      v-model="versionDialogVisible"
      :title="versionDocTitle"
      width="700px"
      class="version-dialog"
    >
      <div class="version-header">
        <span class="version-tip">文档版本历史记录</span>
        <el-upload
          :show-file-list="false"
          :before-upload="(file) => handleUploadNewVersion(file)"
          accept=".txt,.md,.pdf,.doc,.docx,.xls,.xlsx"
        >
          <el-button type="primary" size="small">
            <el-icon><Upload /></el-icon>
            上传新版本
          </el-button>
        </el-upload>
      </div>
      
      <el-table 
        :data="versions" 
        v-loading="versionLoading"
        stripe
        class="version-table"
      >
        <el-table-column prop="version" label="版本" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeSummary" label="变更说明" min-width="200">
          <template #default="{ row }">
            <span class="change-summary">{{ row.changeSummary || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="100">
          <template #default="{ row }">
            <span class="file-size">{{ formatSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button 
              type="warning" 
              size="small" 
              text
              @click="handleRollback(row)"
              :disabled="row.version === currentVersion"
            >
              回滚
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="versions.length === 0 && !versionLoading" description="暂无版本历史" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, Loading, UploadFilled, Document, Close, 
  Delete, Grid, Folder, Collection, CircleCheck, Warning
} from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/knowledge'
import { notifyDocUpload, notifyVersionUpdate, notifyRollback } from '@/utils/notification'

const loading = ref(false)
const documents = ref([])
const stats = ref({
  totalDocs: 0,
  totalChunks: 0,
  totalContentLen: 0
})

// 知识库相关
const knowledgeBases = ref([])
const currentKbId = ref(parseInt(localStorage.getItem('currentKbId')) || 1)

async function loadKnowledgeBases() {
  try {
    const res = await knowledgeApi.listKnowledgeBases()
    knowledgeBases.value = Array.isArray(res) ? res : (res?.value || [])
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

function handleKbChange(kbId) {
  localStorage.setItem('currentKbId', kbId)
  loadDocuments()
  loadStats()
}

// 版本历史相关
const versionDialogVisible = ref(false)
const versionDocId = ref(null)
const versionDocTitle = ref('版本历史')
const versions = ref([])
const versionLoading = ref(false)
const currentVersion = ref(0)

async function handleShowVersions(row) {
  versionDocId.value = row.id
  versionDocTitle.value = `${row.title} - 版本历史`
  versionDialogVisible.value = true
  loadVersions()
}

async function loadVersions() {
  versionLoading.value = true
  try {
    const res = await knowledgeApi.getVersions(versionDocId.value)
    versions.value = Array.isArray(res) ? res : (res?.value || [])
    // 找到最新版本号
    if (versions.value.length > 0) {
      currentVersion.value = Math.max(...versions.value.map(v => v.version))
    } else {
      currentVersion.value = 0
    }
  } catch (error) {
    console.error('加载版本历史失败:', error)
    versions.value = []
  } finally {
    versionLoading.value = false
  }
}

async function handleUploadNewVersion(file) {
  const { value: changeSummary } = await ElMessageBox.prompt('请输入变更说明', '上传新版本', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：修复了文档中的错误'
  }).catch(() => ({ value: null }))
  
  if (changeSummary === null) return false
  
  try {
    await knowledgeApi.uploadNewVersion(versionDocId.value, file, changeSummary)
    ElMessage.success('新版本上传成功')
    notifyVersionUpdate(versionDocTitle.value, '')
    loadVersions()
    loadDocuments()
    loadStats()
  } catch (error) {
    ElMessage.error('上传失败: ' + (error.message || '未知错误'))
  }
  return false
}

async function handleRollback(row) {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到版本 v${row.version} 吗？`,
      '版本回滚',
      { type: 'warning' }
    )
    
    const { value: changeSummary } = await ElMessageBox.prompt('请输入回滚说明（可选）', '版本回滚', {
      confirmButtonText: '确定',
      cancelButtonText: '跳过',
      inputPlaceholder: '例如：回滚到之前正确的版本'
    }).catch(() => ({ value: '回滚到版本 ' + row.version }))
    
    await knowledgeApi.rollback(versionDocId.value, row.version, changeSummary)
    ElMessage.success('回滚成功')
    notifyRollback(versionDocTitle.value, row.version)
    loadVersions()
    loadDocuments()
    loadStats()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('回滚失败: ' + (error.message || '未知错误'))
    }
  }
}

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewDoc = ref(null)
const previewChunks = ref([])

const batchDialogVisible = ref(false)
const batchUploadRef = ref(null)
const batchFileList = ref([])
const batchUploading = ref(false)
const batchResults = ref({})
const batchSummary = ref(null)
const selectedDocs = ref([])

async function loadDocuments() {
  loading.value = true
  try {
    const res = await knowledgeApi.getDocuments(currentKbId.value)
    documents.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载文档列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await knowledgeApi.getStats(currentKbId.value)
    stats.value = res || stats.value
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

async function handlePreview(row) {
  previewDoc.value = row
  previewVisible.value = true
  previewLoading.value = true
  previewChunks.value = []
  
  try {
    const res = await knowledgeApi.getContent(row.id)
    if (res && res.chunks) {
      previewChunks.value = res.chunks
    } else if (res && res.content) {
      previewChunks.value = [res.content]
    } else {
      previewChunks.value = []
    }
  } catch (error) {
    ElMessage.error('加载文档内容失败')
    previewChunks.value = ['加载失败: ' + error.message]
  } finally {
    previewLoading.value = false
  }
}

async function handleUpload(file) {
  const title = file.name.replace(/\.[^/.]+$/, '')
  
  try {
    await knowledgeApi.upload(file, title, currentKbId.value)
    ElMessage.success('上传成功')
    notifyDocUpload(title)
    loadDocuments()
    loadStats()
  } catch (error) {
    ElMessage.error('上传失败')
  }
  
  return false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除文档"${row.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await knowledgeApi.deleteDocument(row.id)
    ElMessage.success('删除成功')
    loadDocuments()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleSelectionChange(selection) {
  selectedDocs.value = selection
}

async function handleBatchDelete() {
  if (selectedDocs.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDocs.value.length} 个文档吗？此操作不可恢复！`,
      '批量删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    
    const ids = selectedDocs.value.map(d => d.id)
    const res = await knowledgeApi.batchDelete(ids)
    
    ElMessage.success(res.message || `删除完成：${res.deleted}/${res.total} 成功`)
    selectedDocs.value = []
    loadDocuments()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

function handleBatchFilesChange(file, fileList) {
  batchFileList.value = fileList
  const newResults = {}
  for (const f of fileList) {
    if (batchResults.value[f.name]) {
      newResults[f.name] = batchResults.value[f.name]
    }
  }
  batchResults.value = newResults
  batchSummary.value = null
}

function handleBatchFilesRemove(file, fileList) {
  batchFileList.value = fileList
  delete batchResults.value[file.name]
}

function removeBatchFile(index) {
  batchFileList.value.splice(index, 1)
  delete batchResults.value[batchFileList.value[index]?.name]
}

async function submitBatchUpload() {
  if (batchFileList.value.length === 0) return
  
  batchUploading.value = true
  batchResults.value = {}
  batchSummary.value = null
  
  const files = batchFileList.value.map(f => f.raw)
  
  try {
    const res = await knowledgeApi.batchUpload(files, [], currentKbId.value)
    
    if (res.results) {
      for (const item of res.results) {
        batchResults.value[item.fileName] = { success: true, data: item }
      }
    }
    if (res.failures) {
      for (const item of res.failures) {
        batchResults.value[item.fileName] = { success: false, error: item.error }
      }
    }
    
    batchSummary.value = {
      success: res.success || 0,
      failed: res.failed || 0,
      total: res.total || files.length
    }
    
    if (res.success === res.total) {
      ElMessage.success(res.message || '全部上传成功')
      notifyDocUpload(`${files.length} 个文档`)
    } else if (res.failed > 0) {
      ElMessage.warning(res.message || `部分成功：${res.success}/${res.total}`)
    }
    
    loadDocuments()
    loadStats()
  } catch (error) {
    ElMessage.error('批量上传失败: ' + (error.message || '未知错误'))
  } finally {
    batchUploading.value = false
  }
}

function resetBatchUpload() {
  batchFileList.value = []
  batchResults.value = {}
  batchSummary.value = null
  if (batchUploadRef.value) {
    batchUploadRef.value.clearFiles()
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  while (bytes >= 1024 && i < units.length - 1) {
    bytes /= 1024
    i++
  }
  return `${bytes.toFixed(1)} ${units[i]}`
}

function getFileType(mimeType) {
  const types = {
    'text/plain': '文本',
    'text/markdown': 'Markdown',
    'application/pdf': 'PDF',
    'application/msword': 'Word',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word'
  }
  return types[mimeType] || mimeType || '未知'
}

function getFileTypeTag(mimeType) {
  const type = getFileType(mimeType)
  if (type === 'PDF') return 'danger'
  if (type === 'Word') return 'primary'
  if (type === 'Markdown') return 'success'
  return 'info'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadKnowledgeBases()
  loadDocuments()
  loadStats()
})
</script>

<style scoped lang="scss">
.knowledge-page {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  min-height: 100vh;
}

.stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1;
  border-radius: 16px;
  border: none;
  transition: all 0.3s;
  
  :deep(.el-card__body) {
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1);
  }
  
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    
    &.blue {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
    
    &.green {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    }
    
    &.purple {
      background: linear-gradient(135deg, #a855f7 0%, #ec4899 100%);
    }
  }
  
  .stat-content {
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
    }
    
    .stat-label {
      margin-top: 4px;
      color: #909399;
      font-size: 14px;
    }
  }
}

.document-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }
}

.doc-table {
  :deep(.el-table__header th) {
    background: #fafafa;
    font-weight: 600;
    color: #606266;
  }
  
  :deep(.el-table__row) {
    transition: all 0.2s;
    
    &:hover > td {
      background: #f5f7fa;
    }
  }
  
  .doc-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    color: #303133;
    
    .el-icon {
      color: #667eea;
    }
  }
  
  .filename-text {
    color: #606266;
    font-size: 13px;
  }
  
  .size-text {
    color: #909399;
    font-size: 13px;
  }
  
  .time-text {
    color: #909399;
    font-size: 12px;
  }
}

// 批量上传对话框样式
.batch-dialog {
  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }
}

.batch-upload {
  :deep(.el-upload-dragger) {
    padding: 40px 20px;
    border-radius: 16px;
    border: 2px dashed #dcdfe6;
    background: #fafafa;
    transition: all 0.3s;
    
    &:hover {
      border-color: #667eea;
      background: #f5f7fa;
    }
  }
  
  .upload-content {
    text-align: center;
    
    .upload-icon {
      font-size: 48px;
      color: #667eea;
      margin-bottom: 16px;
    }
    
    .upload-text {
      font-size: 15px;
      color: #606266;
      margin-bottom: 8px;
      
      em {
        color: #667eea;
        font-style: normal;
        font-weight: 600;
      }
    }
    
    .upload-tip {
      font-size: 12px;
      color: #909399;
    }
  }
}

.batch-file-list {
  margin-top: 20px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  
  .batch-file-header {
    padding: 12px 16px;
    background: #f5f7fa;
    font-size: 13px;
    color: #606266;
    font-weight: 500;
    border-bottom: 1px solid #ebeef5;
  }
  
  .batch-file-items {
    max-height: 250px;
    overflow-y: auto;
    
    .batch-file-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      border-bottom: 1px solid #f0f0f0;
      transition: background 0.2s;
      
      &:last-child {
        border-bottom: none;
      }
      
      &:hover {
        background: #fafafa;
      }
      
      .file-icon {
        color: #667eea;
        font-size: 18px;
      }
      
      .file-name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 14px;
        color: #303133;
      }
      
      .file-size {
        color: #909399;
        font-size: 12px;
      }
      
      .remove-icon {
        cursor: pointer;
        color: #c0c4cc;
        transition: color 0.2s;
        
        &:hover {
          color: #f56c6c;
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .batch-summary {
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
    
    .success {
      color: #67c23a;
      font-weight: 700;
    }
    
    .failed {
      color: #f56c6c;
      font-weight: 700;
    }
  }
  
  .footer-actions {
    display: flex;
    gap: 12px;
  }
}

// 预览对话框样式
.preview-dialog {
  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #909399;
  
  p {
    margin-top: 16px;
    font-size: 14px;
  }
}

.preview-content {
  .preview-meta {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
  
  .preview-text {
    max-height: 55vh;
    overflow-y: auto;
    padding-right: 10px;
    
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 3px;
    }
  }
  
  .chunk-block {
    margin-bottom: 20px;
    padding: 20px;
    background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
    border-radius: 12px;
    border: 1px solid #ebeef5;
    
    .chunk-header {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #667eea;
      margin-bottom: 12px;
      font-weight: 600;
    }
    
    .chunk-content {
      line-height: 1.8;
      white-space: pre-wrap;
      word-break: break-word;
      color: #303133;
    }
  }
}
</style>
