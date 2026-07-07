<template>
  <div class="profile-page">
    <Transition name="slide-up" appear>
      <div class="profile-header-section">
        <div class="header-bg"></div>
        <div class="header-content">
          <div class="avatar-large">{{ userInitial }}</div>
          <div class="user-info-text">
            <h1 class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h1>
            <div class="user-meta">
              <span class="user-role" :class="userStore.userInfo?.role === 'ADMIN' ? 'admin' : 'user'">
                {{ userStore.userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </span>
              <span class="meta-divider">·</span>
              <span class="user-id">ID: {{ userStore.userInfo?.id || '-' }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <div class="profile-content">
      <Transition name="slide-up" appear>
        <div class="profile-card">
          <div class="card-header">
            <div class="card-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <div class="card-title-wrap">
              <h2 class="card-title">基本信息</h2>
              <p class="card-subtitle">您的账户核心信息</p>
            </div>
            <el-button v-if="!editing" type="primary" plain size="small" @click="startEdit">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              编辑资料
            </el-button>
          </div>

          <div class="info-list">
            <div class="info-row">
              <span class="info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                用户名
              </span>
              <span class="info-value readonly">{{ userStore.userInfo?.username }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 20h9"/>
                  <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                </svg>
                昵称
              </span>
              <span class="info-value" v-if="!editing">{{ userStore.userInfo?.nickname || '未设置' }}</span>
              <el-input v-else v-model="editForm.nickname" placeholder="输入昵称" style="width:200px" />
            </div>
            <div class="info-row">
              <span class="info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                邮箱
              </span>
              <span class="info-value" v-if="!editing">{{ userStore.userInfo?.email || '未设置' }}</span>
              <el-input v-else v-model="editForm.email" placeholder="输入邮箱" style="width:200px" />
            </div>
          </div>

          <Transition name="slide-down">
            <div class="card-actions" v-if="editing">
              <el-button @click="cancelEdit">取消</el-button>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
            </div>
          </Transition>
        </div>
      </Transition>

      <Transition name="slide-up" appear>
        <div class="profile-card">
          <div class="card-header">
            <div class="card-icon warning">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <div class="card-title-wrap">
              <h2 class="card-title">修改密码</h2>
              <p class="card-subtitle">定期更换密码保护账户安全</p>
            </div>
          </div>

          <Transition name="slide-down" mode="out-in">
            <div class="password-form expanded" v-if="showPasswordForm" key="form">
              <div class="form-group">
                <label class="form-label">当前密码</label>
                <el-input v-model="passwordForm.currentPassword" type="password" placeholder="输入当前密码" show-password />
              </div>
              <div class="form-group">
                <label class="form-label">新密码</label>
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码" show-password />
                <div class="password-strength" v-if="passwordForm.newPassword">
                  <div class="strength-bar"><div class="strength-fill" :style="{ width: pwdStrength + '%', background: strengthColor }"></div></div>
                  <span class="strength-text" :style="{ color: strengthColor }">{{ strengthText }}</span>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">确认新密码</label>
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
                <span class="form-error" v-if="passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword">两次密码不一致</span>
              </div>
              <div class="card-actions">
                <el-button @click="cancelPassword">取消</el-button>
                <el-button type="primary" :loading="changingPwd" :disabled="!canChangePassword" @click="changePassword">确认修改</el-button>
              </div>
            </div>
            <div class="password-form" v-else key="btn">
              <el-button plain @click="showPasswordForm = true">修改密码</el-button>
            </div>
          </Transition>
        </div>
      </Transition>

      <Transition name="slide-up" appear>
        <div class="profile-card danger-zone">
          <div class="card-header">
            <div class="card-icon danger">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </div>
            <div class="card-title-wrap">
              <h2 class="card-title">危险操作</h2>
              <p class="card-subtitle">此操作不可逆</p>
            </div>
          </div>
          <div class="danger-actions">
            <div class="danger-item">
              <div class="danger-info">
                <span class="danger-title">退出登录</span>
                <span class="danger-desc">退出后需要重新输入用户名和密码</span>
              </div>
              <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  return name ? name[0].toUpperCase() : '?'
})

const editing = ref(false)
const saving = ref(false)
const showPasswordForm = ref(false)
const changingPwd = ref(false)

const editForm = reactive({ nickname: '', email: '' })

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdStrength = computed(() => {
  const pwd = passwordForm.newPassword
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 8) score += 25
  if (pwd.length >= 12) score += 15
  if (/[a-z]/.test(pwd)) score += 15
  if (/[A-Z]/.test(pwd)) score += 15
  if (/[0-9]/.test(pwd)) score += 15
  if (/[^a-zA-Z0-9]/.test(pwd)) score += 15
  return Math.min(score, 100)
})

const strengthColor = computed(() => {
  if (pwdStrength.value < 30) return '#f56c6c'
  if (pwdStrength.value < 60) return '#e6a23c'
  return '#67c23a'
})

const strengthText = computed(() => {
  if (pwdStrength.value < 30) return '弱'
  if (pwdStrength.value < 60) return '中'
  return '强'
})

const canChangePassword = computed(() => {
  return passwordForm.currentPassword &&
    passwordForm.newPassword &&
    passwordForm.newPassword === passwordForm.confirmPassword &&
    pwdStrength.value >= 30
})

function startEdit() {
  editForm.nickname = userStore.userInfo?.nickname || ''
  editForm.email = userStore.userInfo?.email || ''
  editing.value = true
}

function cancelEdit() { editing.value = false }

async function saveProfile() {
  saving.value = true
  try {
    await userStore.updateProfile(editForm)
    editing.value = false
    ElMessage.success('保存成功')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function cancelPassword() {
  showPasswordForm.value = false
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function changePassword() {
  changingPwd.value = true
  try {
    await userStore.changePassword(passwordForm.currentPassword, passwordForm.newPassword)
    showPasswordForm.value = false
    cancelPassword()
    ElMessage.success('密码修改成功')
  } catch (err) {
    ElMessage.error(err.message || '修改失败')
  } finally {
    changingPwd.value = false
  }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.profile-page {
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

/* 滑下动画 */
.slide-down-enter-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.slide-down-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0.055, 0.675, 0.19);
}
.slide-down-enter-from {
  opacity: 0;
  transform: translateY(-16px);
}
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.profile-header-section {
  position: relative;
  margin-bottom: 28px;

  .header-bg {
    position: absolute;
    top: -28px;
    left: -32px;
    right: -32px;
    height: 140px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    z-index: 0;
  }

  .header-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 28px 28px 0 28px;
  }
}

.avatar-large {
  width: 72px;
  height: 72px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #667eea;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.user-info-text {
  padding-bottom: 20px;

  .user-name {
    font-size: 22px;
    font-weight: 700;
    color: white;
    margin: 0 0 6px 0;
  }

  .user-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    color: rgba(255, 255, 255, 0.8);
    font-size: 13px;

    .user-role {
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      &.admin { background: rgba(230, 162, 60, 0.3); color: #ffd180; }
      &.user { background: rgba(255, 255, 255, 0.2); }
    }

    .meta-divider { opacity: 0.5; }
    .user-id { font-family: monospace; opacity: 0.7; }
  }
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
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
  &.danger { background: linear-gradient(135deg, rgba(245, 108, 108, 0.12) 0%, rgba(245, 108, 108, 0.08) 100%); color: #f56c6c; }
}

.card-title-wrap {
  flex: 1;
  .card-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0; }
  .card-subtitle { font-size: 12px; color: #c0c4cc; margin: 2px 0 0 0; }
}

.info-list {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f4f4f5;
  &:last-child { border-bottom: none; }
  .info-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #909399;
    svg { color: #c0c4cc; }
  }
  .info-value {
    font-size: 14px;
    color: #303133;
    font-weight: 500;
    &.readonly { color: #606266; }
  }
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f4f4f5;
}

.password-form {
  &.expanded { display: flex; flex-direction: column; gap: 16px; }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  .form-label { font-size: 13px; color: #909399; }
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: #f4f4f5;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
  min-width: 24px;
}

.form-error {
  font-size: 12px;
  color: #f56c6c;
}

.danger-zone {
  border: 1px solid rgba(245, 108, 108, 0.2);
}

.danger-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.danger-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .danger-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    .danger-title { font-size: 14px; font-weight: 500; color: #303133; }
    .danger-desc { font-size: 12px; color: #909399; }
  }
}
</style>