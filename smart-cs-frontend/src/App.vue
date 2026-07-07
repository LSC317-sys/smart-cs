<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
</script>

<style>
:root {
  /* 背景色 */
  --bg-base: #f0f2f5;
  --bg-surface: rgba(255, 255, 255, 0.08);
  --bg-card: #ffffff;
  --bg-hover: rgba(255, 255, 255, 0.05);
  
  /* 文字颜色 */
  --text-primary: #303133;
  --text-secondary: #909399;
  --text-muted: #c0c4cc;
  
  /* 边框 */
  --border: #ebeef5;
  --border-hover: #dcdfe6;
  
  /* 主题色 */
  --accent: #667eea;
  --accent-hover: #5a70d9;
  --accent-dim: rgba(102, 126, 234, 0.1);
  --accent-border: rgba(102, 126, 234, 0.3);
  
  /* 状态色 */
  --success: #67c23a;
  --warning: #e6a23c;
  --danger: #f56c6c;
  --danger-border: rgba(245, 108, 108, 0.3);
  --danger-dim: rgba(245, 108, 108, 0.05);
  
  /* 圆角 */
  --radius: 8px;
  --radius-sm: 6px;
  --radius-lg: 16px;
  
  /* 过渡 */
  --transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  
  /* 字体 */
  --font-heading: 'SF Pro Text', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

body {
  font-family: var(--font-heading);
  color: var(--text-primary);
  background: var(--bg-base);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  height: 100vh;
}

/* Element Plus 暗黑风格覆盖（如果未来需要） */
.el-button--primary {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
  --el-button-hover-bg-color: var(--accent-hover);
  --el-button-hover-border-color: var(--accent-hover);
}

/* 全局滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>