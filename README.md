# 蜗壳同路 CampusHub

下课顺路帮同学取个快递，把用完的教材转给下一届，或给比赛找一位队友——这些事都可以在[蜗壳同路 CampusHub](https://ustc.suntomb.cn) 上办。这是面向高校同学的校园生活互助平台，把**校园跑腿、二手闲置、学生技能小店、项目与组队宣传**汇聚在一处。你可以用浏览器访问 Web 端；仓库里也有一套原生微信小程序，两端共用同一个后端。

想逛一逛，直接打开线上站点就好。想知道一次抢单、一条购买意向是怎么写进数据库的，可以从下面的源码导览开始。欢迎点个 Star，提 Issue 讨论，或 Fork 一份改成自己学校的版本。

## 核心功能

| 板块 | 你可以做什么 |
| --- | --- |
| **悬赏跑腿** | 填写物品重量、起终点校区、截止时间和报酬，按需附上私密取件码或物品照片。赶时间可选「即时抢单」，想自己挑人可选「报名筛选」。接单后双方可以跟进履约进度，用 6 位完成码核销，或由发单人查看送达照片后确认。尚未有人报名或接单时可修改委托，接单前可撤下。 |
| **校园二手** | 按分类和校区找闲置；卖家可给商品配展示图。买家提交购买意向后才能查看卖家微信或 QQ，约好线下交接；卖家再从意向列表选定实际买家、确认成交。卖家可编辑、下架或删除商品，已有的成交记录不会随商品删除。 |
| **学生技能小店** | 主理人可以开店、设置封面、管理服务项目和配图，并处理预约、接单到完成的流程。顾客完成预约后，可给这项服务评分、写评价。店铺评分先分别计算各项服务的均分，再对有评分的服务取算术平均；还没人评的服务不算零分。 |
| **校园宣传广场** | 发布科研或竞赛组队邀请、作品集、社团纳新和校园活动，可配图展示。同学们可以从精选内容发现项目，收藏感兴趣的帖子，或在留言区交流。 |
| **我的订单与社区管理** | Web 的「我的书院 → 我的订单」和小程序的「个人 → 我的订单」汇总跑腿、二手、预约、宣传四类记录。有交易对方的订单完成后可评价或投诉；宣传记录没有交易对方，不参与评分。平台还提供校园邮箱注册、站内通知、身份协议开通、信用分中心和按职责分权的管理后台。 |

**资金与支付模块的参考实现。** 日常互助不要求把钱转入平台。仓库保留了钱包账本、线下结算与在线支付的抽象，以及独立的支付宝电脑网站支付适配器；默认通过 `CAMPUSHUB_FINANCIAL_FEATURES_ENABLED=false` 关闭新的资金写操作。如果你的校园项目只需要联系和交接，可以保持这个默认值；想研究账本与支付回调，也可以直接读 `wallet` 和 `payment` 包，不用发起真实交易。

## 技术栈与目录结构

- **后端：** Java 17、Spring Boot 3.3、Spring Security + JWT、Spring Data JPA、Flyway。应用使用 MySQL 8.4，集成测试使用 H2。
- **Web：** Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus，测试使用 Vitest。
- **原生微信小程序：** WXML / WXSS / JavaScript（CommonJS），零外部运行时依赖。5 个 Tab（跑腿、二手、小店、宣传、个人），共 15 个页面。

```text
backend/                  Spring Boot 应用、数据库迁移与后端测试
frontend/                 Web 页面、路由、状态管理与前端测试
miniprogram/              微信开发者工具的项目目录，原生客户端在 src/ 下
docker-compose.yml       本地开发用 MySQL
docker-compose.prod.yml  部署用 MySQL、后端和 Web 容器
.env.prod.example         部署环境变量示例，不含你的真实凭据
```

后端业务代码集中在 [`backend/src/main/java/com/campushub/`](backend/src/main/java/com/campushub/)。找注册登录看 `auth`；四个业务入口分别在 `task`、`goods`、`shop`、`projectad`。跨业务订单查询在 `orders`，展示图在 `catalogmedia`，举报与信用治理在 `moderation`，账本与支付参考实现在 `wallet`、`payment`。

## 本地快速启动

准备 JDK 17、Maven 3.9+、Node.js 20+，以及带 Compose 的 Docker（也可以自备 MySQL 8.4）。下面各步都从仓库根目录开始；后端和 Web 开发服务各占一个终端。

**1. 启动数据库。** 本地 Compose 会创建 `campushub` 库并映射到 `3306` 端口。等数据库就绪后再启动后端。

```bash
docker compose up -d mysql
```

**2. 启动后端。** `local` 配置默认连接上述数据库，在 `8080` 端口提供 API。首次启动空库时，Flyway 自动执行 V1–V22 迁移，后续只执行尚未应用的版本。本地默认不发送真实 SMTP 邮件，不配置邮箱也能启动并浏览；要实际收取注册验证码，需要另行配置发信服务。

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**3. 启动 Web。** 在另一个终端运行下列命令，打开 <http://localhost:5173>。Vite 会把 `/api` 请求代理到本地后端。

```bash
cd frontend && npm install && npm run dev
```

**4. 运行原生微信小程序。** 用微信开发者工具导入 `miniprogram/`，不要选它下面的 `src/`。若要连接刚启动的本地后端，在开发版的工具控制台执行下面一行，再重新编译。这个回环地址指向开发电脑，不适用于真机。

```js
wx.setStorageSync('woke_development_api', 'http://127.0.0.1:8080/api')
```

只想先看界面？在仓库根目录运行 `node miniprogram/tools/make-preview.cjs`，再单独导入生成的 `.claude/verification/native-miniprogram/offline-preview/`。它使用内存样例，不需要后端，不要把样例当成正式工程上传。

**常用测试与检查**（每行从仓库根目录运行）：

```bash
cd backend && mvn test
npm --prefix frontend test
npm --prefix frontend run build
npm --prefix miniprogram test
npm --prefix miniprogram run check
```

## 参考本项目开发自己的应用

### 四个值得顺着读的设计

1. **同一张订单列表，不等于同一张数据库表。** 从 [`OrderDeskQuery.java`](backend/src/main/java/com/campushub/orders/OrderDeskQuery.java) 看起：每条查询先选出当前用户参与的记录，再把跑腿、闲置、预约和宣传投影成同一组列供列表读取。业务状态仍由各自的源表维护，不需要再同步一张「万能订单表」。
2. **能看商品图，不等于能看取件凭证。** [`catalogmedia`](backend/src/main/java/com/campushub/catalogmedia/) 根据商品、店铺等父对象的可见状态判断读者权限，并在保存展示图时重新编码以移除元数据。跑腿物品和送达照片则由 [`TaskEvidenceService.java`](backend/src/main/java/com/campushub/task/TaskEvidenceService.java) 管理，通过受保护的下载接口只向任务参与方提供，不复用公开图库地址。
3. **从一次业务变化读懂一次数据库迁移。** [MySQL 迁移目录](backend/src/main/resources/db/migration/) 保留了 V1–V22 的演进过程；例如 V22 给已成交的意向保存成交金额，以后卖家改标价也不会改写当时的交易。Flyway 应用迁移后，Hibernate 用 `ddl-auto: validate` 校验映射。[H2 同步迁移脚本](backend/src/test/resources/db/test-migration/) 让集成测试不用依赖外部数据库。
4. **原生小程序怎样管理登录态？** 从 [`miniprogram/src/`](miniprogram/src/) 的 `lib/request.js` 和 `lib/config.js` 入手：请求路径有白名单，不同 API 环境分别保存会话，私有页面隐藏时清理敏感状态。改完页面可运行 [`miniprogram/tools/check.cjs`](miniprogram/tools/check.cjs) 检查路由、资源引用和包体积，无须引入运行时框架。

### 改成自己校园或社区的版本

1. **先换名片。** 在 `frontend/index.html` 修改站点标题和图标引用，用自己的素材替换 `frontend/public/` 与 `miniprogram/src/assets/` 中的品牌图，再同步更新页面名称和适用的备案信息。
2. **再换校园规则。** 后端校区枚举是 [`CampusZone.java`](backend/src/main/java/com/campushub/task/CampusZone.java)；同步调整 Web 各业务页的选项（例如 [`TasksView.vue`](frontend/src/views/TasksView.vue)）和小程序 [`display.js`](miniprogram/src/lib/display.js)。注册邮箱后缀由 [`RegistrationService.java`](backend/src/main/java/com/campushub/auth/RegistrationService.java) 校验；换学校时，也别忘了改两端注册提示。
3. **配置自己的部署环境。** 复制 `.env.prod.example` 为 `.env`，设置独立的数据库密码、JWT 密钥和 SMTP 发信配置，不要提交 `.env`。配置就绪后用 `docker compose -f docker-compose.prod.yml up -d --build` 启动服务，在外层反向代理配置自己的域名和 HTTPS。支持上传图片时，外层 Nginx 也应设置 `client_max_body_size 6m;`，避免请求还没到应用就被拦下。
4. **接入自己的小程序。** 在 `miniprogram/project.config.json` 填自己的 AppID，在 `miniprogram/src/lib/config.js` 设置自己的 HTTPS API 根地址（含 `/api`）。到微信公众平台配置请求、上传和下载所需的合法域名，再用真机检查图片与网络请求。

## 交流与致谢

欢迎提 Issue 分享你在校园里遇到的场景，或用 PR 带来一个具体改进。如果这份代码帮你理清了课程设计里的某张表，或让你的第一个全栈项目更容易起步，欢迎点个 Star。谢谢！
