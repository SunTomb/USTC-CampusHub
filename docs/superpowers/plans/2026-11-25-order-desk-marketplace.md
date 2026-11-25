# 我的订单与校园市集 Implementation Plan

> **For agentic workers:** use executing-plans / bounded implementation tasks. Fable only reviews the design and the final working tree. No production writes or WeChat release in this implementation.

**Goal:** 两端都能找回本人发布、参与和完成的业务；小程序补齐二手，公开图片、商品口碑、完成后评价/投诉可实际使用。
**Architecture:** 原业务表仍是事实来源，不另造万能订单或复制交易状态。服务端按 JWT 聚合历史记录，把评分与真实完成记录绑定；公开商品图片与任务私密照片彻底分开。仅追加 V20+；V1–V19、已有用户内容和资金关闭状态不变。
**Tech Stack:** Spring Boot 3 / MySQL 8.4、H2 测试；Vue 3 / Element Plus；原生 WXML/WXSS/JS。

## 调查结果
- `goods/GoodsService.createIntent` 只写 `goods_intents` 并解锁联系方式。现有 `markSold` 可不选买家，只有传买家才尝试完成对应意向。`goods_orders` 另有历史订单/托管，不能把每次“我想要”视为成交或为无买家的旧成交伪造买家。
- `task/RunnerTaskService` 有接单、选择报名、私有工作区、管理员撤单；缺发单人修改/撤下。接单/编辑必须用同一个 `findByIdForUpdate` 锁。`task_applications` 里未选中的报名不等于正式成交。
- `shop` 已有顾客/店主两个预约列表及真实进度；`shops.rating` 为存量数值，不是服务评分均值。`review/ReviewService` 现接受客户端指定目标和被评人，不能作为已购口碑直接使用。`moderation/report_records` 已接治理台，但现有通用写入口必须同步限制订单目标。
- `file/FileController` 是已有元数据与绑定，并非公开图片上传。V18 实物/送达照片另有参与者鉴权，不能拿该接口或其硬盘路径作商品图床。
- 小程序 `pages/pass/index` 将身份、通知和三种历史平铺；请求白名单、包检查及测试都写死旧路由/4 Tab。Web 有零散履约入口，没有“我的书院 → 我的订单”。宣传是发布记录，不提供交易评分。

## 信息结构与交互
- 小程序底部：跑腿 / 二手 / 小店 / 宣传 / 个人；保留原 pass 路由以兼容旧码。个人首页：账号摘要 → 我的订单（全部 ›，下方待处理、进行中、待评价、已结束）→ 身份与通知。每个状态直接深链到独立列表，不把订单类型挤回个人页。
- 列表有两层：`全部 / 跑腿 / 二手 / 预约 / 宣传` 和状态。Web 同名入口与同样的筛选，手机单列；卡片只写标题、我在这单的身份、状态、日期、金额（如有）和必要动作。不照抄“待付款、拼团、物流、专属客服”。没有数据即展示该分类的空状态，无假数字/信用承诺。
- 每笔详情：沿用工作区或业务详情；预约保留接单/拒绝/开始/完成/取消，宣传保留编辑和审核反馈。双方可从**已完成的具体订单**评价或投诉；普通商品页不提供交易投诉，店铺与宣传详情各自保留轻量“举报店铺/举报宣传”。履约中的“遇到问题”仍是任务异常，不冒充订单投诉。

## 任务与验收（以下为实施前基线，实际执行及偏差记在文末）
1. **交易记录读模型** `backend/.../orders/OrderDeskService.java`, `OrderDeskController.java`; `frontend/src/{api,views}`、`miniprogram/src/pages/orders/`。`GET /api/orders/mine?type=ALL&stage=ALL&page=0&size=20` 返回 `{items,total,counts}`。`kind:id` 为稳定键（TASK/GOODS_INTENT/GOODS_ORDER/BOOKING/PROJECT）；`objectId` 跳源业务、`role` 区分双方，`status` 保留原值，`stage` 归一，`canReview/canComplain/reviewed/complained` 来自服务端。详情 `/api/orders/{kind}/{id}` 亦校验参与者；拒绝客户端 userId 冒充。排序 createdAt DESC 并按种类、ID 破同秒平局，分页限额。报名人能看自己的报名结果但不是履约参与者。宣传按审核/展示/结束映射，永不虚构报酬、买家或待评价。设计测试：双方均可读/旁观者 403、两人不同记录、各类型 ID 重叠、完成与待评价交集，旧数据不改写。
2. **二手线下闭环** `goods/GoodsService`, `GoodsRepository`, `GoodsIntent`, 两端 goods 页面。从浏览/详情/发布、提交意向、查看本人发布及收到的意向，到卖家点选实际买家确认成交；锁商品并核对意向、只完成选中条，其他意向结束，不可成交两次。没有买家的历史售出不产生评分资格；历史真实订单单独保留。小程序只走线下交易，不暴露托管、钱包或后台 API。测试：多意向不同结果、无意向不能指定成交、卖家不能自购。
3. **发单人修改/撤下** `task/{RunnerTaskService,RewardTaskController,RewardTask}.java`, Web task workspace / native tasks。`PUT/DELETE /api/tasks/{id}` 从 JWT 获取所有者。仅未接、未有人报名才编辑全套发单字段（同发布校验）；有人报名可撤下并通知未处理申请，已接单或进入终态不改删；软撤下保历史、私密证据及审计，撤下后不能再接。测试：非本人/已接/并发接单，栏目刷新，无敏感码出现在公开详情。
4. **评价/投诉** `orders` + `review` + `moderation`。服务端推导对方、完成状态；每人每单一次，1–5 整数、文字最多 1000。记录受事务/唯一键保护，历史泛评价不追认为已购。`POST /api/orders/{kind}/{id}/review {rating,content}`、`.../complaint {reason,description}`；后者进入同一个 `report_records`，不公开投诉细节。同一对象同一人不能重复提交。老的通用评价/举报路径不能绕过约束，独立店铺/宣传举报校验对象与可见性。`GET /api/service-items/{id}/ratings` 展示顾客评价与数量；一个服务先算 `AVG(真实顾客评分)`，店铺算各有评分商品均值的算术平均（不是所有评价总平均；未评不计、显示暂无评分；仅展示时取一位小数）。例：服务甲 5、5、5；服务乙 1；店铺=3.0，不是4.0。双方对人的评价不混入商品评分。并发/重复/伪造目标/角色和治理可见性均要测试。
5. **可选图片** `backend/.../catalogmedia/`、两端公共图片组件。类型 GOODS、SHOP、SERVICE_ITEM、PROJECT_AD；只给所有者上传/删除，店铺一张封面，其他最多三张展示图，均可跳过。JPEG/PNG 每张 ≤5 MiB，真实解码、尺寸限制、移除元数据，独立存储；读图也校验父对象公开状态，草稿/待审/下架不能因知道图片 ID 就公开；不影响任务证据。公开宣传已审核后换图需要重新审，不能先公开再审核。目标先保存后传图；明确区分“已保存，图片待重试”，不能重建商品。Web 与原生的发布/管理、详情/列表和商品评价均可到达。测试：空图发布、两类店图不串用、无权限/不公开图、格式/数量/重试。
6. **客户端与验证**：`frontend/src/config/navigation.ts`、`router/index.ts`、`views/OrdersView.vue` 和业务页面；`miniprogram/src/{app.json,lib/api.js,lib/request.js,pages}`、`tools/check.cjs`、`tests`。保留登录态切换清理、拒绝绝对 URL、隐私/ICP备案与非金融边界。测试不能只放松原“10 页/4 Tab”断言，明确新 Tab、路由、入口/操作及非法路径。运行 `npm --prefix frontend test`、`npm --prefix frontend run build`、`npm --prefix miniprogram test`、`npm --prefix miniprogram run check`、隔离 Maven `test`。新迁移应在空 H2/MySQL 隔离环境验证；不在正式库创建测试账号/业务。最后 Fable 按需求及缺陷复核两端，记录实测 vs 未验证项；未经另行授权不发布、不更动线上用户数据。

### 第一次 Fable 审核后定案
卖家每件闲置展示一条（含零意向商品），成交时转为唯一胜出意向；发单人每个任务一条，未选中者仅看报名，不进私密工作区。`counts` 随类型、不过滤当前状态；待评价专指本人可评未评，已结束包含已评/关闭。新评价用独立 DESK_* 类型键，旧评价不追认为已购；订单投诉以新增可空唯一键防重，不影响既存重复数据。店铺评分不读写旧缓存字段。图片采用父对象状态校验、作者登录态预览，已审宣传换图重新审核；内外代理均需 6 MiB。新页和五 Tab 明确纳入官方包检查。\n

## 执行结果
- [x] 订单查询、两端入口、二手线下闭环、跑腿管理和完成后反馈。
- [x] 独立的店内商品页与顾客评价，选填展示图片、举报进治理台，历史任务私密证据边界保留。公开展示图使用 V21 单独的事务型 BLOB 表，详细容量与备份约束见验收记录。
- [x] H2 测试、真实 MySQL 8.4 迁移及订单查询、Web/原生构建、浏览器与微信开发者工具隔离验收。
- [x] Fable 最终独立代码验收已完成：**有条件通过，不是发布批准**。已核对订单授权、成交/撤下、评分与投诉；详见 `docs/operations/2026-11-25-order-desk.md`「Fable 最终独立验收」。
- [x] 验收后调整二手规则：订单「查看商品」打开原 ID 的当前商品。卖家可原地修改，删除后订单仍在、详情提示「该商品已删除」；不保留历史商品或图片版本。按新规则回归，不按旧的冻结图片建议执行。
- [ ] 原生快捷图标与实拍上传真机验证、公开图片用途与隐私说明复核，部署/发布另行决定。
