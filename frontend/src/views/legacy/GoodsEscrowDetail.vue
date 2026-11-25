<template>
  <section class="escrow-actions">
    <el-button v-if="available && !owner && !currentEscrowOrder" :disabled="busy" @click="startEscrowTrade">发起线上余额担保</el-button>
        <div v-if="currentEscrowOrder" class="info-card escrow-card">
          <div class="panel-topline">
            <strong>担保订单 <span class="mono-num">{{ currentEscrowOrder.orderNo }}</span></strong>
            <el-tag :type="statusTone(currentEscrowOrder.escrowStatus)">{{ formatStatus(currentEscrowOrder.escrowStatus) }}</el-tag>
          </div>
          <p>
            商品金额：<span class="mono-num">¥{{ currentEscrowOrder.escrowAmount }}</span> ·
            服务费：<span class="mono-num">¥{{ currentEscrowOrder.platformServiceFee }}</span>
          </p>
          <div class="payment-actions">
            <el-button v-if="currentEscrowOrder.escrowStatus === 'PENDING_FREEZE'" type="primary" size="small" :disabled="busy" @click="freezeEscrowTrade">
              冻结担保资金
            </el-button>
            <el-button v-if="currentEscrowOrder.escrowStatus === 'FROZEN'" type="success" size="small" :disabled="busy" @click="confirmEscrowTrade">
              确认收货并放款
            </el-button>
          </div>
        </div>
  </section>
</template>
<script setup lang="ts">
import { ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createGoodsEscrowOrder, freezeGoodsEscrow, confirmGoodsEscrow, type GoodsOrderSummary } from '@/api/campushub'
import { formatStatus, statusTone } from '@/utils/display'
import { useAuthStore } from '@/stores/auth'
const props=defineProps<{goodsId:number;owner:boolean;available:boolean}>(), emit=defineEmits<{changed:[]}>()
const auth=useAuthStore(), router=useRouter(), currentEscrowOrder=ref<GoodsOrderSummary|null>(null), busy=ref(false)
let alive=true; onBeforeUnmount(()=>{alive=false})
async function run(work:(id:number)=>Promise<GoodsOrderSummary>, notice:string){
  if(!auth.currentUser){void router.push('/auth');return} if(busy.value)return
  const token=auth.token;busy.value=true
  try{const order=await work(auth.currentUser.id);if(alive && auth.token===token){currentEscrowOrder.value=order;ElMessage.success(notice);emit('changed')}}
  catch(e){if(alive && token===auth.token)ElMessage.error(e instanceof Error?e.message:'操作未完成')}
  finally{if(alive)busy.value=false}
}
function startEscrowTrade(){if(props.available && !props.owner)return run(id=>createGoodsEscrowOrder(props.goodsId,id),'担保订单已创建')}
function freezeEscrowTrade(){const order=currentEscrowOrder.value;if(order?.escrowStatus==='PENDING_FREEZE')return run(id=>freezeGoodsEscrow(order.id,id),'担保资金已冻结')}
function confirmEscrowTrade(){const order=currentEscrowOrder.value;if(order?.escrowStatus==='FROZEN')return run(id=>confirmGoodsEscrow(order.id,id),'交易确认完成')}
</script>
