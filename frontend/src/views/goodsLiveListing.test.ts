import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { createRenderer, defineComponent, getCurrentInstance, h, reactive } from 'vue'
import GoodsDetail from './GoodsDetailView.vue'
import {getGoodsDetail,listGoodsIntents} from '@/api/campushub'
const route=reactive({params:{id:'12'}}),auth=reactive({token:'fixture',currentUser:{id:2}})
vi.mock('vue-router',()=>({useRoute:()=>route,useRouter:()=>({push:vi.fn()})}))
vi.mock('@/stores/auth',()=>({useAuthStore:()=>auth}))
vi.mock('@/stores/platform',()=>({usePlatformStore:()=>({financialEnabled:false})}))
vi.mock('@/api/campushub',()=>({getGoodsDetail:vi.fn(),listGoodsIntents:vi.fn()}))
const renderer=createRenderer<any,any>({createElement:()=>({}),createText:()=>({}),createComment:()=>({}),insert(){},remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
let state:any, cleanup:()=>void;const settle=()=>new Promise(r=>setTimeout(r,0));const listing=(title:string)=>({id:12,sellerId:1,status:'SOLD',title} as Awaited<ReturnType<typeof getGoodsDetail>>)
function mount(){const app=renderer.createApp(defineComponent({render:()=>h({...GoodsDetail,render(){state=(getCurrentInstance() as any).setupState;return null}})}));app.provide(Symbol.for('v-scx'),{modules:new Set()});app.mount({});cleanup=()=>app.unmount()}
beforeEach(()=>{vi.resetAllMocks();route.params.id='12'});afterEach(()=>cleanup?.())
it('reloads current goods by product ID without treating 404 or transport errors as deletion',async()=>{
 vi.mocked(getGoodsDetail).mockResolvedValueOnce(listing('最新标题'));mount();await settle();expect(getGoodsDetail).toHaveBeenCalledWith(12);expect(state.detail.title).toBe('最新标题');
 for(const e of [Object.assign(Error('不可查看'),{response:{status:404}}),Error('Network Error')]){vi.mocked(getGoodsDetail).mockRejectedValueOnce(e);await state.load();expect(state.deleted).toBe(false);expect(state.error).toBeTruthy()}
 vi.mocked(getGoodsDetail).mockRejectedValueOnce(Object.assign(Error('Gone'),{response:{status:410}}));await state.load();expect(state.deleted).toBe(true);expect(state.detail).toBeNull();expect(state.error).toBe('');
});

it('an auxiliary seller request cannot turn an existing listing into a deleted one',async()=>{
 vi.mocked(getGoodsDetail).mockResolvedValue({...listing('在售'),sellerId:2});vi.mocked(listGoodsIntents).mockRejectedValue(Object.assign(Error('无法加载意向'),{response:{status:410}}));
 mount();await settle();expect(state.deleted).toBe(false);expect(state.detail.title).toBe('在售');expect(state.error).toBeTruthy()
})
