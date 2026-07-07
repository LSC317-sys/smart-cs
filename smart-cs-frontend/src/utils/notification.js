/**
 * Notification system for SmartCS
 */

// In-memory notification store (with localStorage persistence)
function loadNotifState() {
  try {
    const data = localStorage.getItem('smartcs_notifications')
    const count = localStorage.getItem('smartcs_unread_count')
    return {
      notifications: data ? JSON.parse(data) : [],
      unreadCount: count ? parseInt(count) : 0
    }
  } catch { return { notifications: [], unreadCount: 0 } }
}
const _init = loadNotifState()
const state = {
  notifications: _init.notifications,
  unreadCount: _init.unreadCount,
  listeners: [],
  permission: 'default'
}

// Request browser notification permission
export async function requestPermission() {
  if (!('Notification' in window)) {
    state.permission = 'unsupported'
    return state.permission
  }
  const result = await Notification.requestPermission()
  state.permission = result
  return result
}

// Add a notification
export function addNotification({ title, message, type = 'info', icon = '' }) {
  const notification = {
    id: Date.now() + Math.random().toString(36).slice(2, 8),
    title,
    message,
    type, // info | success | warning | error
    icon,
    read: false,
    time: new Date().toISOString()
  }

  state.notifications.unshift(notification)
  state.unreadCount++

  // Persist to localStorage
  try {
    localStorage.setItem('smartcs_notifications', JSON.stringify(state.notifications))
    localStorage.setItem('smartcs_unread_count', String(state.unreadCount))
  } catch {}

  // Notify listeners
  state.listeners.forEach(fn => fn(state.notifications, state.unreadCount))

  // Browser notification when page is hidden
  if (document.hidden && state.permission === 'granted') {
    showBrowserNotification(notification)
  }

  return notification
}

// Mark as read
export function markRead(id) {
  const n = state.notifications.find(n => n.id === id)
  if (n && !n.read) {
    n.read = true
    state.unreadCount = Math.max(0, state.unreadCount - 1)
    try {
      localStorage.setItem('smartcs_notifications', JSON.stringify(state.notifications))
      localStorage.setItem('smartcs_unread_count', String(state.unreadCount))
    } catch {}
    state.listeners.forEach(fn => fn(state.notifications, state.unreadCount))
  }
}

// Mark all as read
export function markAllRead() {
  state.notifications.forEach(n => n.read = true)
  state.unreadCount = 0
  try {
    localStorage.setItem('smartcs_notifications', JSON.stringify(state.notifications))
    localStorage.setItem('smartcs_unread_count', '0')
  } catch {}
  state.listeners.forEach(fn => fn(state.notifications, state.unreadCount))
}

// Clear all
export function clearAll() {
  state.notifications = []
  state.unreadCount = 0
  try {
    localStorage.removeItem('smartcs_notifications')
    localStorage.removeItem('smartcs_unread_count')
  } catch {}
  state.listeners.forEach(fn => fn(state.notifications, state.unreadCount))
}

// Subscribe to changes
export function onNotify(fn) {
  state.listeners.push(fn)
  // Return initial state
  fn(state.notifications, state.unreadCount)
  return () => {
    state.listeners = state.listeners.filter(l => l !== fn)
  }
}

// Browser notification
function showBrowserNotification(notification) {
  try {
    const n = new Notification(notification.title, {
      body: notification.message,
      icon: '/favicon.ico',
      tag: notification.id
    })
    n.onclick = () => {
      window.focus()
      n.close()
    }
    // Auto close after 5s
    setTimeout(() => n.close(), 5000)
  } catch (e) {
    console.warn('Browser notification failed:', e)
  }
}

// Convenience methods
export function notifyDocUpload(title) {
  return addNotification({
    title: '文档上传完成',
    message: `"${title}" 已成功上传并建立索引`,
    type: 'success',
    icon: 'Document'
  })
}

export function notifyAnswerComplete(question) {
  return addNotification({
    title: '回答已生成',
    message: question.length > 30 ? question.slice(0, 30) + '...' : question,
    type: 'info',
    icon: 'ChatDotRound'
  })
}

export function notifyLowRating(question, rating) {
  return addNotification({
    title: '低评分提醒',
    message: `用户对回答评分 ${rating} 星："${question.slice(0, 30)}..."`,
    type: 'warning',
    icon: 'WarningFilled'
  })
}

export function notifyVersionUpdate(title, version) {
  return addNotification({
    title: '文档版本更新',
    message: `"${title}" 已更新至版本 ${version}`,
    type: 'info',
    icon: 'RefreshRight'
  })
}

export function notifyRollback(title, version) {
  return addNotification({
    title: '文档版本回滚',
    message: `"${title}" 已回滚至版本 ${version}`,
    type: 'warning',
    icon: 'RefreshLeft'
  })
}

export function getPermission() {
  return state.permission
}
