# 蜗壳同路 CampusHub · 微信小程序前端架构与 UI 设计指南

- **制定日期**：2026-11-11
- **适用对象**：后续负责微信小程序代码实现与联调的 Agent / 开发者
- **小程序信息**：蜗壳同路 CampusHub（简称：蜗壳同路，AppID：`wx94d18423aab8b5df`）
- **后端 API 基准**：`https://ustc.suntomb.cn/api`（复用现有 Spring Boot REST API 与 Bearer JWT 体系）

---

## 一、当前项目进度与小程序边界盘点

在动手写任何小程序代码前，执行 Agent 必须清楚当前仓库（截至 `feat/woke-tonglu-branding` 分支 / `V16` 迁移）已经发生的关键演进：

1. **品牌与合规口径已更新为「蜗壳同路 CampusHub」**
   - 平台全称：**蜗壳同路 CampusHub**（简称：**蜗壳同路**），定位为「独立学生项目 · 校园生活共同体」。
   - 品牌徽章资源已就绪：`frontend/public/woke-tonglu-logo-144.png`（及 `64` / `512` 尺寸）。
2. **资金功能已软关闭（Financial Soft-Close，`V15` 迁移）**
   - `GET /api/platform/features` 返回 `financialEnabled: false`。
   - **小程序端严禁展示或调用**：钱包充值、余额提现、二手线上担保冻结、保证金在线支付等资金接口。
3. **身份开通已升级为「免保证金 · 协议签署制」（`V15`–`V16` 迁移）**
   - 顺路跑腿员（`RUNNER`）、闲置发布者（`GOODS_PUBLISHER`）、技能店主理人（`SHOP_MERCHANT`）均**免收保证金**。
   - 用户通过 `GET /api/platform/protocols/{roleType}` 读取当前生效协议正文与版本号，勾选同意后调用 `POST /api/identity/users/{userId}/roles`（传入 `agreementAccepted: true` 与 `agreementVersion`）：
     - `RUNNER` 与 `GOODS_PUBLISHER`：**同意协议后立即自动开通**；
     - `SHOP_MERCHANT`：**同意协议后进入站务人工核验**。
4. **小程序与网页端（Web）的职责切分**
   - **小程序端只做高频、轻量、移动场景优先的四大核心能力**：
     1. **跑腿（Runner Tasks）**：看单、按校区筛选、抢单/报名、发跑腿单、履约状态推进与完成码核销。
     2. **小店（Student Shops）**：逛同学技能小店、预约服务、查看预约进度与解锁后的微信号/QQ号（含店主轻量接单/确认）。
     3. **宣传（Project Ads & Showcase）**：浏览科研/竞赛组队、活动宣传、作品展示、收藏/留言、发布招募宣传。
     4. **身份开通与个人通行证（Identity & Pass）**：校园邮箱验证码注册/登录、协议签署一键开通身份、我的履约单据、站内通知、微信/QQ联系方式维护。
   - **管理后台 100% 留给电脑网页端**：
     - 运营大盘（`/admin/ops`）、社区治理（`/admin/governance`）、交易管理（`/admin/trade`）、展示管理（`/admin/showcase`）、总站务审批（`/admin/master/admin-applications`）、财务/支付监控等**绝不进入小程序包**，既保持小程序极简纯净，又避免包体积膨胀与审核风险。

---

## 二、核心设计哲学：为什么不照搬网页手机端？

网页端（Web）服务于大屏深度浏览，因此采用了杂志画报式的 Bento 大横幅、多栏叙事和完整表单；但同学打开微信小程序的真实场景是：**走在东西区天桥上顺手看一眼有没有顺路快递单、在食堂排队时发个急送需求、或在宿舍躺着翻看大创招募帖。**

因此，小程序必须将网页端的「瀚海书院 × 流光市集」美学浓缩转化为 **「轻盈书院 · 口袋市集」（Pocket Editorial Craft）**：

| 维度 | 电脑/移动网页端（Web） | 微信小程序端（Mini-Program） |
| :--- | :--- | :--- |
| **首屏结构** | 杂志感 Bento Hero 大横幅 + 宣言导语 + 业务入口 | **零大横幅（Zero Hero Banner）**：顶部直接是 **校区胶囊切换条** + **首屏 3.5 张可操作业务卡片**，0.5 秒直达核心信息 |
| **导航骨骼** | 顶部导航栏 + 底部 5 Tab + 左侧 86% 全量目录抽屉 | **原生感底部 4 Tab + 页面内胶囊分段器（Segmented Control）**，彻底取消汉堡菜单抽屉 |
| **交互容器** | 独立详情长页面 + 居中弹窗（Dialog） | **高频动作全部采用半屏抽屉（Bottom Sheet，高度 65%–85%）**：抢单确认、服务预约、阅读协议开通身份、一键复制微信号均在单手拇指热区内完成 |
| **视觉质感** | 大面积极光晕染 + 鼠标聚光灯 + 多层卡片嵌套 | **宣纸暖白底色 `#F7F5F0` + `1rpx` 极细描边 + 车票齿孔/火漆印章微拟物细节 + 触觉微震动（`wx.vibrateShort`）** |

---

## 三、信息架构与页面路由树（仅 4 个底部 Tab + 精简子页）

### 1. 底部导航栏（TabBar · 4 项）

不再单设一个冗长的“门户宣传首页”，而是**把最高频的「跑腿」作为第一顺位主入口**，并在其顶部嵌入轻巧的校区切换与快捷发单入口：

1. **跑腿**（`pages/tasks/index`）—— 顺路互助大厅（默认首页）
2. **小店**（`pages/shops/index`）—— 同学技能服务橱窗
3. **宣传**（`pages/showcase/index`）—— 科研组队、竞赛招募与校园活动展板
4. **通行证**（`pages/pass/index`）—— 黑金学生通行证、身份协议一键开通、我的单据与站内通知

> **关于二手闲置（可选轻量入口）**：若首版严格聚焦「跑腿、小店、宣传、身份开通」，可将二手闲置暂隐或在「小店/市集」顶部提供二级分段切换；主 Tab 严格保持 4 个，视觉最平衡。

### 2. 完整页面清单（控制在 10 个页面以内，轻量高效）

```text
pages/
├─ tasks/
│  ├─ index              # [Tab 1] 跑腿大厅：校区筛选条 + 车票副券列表 + 右下角「+ 发跑腿」悬浮钮
│  ├─ detail             # 跑腿履约台：起止路线卡 + 状态时间轴 + 完成码核销 / 联系方式一键复制 / 异常上报
│  └─ publish            # 极简发跑腿：3 步胶囊表单（选起止校区 → 填标题与赏金 → 选抢单/报名模式）
├─ shops/
│  ├─ index              # [Tab 2] 同学小店：校区与分类胶囊条 + 双列/单列工作室橱窗卡
│  └─ detail             # 小店详情与预约：店主打招呼卡 + 服务项目列表 + 半屏预约抽屉（预约后展示店主微信/QQ）
├─ showcase/
│  ├─ index              # [Tab 3] 宣传广场：精选轮播横卡 + 类别筛选（组队/活动/作品/纳新）+ 海报流
│  ├─ detail             # 宣传详情：正文与标签 + 附件预览 + 留言互动 + 联系方式按规则展示
│  └─ publish            # 发布宣传帖：移动端精简发布页（支持保存草稿与提交审核）
└─ pass/
   ├─ index              # [Tab 4] 蜗壳同路通行证：黑金身份卡 + 身份点亮区（协议抽屉）+ 我的跑腿/预约/宣传 + 通知角标
   ├─ auth               # 登录与注册：校园邮箱登录 + 邮箱验证码注册（必填微信或QQ联系方式）
   └─ notifications      # 站内通知与个人资料维护（修改昵称、微信/QQ、密码）
```

---

## 四、视觉系统规范（Design Tokens · 兼具效率与书院美感）

小程序必须严守以下视觉 Token，绝不能退化成千篇一律的微信默认白底蓝框或廉价 AI 渐变：

### 1. 色彩体系（CSS Variables in `app.wxss` / `uni.scss`）

```css
page {
  /* 宣纸暖白底色：比纯白 #FFFFFF 更护眼，瞬间建立科大人文书院感 */
  --bg-canvas: #F7F5F0;
  --bg-surface: #FFFFFF;
  --bg-subtle: #EFECE4;

  /* 科大深海墨蓝与学术鎏金 */
  --ink-navy: #0B1626;
  --ink-navy-soft: #16325B;
  --gold-accent: #C6922C;
  --gold-soft: #FBF5E6;
  --gold-border: rgba(198, 146, 44, 0.32);

  /* 文本层级 */
  --text-primary: #141C2B;
  --text-secondary: #526075;
  --text-muted: #8492A6;

  /* 四大场景语义主色（克制点缀，仅用于标签、金额与状态点） */
  --tone-runner: #0D9488;      /* 跑腿：松石青绿，代表流动与效率 */
  --tone-runner-bg: #ECFDF5;
  --tone-shop: #1D4ED8;        /* 小店：书院钴蓝，代表专业与信赖 */
  --tone-shop-bg: #EFF6FF;
  --tone-showcase: #4338CA;    /* 宣传：学术靛蓝，代表创意与招募 */
  --tone-showcase-bg: #EEF2FF;
  --tone-urgent: #D95F43;      /* 急单/提醒：朱砂暖橙 */

  /* 极细书院分割线与柔和弥散投影 */
  --border-hairline: 1rpx solid rgba(15, 23, 42, 0.08);
  --shadow-card: 0 8rpx 28rpx rgba(11, 22, 38, 0.04);
  --shadow-floating: 0 16rpx 40rpx rgba(11, 22, 38, 0.14);

  background-color: var(--bg-canvas);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}
```

### 2. 字体与数字排版规则

- **页面主标题 / 卡片大标题**：`font-weight: 700`，字号 `32rpx–36rpx`，行高 `1.35`，字距 `-0.01em`。
- **辅助正文**：字号 `26rpx–28rpx`，颜色 `var(--text-secondary)`，最多显示 2 行（`-webkit-line-clamp: 2`），绝不在列表卡片上堆砌超过 40 字的长段落。
- **金额、时间、完成码、信用分**：统一加 `.mono-num` 类（`font-family: "DIN Alternate", "JetBrains Mono", monospace; font-variant-numeric: tabular-nums; font-weight: 700;`），让每一张跑腿车票上的 `¥8.00` 都像实体印刷品一样工整。

---

## 五、四大核心模块的 UI 卡片与微交互设计详案

### 1. 【跑腿】口袋车票副券卡（Pocket Runner Ticket）

不要做成普通的上下堆叠列表，保留网页端广受好评的**「实体车票齿孔副券」**隐喻，但针对手机单屏高度做紧凑压缩（单卡高度控制在 `210rpx` 左右，一屏可看 3.5 张）：

```text
┌─────────────────────────────────────────── o ──────────────┐
│ [中校区] 菜鸟驿站  ──→  [西校区] 芳华餐厅    ┆     ¥ 6       │
│ 顺路帮拿两个小件快递，取件码接单后可见        ┆   悬赏报酬     │
│ ⏱ 18:30前送达 · 孟侠 (信用100)              ┆  [ 立即抢单 ]  │
└─────────────────────────────────────────── o ──────────────┘
```

- **左侧主券（72% 宽度）**：
  - 第一行：**起点校区胶囊 → 终点校区胶囊**（如 `中校区 ➔ 西校区`），用粗体深蓝标识，跑腿员扫一眼就知道顺不顺路！
  - 第二行：任务标题 + 详细地点简述（单行截断）。
  - 第三行：截止时间 + 发布者昵称 + 抢单/报名模式小标。
- **中间齿孔线**：`1rpx dashed rgba(15, 23, 42, 0.14)`，上下边缘各放一个直径 `16rpx`、背景色为 `var(--bg-canvas)`（`#F7F5F0`）的半圆缺口（模拟真实撕票口）。
- **右侧副券（28% 宽度）**：
  - 上方展示醒目的等宽金额 `¥6`；
  - 下方为单手拇指极易点击的胶囊按钮 `立即抢单`（若未解锁 `RUNNER` 身份，点击不报错，而是直接弹出**「10秒阅读协议点亮跑腿员身份」半屏抽屉**，无缝衔接！）。

### 2. 【小店】同窗工作室橱窗卡（Studio Bento Card）

- **顶部筛选条**：横向滑动校区胶囊（`全部 / 西区 / 东区 / 中区 / 高新`）+ 技能分类胶囊（`摄影约拍 / 电脑清灰 / 文档排版 / 辅导答疑`）。
- **卡片布局**：
  - 左侧或顶部为小店封面（若无实拍封面，沿用我们在网页端设计的**「书院典藏几何封面」**——深蓝或琥珀色底纹 + 分类篆刻角标 + 店名书法体首字，彻底告别灰底空白框）。
  - 卡片内直接预览该店最热门的 **1–2 项服务项目及价格**（如 `证件照精修 ¥19 / 次`、`笔记本清灰换硅脂 ¥35 / 台`），点击卡片右侧 `[ 预约 ]` 直接呼出**半屏预约抽屉**：选择服务项、期望时间段、填写一句话需求，提交后即可在详情页一键复制店主的微信号/QQ号。

### 3. 【宣传】校园招募与展板画廊（Showcase Poster Stream）

- **顶部精选横滑区（Featured Carousel）**：横向展示管理员置顶的精选项目/活动，采用深海墨蓝底色 + 鎏金角标卡片。
- **瀑布/单列展板流**：
  - 每张卡片顶部用彩色左边框或丝带区分类型：`科研/竞赛组队`（靛蓝）、`个人作品集`（松石绿）、`社团与实验室纳新`（琥珀金）、`校园活动`（珊瑚橙）。
  - 提炼展示 **3 个核心标签 Chips**（如 `#ACM竞赛` `#缺前端` `#西区实验室`）+ 截止日期 + 收藏/评论数。
  - 详情页底部固定操作栏：`[ ☆ 收藏 ]`、`[ 💬 留言提问 ]`、`[ 📋 查看/复制招新联系方式 ]`。

### 4. 【身份开通与通行证】黑金通行证 + 协议半屏抽屉（Woke Tonglu Pass）

这是小程序最具仪式感的页面，也是对 `V15/V16` 免保证金协议制的最佳视觉呈现：

- **顶部「蜗壳同路黑金通行证」卡片**：
  - 深海墨蓝渐变底板（`#0B1626 → #16325B`）+ 右上角半透明品牌徽章水印 + 鎏金细内框。
  - 显示当前昵称、校园邮箱、信用分（如 `信用分 100 · 极好`）以及微信号/QQ号绑定状态。
- **中部「身份徽章一键点亮」横排三宫格**：
  - 展示三个带印章质感的身份卡：
    1. **顺路跑腿员**（状态：`已点亮` 或 `免保证金 · 签协议即开`）
    2. **闲置发布者**（状态：`已点亮` 或 `免保证金 · 签协议即开`）
    3. **技能店主理人**（状态：`已点亮` / `站务核验中` / `免保证金 · 协议申请`）
  - **核心交互优化**：点击未开通的身份卡，弹出 **80% 高度的「书院协议签署半屏抽屉」**：
    - 顶部展示协议标题与版本号（如 `v1.0`）；
    - 中部为可滚动的宣纸底色协议正文框；
    - 底部固定勾选框 `我已阅读并同意上述条款` + 墨蓝鎏金主按钮 `[ 同意协议并立即开通 ]`；
    - 跑腿员与闲置发布者点击后触发 `wx.vibrateShort` 微震动反馈，徽章瞬间由暗灰点亮为鎏金，全程无需跳转复杂页面！

---

## 六、前端工程技术栈与目录架构建议（供执行 Agent 落地）

### 1. 技术选型推荐：`uni-app (Vue 3 + Vite + TypeScript + Pinia)`

- **推荐理由**：
  1. **代码与类型 90% 直接复用**：当前 Web 端就是 `Vue 3 + TypeScript + Pinia`。使用 `uni-app`（Vue 3 组合式 API 模式）可以直接把 `frontend/src/utils/display.ts`（中文枚举格式化）、`frontend/src/utils/identity.ts`（身份权限计算）、`frontend/src/config/roleAgreements.ts` 以及 `frontend/src/api/campushub.ts` 中的 TypeScript 接口定义原封不动拷贝过来，零重写成本、零枚举翻译偏差！
  2. **原生级编译产物**：通过 `npm run build:mp-weixin` 直接产出符合微信开发者工具标准的 `dist/build/mp-weixin` 目录（含 `app.json`、`project.config.json`），可直接用 AppID `wx94d18423aab8b5df` 导入预览和真机调试。
  3. **多端储备**：用户此前提到后期还可能制作手机 App，使用 `uni-app (Vue 3)` 未来可低成本编译为 iOS/Android App。

> **备选说明**：如果后续 Agent 更倾向于使用「微信原生小程序 TypeScript 模板（WXML + WXSS + TS）」，同样可以复用 `utils/display.ts`、`utils/identity.ts` 和 API 类型文件，只需将网络层 `axios` 替换为下方的 `wx.request` 封装即可。

### 2. 推荐新建独立目录：`miniprogram/`（与 `frontend/`、`backend/` 并列）

不要把小程序代码混入 `frontend/` 的 Vite Web 构建树中，建议在仓库根目录新建独立的 `miniprogram/` 目录：

```text
campushub/
├─ backend/
├─ frontend/                 # 现有桌面与移动 Web 端（含全量管理后台）
└─ miniprogram/              # 新增：微信小程序端独立工程
   ├─ src/
   │  ├─ api/
   │  │  ├─ request.ts       # 基于 uni.request / wx.request 的 ApiResponse<T> 解包与 JWT 注入器
   │  │  └─ campushub.ts     # 精简版 API（仅保留 auth、tasks、shops、project-ads、roles、notifications、profile）
   │  ├─ stores/
   │  │  └─ auth.ts          # Pinia 登录态（使用 uni.getStorageSync('campushub_token') 持久化）
   │  ├─ utils/
   │  │  ├─ display.ts       # 直接复用 Web 端的中文枚举与状态色彩映射（消除英文枚举直出）
   │  │  ├─ identity.ts      # 直接复用 Web 端的身份计算逻辑
   │  │  └─ clipboard.ts     # 封装微信号/QQ号一键复制 + 轻触觉震动提示
   │  ├─ components/
   │  │  ├─ CampusZoneBar.vue        # 顶部吸顶校区胶囊筛选条
   │  │  ├─ RunnerTicketCard.vue     # 跑腿车票副券组件
   │  │  ├─ RoleUnlockSheet.vue      # 身份协议阅读与一键开通半屏抽屉
   │  │  ├─ ContactCopyPill.vue      # 隐私联系方式解锁与一键复制胶囊
   │  │  └─ EmptyStateCard.vue       # 书院风温和空状态组件（严禁伪造假数据）
   │  ├─ pages/
   │  │  ├─ tasks/ (index, detail, publish)
   │  │  ├─ shops/ (index, detail)
   │  │  ├─ showcase/ (index, detail, publish)
   │  │  └─ pass/ (index, auth, notifications)
   │  ├─ App.vue
   │  ├─ main.ts
   │  ├─ manifest.json       # 配置 mp-weixin appid: "wx94d18423aab8b5df"
   │  └─ pages.json          # 页面路由、下拉刷新与 4-Tab 底部导航配置
   ├─ package.json
   └─ tsconfig.json
```

### 3. 网络层与鉴权适配要点（`src/api/request.ts`）

小程序没有浏览器的 `localStorage` 和 Axios，执行 Agent 需按以下规则实现 `request.ts`：

1. **环境变量与 BaseURL**：
   - 通过 `import.meta.env.VITE_API_BASE_URL` 配置，默认指向 `https://ustc.suntomb.cn/api`（本地联调时可指向本地后端或演练隧道）。
2. **Token 注入与 `ApiResponse<T>` 解包**：
   - 从 `uni.getStorageSync('campushub_token')` 读取 JWT，注入请求头 `Authorization: Bearer ${token}`。
   - 后端统一返回 `{ success: boolean, message: string, data: T }`。当 HTTP 状态码为 `200` 且 `res.data.success === true` 时返回 `res.data.data`；否则抛出带中文 `message` 的业务异常。
3. **静默会话恢复（防 401 弹窗风暴）**：
   - 沿用 Web 端已验证的 `skipAuthExpireHandling` 机制：小程序启动时调用 `GET /api/auth/me` 恢复身份；若网络超时（`errno` / timeout）保留本地 token 并提示“网络不稳定，点击重试”，仅在后端明确返回 `401/403` 时清空失效 token，且不在游客浏览公开列表时强制跳转登录页。

---

## 七、去 AI 感与微信小程序合规硬性红线（Checklist）

后续开发 Agent 在编写小程序界面与文案时，必须逐条遵守：

1. **严禁英文枚举与开发黑话上屏**：
   - 所有校区（`WEST` → `西校区`）、状态（`PENDING_REVIEW` → `等待站务核验`、`HEADING_TO_PICKUP` → `前往取件中`）、角色（`RUNNER` → `顺路跑腿员`）必须经过 `utils/display.ts` 转换，绝不允许在页面上出现任何原始大写英文枚举。
   - 严禁出现 `Campus Service OS`、`Next Best Actions`、`Workspace`、`Beta`、`V15/V16` 等内部词汇。
2. **微信原生体验增强（小而美的细节）**：
   - **下拉刷新**：所有列表页（跑腿、小店、宣传、通行证）开启原生下拉刷新（`enablePullDownRefresh: true`，背景色 `#F7F5F0`）。
   - **一键复制联系方式**：当跑腿接单成功、店铺预约提交后，`contactVisible === true` 时，提供醒目的 `[ 复制微信号 ]` / `[ 复制 QQ 号 ]` 按钮，调用 `wx.setClipboardData` 并提示 `已复制微信号，可前往微信添加同学`。
   - **安全区适配**：所有底部固定操作栏与半屏抽屉必须包含 `padding-bottom: calc(24rpx + env(safe-area-inset-bottom))`，防止与 iPhone 底部小黑条重叠。
3. **严守功能与合规边界**：
   - 不包含任何管理员后台页面；
   - 不包含任何充值、提现、在线支付按钮（遵循 `financialEnabled: false`）；
   - 未实际接入 `wx.uploadFile` 原生文件上传接口前，不在小程序端开放本地相册上传按钮（文字发布与预设分类封面先行，避免触发未声明的隐私/域名调用）；
   - 空状态（如某校区暂无跑腿单）使用温和的书院插画卡片展示“当前校区暂无待接跑腿单”，绝不硬编码假商品或假订单。
