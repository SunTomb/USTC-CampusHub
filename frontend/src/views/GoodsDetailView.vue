<template>
  <section class="page-stack" v-loading="loading">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Goods Detail</p>
        <h2>{{ detail?.title || '商品详情' }}</h2>
        <p>商品联系方式按购买意向展示，交易本金由买卖双方线下自行协商。</p>
      </div>
      <el-button @click="router.push('/goods')">返回市场</el-button>
    </div>

    <el-empty v-if="!loading && !detail" description="商品不存在" />

    <div v-if="detail" class="goods-detail">
      <div class="goods-gallery">
        <div v-if="detail.images.length === 0" class="goods-cover goods-cover-large">CampusHub</div>
        <img v-else :src="detail.images[0].file.storagePath" :alt="detail.title" />
      </div>

      <el-card class="goods-main-panel">
        <div class="goods-price-row goods-price-large">
          <strong>¥{{ detail.price }}</strong>
          <span v-if="detail.originalPrice">原价 ¥{{ detail.originalPrice }}</span>
        </div>
        <p>{{ detail.description }}</p>
        <div class="goods-meta detail-meta">
          <span>{{ detail.conditionLevel }}</span>
          <span>{{ zoneLabel(detail.campusZone) }}</span>
          <span>{{ detail.tradeLocation }}</span>
          <span>{{ detail.deliveryMethod }}</span>
        </div>
        <div class="detail-actions">
          <el-button type="primary" :disabled="isOwner" @click="intentDialog = true">我想要</el-button>
          <el-button @click="favorite">收藏 {{ detail.favoriteCount }}</el-button>
          <el-button @click="reportDialog = true">举报</el-button>
          <el-button v-if="isOwner && detail.status === 'PUBLISHED'" type="warning" @click="markSold">标记售出</el-button>
        </div>
      </el-card>

      <el-card class="seller-panel">
        <template #header>卖家与联系方式</template>
        <p>{{ detail.sellerNickname }} · 信用 {{ detail.sellerCreditScore }}</p>
        <el-alert v-if="!detail.contactVisible" title="提交购买意向后可查看微信/QQ 联系方式" type="info" show-icon />
        <el-alert v-else :title="detail.contactSnapshot || '联系方式已开放'" type="success" show-icon />
      </el-card>

      <el-card class="interaction-panel">
        <template #header>留言评论</template>
        <div class="comment-form">
          <el-input v-model="commentText" placeholder="询问成色、交易时间等" />
          <el-button type="primary" @click="submitComment">发送</el-button>
        </div>
        <el-empty v-if="detail.comments.length === 0" description="暂无评论" />
        <div v-for="comment in detail.comments" :key="comment.id" class="comment-item">
          <strong>{{ comment.userNickname }}</strong>
          <span>{{ comment.content }}</span>
        </div>
      </el-card>

      <el-card class="review-list">
        <template #header>卖家评价</template>
        <div class="comment-form">
          <el-rate v-model="reviewRating" />
          <el-input v-model="reviewText" placeholder="交易后评价卖家" />
          <el-button @click="submitReview">评价</el-button>
        </div>
        <el-empty v-if="detail.sellerReviews.length === 0" description="暂无评价" />
        <div v-for="review in detail.sellerReviews" :key="review.id" class="comment-item">
          <strong>{{ review.reviewerNickname }} · {{ review.rating }} 星</strong>
          <span>{{ review.content }}</span>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="intentDialog" title="提交购买意向" width="420px">
      <el-input v-model="intentMessage" type="textarea" :rows="3" placeholder="说明想交易的时间或问题" />
      <template #footer>
        <el-button @click="intentDialog = false">取消</el-button>
        <el-button type="primary" @click="createIntent">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialog" title="举报商品" width="420px">
      <el-input v-model="reportReason" placeholder="举报原因" />
      <el-input v-model="reportDescription" type="textarea" :rows="3" placeholder="补充说明" class="dialog-field" />
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReport">举报</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { commentTarget, createGoodsIntent, createReview, favoriteTarget, getGoodsDetail, markGoodsSold, reportTarget, type GoodsDetailSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const detail = ref<GoodsDetailSummary | null>(null)
const loading = ref(false)
const intentDialog = ref(false)
const reportDialog = ref(false)
const intentMessage = ref('')
const commentText = ref('')
const reportReason = ref('虚假信息')
const reportDescription = ref('')
const reviewRating = ref(5)
const reviewText = ref('交易顺利')

const goodsId = computed(() => Number(route.params.id))
const viewerId = computed(() => auth.currentUser?.id)
const isOwner = computed(() => Boolean(detail.value && viewerId.value && detail.value.sellerId === viewerId.value))

const zoneMap: Record<string, string> = { CENTRAL: '中校区', WEST: '西校区', EAST: '东校区', NORTH: '北校区', SOUTH: '南校区', HIGH_TECH: '高新校区', ADVANCED_RESEARCH_INSTITUTE: '先研院', SCIENCE_ISLAND: '科学岛', OTHER: '其他' }
function zoneLabel(zone: string) { return zoneMap[zone] ?? zone }

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
  ElMessage.success('已提交购买意向，联系方式已开放')
  await loadDetail()
}

async function favorite() {
  if (!requireLogin()) return
  await favoriteTarget(viewerId.value!, { targetType: 'GOODS', targetId: goodsId.value })
  ElMessage.success('已收藏')
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
  ElMessage.success('举报已提交')
}

async function submitReview() {
  if (!requireLogin() || !detail.value) return
  await createReview(viewerId.value!, { targetUserId: detail.value.sellerId, targetType: 'GOODS', targetId: goodsId.value, rating: reviewRating.value, content: reviewText.value })
  ElMessage.success('评价已提交')
  await loadDetail()
}

async function markSold() {
  if (!requireLogin()) return
  await markGoodsSold(goodsId.value, viewerId.value!)
  ElMessage.success('已标记售出')
  await loadDetail()
}

onMounted(loadDetail)
</script>
