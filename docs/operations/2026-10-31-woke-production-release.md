# 蜗壳 CampusHub 生产发布记录

## 发布结果

2026-10-31，经用户明确授权，已将 `8bd1cd4`（资金软关闭、协议准入与蜗壳品牌）部署到 `https://ustc.suntomb.qzz.io`。

- 生产目录：`/opt/campushub`。
- 分支：`feat/phase15-editorial-bento-ui`；从 `a57fa81` 快进到 `8bd1cd4`，未合并 master。
- 应用镜像和运行代码以 `8bd1cd4` 为准；后续仅文档提交不需要重复重建。
- 本文件是生产发布记录；`woke-boundary-verification.md` 的自动测试及流程联调部分是此前的隔离验收记录，两者不能混写。

## 发布与数据保护

1. 发布前确认生产仓库干净并保留旧版后端、前端镜像标签：
   - `campushub-backend:rollback-before-8bd1cd4`
   - `campushub-web:rollback-before-8bd1cd4`
2. 在服务器本地生成数据库备份 `/var/backups/campushub/pre-8bd1cd4-20261031T052021Z.sql.gz`；大小 19176 字节，权限 600，`gzip -t` 通过。未下载或展示备份内容，未进行生产恢复演练；压缩文件校验不等于完成恢复验收。
3. 生产拉取明确的应用提交后，串行构建 `campushub-backend`、`campushub-web`，没有安装本地依赖。
4. 仅重建并启动上述两个应用容器；MySQL 容器没有重启，保持健康。未修改或重启 API-Transfer-Station。
5. 不读取或输出真实 `.env` 内容，只追加已知的非敏感覆盖键，并在新容器内逐项确认：
   - `CAMPUSHUB_FINANCIAL_FEATURES_ENABLED=false`
   - `CAMPUSHUB_BETA_DEMO_RESET_ENABLED=false`
   - `CAMPUSHUB_MAIL_FROM_NAME=蜗壳 CampusHub`
6. 没有重置用户密码，没有在生产创建测试身份申请、支付、提现、业务订单或信用调整。访问商品/项目详情等 GET 仍可能产生应用原有的浏览统计，不能据此声称数据库绝对零变化。

邮件显示名的配置已生效；本次没有发出真实测试邮件。支付中心连接及密钥处理架构没有改变，合法历史支付回调仍按原鉴权和幂等边界处理。

## 构建、迁移与运行验证

| 检查 | 结果 |
|---|---|
| 生产后端 Docker 构建 | Maven `BUILD SUCCESS`；Dockerfile 使用 `maven.test.skip=true` |
| 生产前端 Docker 构建 | vue-tsc/Vite 通过；仍有主包大于 500 kB 的已知警告 |
| 后端真实测试 | 发布前隔离环境执行 26 套件、78 项通过；不是本次 Docker 构建执行的测试 |
| 前端真实测试 | 发布前 10 文件、36 项通过，并重新完成本地构建 |
| Flyway | V15 `role agreements`，`success=1`；V1–V14 未改动 |
| 协议版本 | 4 份 v1.0，均为 ACTIVE |
| 历史同意记录 | 迁移后为 0，没有为原身份伪造同意记录 |
| 既有身份 | 迁移后保留 32 条角色关联；浏览器现有账号的全部角色仍存在，无补签要求 |
| 容器 | 后端与 Web 运行，检查时重启次数为 0；MySQL 健康且未重启 |
| 公开资金状态 | `/api/platform/features` 返回 `financialEnabled=false` |

应用镜像 ID：

- 后端：`sha256:86d57b7cbf7e72922e7d9cd8d817b8802e10da812fa04e95ae6e84d559c3c8f9`
- Web：`sha256:ac9c388903da259b1d9794eb2dc1e5db1fb4f34abc71e380f5e934f9718ce748`

**V15 现在已在生产应用。以后只能新增 V16+，不得修改 V1–V15、删除 Flyway 成功记录或直接回退数据库。** 旧镜像用于保留恢复选项，但旧代码不具备本次资金关闭边界，不能简单回滚后继续开放公网；如确需回滚，应另行评估维护隔离、资金入口及 V15 兼容性，优先采用修复版本。

## API 与浏览器验证

服务器经 Web 代理完成 15/15 项检查：

- HTTP 200 且业务响应成功：`/api/platform/features`、`/api/goods`、`/api/tasks`、`/api/shops`、`/api/project-ads`。
- HTTP 200 且完整正文、版本为 v1.0：`/api/platform/protocols/USER_SERVICE`、`RUNNER`、`GOODS_PUBLISHER`、`SHOP_MERCHANT`。
- 匿名 HTTP 401：`/api/auth/me`、`/api/admin/wallet/recharges`、`/api/admin/payment/orders`、`/api/task-applications`、`/api/users`、`/api/wallet/accounts`。
- 额外经公网 HTTPS 核对跑腿协议接口为 HTTP 200。

Playwriter 使用浏览器已有登录会话进行只读验证，没有读取令牌或密码：

- `/roles`：系统名称为蜗壳 CampusHub；五张身份卡均显示既有已开通状态；原角色保留，无钱包导航。
- `/wallet`：显示暂未开放，仅有“查看服务协议”“返回市集”，没有支付链接。
- `/admin/wallet`：显示只读历史说明，唯一业务按钮为“刷新记录”；充值与提现查询均为 HTTP 200。
- `/admin/payment`：显示只读说明，业务按钮为“刷新对账”；订单与回调事件查询均为 HTTP 200。
- `/admin/ops`：运营概览、业务分类、校区、费用、任务、问题、身份、项目和预约查询返回 HTTP 200，未见错误提示或应用 JavaScript pageerror。
- 游客首页没有钱包和管理员入口；公开商品详情没有线上担保操作，联系方式不对游客揭示。

本轮没有向生产提交金融写请求或身份同意请求；拒绝写入、协议版本校验、自动开通、商家待核验、旧回调兼容及并发保护的实际流程证据来自发布前隔离测试。

## 截图与答辩提示词

已更新本地指定文件：

`docs/midterm-defense/2026-10-30/Codex制作提示词_校园集5分钟中期答辩PPT.md`

新版公网素材在同目录 `references-woke-production/`，共 8 张 1600×1000 PNG，包含首页、既有身份页、跑腿协议、商品详情、店铺、项目、用户服务协议、跑腿大厅。身份页账号与历史金额已遮挡。

Playwriter 扩展的截图多次超时，但 DOM/接口检查正常；最终保留成功的身份页截图，其余 7 张公开页面使用已安装驱动和独立无头 Chrome 重新采集，无新增依赖、无登录凭据复制。公开采集记录没有 JavaScript pageerror 或 HTTP 4xx/5xx。管理员及钱包页面完成了只读验证，但没有交付其可用公网截图，不以旧隔离截图冒充。

原有 `references-woke-preview/` 的 9 张隔离流程图保留。提示词明确区分公网页面、隔离申请流程与历史示例数据，已将主讲状态改为新版已部署；仍为 6 页主讲、2 页备用、210+70+20 秒。

这些课程材料及截图保持本地，未随应用或本运维记录推送到 GitHub。

## 已知限制

- 公开协议页在一次交互中出现加载失败，重新加载及后续独立浏览器复查恢复；未复现持续接口故障。不得把整段工具执行过程描述为从未超时。
- `/goods/1` 的既有商品封面无法显示，尽管页面/API 正常；截图保留真实现状，未为演示补造图片或修改生产记录。答辩优先演示首页与店铺。
- 项目列表仍有用户自填的历史标题 `USTC CampusHub`；本次只改系统品牌，没有擅改用户发布内容。提示词要求不将该旧标题沿用为平台品牌。
- 跑腿大厅当前为空状态，没有添加假任务来填充演示。
- 未进行移动端布局修复或完整移动端复验。
- 协议文本不是法律合规认证；运营者身份、权利请求联系渠道、保留期限、存量资金处理仍需人工确认。
- 资金关闭不表示历史余额已经清退，也不表示金融代码可以未经另行审查直接重新开放。
