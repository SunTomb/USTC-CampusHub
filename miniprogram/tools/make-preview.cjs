const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const destination = path.resolve(root, '../.claude/verification/native-miniprogram/offline-preview');
const marker = path.join(destination, '.woke-offline-preview');
if (fs.existsSync(destination) && !fs.existsSync(marker)) throw new Error('Refusing to replace an unrelated directory');
fs.mkdirSync(destination, { recursive: true });
fs.writeFileSync(marker, 'Generated offline demonstration; no external API requests.\n');
function copyTree(source, target) {
  fs.mkdirSync(target, { recursive: true });
  for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
    if (entry.isSymbolicLink()) throw new Error('Unexpected symlink in native source');
    const from = path.join(source, entry.name), to = path.join(target, entry.name);
    if (entry.isDirectory()) copyTree(from, to);
    else if (entry.isFile()) fs.writeFileSync(to, fs.readFileSync(from));
  }
}
copyTree(path.join(root, 'src'), path.join(destination, 'src'));
const project = JSON.parse(fs.readFileSync(path.join(root, 'project.config.json'), 'utf8'));
project.projectname = 'woke-offline-preview'; project.description = '蜗壳同路离线样例，非真实业务';
fs.writeFileSync(path.join(destination, 'project.config.json'), JSON.stringify(project, null, 2));
const appPath = path.join(destination, 'src/app.js');
let app = fs.readFileSync(appPath, 'utf8');
const original = 'request: options => wx.request(options)';
if (!app.includes(original)) throw new Error('Request injection anchor changed; preview not safe to generate');
app = ["const offline = require('./offline-fixture');", app.replace(original, 'request: offline.request')].join('\n');
app = app.replace('this.config = configFor(wx);', "this.config = configFor(wx); this.config.baseUrl = 'https://offline-preview.invalid/api'; this.config.tokenKey = 'woke_offline_preview_token'; this.resetOfflineSample = offline.reset;");
fs.writeFileSync(appPath, app);
const appJsonPath = path.join(destination, 'src/app.json');
const config = JSON.parse(fs.readFileSync(appJsonPath, 'utf8'));
config.window.navigationBarTitleText = '离线样例 · 非真实业务';
fs.writeFileSync(appJsonPath, JSON.stringify(config, null, 2));
for (const route of config.pages) {
  const page = path.join(destination, 'src', route + '.json');
  const options = JSON.parse(fs.readFileSync(page, 'utf8'));
  options.navigationBarTitleText = '样例 · ' + options.navigationBarTitleText;
  fs.writeFileSync(page, JSON.stringify(options, null, 2));
}
const fixture = `
const user={id:900001,username:'offline_student',nickname:'离线样例同学',email:'sample@example.edu.cn',wechatContact:'offline_sample',qqContact:null,creditScore:100,roles:['ROLE_STUDENT']};
const tasks=Array.from({length:4},(_,i)=>({id:i+1,title:['顺路带两本教材到西区','帮忙代取一份食堂午餐','取一份打印资料','搬一箱轻便物品到楼下'][i],description:'离线演示数据，不对应真实同学或订单。',rewardAmount:[6,4,8,10][i],status:'PUBLISHED',workflowStatus:'PUBLISHED',verificationMode:i%2?'PHOTO_AND_CONFIRMATION':'COMPLETION_CODE',acceptanceMode:i%2?'APPLICATION':'GRAB',originZone:['CENTRAL','EAST','WEST','HIGH_TECH'][i],destinationZone:['WEST','CENTRAL','EAST','CENTRAL'][i],originDetail:'公共取件点',destinationDetail:'约定交接点',deadline:'2099-11-11T18:30:00',publisherId:900010+i,publisherNickname:'示例同学',acceptedApplicationId:null}));
const shops=[{id:1,name:'光影小屋',description:'人像约拍与证件照精修',ownerId:900020,ownerNickname:'示例主理人',campusZone:'CENTRAL',openingHours:'周末可约',serviceArea:'中校区',status:'APPROVED',rating:5,ownerCreditScore:100},{id:2,name:'同学修理桌',description:'电脑清灰与数码维护',ownerId:900021,ownerNickname:'示例主理人',campusZone:'WEST',openingHours:'时间协商',serviceArea:'西校区',status:'APPROVED',rating:5,ownerCreditScore:100}];
const services=[{id:1,shopId:1,title:'证件照精修',category:'PHOTO',description:'仅用于离线界面演示。',price:19,priceUnit:'次',durationMinutes:30,status:'PUBLISHED'},{id:2,shopId:2,title:'笔记本清灰',category:'REPAIR',description:'仅用于离线界面演示。',price:35,priceUnit:'台',durationMinutes:60,status:'PUBLISHED'}];
const projects=[{id:1,publisherId:900030,publisherNickname:'示例同学',title:'一起做个校园小工具',summary:'寻找前端与设计伙伴',description:'这是离线界面样例，不是正在招募的真实项目。',adType:'TEAM_UP',tags:'前端,设计,校园',campusZone:'CENTRAL',status:'APPROVED',featured:true,expiresAt:null,contactVisibility:'LOGIN_ONLY',contactVisible:false,contactInfo:null,attachments:[],favoriteCount:0,commentCount:0,favorited:false},{id:2,publisherId:900031,publisherNickname:'示例同学',title:'把秋天留在校园里',summary:'同学的摄影作品',description:'离线展板样例。',adType:'PORTFOLIO',tags:'摄影,作品',campusZone:'WEST',status:'APPROVED',featured:false,contactVisibility:'HIDDEN',contactVisible:false,contactInfo:null,attachments:[],favoriteCount:0,commentCount:0,favorited:false}];
const applications=[],bookings=[],notes=[],roleApplications=[],completionCodes={};let logged=false,nextId=100;
const seed=JSON.stringify({user,tasks,projects});
module.exports.reset=function(){const data=JSON.parse(seed);Object.assign(user,data.user);tasks.splice(0,tasks.length,...data.tasks);projects.splice(0,projects.length,...data.projects);applications.length=0;bookings.length=0;notes.length=0;roleApplications.length=0;Object.keys(completionCodes).forEach(key=>delete completionCodes[key]);logged=false;nextId=100;};
function bookingRow(b){return {kind:'BOOKING',id:b.id,objectId:b.serviceItemId,title:b.serviceItemTitle,role:'CUSTOMER',stage:'PENDING',status:b.status,createdAt:b.appointmentTime,interested:0,amount:b.amount,canReview:false,reviewed:false,canComplain:false,complained:false};}
function resolve(method,path,body,query){
 const id=Number(path.split('/')[2]);
 if(path==='/auth/login'){logged=true;return{accessToken:'offline-not-a-real-token',user:{...user,roles:[...user.roles]}};}
 if(path==='/auth/me'){if(!logged)throw{status:401,message:'请使用离线演示账号登录'};return{...user,roles:[...user.roles]};}
 if(path==='/orders/mine'){const type=query.type||'ALL',stage=query.stage||'ALL';const rows=(type==='ALL'||type==='BOOKING'?bookings:[]).map(bookingRow);const items=rows.filter(r=>stage==='ALL'||r.stage===stage);return {items,total:items.length,counts:{ALL:rows.length,PENDING:rows.length,ACTIVE:0,REVIEW:0,ENDED:0}};}
 if(path.startsWith('/orders/BOOKING/')){const b=bookings.find(x=>x.id===Number(path.split('/')[3]));if(b)return bookingRow(b);}
 if(path.startsWith('/service-orders/')&&Number.isInteger(id)){const b=bookings.find(x=>x.id===id);if(b)return b;}
 if(path.startsWith('/catalog-media/')&&method==='GET')return [];
 if(path==='/goods'&&method==='GET')return [];
 if(path==='/categories')return [{id:1,name:'教材',enabled:true}];
 if(path==='/platform/features')return{financialEnabled:false};
 if(path.startsWith('/platform/protocols/'))return{version:'离线样例',title:'离线演示协议（不是正式协议）',content:('此文本仅供测试阅读和勾选交互，不具有真实平台协议含义。\\n').repeat(25),effectiveAt:'2026-11-11T00:00:00'};
 if(path==='/tasks'&&method==='GET')return tasks.filter(t=>t.workflowStatus==='PUBLISHED');
 if(path==='/tasks/mine')return tasks.filter(t=>t.publisherId===user.id);
 if(path==='/tasks'&&method==='POST'){const t={...body,id:nextId++,publisherId:user.id,publisherNickname:user.nickname,status:'PUBLISHED',workflowStatus:'PUBLISHED'};tasks.unshift(t);return t;}
 if(path.endsWith('/workspace')){const t=tasks.find(x=>x.id===id);return{task:t,publisherContact:{userId:t.publisherId,nickname:t.publisherNickname,wechatContact:'offline_publisher',qqContact:null},runnerContact:{userId:user.id,nickname:user.nickname,wechatContact:user.wechatContact,qqContact:user.qqContact},events:[{id:1,actorId:t.publisherId,actorNickname:t.publisherNickname,eventType:'CREATED',note:'任务已发布',createdAt:'2099-11-11T10:00:00'}]};}
 if(path.endsWith('/completion-code')&&method==='POST'){const t=tasks.find(x=>x.id===id);t.workflowStatus='PENDING_CONFIRMATION';completionCodes[id]='654321';return{completionCode:'654321',expiresAt:'2099-11-11T18:00:00'};}
 if(path.endsWith('/complete-code')&&method==='POST'){const t=tasks.find(x=>x.id===id);if(body.completionCode!==completionCodes[id])throw{status:400,message:'完成码不正确'};t.workflowStatus='COMPLETED';return t;}
 if(/^\\/tasks\\/\\d+$/.test(path))return tasks.find(t=>t.id===id);
 if(path.startsWith('/task-applications/applicant/'))return applications;
 if(path.startsWith('/task-applications/task/'))return [];
 if(/\\/tasks\\/\\d+\\/(grab|applications)$/.test(path)){const t=tasks.find(t=>t.id===id);const a={id:nextId++,taskId:id,taskTitle:t.title,applicantId:user.id,applicantNickname:user.nickname,status:path.endsWith('grab')?'ACCEPTED':'PENDING'};applications.push(a);if(a.status==='ACCEPTED'){t.acceptedApplicationId=a.id;t.workflowStatus='ACCEPTED';}return path.endsWith('grab')?t:a;}
 if(path.includes('/workflow/')){const t=tasks.find(t=>t.id===id);t.workflowStatus=path.split('/').pop();return t;}
 if(path==='/shops')return shops;
 if(/^\\/shops\\/\\d+$/.test(path)){const s=shops.find(x=>x.id===id);const visible=bookings.some(b=>b.shopId===id);return{...s,serviceItems:services.filter(x=>x.shopId===id),contactVisible:visible,contactSnapshot:visible?'微信:offline_sample':null};}
 if(path==='/service-items')return services;
 if(/\\/service-items\\/\\d+\\/orders/.test(path)){const s=services.find(x=>x.id===id);const b={id:nextId++,serviceItemId:id,serviceItemTitle:s.title,shopId:s.shopId,shopName:shops.find(x=>x.id===s.shopId).name,customerId:user.id,providerId:900020,appointmentTime:body.appointmentTime,amount:s.price,status:'REQUESTED',contactSnapshot:'微信:offline_sample',actions:[]};bookings.push(b);return b;}
 if(path.startsWith('/service-orders/customer/'))return bookings;
 if(path.startsWith('/service-orders/provider/'))return [];
 if(path==='/project-ads'&&method==='GET')return projects;
 if(path==='/project-ads/featured')return projects.filter(p=>p.featured);
 if(path.startsWith('/project-ads/users/'))return projects.filter(p=>p.publisherId===user.id);
 if(/^\\/project-ads\\/\\d+$/.test(path)&&method==='GET')return projects.find(p=>p.id===id);
 if(path==='/project-ads'&&method==='POST'){const p={...body,id:nextId++,publisherId:user.id,publisherNickname:user.nickname,status:'PENDING_REVIEW',contactVisible:true,contactInfo:body.contactInfo,attachments:[]};projects.push(p);return p;}
 if(path==='/interactions/favorites'){const p=projects.find(x=>x.id===body.targetId);p.favorited=method!=='DELETE';p.favoriteCount=p.favorited?1:0;return null;}
 if(path.endsWith('/comments')&&method==='GET')return notes;
 if(path==='/interactions/comments'){const n={id:nextId++,content:body.content,status:'VISIBLE',userNickname:user.nickname};notes.push(n);return n;}
 if(path.startsWith('/identity/users/')&&method==='GET')return roleApplications;
 if(path.startsWith('/identity/users/')&&method==='POST'){const existing=roleApplications.find(a=>a.roleType===body.roleType);if(existing)return existing;const review=body.roleType==='SHOP_MERCHANT';if(!review&&!user.roles.includes('ROLE_'+body.roleType))user.roles.push('ROLE_'+body.roleType);const a={id:nextId++,userId:user.id,roleType:body.roleType,reviewStatus:review?'PENDING_REVIEW':'APPROVED',agreementVersion:body.agreementVersion,agreementStatus:'CONSENTED'};roleApplications.push(a);return a;}
 if(path==='/users/'+user.id)return user;
 if(path==='/users/me/contacts'){if(!String(body.wechatContact||'').trim()&&!String(body.qqContact||'').trim())throw{status:400,message:'请至少填写一种联系方式'};Object.assign(user,{wechatContact:String(body.wechatContact||'').trim(),qqContact:String(body.qqContact||'').trim()});return user;}
 if(path==='/users/me/profile'){Object.assign(user,body);return user;}
 if(path==='/notifications/read-all'&&method==='POST')return {updatedCount:0};
 if(path.endsWith('/notifications'))return [];
 throw{status:400,message:'这个操作尚未包含在离线样例中，不会请求真实服务器'};
}
module.exports.request=function(options){setTimeout(()=>{try{const path=options.url.split('/api')[1].split('?')[0];const query=Object.fromEntries((options.url.split('?')[1]||'').split('&').filter(Boolean).map(p=>p.split('=')));const data=resolve(options.method,path,options.data||{},query);options.success({statusCode:200,data:{success:true,data}});}catch(e){options.success({statusCode:e.status||400,data:{success:false,message:e.message||'离线样例操作未完成'}});}},120);};
`;
fs.writeFileSync(path.join(destination, 'src/offline-fixture.js'), fixture);
fs.writeFileSync(path.join(destination, 'README.txt'), '离线样例项目，只供界面预览。账号用户名填任意非空文本、密码填任意非空文本；无真实登录。所有 API 在内存中模拟，重编译清空样例状态，绝不上传/发布。\n');
console.log('Offline-only project generated at: ' + destination);
console.log('Import this separate preview with the authorized AppID; all business APIs remain offline fixtures. Never upload or publish it.');
