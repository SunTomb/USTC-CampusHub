# CampusHub Local Implementation

“校集 CampusHub”是校园二手交易与学生微服务平台的本地实现原型，用于数据库系统课程设计后续开发演示。

## 模块范围

- 二手商品交易
- 悬赏任务与接单
- 学生技能店铺与预约
- 项目广告、收藏、评论、评价
- 文件资源与业务绑定
- 内容审核、举报、违规与信用分
- 钱包流水与服务费记录
- 本地支付抽象，预留 API-Transfer-Station 支付中心接入点

## Phase 1 真实平台升级

CampusHub 当前路线从课程演示原型升级为真实校园服务平台，第一阶段优先跑通校园跑腿/代取履约闭环。

已落地的 Phase 1 基础能力：

- 注册要求提供微信或 QQ 联系方式，至少填写一种，用于交易达成后的私下沟通；
- 身份保证金：跑腿接单者 5 元、二手发布者 10 元、店铺商家 20 元；
- 跑腿接单者和二手发布者保证金后自动开通，店铺商家进入人工审核；
- 跑腿任务支持发布者选择抢单模式或申请模式；
- 位置先按校区枚举采集：中校区、西校区、东校区、北校区、南校区、高新校区、先研院、科学岛、其他；
- 跑腿任务保存起终点校区、地点文字详情、接单模式、履约状态、完成确认方式、事件日志和异常记录；
- 第一阶段通知只做站内通知，覆盖任务接单、申请、状态变更、异常和身份保证金结果；
- 运营后台提供任务监控、异常任务、身份保证金和基础指标入口；
- 前端优先做好响应式 Web，API 保持可复用，为后续微信小程序预留空间。

资金边界：CampusHub 只收平台服务费和身份保证金，不托管交易本金，不做逐单保证金冻结。生产支付可继续通过 API-Transfer-Station 支付中心完成，CampusHub 不读取、不复制、不保存支付宝密钥正文。

## Phase 2 二手交易升级

Phase 2 将二手模块从只读列表升级为校园二手信息发布与可信联系闭环。

已落地能力：

- 二手商品发布接口校验 `GOODS_PUBLISHER` 身份，发布者需先支付 10 元保证金并自动开通；
- 商品市场改为卡片流，展示封面、价格、原价、成色、校区、交易地点、卖家信用和浏览数；
- 新增商品详情页，展示图片、卖家信用、留言、评价、收藏数和联系方式提示；
- 买家提交购买意向后，系统保存卖家微信/QQ 联系方式快照并开放给该买家查看；
- 卖家本人始终可查看自己的联系方式，并可下架或标记商品售出；
- 文件绑定支持 `target_type = GOODS` 的商品图片，最多 9 张；
- 收藏、评论、举报、评价接口支持二手商品目标；
- 二手服务费配置默认关闭，可通过 `CAMPUSHUB_SECONDHAND_SERVICE_FEE_ENABLED` 和 `CAMPUSHUB_SECONDHAND_SERVICE_FEE_INTENT_AMOUNT` 预留未来收费路径。

Phase 2 仍不做购物车、平台本金托管、逐单保证金冻结或强制线上货款支付。

## Phase 3 学生店铺升级

Phase 3 将学生店铺从静态展示升级为校园技能服务预约闭环。

已落地能力：

- 店铺创建接口校验 `SHOP_MERCHANT` 身份，商家需先支付 20 元保证金并通过人工审核；
- 店铺市场支持校区与关键词筛选，展示服务范围、校区、评分、店主和营业时间；
- 新增店铺详情页，展示店铺资料、服务项目、预约入口和联系方式展示规则；
- 预约服务后，系统记录商家微信/QQ 联系方式快照，并通过站内通知提醒商家；
- 新增商家工作台，可创建/编辑店铺、维护服务项目、处理预约接受/开始/完成/取消；
- 后端新增 V7 迁移扩展店铺、服务项目和预约字段，保留服务费记录接口；
- Phase 3 继续不托管服务本金，不做逐单保证金冻结，不读取或保存支付宝密钥正文。

## Phase 4 项目广告与校园展示升级

Phase 4 将项目广告从静态时间线升级为校园展示与协作入口，覆盖项目组队、作品展示、社团招募和校园活动宣传。

已落地能力：

- 后端新增 V9 迁移扩展 `project_ads`，支持广告类型、摘要、标签、校区、封面文件、联系方式展示规则、过期时间、精选权重和审核记录；
- 项目广告支持创建、编辑、提交审核、发布者下架、管理员审核通过/拒绝、精选/取消精选和平台违规下架；
- 公开列表只展示已审核且未过期内容，支持类型、校区、关键词和精选筛选；
- 详情页会记录浏览量，展示标签、附件、外部链接、收藏评论统计和按规则开放联系方式；
- 文件、收藏、评论和举报继续复用通用 `target_type = PROJECT_AD` 能力；
- 新增项目广告详情页、发布者管理页和运营后台项目广告标签页；
- 审核通过、拒绝、精选和平台下架会发送站内通知。

Phase 4 不做项目融资、交易本金托管、逐单广告保证金、平台内实时聊天或复杂推荐算法。生产支付仍通过 API-Transfer-Station 支付中心，CampusHub 不读取、不复制、不保存支付宝密钥正文。

## Phase 5 治理、信用与信任运营

Phase 5 将分散的举报、违规和信用分能力升级为跨业务线治理闭环，服务跑腿、二手、店铺和项目广告四条业务线的真实内测。

已落地能力：

- 后端新增 V10 迁移，扩展举报状态、处理说明、处理结果、违规严重程度、处罚类型、目标对象和管理员记录；
- 新增信用调整记录、用户限制记录和管理员操作审计表，保留 `users.credit_score` 作为当前信用分；
- 举报状态统一为 `OPEN`、`IN_REVIEW`、`RESOLVED`、`REJECTED`、`ESCALATED`；
- 管理端新增 `/api/admin/governance` 治理接口，可查看队列、受理/解决/驳回/升级举报、创建违规、调整信用、设置限制和查看审计；
- 用户侧新增 `/api/credit/users/{userId}` 信用中心接口，返回信用分、当前限制、违规记录、信用变化和我的举报；
- 发布商品、发布跑腿任务、创建店铺/服务项目、提交项目广告等关键动作会检查发布冻结；跑腿接单和店铺履约动作会检查服务冻结；
- 处理举报、创建违规、调整信用和设置限制会写入站内通知与管理员审计；
- 前端新增 `/admin/governance` 治理工作台和 `/credit` 信用中心页面。

Phase 5 不做全量 JWT/RBAC 改造、机器审核、法律级申诉、自动扣除保证金、支付中心改造、交易本金托管或短信/微信通知。生产支付边界不变：CampusHub 不读取、不复制、不保存支付宝密钥正文。

## Phase 6 运营数据与导出

Phase 6 将已上线的跑腿、二手、店铺、项目广告和治理数据汇总到运营后台，用于内测和 Beta 决策。后台在 `/admin/ops` 提供日期范围筛选、平台总览、业务漏斗、校区分析、服务费/身份保证金汇总和 CSV 导出。

本阶段采用轻量实时聚合，不新增 Flyway 迁移，不创建分析快照表或导出日志表。CSV 只导出运营可见字段，不包含 `.env`、SMTP 密码、JWT secret、支付 token、支付宝密钥、密码哈希、邮箱验证码哈希、登录 token 或完整微信/QQ 联系方式。治理备注等自由文本仍需管理员谨慎填写，避免手动写入敏感信息。

支付边界保持不变：CampusHub 只汇总本地服务费记录和角色保证金申请记录，生产支付仍由外部 API-Transfer-Station 支付中心负责，CampusHub 不读取或保存支付宝密钥正文。

## Phase 7 响应式 Web 与用户体验打磨

Phase 7 不新增业务闭环，而是将已上线的跑腿、二手、学生店铺、项目广告、治理信用和运营后台体验打磨到适合真实校园移动浏览器内测。

已落地能力：

- 移动端全局导航从完整侧边栏调整为顶部菜单入口，减少首屏占用；
- 登录注册、通知、身份保证金、钱包、信用中心统一空状态、登录提示和说明文案；
- 跑腿、二手、店铺和项目广告的列表、详情和发布/管理表单完成移动优先排版；
- 发布类表单按信息、地点/范围、金额/服务费、联系方式和附件说明分区，减少手机端填写压力；
- 后台运营、治理和审核页面保留桌面表格效率，同时在移动端避免明显横向撑破页面；
- 基础可访问性检查覆盖按钮文本、表单标签、弹窗尺寸、提示文案和触控区域。

Phase 7 仍不做微信小程序、原生 App、Element Plus 替换、支付中心强化、JWT/RBAC 全量硬化、交易本金托管或逐单保证金冻结。

## Phase 8 支付中心集成强化与服务费运营

Phase 8 将本地 mock 支付升级为可对接 API-Transfer-Station 支付中心的内部支付链路。CampusHub 保存本地支付订单、服务费/身份保证金状态和回调审计；API-Transfer-Station 负责真实支付宝应用配置、支付宝签名验签和外部渠道通知。

已落地能力：

- `mock` 与 `payment-center` provider 由环境变量选择；
- 服务费和角色保证金创建统一支付订单；
- 支付中心内部回调支持内部 token 配置、幂等、金额、订单号和状态校验；
- 管理端新增 `/admin/payment` 支付订单和回调事件监控；
- `.env.prod.example` 只记录占位符，不包含真实 token、secret 或支付宝密钥。

Phase 8 仍不做交易本金托管、逐单保证金冻结、自动扣罚保证金、退款结算、发票、完整对账或 CampusHub 直连支付宝密钥。后续已规划更完整的支付方向：支付宝充值 0.6% 手续费、微信充值人工审核、线下交易服务费阈值、线上余额冻结托管和提现功能，应拆为独立 Phase 设计。

## 本地运行

### 依赖

- JDK 17+
- Maven 3.9+
- Node.js 20+
- Docker Desktop

### 启动数据库

```bash
docker compose up -d mysql
```

### 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

后端默认地址：`http://localhost:8080`。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`。

## 本地账号

演示数据由 Flyway `V2__seed_demo_data.sql` 初始化。默认密码以开发环境哈希形式存储，仅用于本地演示。

## 本地支付说明

当前实现提供 `PaymentProvider` 抽象与 `MockPaymentProvider`：

- `POST /api/payment/service-fees/{feeId}/mock-pay` 创建本地模拟支付；
- `POST /api/payment/service-fees/{feeId}/mock-success` 查询 mock provider 并把待支付服务费幂等标记为 `PAID`；
- 支付成功后会写入 `wallet_flows`，用于演示服务费支付闭环；
- `AlipayPaymentProvider` 只保留生产骨架，不读取、不复制、不保存支付宝密钥。

## 生产部署准备

生产推荐目录为 `/opt/campushub`，部署文件包括：

- `docker-compose.prod.yml`
- `.env.prod.example`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`

生产 compose 包含：

- `campushub-mysql`
- `campushub-backend`
- `campushub-web`

MySQL 不暴露公网端口；前端容器默认只绑定宿主机 `127.0.0.1:18080`，由服务器上的 Caddy/Nginx 反代到公网域名。

### 生产环境变量

复制 `.env.prod.example` 为服务器上的 `.env` 后，必须替换所有 `change-me` 值。不要把真实 `.env`、SMTP 密码、JWT secret、内部支付 token 或任何支付宝密钥提交到 GitHub。

关键变量：

```env
MYSQL_DATABASE=campushub
MYSQL_USER=campushub
MYSQL_PASSWORD=强随机密码
MYSQL_ROOT_PASSWORD=强随机密码
CAMPUSHUB_JWT_SECRET=至少32字节强随机字符串
CAMPUSHUB_MAIL_SMTP_HOST=smtp-relay.brevo.com
CAMPUSHUB_MAIL_SMTP_USERNAME=Brevo SMTP 用户名
CAMPUSHUB_MAIL_SMTP_PASSWORD=Brevo SMTP 密码
CAMPUSHUB_MAIL_FROM=suntomb@suntomb.qzz.io
CAMPUSHUB_MAIL_FROM_NAME=校集 CampusHub
```

### 反代建议

`campushub-web` 已经在容器内把 `/api/` 反代到 `campushub-backend:8080`。服务器公网反代可指向宿主机本地端口：

```caddyfile
ustc.suntomb.qzz.io {
    reverse_proxy 127.0.0.1:18080
}
```

也可以让 Caddy 接入 `campushub` Docker network 后直接反代容器服务。

### GitHub 到服务器部署流程

建议生产部署按以下顺序执行：

1. 本地确认代码不包含真实 `.env`、SMTP 密码、JWT secret、内部支付 token 或支付宝密钥。
2. 提交代码并推送到 GitHub 仓库。
3. 在服务器 `/opt/campushub` 拉取仓库代码。
4. 在服务器创建真实 `.env`，内容参考 `.env.prod.example`，并替换所有 `change-me` 占位符。
5. 使用 `docker-compose.prod.yml` 启动 `campushub-mysql`、`campushub-backend`、`campushub-web`。
6. 配置现有 Caddy/Nginx，把 `https://ustc.suntomb.qzz.io` 反代到 `127.0.0.1:18080` 或容器网络内的 `campushub-web:80`。
7. 用浏览器访问域名，验证首页、注册/登录、API 反代、钱包页和本地 mock 支付演示。

## API-Transfer-Station 支付中心边界

生产支付宝收款应由 API-Transfer-Station 作为支付中心负责，CampusHub 不直接接触支付宝私钥/公钥正文。

CampusHub 负责：

- 创建本地钱包充值或服务费支付订单；
- 调用支付中心内部接口创建收款单；
- 接收支付中心内部回调并校验内部 token；
- 校验订单号、金额和状态，幂等更新本地订单、钱包余额和流水。

API-Transfer-Station 负责：

- 保存支付宝 APPID、应用私钥、支付宝公钥等敏感配置；
- 调用支付宝官方电脑网站支付；
- 接收支付宝异步通知、验签、校验金额和幂等；
- 把支付结果转发给 CampusHub 内部回调。

安全要求：

- 不要读取、打印、复制、提交或写入 `/opt/ai-relay/secrets/alipay/` 下的密钥正文；
- 排查时最多检查文件是否存在、权限和挂载路径，不输出文件内容；
- 内部 token 只保存在服务器 `.env` 或 secret 管理中；
- 日志不要打印 token、完整支付宝回调参数或用户敏感信息。
