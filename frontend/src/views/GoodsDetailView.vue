<template>
  <section class="page-stack" v-loading="loading">
    <header class="page-heading"><div><p class="eyebrow">校园二手市集</p><h2>闲置详情</h2></div>
      <div class="heading-actions"><el-button @click="router.push('/goods')">返回市集</el-button><el-button v-if="auth.currentUser" @click="router.push('/orders?type=GOODS')">我的订单</el-button></div>
    </header>
    <section v-if="deleted" role="status" class="state-card"><h3>该商品已删除</h3><el-button text @click="router.push('/goods')">返回二手市集</el-button></section>
    <el-alert v-else-if="error" :title="error" type="warning" :closable="false"><el-button text @click="load">重新加载</el-button></el-alert>
    <div v-if="detail" class="goods-detail">
      <el-card class="goods-main-panel" shadow="never">
        <div class="catalog-detail-header goods-detail-header">
          <div class="catalog-detail-media">
            <CatalogPhotos kind="GOODS" :object-id="detail.id" layout="detail" :tone="detail.id % 4"
              :fallback-char="detail.title.slice(0, 1) || '物'" :fallback-sub="detail.conditionLevel" />
          </div>
          <div class="catalog-detail-info">
            <p class="eyebrow">{{ detail.categoryName || '其他闲置' }}</p>
            <h3 :title="detail.title">{{ detail.title }}</h3>
            <div class="goods-price-row goods-price-large"><strong>¥{{ detail.price }}</strong><span v-if="detail.originalPrice" class="goods-origin-price">原价 ¥{{ detail.originalPrice }}</span></div>
            <el-tag>{{ statusLabel(detail.status) }}</el-tag>
          </div>
          <div class="goods-detail-copy">
            <p class="preserve-lines">{{ detail.description }}</p>
            <p>{{ detail.conditionLevel }} · {{ formatCampusZone(detail.campusZone) }} · {{ detail.tradeLocation }} · {{ formatDeliveryMethod(detail.deliveryMethod) }}</p>
            <div class="detail-actions">
              <el-button v-if="!owner && detail.status === 'PUBLISHED' && !detail.myIntentStatus" type="primary" @click="openIntent">我想要</el-button>
              <el-tag v-if="detail.myIntentStatus">{{ statusLabel(detail.myIntentStatus) }}</el-tag>
              <el-button v-if="detail.status === 'PUBLISHED'" :disabled="busy" @click="favorite">收藏（{{ detail.favoriteCount }}）</el-button>
            </div>
          </div>
        </div>
        <GoodsEscrowDetail v-if="platform.financialEnabled" :key="detail.id + ':' + viewerId" :goods-id="detail.id" :owner="owner" :available="detail.status === 'PUBLISHED'" @changed="load" />
      </el-card>
      <el-card class="seller-panel" shadow="never"><template #header>卖家与交接方式</template><p>{{ detail.sellerNickname }} · {{ formatCampusZone(detail.campusZone) }}</p>
        <p v-if="detail.contactVisible && detail.contactSnapshot">卖家联系方式：{{ detail.contactSnapshot }}</p>
        <p v-else-if="detail.status === 'PUBLISHED'">提交购买意向后，可联系卖家约定当面交接。</p><p v-else>这件物品已结束交易。</p>
      </el-card>
      <el-card v-if="owner" class="interaction-panel" shadow="never"><template #header>我收到的购买意向</template>
        <p v-if="detail.status==='PUBLISHED'">实际交接后再选择买家。确认后无法换人，其他购买意向会关闭。</p>
        <div v-if="detail.canEdit || detail.canOffShelf || detail.canDelete" class="detail-actions"><el-button v-if="detail.canEdit" :disabled="busy" @click="router.push(`/goods/publish?edit=${detail.id}`)">编辑闲置</el-button><el-button v-if="detail.canOffShelf" :disabled="busy" @click="removeListing">下架闲置</el-button><el-button v-if="detail.canDelete" :disabled="busy" plain type="danger" @click="deleteListing">删除商品</el-button></div>
        <p v-if="!intents.length && !error" class="hint">还没有收到购买意向。</p>
        <div v-for="item in intents" :key="item.id" class="intent-row"><div><strong>{{ item.buyerNickname }}</strong> · {{ statusLabel(item.status) }}<p v-if="item.message">{{ item.message }}</p></div>
          <el-button v-if="detail.canMarkSold && item.status === 'OPEN'" type="primary" plain :disabled="busy" @click="chooseBuyer(item)">与此买家确认成交</el-button></div>
      </el-card>
      <el-card class="interaction-panel" shadow="never"><template #header>关于这件闲置的留言</template>
        <div v-if="detail.status === 'PUBLISHED'" class="comment-form"><el-input v-model="commentText" maxlength="500" placeholder="询问成色、配件或交接地点"/><el-button :disabled="busy || !commentText.trim()" @click="comment">留言</el-button></div>
        <p v-if="!detail.comments.length" class="hint">还没有留言。</p><div v-for="c in detail.comments" :key="c.id" class="comment-item"><strong>{{ c.userNickname }}</strong><span>{{ c.content }}</span></div>
      </el-card>
    </div>
    <el-dialog v-model="intentDialog" append-to-body title="提交购买意向" width="min(420px, 92vw)"><p>这不是成交确认。先与卖家约定交接时间和地点。</p><el-input v-model="message" maxlength="500" type="textarea" placeholder="给卖家留言（选填）"/><template #footer><el-button @click="intentDialog = false">取消</el-button><el-button type="primary" :loading="busy" @click="sendIntent">发送意向</el-button></template></el-dialog>
  </section>
</template>
<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { commentTarget, createGoodsIntent, deleteGoods, favoriteTarget, getGoodsDetail, listGoodsIntents, markGoodsSold, offShelfGoods, type GoodsDetailSummary, type GoodsIntentSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { usePlatformStore } from '@/stores/platform'
import CatalogPhotos from '@/components/CatalogPhotos.vue'
import { formatCampusZone, formatDeliveryMethod } from '@/utils/display'
const GoodsEscrowDetail = defineAsyncComponent(() => import('./legacy/GoodsEscrowDetail.vue'))
const auth = useAuthStore(), platform = usePlatformStore(), route = useRoute(), router = useRouter()
const id = computed(() => Number(route.params.id)), viewerId = computed(() => auth.currentUser?.id)
const detail = ref<GoodsDetailSummary|null>(null), intents = ref<GoodsIntentSummary[]>([]), owner = computed(() => !!detail.value && detail.value.sellerId === viewerId.value)
const deleted=ref(false), error=ref(''), busy=ref(false), loading=ref(false), intentDialog=ref(false), message=ref(''), commentText=ref('')
const labels:Record<string,string>={PUBLISHED:'在售',ON_SALE:'历史在售',SOLD:'已售出',OFF_SHELF:'已下架',OPEN:'已提交意向',COMPLETED:'已成交',CLOSED:'意向已结束'}
const statusLabel=(s:string)=>labels[s] || '暂不可交易'
let context=0, sequence=0
async function load() {
  const n=++sequence, target=id.value, user=viewerId.value
  deleted.value=false; loading.value=true; error.value=''; detail.value=null; intents.value=[]
  try {
    let item:GoodsDetailSummary
    try {item=await getGoodsDetail(target)} catch(e) {
      if(n!==sequence)return
      const status=(e as {response?:{status?:number};status?:number})?.response?.status ?? (e as {status?:number})?.status
      if(status===410){deleted.value=true;return}
      throw e
    }
    if(n!==sequence)return
    detail.value=item
    if(item.sellerId===user){const rows=await listGoodsIntents(target);if(n===sequence)intents.value=rows}
  } catch(e) {if(n===sequence)error.value=e instanceof Error?e.message:'无法加载闲置详情'} finally {if(n===sequence)loading.value=false}
}
function reset(){context++;sequence++;deleted.value=false;detail.value=null;intents.value=[];message.value=commentText.value=error.value='';intentDialog.value=busy.value=loading.value=false}
watch([id,viewerId,()=>auth.token],()=>{reset();void load()},{immediate:true,flush:'sync'}); onBeforeUnmount(reset)
function login(){if(auth.currentUser && auth.token)return true;void router.push({path:'/auth',query:{redirect:route.fullPath}});return false}
function openIntent(){if(login()){message.value='';intentDialog.value=true}}
async function perform(work:(n:number)=>Promise<void>){if(busy.value || !login())return;const n=context; busy.value=true;try{await work(n);if(n===context)await load()}catch(e){if(n===context && e instanceof Error){ElMessage.error(e.message);await load()}}finally{if(n===context)busy.value=false}}
function sendIntent(){return perform(async n=>{await createGoodsIntent(id.value,viewerId.value!,message.value.trim());if(n===context){intentDialog.value=false;ElMessage.success('购买意向已发送')}})}
function favorite(){return perform(async()=>{await favoriteTarget(viewerId.value!,{targetType:'GOODS',targetId:id.value})})}
function comment(){if(commentText.value.trim())return perform(async n=>{await commentTarget(viewerId.value!,{targetType:'GOODS',targetId:id.value,content:commentText.value.trim()});if(n===context)commentText.value=''})}
function chooseBuyer(item:GoodsIntentSummary){if(!owner.value || !detail.value?.canMarkSold || item.status!=='OPEN')return
  return perform(async n=>{await ElMessageBox.confirm(`确认已经与「${item.buyerNickname}」完成线下交接？其他意向将结束。`,'确认实际买家',{type:'warning'});if(n!==context)return;await markGoodsSold(id.value,viewerId.value!,item.buyerId);if(n===context)ElMessage.success('已记录成交')})}
function removeListing(){if(!owner.value||!detail.value?.canOffShelf)return;return perform(async n=>{await ElMessageBox.confirm('下架后不再展示，已有购买意向将关闭。','下架闲置',{type:'warning'});if(n===context)await offShelfGoods(id.value)})}
function deleteListing(){if(!owner.value||!detail.value?.canDelete)return;return perform(async n=>{
  await ElMessageBox.confirm('删除后，订单仍可查看，但不能再打开商品内容或照片。','删除商品？',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'})
  if(n===context)await deleteGoods(id.value)
})}
</script>
<style scoped>.intent-row{display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;border-top:1px solid var(--el-border-color-light);padding:1rem 0}.intent-row p{margin:.3rem 0;overflow-wrap:anywhere}</style>
