<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSysConfig, toggleMaintenance } from '@/api/admin'

const loading = ref(false)
const maintenanceMode = ref(false)

const fetchConfig = async () => {
  loading.value = true
  try {
    const res: any = await getSysConfig()
    const msg = res.data || ''
    maintenanceMode.value = msg.includes('维护中')
  } catch (error) {
    ElMessage.error('获取系统配置失败')
  } finally {
    loading.value = false
  }
}

const handleToggle = async () => {
  try {
    await toggleMaintenance()
    ElMessage.success('切换成功')
    fetchConfig()
  } catch (error) {
    ElMessage.error('切换失败')
  }
}

onMounted(() => {
  fetchConfig()
})
</script>

<template>
  <div class="settings-container">
    <h2 class="page-title">系统设置</h2>

    <el-card v-loading="loading">
      <el-form label-width="120px">
        <el-form-item label="维护模式">
          <el-switch
            :model-value="maintenanceMode"
            @change="handleToggle"
          />
          <span class="mode-status">{{ maintenanceMode ? '已开启' : '已关闭' }}</span>
        </el-form-item>

        <el-form-item label="说明">
          <el-alert
            title="开启维护模式后，普通用户将无法访问系统，只有管理员可以正常使用"
            type="warning"
            :closable="false"
            show-icon
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.settings-container {
  padding: 24px 32px;
}
.page-title {
  margin: 0 0 24px 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}
:deep(.el-card) {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(203,213,225,0.3);
}
:deep(.el-card__body) { color: #334155; }
:deep(.el-form-item__label) { color: #94a3b8; font-weight: 500; }
.mode-status { margin-left: 12px; color: #94a3b8; font-size: 13px; }
</style>
