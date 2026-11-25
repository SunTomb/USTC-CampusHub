import {describe,it,expect} from 'vitest'
import {actions,stageParam,typeParam,goodsDestination} from './orderDesk'
import {navItems} from '@/config/navigation'
describe('我的订单',()=>{
 it('is a signed-in personal entry and accepts only known filters',()=>{expect(navItems.find(i=>i.path==='/orders')).toMatchObject({label:'我的订单',group:'account',requiresAuth:true});expect(stageParam('REVIEW')).toBe('REVIEW');expect(typeParam('GOODS')).toBe('GOODS');expect(typeParam('constructor')).toBe('ALL')})
 it('offers only actions applicable to the actual booking state and side',()=>{expect(actions('MERCHANT','REQUESTED').map(x=>x.key)).toEqual(['accept','reject','cancel']);expect(actions('CUSTOMER','REQUESTED').map(x=>x.key)).toEqual(['cancel']);expect(actions('CUSTOMER','COMPLETED')).toEqual([])})
})

it('links secondhand records to the underlying current listing, not the transaction ID',()=>{
 for(const kind of ['GOODS_INTENT','GOODS_ORDER','GOODS_LISTING']) expect(goodsDestination({kind,objectId:27})).toBe('/goods/27');
 expect(goodsDestination({kind:'BOOKING',objectId:27})).toBe('')
})
