# 蜗壳同路品牌与图标更新 · 2026-11-10

## 状态

已在分支 `feat/woke-tonglu-branding` 完成系统品牌与图标替换，并经用户另行授权于 2026-11-11 发布应用提交 `971adab`。名称为 **蜗壳同路 CampusHub**，简称 **蜗壳同路**。

前后端已部署至原生产站点，V16 成功；后续仅文档提交不改变运行镜像。以下保留实现与发布前验证记录，实际发布范围见文末“生产发布”节。

## 图标选择

用户提供的素材目录：

`output/mini-program/woke-tonglu-2026-11-10/`

采用实际存在的 `gradient` 系列图片，不依据说明文件里已经过时的不带 `gradient` 文件名猜测素材。

| 用途 | 网站资源 | 来源与处理 | 文件体积 |
|---|---|---|---|
| 桌面侧栏、手机页头、目录图标 | `frontend/public/woke-tonglu-logo-144.png` | 直接复制提供的 144px PNG，逐字节一致 | 约 44.23 KiB |
| 浏览器标签 favicon | `frontend/public/woke-tonglu-favicon-48.png` | 从提供的 512px PNG 使用 Lanczos 缩小到 48×48 | 4786 字节 |
| iPhone/iPad 添加到主屏幕 | `frontend/public/woke-tonglu-apple-touch-180.png` | 从提供的 512px PNG 缩小到 180×180 | 44626 字节 |

页面图标显示尺寸保持桌面 44px、手机 38px。144px 素材能够覆盖常见高像素密度屏幕，不让每个页面加载 512px 图片或约 1.89 MiB 的原稿。

所有新引用使用新的文件名，避免继续命中旧 `/campushub-logo.png`、`/favicon.png` 的缓存。旧资源保留但不再被页面引用；用户源图、生成提示词和历史答辩素材没有被改写或删除。

HTML 同时更新标题、应用名称、Apple 主屏幕简称，以及图标的尺寸声明。已添加到手机主屏幕的旧书签可能由系统保留旧图标，不能承诺更新网页后一定自动刷新既有书签缓存。

## 名称覆盖范围

- 桌面侧栏、手机页头、目录标题、图标 alt、通行证及页脚。
- 首页、登录注册、个人信息、身份申请、公开协议与信用页的系统文案。
- 导航说明及最高管理员权限描述。
- 注册邮件标题、正文及发件人默认显示名。
- 保留的财务模块中系统自带的充值/支付标题和提示；没有更改金融开关或开放这些功能。
- README 当前说明、生产配置模板与 Compose 中的非敏感品牌默认值。

不改实际网站域名、邮箱域名、API 路径、数据库名称、包名、用户账号、用户历史发布内容或旧课程报告。

## 协议历史保护

现有协议全文存储于数据库，不能通过修改已应用的 V15 或前端显示替换来悄悄改变用户曾同意的文本。

新增：

- `backend/src/main/resources/db/migration/V16__woke_tonglu_brand_agreements.sql`
- 对应 H2 测试迁移，SQL 内容相同。

V16 从原四份 v1.0 创建 v1.1，只替换 `蜗壳 CampusHub` 为 `蜗壳同路 CampusHub`。原版本标记为 SUPERSEDED，但原 ID、全文、标题、同意时间及身份申请关联不改动，也不新造同意记录。

新申请要求当前 v1.1；已经开通的身份继续保留，不因名称变化要求补签。已有待核验申请仍保留其原协议关联。四类协议的其他责任、信息处理与资金边界条文逐字不变。

V1–V15 的主迁移及对应历史测试迁移均未修改。后续授权发布已成功应用 V16，此后 V1–V16 均不可改写。

## 手机页头兼容

视觉复查发现，320px 访客页头原布局会让较长的 `CampusHub` 文字与“目录”按钮重叠；原先仅检查元素是否越出视口不能发现该问题。

新增实际文本矩形与按钮矩形的重叠断言，先复现失败，再将不超过 360px 的页头调整为左侧完整品牌、右侧两行操作按钮。390px 及更宽手机保持原来的一行控件布局，桌面不变。没有用省略号隐藏新名称。

## 验证结果

- Vitest：11 个文件、50 项通过。
- vue-tsc 与 Vite 生产构建通过；仍有既知大 chunk 和依赖 pure-comment 警告。
- 本地生产构建的 9 项浏览器回归通过：品牌图标、目录、抽屉宽度、弹层遮挡、日期范围、横屏日期、320px 个人信息、恢复登录并发、网络失败重试。
- 品牌检查覆盖游客及登录态的 320/360/390/430px 页头，确认文字与按钮不重叠；1280px 桌面也已截图复核。
- 浏览器请求确认新图标实际可加载，PNG 尺寸分别为 144、48、180px。144px 网站图标与用户提供图像逐字节一致。
- `scripts/check-brand-migration.py`：4 项数据变换检查通过，包括四份新版本只更名、历史签署快照与申请关联保持、重执行不产生重复版本，以及主/测试 SQL 内容一致。
- `git diff --check` 与回归脚本语法检查通过；已核对历史迁移没有改动。

**验证边界：**迁移脚本检查使用 Python 自带 SQLite 的内存数据库验证数据变换逻辑，不是 MySQL/H2/Flyway 实际执行证明。本机未找到 Java/Maven，未安装新依赖，也没有运行后端 Maven 测试。Java 测试期望已同步到 v1.1，实际后端测试与 MySQL/H2 迁移验证需要在后续具备工具链的隔离环境完成。

浏览器使用本地构建与合成 API 数据，没有真实登录、发信、支付或业务写入；不是生产验收，也不等于真机 Safari/微信内置浏览器测试。

## 复查入口

```bash
npm --prefix frontend test
npm --prefix frontend run build
python -I scripts/check-brand-migration.py
```

浏览器入口仍为 `scripts/mobile-ui-smoke.cjs`，新增 `brand-assets` 案例，使用方式见 [手机回归说明](mobile-ui-regression.md)。

本地截图和结果在 `.claude/verification/woke-tonglu-brand/`，包括：

- `woke-tonglu-guest-320.png`
- `woke-tonglu-guest-390.png`
- `woke-tonglu-member-320.png`
- `woke-tonglu-member-390.png`
- `woke-tonglu-desktop.png`
- `results.json`

临时预览在脚本结束时自动关闭；上述截图不是公网部署截图。

## 生产发布 · 2026-11-11

- 应用提交 `971adab` 已推送并部署到 `https://ustc.suntomb.qzz.io`，分支 `feat/woke-tonglu-branding`，未合并 master。
- 后端 Maven 打包和前端 Docker 构建成功；Dockerfile 跳过 Java 测试。按用户要求未重复全套测试或手机矩阵，仅做必要上线检查。
- V16 在生产 MySQL 中执行成功：4 份 v1.1 为 ACTIVE，4 份 v1.0 保留为 SUPERSEDED；没有补造同意记录。
- Playwriter 实际打开公网站点，确认页面标题和导航名称为新品牌，144px 页面图标、48px favicon、180px Apple 图标均正常加载；公开协议返回新名称/v1.1，资金接口为 false。
- 本地 Python 的直接公网请求曾收到 403，改用真实浏览器完成上述检查，没有将该请求描述为通过，也未关闭 TLS 或修改访问控制。
- 服务器只追加已知非敏感显示名及关闭开关；邮件显示名为 `蜗壳同路 CampusHub`，资金与 Beta 重置均为 false。未读取环境秘密，没有发真实测试邮件。
- 前后端容器已替换，MySQL 保持运行且健康；没有操作支付中心、阿里云新机、DNS 或用户业务记录。
- 备份：`/var/backups/campushub/pre-971adab-20261110T162001Z.sql.gz`，24364 字节、权限 600，压缩校验通过；未进行恢复演练。
- 回滚镜像标签：`campushub-backend:rollback-before-971adab`、`campushub-web:rollback-before-971adab`。
- 新后端镜像：`sha256:410b09b1cc56877707693e108ffffa3b47e8a84c9ec5960e2eb92e2f652b826f`；新 Web 镜像：`sha256:d109ec6736c89003a06c24e2f5bd7ac0e69b82bbc37eaca4f4ede5edec0bbec3`。

资金功能仍关闭。历史素材、用户发布内容和原协议快照均保留；本轮未执行数据库恢复或回滚。
