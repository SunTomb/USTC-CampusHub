# 杭州正式切换与独立支付宝（2026-11-24）

> 切换后续：按用户要求已退役香港 CampusHub，不再有可启动的旧后端或旧数据库卷。共享 Caddy 直接返回网页跳转和旧 API 的 410。杭州已核验的最终快照在 `/opt/campushub-backups/cutover.5uEnFG0v`；两份更早的升级前 SQL 已另存 `/opt/campushub-backups/hk-historical.wteSXLJx`。这些是历史备份，不包含杭州切换后的新写入。香港本次实测释放 1,833,070,592 字节，未清理全机 Docker 缓存。下文的旧机停写状态仅为切换当时的记录。

本记录取代当天早些时候的准备状态。杭州 `https://ustc.suntomb.cn` 为唯一写入站点。新 JWT issuer/密钥独立；旧登录会话需重新登录。

- 香港仅停止 CampusHub 后端，专用数据库持久化 `super_read_only=ON`；共享支付中心和共享入口保持运行。旧站 Web 独立挂载 `migration-20261124/legacy.conf`：网页 GET/HEAD 临时重定向新域名，旧 API 返回 410，不转发带令牌的旧请求。香港 Compose 已留 `migration-20261124/docker-compose.before-cutover`。
- 源端最终停写快照 `/opt/campushub-backups/final-snapshots/frozen-20261124T100100Z-KroKDwJQ`，杭州副本 `/opt/campushub-backups/cutover.5uEnFG0v`。数据库、上传归档、逐表计数、表名、上传文件清单均通过 SHA-256。V16 恢复后 42 表/229 行逐表相等，上传目录 0 文件；未补造旧附件。杭州未使用 227 行演练快照作为生产数据。
- V17、V18、V19 成功。V19 只扩大两个现有支付链接列，以容纳 RSA2 签名和 URL 编码后的业务字段。生产镜像 `campushub-hz/backend:20261124-alipay`，其中 JAR SHA-256 `518cb94e42c647d910d36a1dd2f472b4a6e517bccbffe95c6e8c579c34d47e39`。后端替换后注意重新加载内层 Nginx 以解析新容器地址；Web 重建后同理重新加载外层 Nginx。
- 生产 Web/API、匿名 401、资金关闭、两种回调的拒绝响应与旧站跳转均已检查。浏览器已看到正式首页、品牌、网站备案（非占位页）。SMTP 使用新发件地址与 TLS 连接检查；未用真实用户做注册、下单、充值或发送验证邮件。备份清单由 root 保管，不输出用户数据。
- **不能直接反切旧库或清除其只读标志**。杭州已对外开放，有新写入的可能。优先回退杭州应用镜像；数据库回迁必须先停杭州写入并保全、比对和恢复最新数据。

## 支付宝已配置到哪一步

开放平台该应用的可调用产品列表包含「电脑网站支付」。不是手机网站支付、微信内支付或支付宝小程序。后端选择独立 `alipay-page` provider，AppID `2021007124661040`。不使用原支付中心的凭据、签名器或地址。公开 `financialEnabled=false`，独立 `CAMPUSHUB_ALIPAY_ENABLED=false`，mock 关闭。两个开关必须分别对待；不得因为签名器配置就开放钱包或充值。

电脑收银台采用 `alipay.trade.page.pay` / `FAST_INSTANT_TRADE_PAY`；`notify_url` 在每次订单签名里传 `https://ustc.suntomb.cn/api/payment/alipay/notify`，不是旧回调，也不是 OAuth 授权回调。同步浏览器返回不入账。异步通知须验 RSA2、AppID、seller_id、订单归属、金额与状态，再在事务内加锁幂等入账。生产关闭时返回 HTTP 503 / `failure`；永不伪造支付宝通知成功。历史桥接回调仍为 410。

123 项后端测试通过；新测试使用临时 RSA 密钥和 H2，覆盖签名查询串、关闭状态、签名篡改、商户绑定、金额不匹配和重复支付通知。**并非商户真实签名或资金联调。**

## 第一次授权真实收款之前

1. 商户管理员核对已签约产品、商户 PID 与应用的签名模式。本实现按普通 RSA2 公钥模式；若应用实际使用证书模式，必须先接入对应证书 SN 流程，不能直接开启。
2. 由商户管理员在可供后端使用的受控签名设施中准备对应应用密钥，或在设施内生成不可导出 RSA 密钥并通过支付宝官方流程更新**应用公钥**。此操作涉及生产密钥变更，需单独授权。不要复制下载的私钥到服务器、代码、容器环境或聊天中。配置 JDK 可访问的 PKCS#11 提供者、已认证会话和签名别名。若使用不支持 PKCS#11 的云 KMS，须实现该 KMS 的签名适配器，不可将名称填入别名代替。
3. 用受控配置注入 `CAMPUSHUB_ALIPAY_SELLER_ID`、`CAMPUSHUB_ALIPAY_KEY_ALIAS` 和 `CAMPUSHUB_ALIPAY_PLATFORM_PUBLIC_KEY`（平台验签的 X.509 SubjectPublicKeyInfo DER Base64，不是应用公钥）；无须交给助手查看。当前未配置上述设施，故不具备无人值守真实签名条件。
4. 单独验证签名设施、沙箱全链路、同一交易的并发/幂等、真实产品许可及监控后，再决定是否开放具体业务。不要仅翻转全局金融开关：它还影响其他功能。此次没有开启任何真实支付，没有更改开放平台的签约、密钥或网关。
