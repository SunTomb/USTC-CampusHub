# CampusHub Phase 6 运营数据与导出设计

日期：2026-05-22

## 1. 背景与目标

CampusHub 已完成跑腿任务、二手交易、学生店铺、项目广告、治理信用五个阶段。四条业务线已经产生任务、商品、预约、广告、举报、违规、信用调整、服务费与角色保证金等运营数据。Phase 6 的目标是把这些现有数据转化为可用于内测和 Beta 决策的运营后台，而不是建设完整 BI 系统。

Phase 6 采用轻量实时聚合方案：后端直接读取现有业务表并按日期范围聚合，前端在现有 `/admin/ops` 页面展示总览、业务漏斗、校区分布、费用与导出入口。CSV 导出不记录导出日志，不新增 Flyway 迁移，避免给已部署生产数据库增加不必要风险。

## 2. 设计原则

1. **不新增数据库迁移**：Phase 6 只读取现有 V1-V10 数据表，不创建快照表、导出日志表或分析埋点表。
2. **实时聚合优先**：统计结果基于当前数据库状态和已有 `created_at`、`updated_at`、状态字段，适合运营观察和课程展示，不承诺财务级或 BI 级精确口径。
3. **业务线分层展示**：总览回答“平台整体如何”，漏斗回答“每条业务线卡在哪里”，校区分析回答“需求集中在哪里”，费用汇总回答“服务费和身份保证金规模如何”。
4. **支付边界不变**：只汇总 CampusHub 本地服务费记录和角色保证金申请记录，不读取、导出、复制或展示任何支付中心 token、SMTP 密码、JWT secret、支付宝密钥或交易本金信息。
5. **低影响部署**：避免重型图表库、后台调度、批处理回填和复杂 SQL 作业，适配当前小服务器。

## 3. 后端设计

### 3.1 包与职责

在现有 `backend/src/main/java/com/campushub/ops` 包内扩展 Phase 6 能力：

- `OperationsAnalyticsService`：统一组织运营聚合查询、日期范围处理和 CSV 数据生成。
- `OperationsAnalyticsController`：提供管理员运营分析 API。
- DTO records：表达总览指标、业务漏斗、校区分布、费用汇总、CSV 导出行。

不新建独立 `analytics` bounded context，避免把轻量运营聚合扩展成单独 BI 子系统。

### 3.2 日期范围

所有分析 API 支持 `startDate` 和 `endDate` 查询参数，格式为 `yyyy-MM-dd`。默认范围为最近 30 天，包含开始日期和结束日期全天。

后端负责：

- 缺省值填充；
- 结束日期不早于开始日期；
- 限制过长范围，建议最大 366 天；
- 将日期转换为本地时间范围用于 JPA 查询。

### 3.3 总览指标

总览接口返回：

- 新用户数；
- 活跃用户近似数；
- 跑腿任务新增数、完成数、异常/纠纷数；
- 二手商品新增数、在售数、联系意向数；
- 店铺预约新增数、完成数、取消数；
- 项目广告新增数、审核通过数、曝光总量；
- 待处理治理事项：开放举报、待审身份、待审项目广告；
- 服务费记录数、服务费金额合计；
- 角色保证金申请数、保证金金额合计。

“活跃用户近似数”基于现有业务记录中的用户 ID 去重，例如任务发布/接单、商品发布、预约、项目广告发布、评论、举报等，不引入新的行为埋点。

### 3.4 业务线漏斗

Phase 6 提供四条业务线的漏斗聚合。

跑腿任务：

- 已发布任务数；
- 抢单模式任务数；
- 申请模式任务数；
- 申请数；
- 已接单任务数；
- 已完成任务数；
- 异常/纠纷任务数。

二手交易：

- 新增商品数；
- 当前在售商品数；
- 收藏数；
- 留言/评论数；
- 联系意向或联系方式开放记录数；
- 已下架/已完成近似数。

学生店铺：

- 店铺数；
- 服务项目数；
- 预约请求数；
- 已接受预约数；
- 服务中预约数；
- 已完成预约数；
- 已取消/拒绝预约数。

项目广告：

- 新增广告数；
- 待审核数；
- 已通过数；
- 已拒绝/关闭/屏蔽数；
- 浏览量合计；
- 收藏数；
- 评论数；
- 联系方式可见或联系意向数。

### 3.5 校区分析

校区分析优先使用现有结构化校区字段，不解析自由文本。

- 跑腿：起点校区分布、终点校区分布、起点到终点路线 Top N。
- 二手：商品交易/发布校区分布，若现有字段不足则只统计已结构化字段。
- 店铺：店铺服务校区分布。
- 项目广告：广告校区分布。

校区枚举继续沿用：中校区、西校区、东校区、北校区、南校区、高新校区、先研院、科学岛、其他。Phase 6 不引入 POI、地图 API 或距离计价。

### 3.6 费用与保证金汇总

费用接口只统计本地记录：

- 服务费记录总数；
- 已支付服务费金额；
- 待支付服务费金额；
- 按业务目标类型分组的服务费金额；
- 角色申请保证金申请数与金额；
- 按角色类型分组的保证金金额：跑腿接单者、二手发布者、店铺商家。

该接口不展示支付渠道密钥、支付 token、外部支付单敏感字段，也不改变 API-Transfer-Station 支付中心边界。

## 4. CSV 导出设计

### 4.1 导出范围

Phase 6 提供管理员可点击下载的 CSV：

1. 跑腿任务运营表；
2. 二手商品运营表；
3. 店铺预约运营表；
4. 项目广告运营表；
5. 举报治理摘要表；
6. 服务费与角色保证金摘要表。

所有导出接受同一日期范围参数，并使用 UTF-8 with BOM，方便中文 Excel 打开。

### 4.2 字段边界

CSV 只包含运营所需字段，例如 ID、标题摘要、状态、校区、金额、创建时间、更新时间、处理状态和公开昵称。导出不得包含：

- `.env` 内容；
- SMTP 密码；
- JWT secret；
- 支付中心 token；
- 支付宝私钥/公钥正文；
- 用户密码哈希；
- 邮箱验证码哈希；
- 登录会话 token；
- 非必要的完整联系方式。

若某个导出需要展示联系方式，Phase 6 默认只展示“是否已开放联系”或脱敏摘要，不导出完整微信/QQ。

### 4.3 不记录导出日志

根据当前 Phase 6 范围选择，CSV 导出不写导出审计日志，因此不需要新增 V11 迁移。后续若 Phase 9/10 需要合规级审计，可单独设计导出日志表和权限策略。

## 5. 前端设计

### 5.1 页面组织

在现有 `frontend/src/views/AdminOperationsView.vue` 上增强，不新增大型独立 BI 页面。页面结构：

- 顶部日期范围筛选；
- 总览卡片区；
- 业务漏斗标签页；
- 校区分析标签页；
- 费用与保证金标签页；
- CSV 导出区；
- 保留并整理现有任务、店铺预约、项目广告等运营表格入口。

### 5.2 可视化方式

不引入重型图表库。Phase 6 使用 Element Plus 组件、数字卡、进度条、表格、Tag、简单分布条展示数据。这样能控制包体和实现复杂度，也更适合当前课程项目与小服务器部署。

### 5.3 交互行为

- 日期范围变化后刷新所有分析卡片和表格。
- 各 CSV 按当前日期范围导出。
- API 加载失败时显示清晰错误提示，不影响其他已加载区域。
- 空数据状态明确说明“当前时间范围暂无数据”。
- 移动端保持基本可读，但 Phase 6 不做全站移动 UX 重构。

## 6. API 草案

建议新增或扩展以下接口：

- `GET /api/admin/ops/analytics/overview`
- `GET /api/admin/ops/analytics/funnels`
- `GET /api/admin/ops/analytics/zones`
- `GET /api/admin/ops/analytics/fees`
- `GET /api/admin/ops/exports/tasks.csv`
- `GET /api/admin/ops/exports/goods.csv`
- `GET /api/admin/ops/exports/shop-orders.csv`
- `GET /api/admin/ops/exports/project-ads.csv`
- `GET /api/admin/ops/exports/governance.csv`
- `GET /api/admin/ops/exports/fees.csv`

所有接口继续使用现有课程原型安全边界，不在 Phase 6 中做认证授权硬化。CSV 接口返回 `text/csv; charset=UTF-8`，文件名包含导出类型和日期范围。

## 7. 错误处理与性能

后端沿用 `BusinessException` 和 `ApiResponse<T>` 处理 JSON API 错误。CSV 端点在参数错误时可以返回 API 错误响应或 HTTP 400 文本错误，实施计划中应选择与现有控制器风格一致的方式。

性能策略：

- 使用聚合查询和有限的 Top N；
- 避免一次性导出无限历史，最大日期范围 366 天；
- CSV 导出字段保持简洁；
- 不做复杂联表展示用户隐私字段；
- 不在生产服务器反复运行重型本地构建或压力测试。

## 8. 验证计划

Phase 6 完成后至少验证：

1. 后端 Docker 构建成功，Maven package 在 Docker 中完成；
2. 前端 Docker 构建成功；
3. API smoke 返回 HTTP 200：overview、funnels、zones、fees、至少两个 CSV 导出；
4. `/admin/ops` 浏览器检查：日期范围、总览卡片、业务漏斗、校区分析、费用汇总、CSV 导出按钮；
5. 回归检查 `/goods`、`/tasks`、`/shops`、`/project-ads`、`/admin/governance`、`/credit` 无白屏；
6. 检查 CSV 中不包含 secret、token、支付宝密钥、密码哈希或非必要完整联系方式。

完整构建优先在服务器低影响 Docker build 中完成，不在本地安装依赖。

## 9. 非目标

Phase 6 不做：

- 支付中心强化；
- 认证授权硬化；
- 移动 UX 全面重构；
- 实时流式分析；
- 行为埋点系统；
- 分析快照表；
- 导出审计日志；
- 新增 Flyway 迁移；
- 交易本金托管；
- 逐单保证金冻结；
- 校园 POI、地图 API 或距离计价规则。

## 10. Self-review

- Placeholder scan: No TBD/TODO placeholders remain. Optional future export logging is explicitly out of scope.
- Internal consistency: The design consistently uses real-time aggregation, existing tables, no V11 migration, and `/admin/ops` enhancement.
- Scope check: The work fits one Phase-4-sized subsystem: one backend ops expansion, one admin UI refinement, CSV exports, docs, and verification.
- Ambiguity check: Date range behavior, CSV privacy boundary, payment boundary, and non-goals are explicitly defined.
