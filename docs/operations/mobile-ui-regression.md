# 手机界面回归检查

## 用途

`scripts/mobile-ui-smoke.cjs` 在本机浏览器中运行实际 Vue 前端，使用浏览器内的合成数据验证手机布局和交互。它只接受回环地址，并拦截全部 `/api/` 请求，因此不连接生产数据库，不使用真实账号，不提交真实任务、预约或资金操作。

它不是后端端到端测试，也不能代替 iOS Safari、微信内置浏览器、系统键盘与真实上传流程的真机检查。

## 环境

- 使用项目现有前端依赖。
- 使用已经安装的 Playwright 兼容驱动及 Chrome；不需要为了此检查安装本地后端或数据库。
- `CAMPUSHUB_BROWSER_DRIVER` 可以是已有 `playwright`、`playwright-core` 或兼容驱动的模块名/绝对路径。
- `CAMPUSHUB_CHROME_PATH` 指向现有 Chrome；不设置时使用驱动默认浏览器。
- 不将真实令牌、生产 `.env` 或密码传给脚本。

## 执行

在仓库根目录启动前端：

```bash
npm --prefix frontend run dev -- --host 127.0.0.1 --port 5178 --strictPort
```

另一个终端设置本机已有驱动与浏览器位置后运行：

```bash
CAMPUSHUB_BROWSER_DRIVER="<已安装驱动的模块名或绝对路径>" \
CAMPUSHUB_CHROME_PATH="<Chrome 可执行文件路径>" \
node scripts/mobile-ui-smoke.cjs
```

也可以针对构建产物验证：

```bash
npm --prefix frontend run build
npm --prefix frontend run preview -- --host 127.0.0.1 --port 5179 --strictPort
```

随后在上述测试命令中增加 `CAMPUSHUB_SMOKE_URL=http://127.0.0.1:5179`。

结果默认写入 `.claude/verification/mobile-fixes/`，包括 JSON 与关键截图。通过 `CAMPUSHUB_SMOKE_OUTPUT` 可以改为其他本地目录，不应把截图自动上传至生产。

单独检查可在命令末尾追加案例名：

- `mobile-directory`：手机“更多”目录跳转通知后关闭，资金关闭时底栏没有钱包入口。
- `drawer-width`：320/360/390/430px 下的跑腿抽屉及输入框边界，桌面 520px 宽度。
- `modal-layer`：滚到抽屉底部后的提交按钮命中测试；预约弹窗遮罩拦住底栏触控。
- `date-range`：手机日期范围面板宽度，触控选择起止日及分析接口参数，桌面双月宽度。
- `date-time`：844×390 横屏中的预约日期面板与确认按钮可达性。
- `profile-width`：320px 个人信息页无整页横向溢出。
- `session-retry`：网络错误保留会话但不显示私人页，显式重试成功，401 清除会话。
- `session-race`：重试与新导航重叠时，旧失败不得清除新验证结果或改变新导航。

脚本先等待有限 CSS 动画完成，再测量边界，避免把宽度变化过程或弹出动画当作最终布局。初次启动 Vite 会编译依赖；如果选择器超时，先核对页面错误、准备状态和接口拦截范围，不能把测试基础设施错误算成业务回归。

单元测试与类型/生产构建检查：

```bash
npm --prefix frontend test
npm --prefix frontend run build
```

此文档只说明复现方式；某一次运行是否通过，以该次完整日志和结果文件为准。
