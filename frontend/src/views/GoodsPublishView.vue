<template>
  <section class="page-stack goods-publish-form">
    <div class="page-heading">
      <div>
        <p class="eyebrow">闲置上架 · 校友流转</p>
        <h2>{{ route.query.edit ? '编辑闲置' : '上架闲置好物' }}</h2>
        <p>阅读并同意闲置发布与交易规范后即可发布好物，联系方式默认在买家提交意向后展示。</p>
      </div>
      <el-button @click="router.push('/roles')">查看我的身份印章</el-button>
    </div>

    <el-alert v-if="!auth.currentUser" title="请先登录校园账号后再上架闲置" type="warning" show-icon />

    <LockedState
      v-if="auth.currentUser && !auth.canAccessIdentity('goodsPublisher')"
      title="需先点亮「闲置发布者」身份"
      description="请先阅读并同意闲置发布与交易规范，协议确认后即可自动开通发布权限。"
      primary-text="去开通身份"
      primary-to="/roles"
      secondary-text="返回二手市集"
      secondary-to="/goods"
    />

    <el-alert v-else-if="error" :title="error" type="warning" :closable="false"><el-button text @click="initialize">重试</el-button></el-alert>
    <el-card v-else-if="ready" shadow="never" class="premium-panel">
      <el-form label-position="top" :model="form" class="form-section-stack" :disabled="submitting || !!savedId || uncertain">
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
                <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
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

      </el-form>
      <CatalogPhotos ref="photos" kind="GOODS" :fallback-char="form.title.slice(0, 1)" :object-id="savedId || editingId || 0" editable />
      <p v-if="savedId" role="status">闲置 #{{ savedId }} 已保存。未传完的照片可重试，不用重新发布。</p>
      <el-alert v-if="uncertain" title="未收到上架结果。请先在「我的订单」核对，避免重复发布。" type="warning" :closable="false" />
      <div class="form-actions">
        <el-button @click="router.push('/orders?type=GOODS')">我的订单</el-button>
        <el-button v-if="savedId" @click="router.push(`/goods/${savedId}`)">查看已保存的闲置</el-button>
        <el-button type="primary" :disabled="uncertain" :loading="submitting" @click="submit">{{ savedId ? '重试未完成照片' : editingId ? '保存修改' : '确认上架' }}</el-button>
      </div>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publishGoods, getGoodsDetail, updateGoods, listGoodsCategories, type CreateGoodsPayload } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import LockedState from '@/components/common/LockedState.vue'
import FormSection from '@/components/common/FormSection.vue'

import CatalogPhotos from '@/components/CatalogPhotos.vue'
const router = useRouter(), route = useRoute()
const auth = useAuthStore()
const submitting = ref(false)

const zones = [
  { label: '中校区', value: 'CENTRAL' },
  { label: '西校区', value: 'WEST' },
  { label: '东校区', value: 'EAST' },
  { label: '北校区', value: 'NORTH' },
  { label: '南校区', value: 'SOUTH' },
  { label: '高新校区', value: 'HIGH_TECH' },
  { label: '先研院', value: 'ADVANCED_RESEARCH_INSTITUTE' }, { label: '科学岛', value: 'SCIENCE_ISLAND' },
  { label: '其他', value: 'OTHER' },
]

const blank = ():CreateGoodsPayload => ({
  categoryId: 0,
  title: '',
  description: '',
  price: 1,
  originalPrice: null,
  conditionLevel: '九成新',
  campusZone: 'CENTRAL',
  tradeLocation: '中校区',
  deliveryMethod: 'OFFLINE_MEETUP',
  contactVisibility: 'INTENT_ONLY',
})

const form=reactive(blank()), editingId=computed(()=>Number(route.query.edit) || 0)
const categories=ref<Awaited<ReturnType<typeof listGoodsCategories>>>([]), savedId=ref(0), photos=ref<InstanceType<typeof CatalogPhotos>>()
const ready=ref(false), error=ref(''), uncertain=ref(false)
let version=0
async function initialize(){
  const v=++version; Object.assign(form,blank()); ready.value=false; savedId.value=0; error.value=''; uncertain.value=submitting.value=false
  if(!auth.currentUser || !auth.token)return
  try {
    if(route.query.edit!=null && (!Number.isSafeInteger(editingId.value)||editingId.value<1))throw Error('无效的闲置编号')
    const [options,item]=await Promise.all([listGoodsCategories(),editingId.value?getGoodsDetail(editingId.value):null]);if(v!==version)return
    categories.value=options.filter(c=>c.enabled); if(!categories.value.length)throw Error('暂无可用的商品分类')
    form.categoryId=categories.value[0].id
    if(item){if(!item.canEdit || item.sellerId!==auth.currentUser.id)throw Error('该闲置不可编辑');for(const key of Object.keys(blank()) as (keyof CreateGoodsPayload)[])Object.assign(form,{[key]:item[key]})}
    ready.value=true
  }catch(e){if(v===version)error.value=e instanceof Error?e.message:'无法加载发布表单'}
}
watch([()=>route.query.edit,()=>auth.currentUser?.id,()=>auth.token],initialize,{immediate:true,flush:'sync'});onBeforeUnmount(()=>{version++})
async function submit(){
  if(!ready.value || !auth.currentUser || !auth.token || submitting.value || uncertain.value)return
  if(!savedId.value && (!form.title.trim() || !form.description.trim() || !form.conditionLevel.trim() || !form.tradeLocation.trim() || !form.price || !categories.value.some(c=>c.id===form.categoryId))){ElMessage.warning('请填写分类、名称、成色、价格、说明与交接地点');return}
  const v=version;submitting.value=true
  try {
    if(!savedId.value){const payload={...form};const item=editingId.value?await updateGoods(editingId.value,payload):await publishGoods(auth.currentUser.id,payload);if(v!==version)return;savedId.value=item.id}
    await photos.value?.saveTo(savedId.value);if(v!==version)return;ElMessage.success('闲置已保存');await router.push(`/goods/${savedId.value}`)
  }catch(e){if(v===version){const err=e as {isAxiosError?:boolean;response?:{status:number}};if(!editingId.value && !savedId.value && err?.isAxiosError && (!err.response || err.response.status>=500 || err.response.status===408))uncertain.value=true;ElMessage.error(savedId.value?'闲置已保存，照片未传完，请重试':e instanceof Error?e.message:'保存失败')}}
  finally{if(v===version)submitting.value=false}
}
</script>
