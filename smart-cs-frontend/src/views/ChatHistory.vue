<template>
  <div class="chat-history" :class="{ expanded }">
    <!-- 展开/收起按钮 -->
    <button class="expand-btn" @click="toggle" :title="expanded ? '收起历史' : '搜索历史'">
      <el-icon><Search /></el-icon>
    </button>
    
    <!-- 历史面板 -->
    <div v-show="expanded" class="history-panel">
      <div class="panel-header">
        <span class="panel-title">搜索历史</span>
        <button class="close-btn" @click="expanded = false">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索历史记录..."
          clearable
          size="small"
          @input="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      
      <!-- 筛选 -->
      <div class="filters">
        <el-select v-model="filterRating" size="small" placeholder="评分筛选" clearable @change="handleSearch">
          <el-option label="全部" :value="''" />
          <el-option label="👍 有帮助" :value="1" />
          <el-option label="👎 需改进" :value="-1" />
          <el-option label="未评分" :value="0" />
        </el-select>
      </div>
      
      <!-- 历史列表 -->
      <div class="history-list" v-loading="loading">
        <div v-if="historyList.length === 0 && !loading" class="empty-tip">
          暂无历史记录
        </div>
        <div
          v-for="item in historyList"
          :key="item.id"
          class="history-item"
          @click="selectItem(item)"
        >
          <div class="item-question">{{ item.question }}</div>
          <div class="item-meta">
            <span class="item-time">{{ formatTime(item.createdAt) }}</span>
            <span class="item-rating" v-if="item.rating === 1">👍</span>
            <span class="item-rating" v-else-if="item.rating === -1">👎</span>
          </div>
          <button class="item-delete" @click.stop="deleteItem(item.id)" title="删除">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Close, Delete } from '@element-plus/icons-vue'
import { chatApi } from '@/api/chat'

const emit = defineEmits(['select'])

const expanded = ref(false)
const loading = ref(false)
const searchKeyword = ref('')
const filterRating = ref('')
const historyList = ref([])

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value && historyList.value.length === 0) {
    loadHistory()
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const res = await chatApi.historyList({ userId: 1, limit: 50 })
    historyList.value = res.data?.records || []
  } catch (e) {
    console.error('加载历史失败:', e)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  if (!searchKeyword.value && !filterRating.value) {
    await loadHistory()
    return
  }
  loading.value = true
  try {
    const res = await chatApi.historySearch({
      keyword: searchKeyword.value,
      rating: filterRating.value
    })
    historyList.value = res.data || []
  } catch (e) {
    console.error('搜索失败:', e)
  } finally {
    loading.value = false
  }
}

function selectItem(item) {
  emit('select', item.question)
}

async function deleteItem(id) {
  try {
    await ElMessageBox.confirm('确定删除这条记录？', '提示', { type: 'warning' })
    await chatApi.historyDelete(id)
    historyList.value = historyList.value.filter(h => h.id !== id)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
</script>

<style scoped>
.chat-history {
  position: relative;
  display: flex;
  align-items: flex-start;
}

.expand-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  color: #606266;  /* Changed from #fff to be visible on white background */
  font-size: 18px;
  transition: background 0.2s;
}
.expand-btn:hover { background: rgba(255,255,255,0.1); }

.history-panel {
  position: absolute;
  left: -280px;  /* Changed from 50px to move panel left of button */
  top: 40px;  /* Changed from 0 to position panel below button */
  width: 320px;
  max-height: 70vh;  /* Changed from 500px to viewport-relative */
  background: #1a1f35;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.panel-title { color: #fff; font-weight: 600; font-size: 14px; }
.close-btn { background: none; border: none; cursor: pointer; color: #8b9cc7; padding: 4px; border-radius: 4px; }
.close-btn:hover { background: rgba(255,255,255,0.1); }

.search-box { padding: 10px 12px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.search-box :deep(.el-input__wrapper) { background: rgba(255,255,255,0.08); box-shadow: none; border: 1px solid rgba(255,255,255,0.1); }
.search-box :deep(.el-input__inner) { color: #fff; }
.search-box :deep(.el-input__inner::placeholder) { color: #5a6a8a; }

.filters { padding: 8px 12px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.filters :deep(.el-select .el-input__wrapper) { background: rgba(255,255,255,0.08); box-shadow: none; border: 1px solid rgba(255,255,255,0.1); }
.filters :deep(.el-select__placeholder) { color: #8b9cc7; }

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  max-height: 360px;
}
.history-list::-webkit-scrollbar { width: 4px; }
.history-list::-webkit-scrollbar-track { background: transparent; }
.history-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 2px; }

.empty-tip { text-align: center; color: #5a6a8a; padding: 24px 0; font-size: 13px; }

.history-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  position: relative;
  transition: background 0.15s;
  border: 1px solid transparent;
}
.history-item:hover { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.1); }

.item-question {
  color: #c8d0e7;
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  padding-right: 24px;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.item-time { color: #5a6a8a; font-size: 11px; }
.item-rating { font-size: 12px; }

.item-delete {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #5a6a8a;
  padding: 4px;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.15s;
}
.history-item:hover .item-delete { opacity: 1; }
.item-delete:hover { background: rgba(239,65,54,0.2); color: #ef4136; }
</style>