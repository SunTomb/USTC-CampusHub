<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Task Workspace</p>
        <h2>履约工作台 #{{ taskId }}</h2>
        <p>展示当前步骤、下一步操作、联系提示和异常上报入口。</p>
      </div>
      <el-button @click="$router.push('/tasks')">返回任务大厅</el-button>
    </div>

    <el-card shadow="never" class="workspace-card">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="已接单" />
        <el-step title="前往取件" />
        <el-step title="已取件" />
        <el-step title="配送中" />
        <el-step title="完成确认" />
      </el-steps>

      <div class="workspace-actions">
        <el-button type="primary" @click="advance('HEADING_TO_PICKUP')">前往取件点</el-button>
        <el-button type="primary" @click="advance('PICKED_UP')">已取件</el-button>
        <el-button type="primary" @click="advance('DELIVERING')">配送中</el-button>
        <el-button type="success" @click="complete">完成码完成</el-button>
      </div>

      <el-alert title="联系提示" type="info" show-icon :closable="false">
        <p>平台保留关键状态与异常记录；具体交接可使用注册时填写的微信或 QQ 联系方式。</p>
      </el-alert>
    </el-card>

    <el-card shadow="never" class="workspace-card">
      <template #header>异常上报</template>
      <el-form label-position="top" :model="issueForm">
        <el-form-item label="异常类型">
          <el-select v-model="issueForm.issueType" class="wide">
            <el-option label="无法取件" value="PICKUP_FAILED" />
            <el-option label="地点错误" value="LOCATION_ERROR" />
            <el-option label="联系不上" value="CONTACT_FAILED" />
            <el-option label="超时" value="TIMEOUT" />
            <el-option label="物品异常" value="ITEM_ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="issueForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-button type="danger" plain @click="reportIssue">提交异常</el-button>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { advanceRunnerTask, completeRunnerTaskWithCode, reportRunnerTaskIssue } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const taskId = computed(() => Number(route.params.id))
const activeStep = 1
const issueForm = reactive({ issueType: 'LOCATION_ERROR', description: '' })

async function advance(nextStatus: string) {
  await advanceRunnerTask(taskId.value, auth.currentUser?.id ?? 2, nextStatus, { note: '工作台操作' })
  ElMessage.success('状态已更新')
}

async function complete() {
  await completeRunnerTaskWithCode(taskId.value, auth.currentUser?.id ?? 2, { note: '已送达', completionCode: '123456' })
  ElMessage.success('任务已完成')
}

async function reportIssue() {
  await reportRunnerTaskIssue(taskId.value, auth.currentUser?.id ?? 2, issueForm)
  ElMessage.success('异常已上报')
}
</script>
