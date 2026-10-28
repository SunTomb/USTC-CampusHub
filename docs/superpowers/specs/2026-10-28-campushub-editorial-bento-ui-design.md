# CampusHub Phase 15「瀚海书院 × 流光市集」前端美学与去 AI 感设计规范

日期：2026-10-28
分支：`feat/phase15-editorial-bento-ui`

## 1. 背景与核心目标

CampusHub 在完成 Phase 1–14 的功能闭环后，页面视觉与文案仍然存在明显的 AI 模板痕迹（“AI Slop”）：
- **文案 AI 味与开发交接腔**：大量生硬的英文副标题（`Campus Service OS`、`Next Best Actions`、`Trust Boundary`、`Beta readiness` 等），把内部系统架构与阶段说明（如“12 个阶段”、“API-Transfer-Station”、“Phase 9 提现”）直接展示给终端学生用户；大量状态、渠道、业务类型直接裸露英文枚举（`PAID`、`PENDING_REVIEW`、`ALIPAY`、`Provider`）。
- **视觉同质化与粗糙细节**：默认 `Inter` 无衬线体配廉价高饱和蓝绿渐变、千篇一律的大圆角套卡片、侧边栏图标因字符截断变成“首/悬/二/学”单字方块、顶栏 `116px` 过高且与侧边栏品牌重复、`body { min-width: 1200px }` 导致窄桌面窗口横向滚动条。

本阶段吸收 **Impeccable**、**Taste-Skill** 与 **UI/UX Pro Max** 的反套路准则，结合 **Inspira UI / Vue Bits** 的现代流光微交互，打造具有中国科大（USTC）书院人文气质与现代旗舰水准的 **「瀚海书院 × 流光市集（USTC Editorial Bento & Luminous Craft）」** 视觉体系。

---

## 2. Anti-Slop 铁律（Impeccable & Taste-Skill 规范）

1. **禁止无意义的英文 Eyebrow 标签**：全面清除或替换为自然雅致的中文短笺（如 `瀚海集市 · 科大校园生活共同体`、`好物闲置 · 校友流转`、`顺路互助 · 悬赏告示`）。
2. **禁止开发黑话与架构名词外露**：用户界面严禁出现 `Phase X`、`12 个阶段`、`API-Transfer-Station`、`Provider`、`Beta readiness`、`Token` 等工程词汇，一律转化为温暖、清晰、守信的校园社区语言。
3. **禁止裸露后端英文枚举**：所有交易状态、审核状态、支付渠道、校区、业务类型必须通过统一的中文展示字典（`src/utils/display.ts`）转换为中文语义标签与徽章。
4. **禁止廉价大面积实心霓虹渐变**：用微暖宣纸画布（`#F8F6F1`）、瀚海深蓝（`#0B192C`）、1px 半透明高光描边、SVG 噪点肌理与鼠标跟随聚光灯（Spotlight Hover）取代廉价蓝绿实心渐变。

---

## 3. 视觉系统（Design Tokens）

### 3.1 色彩体系
- **画布背景**：`--bg-canvas: #F7F5F0`（微暖宣纸白），搭配顶部随路由切换的柔和极光光晕（Aurora Mesh）。
- **侧栏基底**：`--bg-sidebar: #0B1626`（瀚海深夜蓝，沉稳内敛的学院质感）。
- **四大板块氛围色**：
  - 跑腿悬赏（Tasks）：松石翠 `#0D9488` + 琥珀金赏金签 `#D97706`
  - 二手市集（Goods）：赤陶暖橙 `#EA580C` + 暖米画廊底 `#FFFDF9`
  - 学生店铺（Shops）：瀚海学院蓝 `#1D4ED8`
  - 项目广场（Showcase）：星夜靛紫 `#4F46E5`

### 3.2 字体与排版（Editorial Typography）
- **人文标题衬线体（`--font-display`）**：`'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', 'PingFang SC', serif`，赋予大标题、板块名与导语深厚的学院书卷气。
- **正文无衬线体（`--font-sans`）**：`'Plus Jakarta Sans', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif`。
- **金融级等宽数字（`--font-mono`）**：`'JetBrains Mono', 'SF Mono', 'Consolas', monospace`，开启 `font-variant-numeric: tabular-nums`，用于所有价格、赏金、信用分、余额与订单号。

---

## 4. 核心组件与页面升级设计

1. **全局壳层（`MainLayout.vue`）**：
   - 接入 `@element-plus/icons-vue` 真实矢量图标，彻底替换单字截断方块；
   - 侧边栏精简为优雅的单行图标+标题胶囊，移除冗余机械的说明小字；
   - 顶栏精简为 `72px` 毛玻璃状态栏，动态展示当前频道人文导语与快捷入口。
2. **首页（`HomeView.vue`）**：
   - 采用 **Inspira UI 风格 Bento Grid 便当盒布局** + **鼠标跟随聚光灯（Spotlight Card）**；
   - 左侧主视觉结合科大校区场景（中区、西区、东区、高新、先研院），右侧为黑金质感校园通行证/资产概览卡。
3. **跑腿大厅（`TasksView.vue`）**：
   - 任务卡片重构为 **「悬赏告示票据（Ticket Stub）」**，带左右撕纸半圆缺口、虚线分割线、醒目的等宽赏金签与可视化校区路线。
4. **二手市集（`GoodsView.vue`）**：
   - 商品卡片重构为 **「策展画廊拍立得（Gallery Card）」**，无图商品按校区/类别呈现雅致的定制几何纹样与中文题签，告别简陋的 `CampusHub` 文字占位。
5. **钱包中心（`WalletView.vue`）与全站状态汉化**：
   - 顶部资产条升级为 **「瀚海联名黑金卡」**，所有充值、提现、冻结、服务费、流水与后台管理表格全面应用中文语义徽章。
