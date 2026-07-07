<template>
  <div class="admin-page">
    <!-- 动态背景 -->
    <div class="bg-particles">
      <div class="particle" v-for="i in 20" :key="i" :style="getParticleStyle(i)"></div>
    </div>
    
    <!-- 标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ TITLE }}</h1>
        <span class="page-subtitle">{{ SUBTITLE }}</span>
      </div>
      <div class="header-right">
        <span class="status-badge" :class="systemStatus">
          <span class="status-dot"></span>
          {{ systemStatusText }}
        </span>
        <span class="refresh-time">数据更新于 {{ refreshTime }}</span>
        <el-dropdown trigger="click" @command="handleExport">
          <el-button type="success" plain>
            导出数据<el-icon class="el-icon--right"><Download /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="chat-markdown">会话历史 (Markdown)</el-dropdown-item>
              <el-dropdown-item command="chat-csv">会话历史 (CSV)</el-dropdown-item>
              <el-dropdown-item command="docs-csv" divided>文档列表 (CSV)</el-dropdown-item>
              <el-dropdown-item command="stats-csv">统计报表 (CSV)</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" :icon="Refresh" circle @click="refreshAll" :loading="refreshing" />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div 
        class="stat-card" 
        v-for="(stat, i) in statCards" 
        :key="i" 
        :style="{ '--accent': stat.color, animationDelay: `${i * 0.1}s` }"
      >
        <div class="stat-icon-wrap">
          <el-icon class="stat-icon"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stat.formattedValue }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-trend" v-if="stat.trend">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ stat.trend }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表行 -->
    <div class="charts-row">
      <el-card class="chart-card chart-large">
        <template #header>
          <div class="chart-header">
            <span>{{ CHART_TITLES.DOCS }}</span>
            <el-radio-group v-model="chartPeriod" size="small">
              <el-radio-button value="7d">{{ PERIOD_LABELS['7d'] }}</el-radio-button>
              <el-radio-button value="30d">{{ PERIOD_LABELS['30d'] }}</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="docChartRef" class="chart-container"></div>
      </el-card>

      <el-card class="chart-card chart-small">
        <template #header>
          <span>{{ CHART_TITLES.SESSIONS }}</span>
        </template>
        <div ref="sessionChartRef" class="chart-container chart-container-sm"></div>
      </el-card>
    </div>

    <div class="charts-row">
      <el-card class="chart-card chart-small">
        <template #header>
          <span>{{ CHART_TITLES.CONTENT }}</span>
        </template>
        <div ref="contentChartRef" class="chart-container chart-container-sm"></div>
      </el-card>

      <el-card class="chart-card chart-large">
        <template #header>
          <span>{{ CHART_TITLES.MODEL }}</span>
        </template>
        <div class="model-config">
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="MODEL_CONFIG_LABELS.PROVIDER">
              <el-tag type="primary">{{ PROVIDER_NAME }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="MODEL_CONFIG_LABELS.EMBEDDING">
              <el-tag type="success">{{ modelConfig.embeddingModel || '—' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="MODEL_CONFIG_LABELS.CHAT">
              <el-tag type="warning">{{ modelConfig.chatModel || '—' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="MODEL_CONFIG_LABELS.VECTOR_DIM">{{ VECTOR_DIM }}</el-descriptions-item>
            <el-descriptions-item :label="MODEL_CONFIG_LABELS.CHUNK_STRATEGY">{{ CHUNK_STRATEGY }}</el-descriptions-item>
          </el-descriptions>

          <div class="model-links">
            <el-link type="primary" :href="PROVIDER_URL" target="_blank">
              {{ PROVIDER_NAME }} 控制台
              <el-icon><Link /></el-icon>
            </el-link>
            <el-link type="primary" :href="BALANCE_URL" target="_blank" style="margin-left: 16px;">
              API 余额查询
              <el-icon><Link /></el-icon>
            </el-link>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 会话列表 -->
    <el-card class="session-card">
      <template #header>
        <div class="chart-header">
          <span>活跃会话 ({{ sessions.length }})</span>
          <el-button type="danger" size="small" plain @click="clearAllSessions" v-if="sessions.length > 0">
            清空全部会话
          </el-button>
        </div>
      </template>

      <el-table :data="sessions" stripe v-loading="loading">
        <el-table-column prop="sessionId" label="会话 ID" width="200" />
        <el-table-column prop="messageCount" label="消息数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.messageCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastMessage" label="最后消息" min-width="300">
          <template #default="{ row }">
            <span class="last-msg">{{ row.lastMessage || '暂无消息' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="clearSession(row.sessionId)">
              清除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && sessions.length === 0" description="暂无活跃会话" />
    </el-card>

    <!-- 用户反馈统计 -->
    <el-card class="session-card" style="margin-top: 16px;">
      <template #header>
        <div class="chart-header">
          <span>用户反馈 ({{ feedbackStats.total }} 条评价)</span>
          <div style="display:flex;gap:12px;align-items:center;">
            <el-tag type="success">👍 好评 {{ feedbackStats.good }}</el-tag>
            <el-tag type="danger">👎 差评 {{ feedbackStats.bad }}</el-tag>
            <el-tag type="warning">满意度 {{ feedbackStats.satisfactionRate }}%</el-tag>
          </div>
        </div>
      </template>

      <el-table :data="feedbackList" stripe v-loading="feedbackLoading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="question" label="问题" min-width="200">
          <template #default="{ row }">
            <span class="last-msg">{{ row.question || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="answer" label="回答摘要" min-width="200">
          <template #default="{ row }">
            <span class="last-msg">{{ row.answer || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.rating === 'good' ? 'success' : row.rating === 'bad' ? 'danger' : 'info'" size="small">
              {{ row.rating === 'good' ? '👍 好评' : row.rating === 'bad' ? '👎 差评' : row.rating }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            <span style="font-size:12px;color:#8b9bb4;">{{ row.createdAt || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!feedbackLoading && feedbackList.length === 0" description="暂无用户反馈" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Refresh, Download, TrendCharts, Link, Document, ChatDotRound, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api/admin'
import * as echarts from 'echarts'

// ==================== Constants ====================
const TITLE = 'SmartCS 管理后台'
const SUBTITLE = '智能客服系统运维监控'
const PROVIDER_NAME = '硅基流动'
const PROVIDER_URL = 'https://cloud.siliconflow.cn'
const BALANCE_URL = 'https://cloud.siliconflow.cn/account/balance'
const VECTOR_DIM = 1024
const CHUNK_STRATEGY = 'BGE-M3 / 固定512字符'

const CHART_TITLES = {
  DOCS: '文档增长趋势',
  SESSIONS: '会话分布',
  CONTENT: '内容类型分布',
  MODEL: '模型配置'
}

const PERIOD_LABELS = {
  '7d': '近7天',
  '30d': '近30天'
}

const MODEL_CONFIG_LABELS = {
  PROVIDER: 'AI 服务商',
  EMBEDDING: 'Embedding 模型',
  CHAT: 'Chat 模型',
  VECTOR_DIM: '向量维度',
  CHUNK_STRATEGY: '分块策略'
}

// ==================== Reactive Data ====================
const loading = ref(false)
const refreshing = ref(false)
const refreshTime = ref(new Date().toLocaleString('zh-CN'))
const systemStatus = ref('normal')
const systemStatusText = ref('系统正常')
const chartPeriod = ref('7d')

const overview = reactive({
  totalDocs: 0,
  totalChunks: 0,
  activeSessions: 0
})

const sessions = ref([])
const modelConfig = reactive({
  embeddingModel: '',
  chatModel: ''
})

const feedbackStats = reactive({
  total: 0,
  rated: 0,
  good: 0,
  bad: 0,
  satisfactionRate: '0.0'
})
const feedbackList = ref([])
const feedbackLoading = ref(false)

// ==================== Computed ====================
const statCards = computed(() => [
  {
    label: '文档总数',
    formattedValue: overview.totalDocs,
    icon: Document,
    color: '#409EFF',
    trend: null
  },
  {
    label: '向量切片',
    formattedValue: overview.totalChunks,
    icon: TrendCharts,
    color: '#67C23A',
    trend: null
  },
  {
    label: '活跃会话',
    formattedValue: overview.activeSessions,
    icon: ChatDotRound,
    color: '#E6A23C',
    trend: null
  },
  {
    label: '用户好评率',
    formattedValue: feedbackStats.satisfactionRate + '%',
    icon: Star,
    color: '#F56C6C',
    trend: feedbackStats.good > 0 ? `好评${feedbackStats.good} / 差评${feedbackStats.bad}` : null
  }
])

// ==================== Chart refs ====================
const docChartRef = ref(null)
const sessionChartRef = ref(null)
const contentChartRef = ref(null)
let docChart = null
let sessionChart = null
let contentChart = null

// ==================== Functions ====================
function getParticleStyle(i) {
  const size = Math.random() * 4 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 20
  const duration = Math.random() * 10 + 10
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

function refreshAll() {
  refreshing.value = true
  Promise.all([loadOverview(), loadSessions(), loadFeedback()])
    .then(() => {
      refreshTime.value = new Date().toLocaleString('zh-CN')
      systemStatus.value = 'normal'
      systemStatusText.value = '系统正常'
    })
    .catch(() => {
      systemStatus.value = 'error'
      systemStatusText.value = '部分服务异常'
    })
    .finally(() => { refreshing.value = false })
}

async function loadOverview() {
  try {
    const res = await adminApi.getOverview()
    overview.totalDocs = res?.totalDocs ?? 0
    overview.totalChunks = res?.totalChunks ?? 0
    overview.activeSessions = res?.activeSessions ?? 0
  } catch (e) { console.error('加载概览失败:', e) }
}

async function loadSessions() {
  try {
    const res = await adminApi.getConversations()
    sessions.value = res || []
  } catch (e) { console.error('加载会话失败:', e) }
}

async function loadFeedback() {
  feedbackLoading.value = true
  try {
    const [statsRes, listRes] = await Promise.all([
      adminApi.getFeedbackStats(),
      adminApi.getFeedbackList()
    ])
    const sd = statsRes || {}
    feedbackStats.total = sd.total ?? 0
    feedbackStats.rated = sd.rated ?? 0
    feedbackStats.good = sd.good ?? 0
    feedbackStats.bad = sd.bad ?? 0
    feedbackStats.satisfactionRate = sd.satisfactionRate ?? '0.0'
    feedbackList.value = listRes || []
  } catch (e) { console.error('加载反馈失败:', e) }
  finally { feedbackLoading.value = false }
}

async function clearSession(sessionId) {
  try {
    await adminApi.clearSession(sessionId)
    ElMessage.success('会话已清除')
    loadSessions()
  } catch (e) { ElMessage.error('清除失败') }
}

function clearAllSessions() {
  if (sessions.value.length === 0) return
  sessions.value.forEach(s => clearSession(s.sessionId))
}

function handleExport(command) {
  let content = ''
  let filename = ''
  let type = 'text/plain'

  if (command === 'chat-markdown') {
    content = sessions.value.map(s => `## ${s.sessionId}\n消息数: ${s.messageCount}\n最后消息: ${s.lastMessage}`).join('\n\n')
    filename = 'chat-history.md'
    type = 'text/markdown'
  } else if (command === 'chat-csv') {
    content = 'sessionId,messageCount,lastMessage\n' + sessions.value.map(s => `${s.sessionId},${s.messageCount},${s.lastMessage}`).join('\n')
    filename = 'chat-history.csv'
  } else if (command === 'docs-csv') {
    content = 'totalDocs,totalChunks\n' + `${overview.totalDocs},${overview.totalChunks}`
    filename = 'docs.csv'
  } else if (command === 'stats-csv') {
    content = 'metric,value\n文档总数,' + overview.totalDocs + '\n切片总数,' + overview.totalChunks + '\n活跃会话,' + overview.activeSessions
    filename = 'stats.csv'
  }

  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// ==================== Charts ====================
function initCharts() {
  if (docChartRef.value) {
    docChart = echarts.init(docChartRef.value)
    docChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value' },
      series: [{ data: [0, 0, 0, overview.totalDocs, 0, 0, 0], type: 'line', smooth: true, areaStyle: {} }]
    })
  }
  if (sessionChartRef.value) {
    sessionChart = echarts.init(sessionChartRef.value)
    sessionChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data: [{ value: overview.activeSessions, name: '活跃会话' }] }]
    })
  }
  if (contentChartRef.value) {
    contentChart = echarts.init(contentChartRef.value)
    contentChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data: [{ value: overview.totalChunks, name: '向量切片' }] }]
    })
  }
}

// ==================== Lifecycle ====================
onMounted(async () => {
  await refreshAll()
  initCharts()
})

onUnmounted(() => {
  docChart?.dispose()
  sessionChart?.dispose()
  contentChart?.dispose()
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #0c1445 0%, #1a237e 50%, #283593 100%);
  color: #e0e6ed;
}

.bg-particles {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  bottom: -10px;
  background: rgba(100, 181, 246, 0.6);
  border-radius: 50%;
  animation: float-up linear infinite;
}

@keyframes float-up {
  0% { transform: translateY(0) scale(1); opacity: 0.6; }
  100% { transform: translateY(-100vh) scale(0); opacity: 0; }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.header-left .page-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.header-left .page-subtitle {
  font-size: 14px;
  color: #90caf9;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-badge {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-badge.normal { background: rgba(76,175,80,0.2); color: #66bb6a; }
.status-badge.error { background: rgba(244,67,54,0.2); color: #ef5350; }
.status-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.refresh-time { font-size: 12px; color: #8b9bb4; }

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.stat-card {
  background: rgba(255,255,255,0.08);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  animation: card-in 0.6s ease-out both;
  border: 1px solid rgba(255,255,255,0.1);
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-icon-wrap {
  width: 48px; height: 48px;
  border-radius: 12px;
  background: var(--accent, #409EFF);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-value { font-size: 28px; font-weight: 700; color: #fff; }
.stat-label { font-size: 13px; color: #8b9bb4; }
.stat-trend { font-size: 12px; color: #90caf9; display: flex; align-items: center; gap: 4px; }

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.chart-card {
  background: rgba(255,255,255,0.08) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  color: #e0e6ed;
}

.chart-card .el-card__header {
  border-bottom-color: rgba(255,255,255,0.1);
  color: #e0e6ed;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container { height: 300px; }
.chart-container-sm { height: 260px; }

.model-config { color: #e0e6ed; }
.model-links { margin-top: 16px; }

.session-card {
  background: rgba(255,255,255,0.08) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  color: #e0e6ed;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.session-card .el-card__header {
  border-bottom-color: rgba(255,255,255,0.1);
  color: #e0e6ed;
}

.last-msg {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
</style>
