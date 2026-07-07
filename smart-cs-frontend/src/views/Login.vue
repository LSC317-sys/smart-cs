<template>
  <div class="login-container">
    <!-- 粒子连线背景 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    
    <!-- 左侧动画区域 -->
    <div class="left-panel">
      <div class="ai-showcase">
        <div class="ai-core">
          <div class="ai-orb"></div>
          <div class="ai-ring ring1"></div>
          <div class="ai-ring ring2"></div>
          <div class="ai-ring ring3"></div>
          <div class="ai-center">
            <el-icon :size="60"><ChatDotRound /></el-icon>
          </div>
        </div>
        
        <div class="ai-features">
          <div class="feature-item" v-for="(f, i) in features" :key="i" :style="{ animationDelay: `${i * 0.2}s` }">
            <div class="feature-icon">
              <el-icon :size="20"><component :is="f.icon" /></el-icon>
            </div>
            <div class="feature-text">
              <span class="feature-title">{{ f.title }}</span>
              <span class="feature-desc">{{ f.desc }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="slogan">
        <h2>智能客服体验</h2>
        <p>基于 RAG 技术的下一代 AI 对话系统</p>
      </div>
    </div>
    
    <!-- 右侧登录卡片 -->
    <div 
      class="right-panel"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
      ref="loginBoxRef"
    >
      <div class="card-glow"></div>
      
      <div class="login-header">
        <div class="logo-icon">
          <el-icon :size="38"><ChatDotRound /></el-icon>
        </div>
        <h1>SmartCS</h1>
        <p>AI RAG 智能客服系统</p>
      </div>
      
      <el-tabs v-model="activeTab" class="login-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef">
            <el-form-item prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
                class="input-animate"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleLogin"
                class="input-animate"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                size="large" 
                :loading="loading"
                @click="handleLogin"
                class="login-btn"
              >
                <span v-if="!loading">登 录</span>
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
            <el-form-item prop="username">
              <el-input 
                v-model="registerForm.username" 
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
                class="input-animate"
              />
            </el-form-item>
            <el-form-item prop="email">
              <el-input 
                v-model="registerForm.email" 
                placeholder="请输入邮箱"
                :prefix-icon="Message"
                size="large"
                class="input-animate"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                placeholder="请输入密码 (6-20位)"
                :prefix-icon="Lock"
                size="large"
                show-password
                class="input-animate"
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input 
                v-model="registerForm.confirmPassword" 
                type="password" 
                placeholder="请确认密码"
                :prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleRegister"
                class="input-animate"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                size="large" 
                :loading="loading"
                @click="handleRegister"
                class="login-btn"
              >
                <span v-if="!loading">注 册</span>
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <span>管理员？</span>
        <router-link to="/admin-register">申请管理员账号</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, ChatDotRound, DataLine, MessageBox, ScaleToOriginal, Cpu } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginBoxRef = ref(null)
const canvasRef = ref(null)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const features = [
  { icon: 'DataLine', title: '向量检索', desc: '语义理解，精准匹配' },
  { icon: 'MessageBox', title: '智能问答', desc: '多轮对话，理解上下文' },
  { icon: 'ScaleToOriginal', title: '知识增强', desc: 'RAG 技术，实时检索' },
  { icon: 'Cpu', title: '大模型驱动', desc: 'LLM 赋能，流畅交互' }
]

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

let animationId = null
let particles = []

function initParticleCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  
  const ctx = canvas.getContext('2d')
  let width = window.innerWidth
  let height = window.innerHeight
  
  canvas.width = width
  canvas.height = height
  
  const particleCount = 60
  
  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      radius: Math.random() * 2 + 1,
      color: `rgba(${Math.random() > 0.5 ? '102, 126, 234' : '118, 75, 162'}, ${Math.random() * 0.4 + 0.2})`
    })
  }
  
  function animate() {
    ctx.clearRect(0, 0, width, height)
    
    particles.forEach((p, i) => {
      p.x += p.vx
      p.y += p.vy
      
      if (p.x < 0 || p.x > width) p.vx *= -1
      if (p.y < 0 || p.y > height) p.vy *= -1
      
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = p.color
      ctx.fill()
      
      particles.forEach((p2, j) => {
        if (i === j) return
        const dx = p.x - p2.x
        const dy = p.y - p2.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        
        if (dist < 150) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = `rgba(102, 126, 234, ${0.15 * (1 - dist / 150)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      })
    })
    
    animationId = requestAnimationFrame(animate)
  }
  
  animate()
  
  window.addEventListener('resize', () => {
    width = window.innerWidth
    height = window.innerHeight
    canvas.width = width
    canvas.height = height
  })
}

onMounted(() => {
  initParticleCanvas()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})

function handleTabChange() {
  if (activeTab.value === 'login' && loginFormRef.value) {
    loginFormRef.value.clearValidate()
  } else if (activeTab.value === 'register' && registerFormRef.value) {
    registerFormRef.value.clearValidate()
  }
}

function handleMouseMove(e) {
  if (!loginBoxRef.value) return
  const rect = loginBoxRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  const rotateX = (y - centerY) / 25
  const rotateY = (centerX - x) / 25
  
  loginBoxRef.value.style.transform = `perspective(1000px) translateY(-50%) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`
}

function handleMouseLeave() {
  if (!loginBoxRef.value) return
  loginBoxRef.value.style.transform = 'perspective(1000px) translateY(-50%) rotateX(0) rotateY(0) scale3d(1, 1, 1)'
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  const result = await userStore.login(loginForm.username, loginForm.password)
  loading.value = false
  
  if (result.success) {
    ElMessage.success('登录成功')
    const role = userStore.userInfo?.role
    if (role === 'ADMIN') {
      router.push('/admin')
    } else {
      router.push('/chat')
    }
  } else {
    ElMessage.error(result.message || '登录失败')
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  const result = await userStore.register({
    username: registerForm.username,
    email: registerForm.email,
    password: registerForm.password
  })
  loading.value = false
  
  if (result.success) {
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    registerForm.username = ''
    registerForm.email = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
  } else {
    ElMessage.error(result.message || '注册失败')
  }
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a0e1a 0%, #1a1f3a 50%, #0d1025 100%);
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

// 左侧动画区域
.left-panel {
  position: fixed;
  left: 0;
  top: 0;
  width: 50%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
  padding: 40px;
}

.ai-showcase {
  text-align: center;
}

.ai-core {
  position: relative;
  width: 200px;
  height: 200px;
  margin: 0 auto 50px;
}

.ai-orb {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  border-radius: 50%;
  filter: blur(20px);
  opacity: 0.6;
  animation: pulse-orb 3s ease-in-out infinite;
}

.ai-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 
    0 0 40px rgba(102, 126, 234, 0.5),
    0 0 80px rgba(118, 75, 162, 0.3);
  z-index: 10;
}

.ai-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 2px solid rgba(102, 126, 234, 0.3);
  
  &.ring1 {
    width: 160px;
    height: 160px;
    animation: rotate-ring 8s linear infinite;
    border-top-color: rgba(102, 126, 234, 0.6);
    border-right-color: transparent;
    border-bottom-color: transparent;
    border-left-color: transparent;
  }
  
  &.ring2 {
    width: 180px;
    height: 180px;
    animation: rotate-ring 12s linear infinite reverse;
    border-right-color: rgba(240, 147, 251, 0.4);
    border-top-color: transparent;
  }
  
  &.ring3 {
    width: 200px;
    height: 200px;
    animation: rotate-ring 16s linear infinite;
    border-bottom-color: rgba(102, 126, 234, 0.3);
    border-top-color: transparent;
    border-left-color: transparent;
  }
}

@keyframes pulse-orb {
  0%, 100% { 
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.6;
  }
  50% { 
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.8;
  }
}

@keyframes rotate-ring {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.ai-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  animation: slide-in 0.5s ease-out backwards;
  
  &:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(102, 126, 234, 0.3);
    transform: translateX(10px);
  }
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  text-align: left;
  
  .feature-title {
    color: #fff;
    font-weight: 600;
    font-size: 15px;
  }
  
  .feature-desc {
    color: rgba(255, 255, 255, 0.5);
    font-size: 12px;
    margin-top: 2px;
  }
}

.slogan {
  margin-top: 60px;
  text-align: center;
  
  h2 {
    font-size: 28px;
    font-weight: 700;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 12px 0;
  }
  
  p {
    color: rgba(255, 255, 255, 0.4);
    font-size: 14px;
    margin: 0;
  }
}

// 右侧登录卡片
.right-panel {
  position: fixed;
  right: 8%;
  top: 50%;
  transform: translateY(-50%);
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  padding: 40px;
  z-index: 20;
  transition: all 0.3s;
  transform-style: preserve-3d;
  perspective: 1000px;
  
  &:hover {
    box-shadow: 
      0 25px 80px rgba(102, 126, 234, 0.4),
      0 0 0 1px rgba(255, 255, 255, 0.2),
      inset 0 1px 0 rgba(255, 255, 255, 0.8);
  }
}

.card-glow {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #667eea, #764ba2, #f093fb);
  border-radius: 26px;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.3s;
  filter: blur(8px);
}

.right-panel:hover .card-glow {
  opacity: 0.3;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  
  .logo-icon {
    width: 65px;
    height: 65px;
    margin: 0 auto 16px;
    border-radius: 18px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  }
  
  h1 {
    font-size: 26px;
    font-weight: 800;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 8px 0;
  }
  
  p {
    color: #909399;
    font-size: 13px;
    margin: 0;
  }
}

.login-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
  
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  
  :deep(.el-tabs__nav) {
    width: 100%;
  }
  
  :deep(.el-tabs__item) {
    width: 50%;
    text-align: center;
    font-size: 15px;
    font-weight: 600;
    height: 40px;
    line-height: 40px;
    padding: 0;
    color: #909399;
    
    &:hover {
      color: #667eea;
    }
  }
  
  :deep(.el-tabs__active-bar) {
    height: 3px;
    border-radius: 3px;
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  }
  
  :deep(.el-tabs__item.is-active) {
    color: #667eea;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }
  
  :deep(.el-form-item__error) {
    padding-top: 4px;
    font-size: 12px;
  }
}

.input-animate {
  :deep(.el-input__wrapper) {
    padding: 4px 15px;
    border-radius: 12px;
    transition: all 0.3s;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    background: rgba(255, 255, 255, 0.9);
    
    &:hover {
      box-shadow: 0 0 0 1px #667eea inset;
    }
    
    &:focus-within {
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
      transform: translateY(-1px);
    }
  }
  
  :deep(.el-input__inner) {
    font-size: 15px;
    height: 40px;
    line-height: 40px;
    
    &::placeholder {
      color: #c0c4cc;
    }
  }
  
  :deep(.el-input__prefix-inner) {
    color: #667eea;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  border: none;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  &:disabled {
    background: #dcdfe6;
    box-shadow: none;
    color: #909399;
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #909399;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.login-footer a {
  color: #667eea;
  text-decoration: none;
  margin-left: 4px;
  font-weight: 600;
  
  &:hover {
    color: #764ba2;
    text-decoration: underline;
  }
}

// 响应式
@media (max-width: 1200px) {
  .left-panel {
    display: none;
  }
  
  .right-panel {
    position: relative;
    right: auto;
    top: auto;
    transform: none;
    margin: 0 auto;
  }
}
</style>