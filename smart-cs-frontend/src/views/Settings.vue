<template>
  <div class="settings-page">
    <Transition name="slide-up" appear>
      <div class="settings-header-section">
        <div class="header-bg"></div>
        <div class="header-content">
          <div class="header-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06-.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
          </div>
          <div class="header-text">
            <h1 class="page-title">系统设置</h1>
            <p class="page-desc">配置 RAG 检索参数和系统偏好</p>
          </div>
        </div>
      </div>
    </Transition>

    <div class="settings-content">
      <Transition name="slide-up" appear>
        <div class="settings-card">
          <div class="card-header">
            <div class="card-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
              </svg>
            </div>
            <div class="card-title-wrap">
              <h2 class="card-title">RAG 检索参数</h2>
              <p class="card-subtitle">调整检索匹配行为</p>
            </div>
          </div>

          <div class="param-list">
            <div class="param-item">
              <div class="param-header">
                <div class="param-info">
                  <span class="param-label">Top-K</span>
                  <span class="param-desc">返回最相似的前 K 个文本块</span>
                </div>
                <div class="param-value-badge">{{ ragConfig.topK }}</div>
              </div>
              <el-slider v-model="ragConfig.topK" :min="1" :max="20" :step="1" @change="saveConfig" />
              <div class="param-range"><span>1</span><span>20</span></div>
            </div>

            <div class="param-item">
              <div class="param-header">
                <div class="param-info">
                  <span class="param-label">相似度阈值</span>
                  <span class="param-desc">只返回高于此值的结果</span>
                </div>
                <div class="param-value-badge">{{ (ragConfig.minSimilarity * 100).toFixed(0) }}%</div>
              </div>
              <el-slider v-model="ragConfig.minSimilarity" :min="0" :max="1" :step="0.05" @change="saveConfig" />
              <div class="param-range"><span>0%</span><span>100%</span></div>
            </div>
          </div>

          <div class="preset-section">
            <span class="preset-label">快速预设</span>
            <div class="preset-btns">
              <el-button size="small" :type="activePreset === 'precise' ? 'primary' : ''" @click="applyPreset('precise')">精确</el-button>
              <el-button size="small" :type="activePreset === 'balanced' ? 'primary' : ''" @click="applyPreset('balanced')">均衡</el-button>
              <el-button size="small" :type="activePreset === 'broad' ? 'primary' : ''" @click="applyPreset('broad')">广泛</el-button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="slide-up" appear>
        <div class="settings-card">
          <div class="card-header">
            <div class="card-icon warning">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </div>
            <div class="card-title-wrap">
              <h2 class="card-title">系统信息</h2>
              <p class="card-subtitle">运行环境状态</p>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">前端版本</span>
              <span class="info-value">v1.0.0</span>
            </div>
            <div class="info-item">
              <span class="info-label">后端服务</span>
              <span class="info-value" :class="backendOnline ? 'status-online' : 'status-offline'">
                <span class="status-dot"></span>
                {{ backendOnline ? '在线' : '离线' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">嵌入模型</span>
              <el-tag size="small" type="info">BAAI/bge-m3</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">对话模型</span>
              <el-tag size="small" type="info">Qwen2.5-7B</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">向量存储</span>
              <el-tag size="small" type="info">Redis</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">运行环境</span>
              <el-tag size="small" type="info">Spring Cloud</el-tag>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="slide-up" appear>
        <div class="settings-card about-card">
          <div class="about-logo">
            <div class="logo-icon">SC</div>
            <div class="logo-info">
              <span class="logo-name">SmartCS</span>
              <span class="logo-version">版本 1.0.0</span>
            </div>
          </div>
          <p class="about-desc">基于 RAG 技术的智能客服系统，支持文档上传、智能问答和多轮对话。</p>
          <div class="about-features">
            <div class="feature-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              <span>向量语义检索</span>
            </div>
            <div class="feature-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              <span>多轮上下文对话</span>
            </div>
            <div class="feature-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              <span>文档知识库管理</span>
            </div>
            <div class="feature-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              <span>实时流式输出</span>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api/chat'

const backendOnline = ref(false)

const ragConfig = reactive({ topK: 5, minSimilarity: 0.3 })

const presets = {
  precise: { topK: 3, minSimilarity: 0.6 },
  balanced: { topK: 5, minSimilarity: 0.3 },
  broad: { topK: 10, minSimilarity: 0.15 }
}

const activePreset = computed(() => {
  if (ragConfig.topK === 3 && ragConfig.minSimilarity === 0.6) return 'precise'
  if (ragConfig.topK === 5 && ragConfig.minSimilarity === 0.3) return 'balanced'
  if (ragConfig.topK === 10 && ragConfig.minSimilarity === 0.15) return 'broad'
  return null
})

async function loadConfig() {
  try {
    const res = await chatApi.getConfig()
    ragConfig.topK = res.topK
    ragConfig.minSimilarity = res.minSimilarity
  } catch (e) { console.warn('加载配置失败:', e) }
  checkBackend()
}

async function saveConfig() {
  try {
    await chatApi.updateConfig(ragConfig.topK, ragConfig.minSimilarity)
    ElMessage.success({ message: '配置已保存', duration: 1500 })
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  }
}

function applyPreset(name) {
  ragConfig.topK = presets[name].topK
  ragConfig.minSimilarity = presets[name].minSimilarity
  saveConfig()
}

async function checkBackend() {
  try {
    await fetch('http://localhost:8080/api/health')
    backendOnline.value = true
  } catch { backendOnline.value = false }
}

onMounted(() => { loadConfig() })
</script>

<style scoped lang="scss">
.settings-page {
  overflow-x: hidden;
}

/* 滑入动画 */
.slide-up-enter-active {
  transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.slide-up-leave-active {
  transition: all 0.35s cubic-bezier(0.55, 0.055, 0.675, 0.19);
}
.slide-up-enter-from {
  opacity: 0;
  transform: translateY(40px);
}
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.settings-header-section {
  position: relative;
  margin-bottom: 28px;

  .header-bg {
    position: absolute;
    top: -28px;
    left: -32px;
    right: -32px;
    height: 120px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    z-index: 0;
  }

  .header-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 24px 24px 0 24px;
  }
}

.header-icon {
  width: 52px;
  height: 52px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  backdrop-filter: blur(10px);
}

.header-text {
  .page-title { font-size: 22px; font-weight: 700; color: white; margin: 0 0 4px 0; }
  .page-desc { font-size: 13px; color: rgba(255, 255, 255, 0.7); margin: 0; }
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s;
  &:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f4f4f5;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.08) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
  &.warning { background: linear-gradient(135deg, rgba(230, 162, 60, 0.12) 0%, rgba(230, 162, 60, 0.08) 100%); color: #e6a23c; }
}

.card-title-wrap {
  flex: 1;
  .card-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0; }
  .card-subtitle { font-size: 12px; color: #c0c4cc; margin: 2px 0 0 0; }
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.param-item {
  .param-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 14px;
  }
  .param-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .param-label { font-size: 14px; font-weight: 500; color: #303133; }
  .param-desc { font-size: 12px; color: #909399; }
  .param-value-badge {
    min-width: 48px;
    padding: 4px 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    font-size: 14px;
    font-weight: 600;
    border-radius: 8px;
    text-align: center;
  }
  .param-range {
    display: flex;
    justify-content: space-between;
    margin-top: 6px;
    font-size: 11px;
    color: #c0c4cc;
  }
}

.preset-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f4f4f5;
  .preset-label { font-size: 13px; color: #909399; }
  .preset-btns { display: flex; gap: 8px; }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  .info-label { font-size: 12px; color: #909399; }
  .info-value {
    font-size: 13px;
    font-weight: 500;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 6px;
    &.status-online { color: #67c23a; .status-dot { width: 7px; height: 7px; background: #67c23a; border-radius: 50%; animation: pulse 2s infinite; } }
    &.status-offline { color: #f56c6c; .status-dot { width: 7px; height: 7px; background: #f56c6c; border-radius: 50%; } }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.about-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  border: 1px solid rgba(102, 126, 234, 0.15);
}

.about-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  .logo-icon {
    width: 44px;
    height: 44px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 700;
    color: white;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }
  .logo-info { display: flex; flex-direction: column; gap: 3px; }
  .logo-name { font-size: 16px; font-weight: 700; color: #303133; }
  .logo-version { font-size: 12px; color: #909399; }
}

.about-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.about-features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  svg { color: #67c23a; flex-shrink: 0; }
}
</style>