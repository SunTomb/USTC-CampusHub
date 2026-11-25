# 杭州正式域名入口（静态准备页，不是业务切换）

2026-11-24：用户提供网站备案号 **浙ICP备2026099873号-2**。微信小程序备案配置独立，不在本次修改范围。

阿里云权威 DNS 新增 `ustc` / `A` / 默认 / `120.26.94.61` / TTL 600 秒。控制台启用；两台权威 NS 和 `223.5.5.5` 复核一致。未修改根域、NS、MX、旧域名或安全组。管理电脑的 DNS 代理曾返回保留测试地址，不能用作阿里云真实记录值。

本目录对应杭州 `/opt/campushub/domain-entry/`，**不要用演练反代配置替换它**。复用本地已有的 Nginx 镜像，使用独立 Compose 网络和配置（无上游、无数据库、无生产凭据），只向公网提供静态首页及 ACME HTTP-01 验证。`/api/` 返回 503；`127.0.0.1:18080` 仍只用于演练。已从宿主外验证 HTTP 备案链接、503 和一次性 challenge 文件，验证文件已移除。

## 完成 TLS

Certbot 已安装，续期定时器已启动。签发命令在杭州 Workbench 中等待账户联系人输入：

```sh
certbot certonly --server https://acme-v02.api.letsencrypt.org/directory --webroot -w /opt/campushub/domain-entry/acme -d ustc.suntomb.cn --cert-name ustc.suntomb.cn && /opt/campushub/domain-entry/enable-tls
```

由域名负责人自行填写接收证书通知的邮箱、阅读并决定是否接受 CA 服务条款；可选邮件分享不影响签发。不要把私钥、验证码或控制台票据发到聊天。若退出 Workbench，在同一实例重新执行上述命令即可。签发后 `enable-tls` 对 HTTPS 配置做语法测试，启动只读 TLS 网关并在本机验证可信证书及备案首页，成功后才将 HTTP 改为固定主机名的 HTTPS 重定向；保留挑战路径继续使用 HTTP。`/etc/letsencrypt/renewal-hooks/deploy/50-campushub-domain-entry` 指向同一脚本，只对指定 lineage 生效。当前未宣称签发、443、续期演练或业务上线已完成。

签发后可从外网运行 `curl -I https://ustc.suntomb.cn/` 和 `curl -I https://ustc.suntomb.cn/api/tasks`，预期分别为 200、503；在服务器执行 `certbot renew --dry-run` 检查续期。正式迁移仍需停写窗口、最新数据恢复校验及独立生产服务，不能直接向真实用户开放历史演练副本。
