import {getApi,postApi} from './client'
export const types = {ALL:'全部',TASK:'跑腿',GOODS:'二手',BOOKING:'预约',PROJECT:'宣传'} as const
export const stages = {ALL:'全部状态',PENDING:'待处理',ACTIVE:'进行中',REVIEW:'待评价',ENDED:'已结束'} as const
export type Type = keyof typeof types; export type Stage = keyof typeof stages
export interface DeskRow {kind:string;id:number;objectId:number;title:string;role:string;stage:Stage;status:string;amount:number|null;createdAt:string;interested:number;canReview:boolean;reviewed:boolean;canComplain:boolean;complained:boolean}
export interface DeskPage {items:DeskRow[];counts:Record<Stage,number>;total:number}
export interface ItemRatings {score:number|null;count:number;entries:{nickname:string;rating:number;content:string|null;createdAt:string}[]}
export const roleLabels:Record<string,string>={PUBLISHER:'我发布的',RUNNER:'我接的',APPLICANT:'我报名的',SELLER:'我卖的',BUYER:'我买的',CUSTOMER:'我的预约',MERCHANT:'店铺预约',AUTHOR:'我发布的'}
export function stageParam(s:unknown): Stage {return typeof s==='string' && Object.prototype.hasOwnProperty.call(stages,s)?s as Stage:'ALL'}
export function typeParam(s:unknown): Type {return typeof s==='string' && Object.prototype.hasOwnProperty.call(types,s)?s as Type:'ALL'}
export const listOrders=(type:Type,stage:Stage,page:number)=>getApi<DeskPage>(`/orders/mine?type=${type}&stage=${stage}&page=${page}&size=20`)
export const loadRow=(r:DeskRow)=>getApi<DeskRow>(`/orders/${r.kind}/${r.id}`)
export const submitRating=(r:DeskRow,rating:number,content:string)=>postApi(`/orders/${r.kind}/${r.id}/review`,{rating,content})
export const submitComplaint=(r:DeskRow,reason:string,description:string)=>postApi(`/orders/${r.kind}/${r.id}/complaint`,{reason,description})
export const itemRatings=(id:number,page=0)=>getApi<ItemRatings>(`/service-items/${id}/ratings?page=${page}`)
export const bookingDetail=(id:number)=>getApi<import('./campushub').ServiceOrderSummary>(`/service-orders/${id}`)
export const bookingAction=(id:number,action:string,cancelReason?:string)=>postApi(`/service-orders/${id}/${action}`,{cancelReason})
export function actions(role:string,status:string){if(!['MERCHANT','CUSTOMER'].includes(role))return [];const a:{key:string;label:string}[]=[];if(role==='MERCHANT'&&status==='REQUESTED')a.push({key:'accept',label:'接受预约'},{key:'reject',label:'拒绝预约'});if(role==='MERCHANT'&&status==='ACCEPTED')a.push({key:'start',label:'开始服务'});if(status==='IN_SERVICE')a.push({key:'complete',label:'确认完成'});if(['REQUESTED','ACCEPTED'].includes(status))a.push({key:'cancel',label:'取消预约'});return a}

export function goodsDestination(row: Pick<DeskRow,'kind'|'objectId'>) {
  return ['GOODS_INTENT','GOODS_ORDER','GOODS_LISTING'].includes(row.kind) ? `/goods/${row.objectId}` : ''
}
