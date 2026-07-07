<template>
  <div class="admin-register-page">
    <!-- 动态背景 -->
    <div class="bg-animation">
      <div class="bg-circle circle1"></div>
      <div class="bg-circle circle2"></div>
      <div class="bg-circle circle3"></div>
    </div>
    
    <div class="register-card" :class="{ 'card-active': showCard }">
      <div class="card-header">
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="32"><UserFilled /></el-icon>
          </div>
          <span class="logo-text">SmartCS</span>
        </div>
        <h2>管理员注册</h2>
        <p class="subtitle">需要有效的邀请码才能注册管理员账号</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="管理员用户名"
            :prefix-icon="User"
            size="large"
            class="input-animate"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（至少6位）"
            :prefix-icon="Lock"
            size="large"
            show-password
            class="input-animate"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            class="input-animate"
          />
        </el-form-item>

        <el-form-item prop="invitationCode">
          <el-input
            v-model="form.invitationCode"
            placeholder="邀请码"
            :prefix-icon="Key"
            size="large"
            class="input-animate"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱（选填）"
            :prefix-icon="Message"
            size="large"
            class="input-animate"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="register-btn"
            native-type="submit"
          >
            {{ loading ? '注册中...' : '注册管理员' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { User, Lock, Key, Message, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const showCard = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  invitationCode: '',
  email: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  invitationCode: [
    { required: true, message: '请输入邀请码', trigger: 'blur' }
  ]
}

onMounted(() => {
  setTimeout(() => {
    showCard.value = true
  }, 100)
})

const handleRegister = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authApi.registerAdmin({
      username: form.username,
      password: form.password,
      invitationCode: form.invitationCode,
      email: form.email || null
    })
    ElMessage.success('管理员注册成功！请使用您的账号登录。')
    router.push('/login')
  } catch (err) {
    ElMessage.error(err.message || '注册失败，请检查邀请码是否正确')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.admin-register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f1923 0%, #1a2744 50%, #0d1f3c 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

// 动态背景
.bg-animation {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  
  .bg-circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.2;
    
    &.circle1 {
      width: 500px;
      height: 500px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      top: -200px;
      right: -200px;
      animation: float1 10s ease-in-out infinite;
    }
    
    &.circle2 {
      width: 400px;
      height: 400px;
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      bottom: -150px;
      left: -150px;
      animation: float2 8s ease-in-out infinite;
    }
    
    &.circle3 {
      width: 300px;
      height: 300px;
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      animation: float3 9s ease-in-out infinite;
    }
  }
  
  @keyframes float1 {
    0%, 100% { transform: translate(0, 0); }
    50% { transform: translate(-30px, 30px); }
  }
  
  @keyframes float2 {
    0%, 100% { transform: translate(0, 0); }
    50% { transform: translate(20px, -20px); }
  }
  
  @keyframes float3 {
    0%, 100% { transform: translate(-50%, -50%) scale(1); }
    50% { transform: translate(-50%, -50%) scale(1.1); }
  }
}

.register-card {
  width: 100%;
  max-width: 440px;
  background: rgba(20, 30, 48, 0.95);
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 20px;
  padding: 44px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  position: relative;
  z-index: 10;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  
  &.card-active {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
  
  .logo-icon {
    width: 50px;
    height: 50px;
    border-radius: 14px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  }
  
  .logo-text {
    font-size: 24px;
    font-weight: 700;
    background: linear-gradient(90deg, #40c9ff, #36a9ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.card-header h2 {
  color: #e8f4ff;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 10px;
}

.subtitle {
  color: #6b7c99;
  font-size: 13px;
  margin: 0;
}

.register-form {
  margin-bottom: 24px;
  
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  
  :deep(.el-form-item__error) {
    padding-top: 4px;
    font-size: 12px;
  }
}

// 输入框动画
.input-animate {
  :deep(.el-input__wrapper) {
    padding: 4px 15px;
    border-radius: 12px;
    transition: all 0.3s;
    box-shadow: 0 0 0 1px #2d3a5c inset;
    background: rgba(255, 255, 255, 0.02);
    
    &:hover {
      box-shadow: 0 0 0 1px #667eea inset;
    }
    
    &:focus-within {
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
      background: rgba(102, 126, 234, 0.05);
    }
  }
  
  :deep(.el-input__inner) {
    font-size: 15px;
    height: 42px;
    line-height: 42px;
    color: #c5ccd8;
    
    &::placeholder {
      color: #4a5568;
    }
  }
  
  :deep(.el-input__prefix-inner) {
    color: #667eea;
  }
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  &:disabled {
    background: #2d3a5c;
    box-shadow: none;
    transform: none;
    color: #6b7c99;
  }
}

.card-footer {
  text-align: center;
  color: #6b7c99;
  font-size: 14px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.card-footer a {
  color: #667eea;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 600;
  transition: color 0.3s;
  
  &:hover {
    color: #764ba2;
    text-decoration: underline;
  }
}
</style>
