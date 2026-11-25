import { createRenderer, defineComponent, nextTick, reactive, type ComponentInternalInstance, getCurrentInstance, h } from 'vue'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import Workspace from './TaskWorkspaceView.vue'
import Hall from './TasksView.vue'
import * as api from '@/api/campushub'

const state = reactive({ token: 'runner', currentUser: { id: 2 } })
const route = reactive({ params: { id: '10' }, query: {}, path: '/tasks' })
const push = vi.fn()
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ push }) }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => state }))
vi.mock('@/api/campushub', () => ({ issueRunnerCompletionCode: vi.fn(), listMyPublishedTasks: vi.fn(), listMyTaskApplications: vi.fn(), getTaskWorkspace: vi.fn(), getTaskEvidenceBlob: vi.fn(), publishRunnerTask: vi.fn(), uploadTaskEvidence: vi.fn(), listTasks: vi.fn(async () => []) }))
vi.mock('element-plus', () => ({ ElMessage: {error:vi.fn(), success:vi.fn(), warning:vi.fn()} }))

// Mount the actual setup/lifecycle with an in-memory Vue host; no DOM or API server required.
const renderer = createRenderer<any, any>({createElement:()=>({}),createText:()=>({}),createComment:()=>({}),insert(){},remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
let unmount: () => void
function mount(view: typeof Workspace) {
  let instance!: ComponentInternalInstance
  const app = renderer.createApp(defineComponent({render:()=> h({...view, render() { instance=getCurrentInstance()!; return null }})}))
  app.provide(Symbol.for('v-scx'), { modules: new Set() }); app.mount({}); unmount=()=>app.unmount(); return (instance as any).setupState
}
function workspace(type: string, status='DELIVERING', mode='PHOTO_AND_CONFIRMATION') {
  return {task:{id:10,verificationMode:mode,workflowStatus:status},publisherContact:{userId:1},runnerContact:{userId:2},events:[],evidence:[{id:5,evidenceType:type,sizeBytes:50}]} as unknown as Awaited<ReturnType<typeof api.getTaskWorkspace>>
}
const settle=()=>new Promise(done=>setTimeout(done,0))
beforeEach(()=>{vi.resetAllMocks();route.params.id='10';state.currentUser={id:2};state.token='runner';vi.stubGlobal('document',Object.assign(new EventTarget(),{hidden:false,visibilityState:'visible'}));vi.mocked(api.getTaskEvidenceBlob).mockResolvedValue(new Blob(['photo']))})
afterEach(()=>{unmount?.();vi.unstubAllGlobals()})
it('enables submit only for server-side delivery evidence; code mode uses pending then redeem',async()=>{
  vi.mocked(api.getTaskWorkspace).mockResolvedValue(workspace('REQUEST_IMAGE'));const s=mount(Workspace);await settle();expect(s.canSubmitDelivery).toBe(false)
  vi.mocked(api.getTaskWorkspace).mockResolvedValue(workspace('DELIVERY_PHOTO'));await s.load();expect(s.canSubmitDelivery).toBe(true)
  vi.mocked(api.getTaskWorkspace).mockResolvedValue(workspace('REQUEST_IMAGE','DELIVERING','COMPLETION_CODE'));await s.load();expect(s.canAdvance('PENDING_CONFIRMATION')).toBe(true);expect(s.canRedeemCode).toBe(false)
})
it('does not attach an old route blob after a task switch or leave contacts when hidden',async()=>{
  let resolve!: (blob:Blob)=>void;vi.mocked(api.getTaskEvidenceBlob).mockImplementationOnce(()=>new Promise(done=>{resolve=done}))
  vi.mocked(api.getTaskWorkspace).mockResolvedValueOnce(workspace('REQUEST_IMAGE')).mockResolvedValueOnce({...workspace('REQUEST_IMAGE'),evidence:[]})
  const s=mount(Workspace);await settle();route.params.id='11';await settle();resolve(new Blob(['old']));await settle();expect(s.evidenceUrls.size).toBe(0)
  Object.assign(document,{hidden:true,visibilityState:'hidden'});document.dispatchEvent(new Event('visibilitychange'));expect(s.workspace).toBeNull()
})
it('retries only missing publisher photos on the already-created task, including double click',async()=>{
  vi.mocked(api.publishRunnerTask).mockResolvedValue({id:23} as any);vi.mocked(api.uploadTaskEvidence).mockResolvedValueOnce({id:1} as any).mockRejectedValueOnce(Error('offline')).mockResolvedValueOnce({id:2} as any)
  const s=mount(Hall);s.openPublishDialog();Object.assign(s.publishForm,{weightKg:'1',originDetail:'a',destinationDetail:'b'});s.publishPhotos=['one','two'].map(key=>({file:new Blob(['x']),clientRequestId:key,url:'blob:'+key,uploaded:false}));await Promise.all([s.publish(),s.publish()]);await s.publish();
  expect(api.publishRunnerTask).toHaveBeenCalledTimes(1);expect(vi.mocked(api.uploadTaskEvidence).mock.calls.map(c=>[c[0],c[3]])).toEqual([[23,'one'],[23,'two'],[23,'two']]);await nextTick()
})
it('keeps both photo groups readable for the publisher and the assigned runner but not mutually editable',async()=>{
  const data=workspace('REQUEST_IMAGE');data.evidence!.push({...data.evidence![0],id:6,evidenceType:'DELIVERY_PHOTO'})
  vi.mocked(api.getTaskWorkspace).mockResolvedValue(data);const s=mount(Workspace);await settle()
  expect([...s.evidenceUrls.keys()]).toEqual([5,6]);expect(s.canUpload('REQUEST_IMAGE')).toBe(false);expect(s.canUpload('DELIVERY_PHOTO')).toBe(true)
  const oldUrls=Array.from(s.evidenceUrls.values());const revoked=vi.spyOn(URL,'revokeObjectURL');state.currentUser={id:1};await settle()
  for(const url of oldUrls)expect(revoked).toHaveBeenCalledWith(url)
  expect([...s.evidenceUrls.keys()]).toEqual([5,6]);expect(s.canUpload('REQUEST_IMAGE')).toBe(true);expect(s.canSubmitDelivery).toBe(false);revoked.mockRestore()
})
it('unknown create outcome blocks repeat create; opening the publish route never submits',async()=>{
  route.query={open:'publish'};vi.mocked(api.publishRunnerTask).mockRejectedValue(Error('connection lost'));const s=mount(Hall)
  expect(s.publisherOpen).toBe(true);expect(api.publishRunnerTask).not.toHaveBeenCalled();Object.assign(s.publishForm,{weightKg:'1',originDetail:'a',destinationDetail:'b'});await s.publish();await s.publish()
  expect(s.uncertain).toBe(true);expect(api.publishRunnerTask).toHaveBeenCalledTimes(1);route.query={}
})
it('drops a late create response and its pending photo queue on session change',async()=>{
  let finish!: (v:any)=>void;vi.mocked(api.publishRunnerTask).mockImplementationOnce(()=>new Promise(done=>{finish=done}));const s=mount(Hall);s.openPublishDialog()
  Object.assign(s.publishForm,{pickupCode:'private',weightKg:'1',originDetail:'a',destinationDetail:'b'});s.publishPhotos=[{clientRequestId:'key',file:new Blob(['x']),url:'blob:example',uploaded:false}]
  const pending=s.publish();await settle();state.token='replacement';expect(s.publishPhotos).toHaveLength(0);expect(s.publishForm.pickupCode).toBe('');finish({id:23});await pending
  expect(s.pendingTaskId).toBeNull();expect(api.uploadTaskEvidence).not.toHaveBeenCalled();expect(push).not.toHaveBeenCalled()
})
it('owner can issue a code during an assignment; hide discards a late code',async()=>{
  state.currentUser={id:1};vi.mocked(api.getTaskWorkspace).mockResolvedValue(workspace('REQUEST_IMAGE','DELIVERING','COMPLETION_CODE'))
  const s=mount(Workspace);await settle();expect(s.canIssueCode).toBe(true);expect(s.canRedeemCode).toBe(false)
  let finish!: (v:{completionCode:string;expiresAt:string})=>void;vi.mocked(api.issueRunnerCompletionCode).mockImplementationOnce(()=>new Promise(done=>{finish=done}))
  const pending=s.issueCode();Object.assign(document,{hidden:true});document.dispatchEvent(new Event('visibilitychange'));finish({completionCode:'654321',expiresAt:'later'});await pending;expect(s.issuedCode).toBeNull()
})
it('workspace history links only owned tasks and accepted or completed runner applications',async()=>{
  vi.mocked(api.listMyPublishedTasks).mockResolvedValue([{id:1,title:'published'}] as any)
  vi.mocked(api.listMyTaskApplications).mockResolvedValue(['ACCEPTED','PENDING','COMPLETED'].map((status,i)=>({taskId:i+2,status,taskTitle:status})) as any)
  const s=mount(Hall);await s.loadMine();expect(s.mine.map((item:{id:number})=>item.id)).toEqual([1,2,4]);state.token='replacement';expect(s.mine).toBeNull()
})
