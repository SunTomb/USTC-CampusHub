<template>
  <section class="page-stack goods-publish-form">
    <div class="page-heading">
      <div>
        <p class="eyebrow">闲置上架 · 校友流转</p>
        <h2>上架闲置好物</h2>
        <p>开通闲置发布者身份（10 元诚信保证金）即可随时发布好物，联系方式默认在买家提交意向后展示。</p>
      </div>
      <el-button @click="router.push('/roles')">查看我的身份印章</el-button>
    </div>

    <el-alert v-if="!auth.currentUser" title="请先登录校园账号后再上架闲置" type="warning" show-icon />

    <LockedState
      v-if="auth.currentUser && !auth.canAccessIdentity('goodsPublisher') && !auth.isAdmin"
      title="需先点亮「闲置发布者」身份"
      description="缴纳 10 元诚信保证金即可实时自动开通，让校内二手交易更真实可靠。"
      primary-text="去开通身份"
      primary-to="/roles"
      secondary-text="返回二手市集"
      secondary-to="/goods"
    />

    <el-card v-else shadow="never" class="premium-panel">
      <el-form label-position="top" :model="form" class="form-section-stack">
        <FormSection title="物品基本情况" description="真实的成色说明与细节描述，能帮你的好物更快找到下一位主人。">
          <el-form-item label="物品名称">
            <el-input v-model="form.title" placeholder="例如：九五新 Keychron 机械键盘 / 数学分析教材全套" />
          </el-form-item>
          <el-form-item label="成色与细节说明">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="说明购入时间、使用痕迹、包含配件或附赠物品..." />
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="物品分类">
              <el-select v-model="form.categoryId">
                <el-option label="数码与外设" :value="1" />
                <el-option label="教材与考研资料" :value="2" />
                <el-option label="代步车与运动器材" :value="3" />
                <el-option label="宿舍电器与生活好物" :value="4" />
                <el-option label="其他闲置" :value="5" />
              </el-select>
            </el-form-item>
            <el-form-item label="成色印象">
              <el-input v-model="form.conditionLevel" placeholder="如：九成新 / 全新未拆" />
            </el-form-item>
          </div>
        </FormSection>

        <FormSection title="转让价格与交接地点" description="注明所在校区与常去地点，方便同校区同学顺路自提。">
          <div class="form-grid">
            <el-form-item label="转让价（元）">
              <el-input-number v-model="form.price" :min="0.01" :precision="2" class="wide" />
            </el-form-item>
            <el-form-item label="入手原价（选填）">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" class="wide" />
            </el-form-item>
            <el-form-item label="所在校区">
              <el-select v-model="form.campusZone">
                <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
              </el-select>
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item label="建议交接点">
              <el-input v-model="form.tradeLocation" placeholder="如：西区三教一楼 / 中区桃李苑门口" />
            </el-form-item>
            <el-form-item label="交接方式">
              <el-select v-model="form.deliveryMethod">
                <el-option label="校内当面交接" value="OFFLINE_MEETUP" />
                <el-option label="买家宿舍楼下自提" value="SELF_PICKUP" />
                <el-option label="双方私聊协商" value="NEGOTIATED" />
              </el-select>
            </el-form-item>
          </div>
        </FormSection>

        <FormSection title="隐私保护约定" description="系统会自动关联你注册时填写的微信号或 QQ，仅对提交了购买意向的校友开放。">
          <el-alert title="请勿在公开的商品描述中直接暴露个人手机号、宿舍门牌号或收款码图片。" type="info" show-icon :closable="false" />
        </FormSection>

        <div class="form-actions">
          <el-button @click="router.push('/goods')">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">确认上架</el-button>
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
import LockedState from '@/components/common/LockedState.vue'
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
    ElMessage.error('请填写完整商品名称、描述与交接地点')
    return
  }
  submitting.value = true
  try {
    const detail = await publishGoods(auth.currentUser.id, form)
    ElMessage.success('闲置好物已上架')
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
