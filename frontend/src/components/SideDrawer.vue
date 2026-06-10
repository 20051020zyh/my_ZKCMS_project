<script setup lang="ts">
import ArticleRanking from './ArticleRanking.vue'
import BestArticles from './BestArticles.vue'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <transition name="drawer">
      <div v-if="visible" class="drawer-panel">
        <div class="drawer-body">
          <ArticleRanking />
          <BestArticles />
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.drawer-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 340px;
  z-index: 999;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-left: 1px solid rgba(203,213,225,0.2);
  box-shadow: -4px 0 40px rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 72px 20px 24px;
  scrollbar-width: thin;
}

.drawer-body::-webkit-scrollbar { width: 4px; }
.drawer-body::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 2px; }

/* Transition */
.drawer-enter-active,
.drawer-leave-active { transition: opacity 0.2s ease; }
.drawer-enter-from,
.drawer-leave-to { opacity: 0; }

.drawer-leave-active .drawer-panel {
  animation: slideOut 0.2s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

@keyframes slideOut {
  from { transform: translateX(0); }
  to { transform: translateX(100%); }
}
</style>
