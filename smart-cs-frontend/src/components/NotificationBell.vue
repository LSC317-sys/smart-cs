<template>
  <div class="notification-bell">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="bell-badge">
      <el-button :icon="Bell" circle @click="drawerVisible = true" />
    </el-badge>

    <el-drawer
      v-model="drawerVisible"
      title="通知中心"
      direction="rtl"
      size="360px"
      :show-close="true"
    >
      <template #header>
        <div class="drawer-header">
          <span>通知中心</span>
          <div class="header-actions">
            <el-button text size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">
              全部已读
            </el-button>
            <el-button text size="small" type="danger" @click="handleClearAll" :disabled="notifications.length === 0">
              清空
            </el-button>
          </div>
        </div>
      </template>

      <div class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.read, ['type-' + item.type]: true }"
          @click="handleClick(item)"
        >
          <div class="item-icon">
            <el-icon :size="18">
              <component :is="getIcon(item)" />
            </el-icon>
          </div>
          <div class="item-body">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-message">{{ item.message }}</div>
            <div class="item-time">{{ formatTime(item.time) }}</div>
          </div>
          <div class="item-dot" v-if="!item.read"></div>
        </div>

        <el-empty v-if="notifications.length === 0" description="暂无通知" :image-size="80" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell, SuccessFilled, WarningFilled, InfoFilled, CircleCloseFilled, RefreshRight, RefreshLeft, Document, ChatDotRound } from '@element-plus/icons-vue'
import { onNotify, markRead, markAllRead as markAll, clearAll as clear, requestPermission } from '@/utils/notification'

const drawerVisible = ref(false)
const notifications = ref([])
const unreadCount = ref(0)

let unsubscribe = null

onMounted(() => {
  // Request browser notification permission on first mount
  requestPermission()
  // Subscribe to notification changes
  unsubscribe = onNotify((items, count) => {
    notifications.value = items
    unreadCount.value = count
  })
})

onUnmounted(() => {
  if (unsubscribe) unsubscribe()
})

function getIcon(item) {
  const iconMap = {
    Document, ChatDotRound, WarningFilled, InfoFilled,
    RefreshRight, RefreshLeft, SuccessFilled, CircleCloseFilled
  }
  return iconMap[item.icon] || InfoFilled
}

function formatTime(iso) {
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now - d
  const diffMin = Math.floor(diffMs / 60000)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`

  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`

  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay} 天前`

  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function handleClick(item) {
  if (!item.read) {
    markRead(item.id)
  }
}

function handleMarkAllRead() {
  markAll()
}

function handleClearAll() {
  clear()
}
</script>

<style scoped lang="scss">
.notification-bell {
  display: inline-flex;
  align-items: center;
}

.bell-badge {
  :deep(.el-badge__content) {
    font-size: 11px;
    top: 6px;
    right: 6px;
  }
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  font-size: 16px;
  font-weight: 600;

  .header-actions {
    display: flex;
    gap: 4px;
  }
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  &.unread {
    background: rgba(64, 158, 255, 0.06);
    border-left: 3px solid var(--notify-color, #409eff);
  }

  &.type-success { --notify-color: #67c23a; }
  &.type-warning { --notify-color: #e6a23c; }
  &.type-error { --notify-color: #f56c6c; }
  &.type-info { --notify-color: #409eff; }

  .item-icon {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    background: var(--notify-color, #409eff);
    color: #fff;
    opacity: 0.85;
  }

  .item-body {
    flex: 1;
    min-width: 0;

    .item-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--el-text-color-primary, #c5ccd8);
      margin-bottom: 2px;
    }

    .item-message {
      font-size: 12px;
      color: var(--el-text-color-secondary, #8b9bb4);
      line-height: 1.4;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .item-time {
      font-size: 11px;
      color: var(--el-text-color-placeholder, #6c7a8d);
      margin-top: 4px;
    }
  }

  .item-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--notify-color, #409eff);
    position: absolute;
    top: 14px;
    right: 10px;
    animation: pulse 2s infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}
</style>
