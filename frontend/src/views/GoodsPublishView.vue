<template>
  <section class="page-stack goods-publish-form">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Publish</p>
        <h2>发布二手商品</h2>
        <p>需要先开通 10 元二手发布者保证金身份；平台不托管交易本金。</p>
      </div>
      <el-button @click="router.push('/roles')">身份保证金</el-button>
    </div>

    <el-alert v-if="!auth.currentUser" title="请先登录后发布商品" type="warning" show-icon />

    <el-card>
      <el-form label-position="top" :model="form" class="form-section-stack">
        <FormSection title="商品信息" description="标题、分类和成色决定买家是否会继续查看详情。">
          <el-form-item label="标题">
            <el-input v-model="form.title" placeholder="例如：九成新机械键盘" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="说明成色、配件、交易说明" />
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="分类 ID">
              <el-input-number v-model="form.categoryId" :min="1" />
            </el-form-item>
            <el-form-item label="成色">
              <el-input v-model="form.conditionLevel" />
            </el-form-item>
          </div>
        </FormSection>
        <FormSection title="价格与地点" description="标清价格、原价和交易校区，便于校内线下沟通。">
          <div class="form-grid">
            <el-form-item label="价格">
              <el-input-number v-model="form.price" :min="0.01" :precision="2" />
            </el-form-item>
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" />
            </el-form-item>
            <el-form-item label="校区">
              <el-select v-model="form.campusZone">
                <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
              </el-select>
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item label="交易地点">
              <el-input v-model="form.tradeLocation" />
            </el-form-item>
            <el-form-item label="交易方式">
              <el-select v-model="form.deliveryMethod">
                <el-option label="线下面交" value="OFFLINE_MEETUP" />
                <el-option label="自取" value="SELF_PICKUP" />
                <el-option label="协商" value="NEGOTIATED" />
              </el-select>
            </el-form-item>
          </div>
        </FormSection>
        <FormSection title="联系与风险提示" description="联系方式默认在买家提交购买意向后展示，请勿在描述中直接写手机号或敏感信息。">
          <el-alert title="CampusHub 不托管交易本金，不做逐单保证金冻结；线下交易请保留必要聊天和交接证据。" type="info" show-icon />
        </FormSection>
        <div class="form-actions">
          <el-button @click="router.push('/goods')">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
        </div>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publishGoods, type CreateGoodsPayload } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import FormSection from '@/components/common/FormSection.vue'

const router = useRouter()
const auth = useAuthStore()
const submitting = ref(false)

const zones = [
  { label: '中校区', value: 'CENTRAL' },
  { label: '西校区', value: 'WEST' },
  { label: '东校区', value: 'EAST' },
  { label: '北校区', value: 'NORTH' },
  { label: '南校区', value: 'SOUTH' },
  { label: '高新校区', value: 'HIGH_TECH' },
  { label: '其他', value: 'OTHER' },
]

const form = reactive<CreateGoodsPayload>({
  categoryId: 1,
  title: '',
  description: '',
  price: 1,
  originalPrice: 0,
  conditionLevel: '九成新',
  campusZone: 'CENTRAL',
  tradeLocation: '中校区',
  deliveryMethod: 'OFFLINE_MEETUP',
  contactVisibility: 'INTENT_ONLY',
})

async function submit() {
  if (!auth.currentUser) {
    ElMessage.error('请先登录')
    return
  }
  if (!form.title || !form.description || !form.tradeLocation) {
    ElMessage.error('请填写完整商品信息')
    return
  }
  submitting.value = true
  try {
    const detail = await publishGoods(auth.currentUser.id, form)
    ElMessage.success('商品已发布')
    router.push(`/goods/${detail.id}`)
  } catch (error) {
    const message = error instanceof Error ? error.message : '发布失败'
    ElMessage.error(message)
    if (message.includes('二手发布者')) {
      router.push('/roles')
    }
  } finally {
    submitting.value = false
  }
}
</script>
