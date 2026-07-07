<template>
  <div class="sources-wrapper">
    <div class="sources-header" @click="$emit('toggle')">
      <el-icon><Document /></el-icon>
      <span>参考来源 ({{ sources.length }})</span>
      <el-icon class="toggle-icon" :class="{ expanded }">
        <ArrowRight />
      </el-icon>
    </div>
    <div v-if="expanded" class="sources-list">
      <div v-for="(src, idx) in sources" :key="idx" class="source-item">
        <div class="source-title">
          <el-tag size="small" type="info">{{ src.docTitle }}</el-tag>
          <el-tag size="small" :type="getSimilarityType(src.similarity)">
            相关度: {{ src.similarity }}%
          </el-tag>
        </div>
        <div class="source-preview">{{ src.contentPreview }}</div>
        <div class="source-actions">
          <el-button size="small" link @click="$emit('view', src)">查看原文</el-button>
          <el-button size="small" link @click="$emit('copy', src)">复制</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowRight, Document } from '@element-plus/icons-vue'

defineProps({
  sources: { type: Array, required: true },
  expanded: { type: Boolean, default: false },
})

defineEmits(['toggle', 'view', 'copy'])

function getSimilarityType(similarity) {
  if (similarity >= 70) return 'success'
  if (similarity >= 40) return 'warning'
  return 'info'
}
</script>

<style scoped lang="scss">
.sources-wrapper {
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 8px;
  border: 1px solid rgba(64, 158, 255, 0.2);
}

.sources-header {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #409eff;
  font-size: 13px;
  user-select: none;

  .toggle-icon {
    margin-left: auto;
    transition: transform 0.2s;
    &.expanded { transform: rotate(90deg); }
  }
  &:hover { color: #66b1ff; }
}

.sources-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-item {
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;

  .source-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }
  .source-preview {
    font-size: 12px;
    color: #606266;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .source-actions {
    display: flex;
    gap: 8px;
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed #eee;
  }
}
</style>