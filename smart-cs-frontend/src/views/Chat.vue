<template>
  <div class="chat-page">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="header-left">
        <div class="ai-avatar">
          <el-icon :size="24"><ChatDotRound /></el-icon>
        </div>
        <div class="header-info">
          <span class="title">SmartCS 智能客服</span>
          <span class="status">
            <span class="status-dot"></span>
            {{ loading ? '正在思考...' : '在线' }}
          </span>
        </div>
      </div>
      <div class="header-actions">
        <el-tooltip content="清空对话" placement="bottom">
          <el-button circle size="small" @click="clearMessages" :disabled="messages.length === 0">
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="RAG 检索配置" placement="bottom">
          <el-button circle size="small" @click="showConfigDialog = true">
            <el-icon><Setting /></el-icon>
          </el-button>
        </el-tooltip>
        <ChatHistory ref="chatHistoryRef" @select="onHistorySelect" />
      </div>
    </div>

    <div class="chat-container">
      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesRef">
        <!-- 空状态 -->
        <div v-if="messages.length === 0 && !streamingContent" class="empty-state">
          <div class="empty-icon">
            <div class="ai-mascot">
              <el-icon :size="80" color="#409eff"><ChatDotRound /></el-icon>
            </div>
            <div class="welcome-text">
              <h2>👋 你好！我是 SmartCS</h2>
              <p>基于 RAG 技术的智能问答助手</p>
            </div>
          </div>
          
          <div class="quick-questions">
            <p class="quick-title">💡 快速提问：</p>
            <div class="question-tags">
              <el-tag
                v-for="(q, idx) in quickQuestions"
                :key="idx"
                class="question-tag"
                @click="quickAsk(q)"
                effect="plain"
                type="info"
              >
                {{ q }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <!-- 消息列表 -->
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message"
          :class="[msg.role, { 'message-enter': msg.animate }]"
          :style="{ animationDelay: msg.animate ? `${index * 0.1}s` : '0s' }"
        >
          <!-- AI 头像 -->
          <div v-if="msg.role === 'assistant'" class="avatar assistant-avatar">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </div>
          
          <div class="message-body">
            <div class="message-content" v-html="formatMessage(msg.content)"></div>
            
            <!-- 消息操作 -->
            <div class="message-actions" v-if="msg.role === 'assistant'">
              <el-tooltip content="复制回答" placement="top">
                <el-button size="small" text @click="copyMessage(msg.content)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="有帮助" placement="top">
                <el-button
                  size="small"
                  text
                  :type="msg.feedback === 'like' ? 'primary' : 'default'"
                  @click="feedback(msg, 'like')"
                >
                  <el-icon><CaretTop /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="需改进" placement="top">
                <el-button
                  size="small"
                  text
                  :type="msg.feedback === 'dislike' ? 'danger' : 'default'"
                  @click="feedback(msg, 'dislike')"
                >
                  <el-icon><CaretBottom /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
            
            <!-- 来源列表（使用提取的组件） -->
            <SourceList
              v-if="msg.sources && msg.sources.length > 0"
              :sources="msg.sources"
              :expanded="expandedSources[index]"
              @toggle="toggleSources(index)"
              @view="viewSourceContent"
              @copy="copySourceContent"
            />
            
            <!-- 时间戳 -->
            <div class="message-time">{{ msg.time }}</div>
          </div>
          
          <!-- 用户头像 -->
          <div v-if="msg.role === 'user'" class="avatar user-avatar">
            <el-icon :size="20"><User /></el-icon>
          </div>
        </div>
        
        <!-- 流式输出 -->
        <div v-if="streamingContent" class="message assistant message-enter">
          <div class="avatar assistant-avatar">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </div>
          <div class="message-body">
            <div class="message-content" v-html="formatMessage(streamingContent)"></div>
            
            <!-- 流式输出中的来源（使用提取的组件） -->
            <SourceList
              v-if="streamingSources && streamingSources.length > 0"
              :sources="streamingSources"
              :expanded="showStreamingSources"
              @toggle="showStreamingSources = !showStreamingSources"
              @view="viewSourceContent"
              @copy="copySourceContent"
            />
          </div>
        </div>
        
        <!-- 加载动画 -->
        <div v-if="loading && !streamingContent" class="message assistant">
          <div class="avatar assistant-avatar">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </div>
          <div class="message-body">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 输入框 -->
      <div class="chat-input">
        <div class="input-wrapper">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="请输入您的问题... (Ctrl + Enter 发送)"
            @keydown.enter.ctrl="handleSend"
            :disabled="loading"
            class="input-area"
          />
          <div class="input-actions">
            <div class="left-actions">
              <el-checkbox v-model="useStream" size="small">
                流式输出
              </el-checkbox>
              <el-tag v-if="ragConfig.minSimilarity !== undefined" size="small">
                阈值: {{ (ragConfig.minSimilarity * 100).toFixed(0) }}%
              </el-tag>
            </div>
            <el-button 
              type="primary" 
              :loading="loading"
              :disabled="!inputText.trim()"
              @click="handleSend"
              circle
              class="send-btn"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- RAG 配置对话框 -->
    <el-dialog
      v-model="showConfigDialog"
      title="RAG 检索配置"
      width="420px"
    >
      <div class="config-item">
        <label>最小相关度阈值</label>
        <div class="config-value">{{ (localMinSimilarity * 100).toFixed(0) }}%</div>
        <el-slider
          v-model="localMinSimilarity"
          :min="0"
          :max="1"
          :step="0.05"
          :format-tooltip="val => (val * 100).toFixed(0) + '%'"
        />
        <div class="config-hint">
          阈值越高，检索结果越严格（更少但更相关）
        </div>
      </div>

      <div class="config-item">
        <label>检索数量 (Top-K)</label>
        <div class="config-value">{{ localTopK }}</div>
        <el-slider
          v-model="localTopK"
          :min="1"
          :max="10"
          :step="1"
        />
        <div class="config-hint">
          返回的参考文档片段数量
        </div>
      </div>

      <div class="config-info">
        <el-alert
          :title="'当前配置（来自服务器）: 相关度≥' + (ragConfig.minSimilarity * 100).toFixed(0) + '%，Top-K=' + ragConfig.topK"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <template #footer>
        <el-button @click="showConfigDialog = false">取消</el-button>
        <el-button @click="resetConfig">重置默认</el-button>
        <el-button type="primary" @click="applyConfig" :loading="configLoading">
          应用配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
export default { name: 'Chat' }
</script>
<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Delete,
  Setting,
  Document,
  ArrowRight,
  CopyDocument,
  CaretTop,
  CaretBottom,
  User,
  Promotion
} from '@element-plus/icons-vue'
import { chatApi } from '@/api/chat'
import { marked } from 'marked'
import ChatHistory from './ChatHistory.vue'
import SourceList from '@/components/SourceList.vue'
import { notifyAnswerComplete, notifyLowRating } from '@/utils/notification'

// localStorage persistence for chat messages
function loadMessages() {
  try { const d = localStorage.getItem('smartcs_messages'); return d ? JSON.parse(d) : [] } catch { return [] }
}
function saveMessages() {
  try { localStorage.setItem('smartcs_messages', JSON.stringify(messages.value)) } catch {}
}
const messages = ref(loadMessages())
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref(null)
const useStream = ref(true)
const streamingContent = ref('')
const currentSessionId = ref('')

// 搜索历史
const chatHistoryRef = ref(null)

// 搜索历史选择回调
function onHistorySelect(question) {
  inputText.value = question
  nextTick(() => {
    const textarea = document.querySelector('.chat-input .el-textarea__inner')
    if (textarea) textarea.focus()
  })
}

const showStreamingSources = ref(false)
const expandedSources = ref({})
const streamingSources = ref([])

// RAG 配置
const showConfigDialog = ref(false)
const configLoading = ref(false)
const ragConfig = ref({})
const localMinSimilarity = ref(0.3)
const localTopK = ref(5)

// 快速提问示例
const quickQuestions = ref([
  '什么是 RAG？',
  '如何使用智能客服？',
  '系统支持哪些功能？',
  '如何上传知识库文档？'
])

// 加载配置
async function loadRAGConfig() {
  try {
    const res = await chatApi.getRAGConfig()
    if (res.data) {
      ragConfig.value = res.data
      localMinSimilarity.value = res.data.minSimilarity || 0.3
      localTopK.value = res.data.topK || 5
    }
  } catch (e) {
    console.error('加载 RAG 配置失败:', e)
  }
}

// 应用配置
async function applyConfig() {
  configLoading.value = true
  try {
    const res = await chatApi.updateRAGConfig({
      minSimilarity: localMinSimilarity.value,
      topK: localTopK.value
    })
    if (res.data) {
      ragConfig.value = res.data
      ElMessage.success('配置已更新')
      showConfigDialog.value = false
    }
  } catch (e) {
    ElMessage.error('配置更新失败')
  } finally {
    configLoading.value = false
  }
}

// 重置配置
async function resetConfig() {
  try {
    const res = await chatApi.resetRAGConfig()
    if (res.data) {
      ragConfig.value = res.data
      localMinSimilarity.value = res.data.minSimilarity || 0.3
      localTopK.value = res.data.topK || 5
      ElMessage.success('配置已重置为默认值')
    }
  } catch (e) {
    ElMessage.error('重置失败')
  }
}

// 清空对话
async function clearMessages() {
  try {
    await ElMessageBox.confirm('确定要清空所有对话记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    messages.value = []
    ElMessage.success('对话已清空')
    try { localStorage.removeItem('smartcs_messages') } catch {}
  } catch {
    // 用户取消
  }
}

// 快速提问
function quickAsk(question) {
  inputText.value = question
  handleSend()
}

// 复制消息
async function copyMessage(content) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 反馈
function feedback(msg, type) {
  if (msg.feedback === type) {
    msg.feedback = null
  } else {
    msg.feedback = type
    const rating = type === 'like' ? 'good' : 'bad'
    fetch('/api/chat/feedback', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSessionId.value || msg.sessionId || '',
        question: msg.question || '',
        answer: msg.content || '',
        rating: rating,
        comment: ''
      })
    }).catch(e => console.error('反馈提交失败:', e))
    ElMessage.success(type === 'like' ? '感谢您的反馈！' : '我们会继续改进')
    if (type === 'dislike') {
      notifyLowRating(msg.content?.slice(0, 50) || '用户不满意', 1)
    }
  }
}

function formatMessage(content) {
  if (!content) return ''
  try {
    return marked.parse(content)
  } catch {
    return content
  }
}

function getCurrentTime() {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 滚动到底部
function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTo({
      top: messagesRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

// 发送消息（统一入口）
async function handleSend() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  const userMessage = {
    role: 'user',
    content: question,
    time: getCurrentTime(),
    animate: true
  }

  messages.value.push(userMessage)
  inputText.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  if (useStream.value) {
    await handleStreamChat(question)
  } else {
    await handleSyncChat(question)
  }
}

// 同步对话
async function handleSyncChat(question) {
  try {
    const res = await chatApi.ask(question)

    messages.value.push({
      role: 'assistant',
      content: res.answer || '抱歉，我无法回答这个问题。',
      time: getCurrentTime(),
      animate: true,
      feedback: null
    })

    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('请求失败，请重试')
    messages.value.pop()
  } finally {
    loading.value = false
  }
}

// 流式对话（使用对象回调，更清晰）
async function handleStreamChat(question) {
  streamingContent.value = ''
  streamingSources.value = []
  showStreamingSources.value = false

  try {
    await chatApi.chatStream(question, currentSessionId.value, {
      onMessage(chunk) {
        streamingContent.value += chunk
        nextTick(() => scrollToBottom())
      },
      onError(error) {
        ElMessage.error('流式请求失败: ' + error.message)
        streamingContent.value = ''
        streamingSources.value = []
        loading.value = false
      },
      onComplete() {
        if (streamingContent.value) {
          const lastQ = inputText.value || ''
          messages.value.push({
            role: 'assistant',
            content: streamingContent.value,
            sources: streamingSources.value.length > 0 ? [...streamingSources.value] : null,
            time: getCurrentTime(),
            animate: true,
            feedback: null
          })
          if (lastQ) notifyAnswerComplete(lastQ)
        }
        streamingContent.value = ''
        streamingSources.value = []
        loading.value = false
        nextTick(() => scrollToBottom())
      },
      onSources(sources) {
        streamingSources.value = sources || []
        nextTick(() => scrollToBottom())
      }
    })
  } catch (error) {
    ElMessage.error('请求失败，请重试')
    messages.value.pop()
    loading.value = false
    streamingContent.value = ''
    streamingSources.value = []
  }
}

// 切换来源展开
function toggleSources(index) {
  expandedSources.value[index] = !expandedSources.value[index]
}

// 查看原文内容
function viewSourceContent(src) {
  ElMessageBox.alert(
    src.content || src.contentPreview,
    src.docTitle || '原文内容',
    { confirmButtonText: '关闭', dangerouslyUseHTMLString: false }
  )
}

// 复制原文内容
function copySourceContent(src) {
  const content = src.content || src.contentPreview
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => {
  // Auto-save messages every 3 seconds
  const saveTimer = setInterval(saveMessages, 3000)
  // Save on page unload
  window.addEventListener('beforeunload', saveMessages)

  watch(() => chatHistoryRef.value?.expanded, (val) => {
    if (val) chatHistoryRef.value?.loadHistory && chatHistoryRef.value.loadHistory()
  }, { immediate: true })
  loadRAGConfig()
})
</script>

<style scoped lang="scss">
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea11 0%, #764ba211 100%);

  .chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid #e6e6e6;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    max-width: 900px;
    width: 100%;
    margin: 0 auto;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .ai-avatar {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
      }

      .header-info {
        display: flex;
        flex-direction: column;

        .title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }

        .status {
          font-size: 12px;
          color: #909399;
          display: flex;
          align-items: center;
          gap: 6px;

          .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #67c23a;
            animation: pulse 2s infinite;
          }

          @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
          }
        }
      }
    }

    .header-actions {
      display: flex;
      gap: 8px;
    }
  }

  .chat-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    max-width: 900px;
    width: 100%;
    margin: 0 auto;
    padding: 20px;
    overflow: hidden;
  }

  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 3px;

      &:hover {
        background: #c0c4cc;
      }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: #909399;

      .empty-icon {
        text-align: center;
        margin-bottom: 40px;

        .ai-mascot {
          width: 120px;
          height: 120px;
          margin: 0 auto 20px;
          border-radius: 50%;
          background: linear-gradient(135deg, #667eea22 0%, #764ba222 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          animation: float 3s ease-in-out infinite;
        }

        @keyframes float {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-10px); }
        }

        .welcome-text {
          h2 {
            font-size: 24px;
            color: #303133;
            margin-bottom: 8px;
          }

          p {
            font-size: 14px;
            color: #909399;
          }
        }
      }

      .quick-questions {
        width: 100%;
        max-width: 600px;

        .quick-title {
          font-size: 14px;
          color: #606266;
          margin-bottom: 12px;
          text-align: center;
        }

        .question-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;
          justify-content: center;

          .question-tag {
            cursor: pointer;
            transition: all 0.3s;
            padding: 8px 16px;

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
            }
          }
        }
      }
    }
  }
}

.message {
  margin-bottom: 24px;
  display: flex;
  gap: 12px;
  animation: slideIn 0.3s ease-out;

  @keyframes slideIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &.user {
    flex-direction: row-reverse;

    .message-body {
      .message-content {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        border-radius: 16px 16px 4px 16px;
      }
    }
  }

  &.assistant {
    .message-body {
      .message-content {
        background: #fff;
        color: #303133;
        border-radius: 16px 16px 16px 4px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      }
    }
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.assistant-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    &.user-avatar {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      color: white;
    }
  }

  .message-body {
    max-width: 70%;
    display: flex;
    flex-direction: column;

    .message-content {
      padding: 12px 16px;
      line-height: 1.6;
      word-break: break-word;

      :deep(p) {
        margin: 0;

        &:not(:last-child) {
          margin-bottom: 8px;
        }
      }

      :deep(code) {
        background: rgba(0, 0, 0, 0.05);
        padding: 2px 6px;
        border-radius: 4px;
        font-family: 'Monaco', 'Menlo', monospace;
        font-size: 0.9em;
      }

      :deep(pre) {
        background: #282c34;
        color: #abb2bf;
        padding: 12px;
        border-radius: 8px;
        overflow-x: auto;
        margin: 8px 0;

        code {
          background: transparent;
          padding: 0;
          color: inherit;
        }
      }
    }

    .message-actions {
      display: flex;
      gap: 4px;
      margin-top: 8px;
      opacity: 0;
      transition: opacity 0.2s;
    }

    &:hover .message-actions {
      opacity: 1;
    }

    .message-time {
      font-size: 11px;
      color: #c0c4cc;
      margin-top: 4px;
    }
  }
}

// 打字动画
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 16px 16px 16px 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #909399;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }

  @keyframes typing {
    0%, 60%, 100% {
      transform: translateY(0);
      opacity: 0.7;
    }
    30% {
      transform: translateY(-10px);
      opacity: 1;
    }
  }
}

// 输入区域
.chat-input {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid #e6e6e6;

  .input-wrapper {
    max-width: 900px;
    margin: 0 auto;

    .input-area {
      :deep(.el-textarea__inner) {
        border-radius: 12px;
        padding: 12px 16px;
        font-size: 14px;
        line-height: 1.6;
        resize: none;
        transition: all 0.3s;

        &:focus {
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
        }
      }
    }

    .input-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 12px;

      .left-actions {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .send-btn {
        width: 40px;
        height: 40px;
        font-size: 16px;
      }
    }
  }
}

// RAG 配置对话框
.config-item {
  margin-bottom: 24px;

  label {
    display: block;
    font-weight: 500;
    color: #303133;
    margin-bottom: 8px;
  }

  .config-value {
    font-size: 28px;
    font-weight: bold;
    color: #409eff;
    margin-bottom: 8px;
  }

  .config-hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
