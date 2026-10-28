<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">跑腿互助 · 履约跟进</p>
        <h2>跑腿履约台 <span class="mono-num">#{{ taskId }}</span></h2>
        <p>在这里同步你的取件、派送与送达进度；若途中遇到异常可随时向委托人和站务报备。</p>
      </div>
      <el-button @click="$router.push('/tasks')">返回跑腿大厅</el-button>
    </div>

    <el-card shadow="never" class="workspace-card premium-panel">
      <div class="workspace-summary">
        <div>
          <span>悬赏单编号</span>
          <strong class="mono-num">#{{ taskId }}</strong>
        </div>
        <div>
          <span>当前接单同学</span>
          <strong>{{ auth.currentUser?.nickname || '未登录访客' }}</strong>
        </div>
        <div>
          <span>联络与交接方式</span>
          <strong>站内同步节点 + 微信/QQ 沟通</strong>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="workspace-card premium-panel">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="已接单" />
        <el-step title="前往取件" />
        <el-step title="已取件" />
        <el-step title="派送途中" />
        <el-step title="送达确认" />
      </el-steps>

      <div class="workspace-actions">
        <el-button type="primary" plain @click="advance('HEADING_TO_PICKUP')">出发前往取件点</el-button>
        <el-button type="primary" plain @click="advance('PICKED_UP')">已拿到物品</el-button>
        <el-button type="primary" @click="advance('DELIVERING')">开始派送</el-button>
        <el-button type="success" @click="complete">凭完成码确认送达</el-button>
      </div>

      <el-alert title="交接小贴士" type="info" show-icon :closable="false">
        <p>接单后请通过微信或 QQ 与委托同学确认取件码及具体楼层，送达后输入对方提供的完成码即可结单。</p>
      </el-alert>
    </el-card>

    <el-card shadow="never" class="workspace-card premium-panel">
      <template #header>遇到突发状况？在此报备异常</template>
      <el-form label-position="top" :model="issueForm">
        <el-form-item label="异常情况类别">
          <el-select v-model="issueForm.issueType" class="wide">
            <el-option label="驿站/柜机无法取件" value="PICKUP_FAILED" />
            <el-option label="所填地点有误" value="LOCATION_ERROR" />
            <el-option label="暂时联系不上委托同学" value="CONTACT_FAILED" />
            <el-option label="临时有事或时间来不及" value="TIMEOUT" />
            <el-option label="物品破损或超重异常" value="ITEM_ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="现场情况说明">
          <el-input v-model="issueForm.description" type="textarea" :rows="3" placeholder="简述现场遇到的问题，以便委托人与站务协助处理..." />
        </el-form-item>
        <el-button type="danger" plain @click="reportIssue">提交异常报备</el-button>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { advanceRunnerTask, completeRunnerTaskWithCode, reportRunnerTaskIssue } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const taskId = computed(() => Number(route.params.id))
const activeStep = 1
const issueForm = reactive({ issueType: 'LOCATION_ERROR', description: '' })

async function requireUserId() {
  if (!auth.currentUser) {
    ElMessage.warning('请先登录')
    await router.push('/auth')
    throw new Error('请先登录')
  }
  return auth.currentUser.id
}

async function advance(nextStatus: string) {
  await advanceRunnerTask(taskId.value, await requireUserId(), nextStatus, { note: '履约进度更新' })
  ElMessage.success('履约进度已同步')
}

async function complete() {
  await completeRunnerTaskWithCode(taskId.value, await requireUserId(), { note: '已准时送达', completionCode: '123456' })
  ElMessage.success('已确认送达，感谢你的顺路相助！')
}

async function reportIssue() {
  await reportRunnerTaskIssue(taskId.value, await requireUserId(), issueForm)
  ElMessage.success('异常情况已报备')
}
</script>
