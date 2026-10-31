<template>
  <GoodsEscrowDetail v-if="platform.financialEnabled" />
  <section v-else class="page-stack" v-loading="loading">
    <div class="page-heading">
      <div>
        <p class="eyebrow">二手市集 · 好物详情</p>
        <h2>{{ detail?.title || '商品详情' }}</h2>
        <p>提交购买意向即可解锁卖家微信或 QQ；当前仅支持双方协商校内当面交接，线上余额担保暂未开放。</p>
      </div>
      <el-button @click="router.push('/goods')">返回二手市集</el-button>
    </div>

    <EmptyState
      v-if="!loading && !detail"
      eyebrow="好物详情"
      title="该商品暂不可查看"
      description="商品可能已经售出、由卖家下架或正在核验中，去市集看看其他好物吧。"
      action-text="返回二手市集"
      @action="router.push('/goods')"
    />

    <div v-if="detail" class="goods-detail">
      <div class="goods-gallery">
        <div v-if="detail.images.length === 0" class="goods-cover goods-cover-large cover-tone-0">
          <div class="goods-cover-art">
            <span class="cover-art-zone">{{ formatCampusZone(detail.campusZone) }}</span>
            <strong class="cover-art-glyph">{{ detail.title.slice(0, 1) || '物' }}</strong>
            <small class="cover-art-sub">{{ detail.conditionLevel || '校友闲置' }}</small>
          </div>
        </div>
        <img v-else :src="detail.images[0].file.storagePath" :alt="detail.title" />
      </div>

      <el-card class="goods-main-panel">
        <div class="goods-price-row goods-price-large">
          <strong class="mono-num">¥{{ detail.price }}</strong>
          <span v-if="detail.originalPrice" class="mono-num">原价 ¥{{ detail.originalPrice }}</span>
        </div>
        <p class="preserve-lines">{{ detail.description }}</p>
        <div class="goods-meta detail-meta">
          <span class="meta-pill">{{ detail.conditionLevel }}</span>
          <span class="meta-pill">{{ formatCampusZone(detail.campusZone) }}</span>
          <span class="meta-pill">📍 {{ detail.tradeLocation }}</span>
          <span class="meta-pill">{{ formatDeliveryMethod(detail.deliveryMethod) }}</span>
        </div>
        <div class="detail-actions">
          <el-button type="primary" :disabled="isOwner" @click="intentDialog = true">我想要（解锁联系方式）</el-button>
          <el-button @click="favorite">收藏 ({{ detail.favoriteCount }})</el-button>
          <el-button @click="reportDialog = true">反馈异常</el-button>
          <el-button v-if="isOwner && detail.status === 'PUBLISHED'" type="warning" @click="markSold">标记已售出</el-button>
        </div>
        <el-alert
          type="info"
          show-icon
          :closable="false"
          title="当前资金功能暂未开放。提交购买意向后，你可以与卖家协商校内公共场所当面交接；请自行核验物品并保留沟通凭证。"
        />
      </el-card>

      <el-card class="seller-panel">
        <template #header>校友卖家与联络方式</template>
        <p><strong>{{ detail.sellerNickname }}</strong> · 校内信用分 <span class="mono-num">{{ detail.sellerCreditScore }}</span></p>
        <p class="hint">为防止公开骚扰，卖家的微信号或 QQ 仅对提交了购买意向的同学展示。</p>
        <el-alert v-if="!detail.contactVisible" title="点击上方「我想要」提交购买意向后，即可在此查看卖家微信 / QQ" type="info" show-icon :closable="false" />
        <el-alert v-else :title="`卖家联系方式：${detail.contactSnapshot || '已开放查看'}`" type="success" show-icon :closable="false" />
      </el-card>

      <el-card class="interaction-panel">
        <template #header>同窗问一问</template>
        <div class="comment-form">
          <el-input v-model="commentText" placeholder="询问实物细节、使用时长或方便交接的时间..." />
          <el-button type="primary" @click="submitComment">留言</el-button>
        </div>
        <el-empty v-if="detail.comments.length === 0" description="还没有人提问，来问第一个问题吧" />
        <div v-for="comment in detail.comments" :key="comment.id" class="comment-item">
          <strong>{{ comment.userNickname }}</strong>
          <span>{{ comment.content }}</span>
        </div>
      </el-card>

      <el-card class="review-list">
        <template #header>历史买家评价</template>
        <div class="comment-form">
          <el-rate v-model="reviewRating" />
          <el-input v-model="reviewText" placeholder="完成交接后，写下对卖家的评价..." />
          <el-button @click="submitReview">发布评价</el-button>
        </div>
        <el-empty v-if="detail.sellerReviews.length === 0" description="暂无历史评价" />
        <div v-for="review in detail.sellerReviews" :key="review.id" class="comment-item">
          <strong>{{ review.reviewerNickname }} · {{ review.rating }} 星</strong>
          <span>{{ review.content }}</span>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="intentDialog" title="发送购买意向" width="420px">
      <el-input v-model="intentMessage" type="textarea" :rows="3" placeholder="告诉卖家你想在哪个校区交接、什么时间方便..." />
      <template #footer>
        <el-button @click="intentDialog = false">取消</el-button>
        <el-button type="primary" @click="createIntent">确认并解锁联系方式</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialog" title="反馈商品问题" width="420px">
      <el-input v-model="reportReason" placeholder="问题简述（如：虚假价格、违禁物品）" />
      <el-input v-model="reportDescription" type="textarea" :rows="3" placeholder="详细情况说明" class="dialog-field" />
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReport">提交反馈</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref } from 'vue'
import { usePlatformStore } from '@/stores/platform'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  commentTarget,
  createGoodsIntent,
  createReview,
  favoriteTarget,
  getGoodsDetail,
  markGoodsSold,
  reportTarget,
  type GoodsDetailSummary,
} from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { formatCampusZone, formatDeliveryMethod } from '@/utils/display'
import EmptyState from '@/components/common/EmptyState.vue'

const platform = usePlatformStore()
const GoodsEscrowDetail = defineAsyncComponent(() => import('./legacy/GoodsEscrowDetail.vue'))
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const detail = ref<GoodsDetailSummary | null>(null)
const loading = ref(false)
const intentDialog = ref(false)
const reportDialog = ref(false)
const intentMessage = ref('')
const commentText = ref('')
const reportReason = ref('信息不实')
const reportDescription = ref('')
const reviewRating = ref(5)
const reviewText = ref('校友很爽快，成色和描述一致！')

const goodsId = computed(() => Number(route.params.id))
const viewerId = computed(() => auth.currentUser?.id)
const isOwner = computed(() => Boolean(detail.value && viewerId.value && detail.value.sellerId === viewerId.value))

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getGoodsDetail(goodsId.value, viewerId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品详情加载失败')
  } finally {
    loading.value = false
  }
}

function requireLogin() {
  if (!viewerId.value) {
    ElMessage.error('请先登录')
    router.push('/auth')
    return false
  }
  return true
}

async function createIntent() {
  if (!requireLogin()) return
  await createGoodsIntent(goodsId.value, viewerId.value!, intentMessage.value)
  intentDialog.value = false
  ElMessage.success('已发送购买意向，卖家联系方式已为你开放')
  await loadDetail()
}

async function favorite() {
  if (!requireLogin()) return
  await favoriteTarget(viewerId.value!, { targetType: 'GOODS', targetId: goodsId.value })
  ElMessage.success('已收入收藏夹')
  await loadDetail()
}

async function submitComment() {
  if (!requireLogin() || !commentText.value.trim()) return
  await commentTarget(viewerId.value!, { targetType: 'GOODS', targetId: goodsId.value, content: commentText.value })
  commentText.value = ''
  await loadDetail()
}

async function submitReport() {
  if (!requireLogin()) return
  await reportTarget(viewerId.value!, { targetType: 'GOODS', targetId: goodsId.value, reason: reportReason.value, description: reportDescription.value })
  reportDialog.value = false
  ElMessage.success('反馈已提交，站务将尽快核查')
}

async function submitReview() {
  if (!requireLogin() || !detail.value) return
  await createReview(viewerId.value!, { targetUserId: detail.value.sellerId, targetType: 'GOODS', targetId: goodsId.value, rating: reviewRating.value, content: reviewText.value })
  ElMessage.success('评价已发布')
  await loadDetail()
}

async function markSold() {
  if (!requireLogin()) return
  await markGoodsSold(goodsId.value, viewerId.value!)
  ElMessage.success('已标记为售出')
  await loadDetail()
}

onMounted(() => { if (!platform.financialEnabled) void loadDetail() })
</script>
