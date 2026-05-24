# CampusHub Admin Playbook

## 1. Principles

- Use admin actions to protect Beta users, not to silently rewrite history.
- Write short, factual notes. Do not enter passwords, tokens, private keys, or payment secrets into admin notes.
- Prefer restriction and review workflows over direct data deletion.
- Never ask users for their password.

## 2. Reports and violations

1. Open `/admin/governance`.
2. Review report target, reason, reporter, and current status.
3. Use `IN_REVIEW` for reports requiring investigation.
4. Use `REJECTED` when the report is clearly invalid.
5. Use `RESOLVED` when action is taken.
6. Create a violation when the target behavior breaks platform rules.
7. Choose severity based on impact:
   - low: misleading content, duplicate spam, minor rude wording;
   - medium: repeated spam, unsafe transaction behavior, false information;
   - high: fraud, harassment, malicious content, serious safety risk.
8. Apply credit delta and restrictions consistently with the severity.

## 3. Credit adjustments and restrictions

Use `/admin/governance` for manual credit adjustments and restrictions.

- Add credit for verified helpful behavior or successful issue resolution.
- Subtract credit for confirmed violations.
- Use posting freeze for unsafe content publishing.
- Use service freeze for unsafe task/shop fulfillment behavior.
- Use disabled state only for serious or repeated abuse.

## 4. Wallet operations

Use `/admin/wallet`.

- Alipay recharge should normally settle through payment-center callback.
- WeChat recharge requires manual review because Phase 9 treats it as offline/manual.
- Withdrawal requests freeze balance at submit time.
- Approve only after checking account, amount, and risk notes.
- Complete after offline payout is done.
- Reject if information is invalid or risk is unresolved; the frozen amount should be released by the application flow.

## 5. Payment monitor

Use `/admin/payment`.

- Pending orders may be unpaid or waiting for callback.
- Paid orders should have a matching callback event.
- Failed callback events should be investigated with payment-center logs without printing tokens or Alipay payload secrets.
- Repeated callbacks should be idempotent; do not manually duplicate wallet flows.

## 6. Review queues

Use `/admin/review`, `/admin/ops`, and related admin tabs.

- Role applications: runner and goods publisher are normally auto-approved after deposit; shop merchant requires review.
- Project ads: approve only campus-relevant, safe, non-fraudulent content.
- Shop content: verify service scope and contact safety.

## 7. User support scripts

Safe support response:

> 请不要发送密码、验证码或支付密钥。请提供页面路径、操作时间、订单/记录编号和问题截图，我们会在后台核对状态。

Unsafe requests to reject:

- asking admin to change a real user's password without identity verification;
- asking for raw database dumps;
- asking for Alipay key contents or payment-center tokens;
- asking to delete production data broadly.
