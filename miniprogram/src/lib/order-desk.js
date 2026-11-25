const d=require('./display');
const types={ALL:'全部',TASK:'跑腿',GOODS:'二手',BOOKING:'预约',PROJECT:'宣传'};
const stages={ALL:'全部状态',PENDING:'待处理',ACTIVE:'进行中',REVIEW:'待评价',ENDED:'已结束'};
const roles={PUBLISHER:'我发布的',RUNNER:'我接的',APPLICANT:'我报名的',SELLER:'我卖的',BUYER:'我买的',MERCHANT:'店铺预约',CUSTOMER:'我的预约',AUTHOR:'我发布的'};
const options=o=>Object.keys(o).map(key=>({key,label:o[key]}));
function row(r){const type=r.kind.startsWith('GOODS')?'GOODS':r.kind;const statusLabel=r.role==='APPLICANT'&&['PENDING','APPLIED'].includes(r.status)?'已报名':r.kind==='GOODS_INTENT'&&r.status==='OPEN'?'已联系':r.kind==='GOODS_LISTING'&&r.status==='PUBLISHED'?'在售':({PAID:'已支付',SOLD:'已售出',OPEN:'待交接',CREATED:'已创建',ACCEPTED:r.kind==='BOOKING'?'已接受预约':'已接单'})[r.status]||d.status(r.status);return {...r,key:r.kind+':'+r.id,kindLabel:types[type],goodsUrl:type==='GOODS'?destination(r):'',roleLabel:roles[r.role],statusLabel,amountLabel:r.amount==null?'':d.money(r.amount),dateLabel:d.dateTimeText(r.createdAt)};}
function destination(r,b){if(r.kind==='TASK')return r.role==='APPLICANT'?'':'/pages/tasks/detail?id='+r.objectId;if(r.kind.startsWith('GOODS'))return '/pages/goods/detail?id='+r.objectId;if(r.kind==='PROJECT')return '/pages/showcase/detail?id='+r.objectId;return b?'/pages/shops/detail?id='+b.shopId:'';}
module.exports={types,stages,options,row,destination};
