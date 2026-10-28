# 校集 CampusHub · 平台图标（单张图片）生成提示词

本文件提供用于生成 **校集 CampusHub** 左上角品牌图标（单张图片，不修改项目任何其他代码文件）的提示词。可根据你使用 Codex 的方式选择下方对应版本直接复制粘贴。

---

## 版本一：让 Codex 直接在项目内生成单张高精图标图片文件（推荐）

> 适用场景：在终端或 IDE 中对 Codex 下达任务，只要求它输出一张图标图片文件（如 `frontend/public/logo.png` 或 `frontend/public/logo.svg`），**严禁创建或修改项目中的任何其他文件**。

```text
请只为项目生成一张平台品牌图标图片文件，保存在 `frontend/public/campushub-logo.png`（同时可保留矢量源文件 `frontend/public/campushub-logo.svg`），**绝对不要创建或修改项目中的任何其他文件（不要改 Vue 组件、HTML 或 CSS）**。

### 图标用途与尺寸规格
- 用途：中国科学技术大学（USTC）校园生活共同体平台「校集 CampusHub」页面左上角品牌徽章（App Icon / Brand Emblem）。
- 画布尺寸：1024 × 1024 px 正方形（高分辨率 PNG，背景为圆角矩形徽章或透明通道，确保缩放到 40×40 px 和 32×32 px 时依然边缘锐利、主体饱满、零锯齿）。

### 视觉美学与配色（瀚海书院 × 流光市集）
- 整体气质：兼具中国科大的学术书院底蕴、理性的科技探索精神与温暖的校园烟火市集感，呈现出 Apple App Store 级别的精致徽章质感（Tactile Craft Emblem），拒绝廉价扁平的 AI 模板感。
- 严格遵循平台配色：
  1. 底板主色：科大深海墨蓝渐变（`#0b1626` → `#16325b` → `#1f457a`），四角圆润的现代圆角矩形盾徽（Squircle），带有细腻的暗部内阴影与 1.5px 哑光金属内边框。
  2. 主体图形色：学术鎏金金属渐变（`#c6922c` → `#e6be68` → `#f9e7b2`），辅以宣纸暖白高光（`#f7f5f0`）与微弱的珊瑚暖橙点缀（`#d95f43`）。

### 核心图形构图（居中对称、简洁耐看）
- 徽章外环：精致的双线鎏金书院印章内框，四角微镶传统书院云纹/几何切角。
- 徽章中心（三重合一意象）：
  1. 下半部为「翻开的立体书卷与瀚海波涛」几何线条，致敬中国科大“瀚海星云”与书院治学传统；
  2. 中上部为交织环绕的「双椭圆星轨 / 四叶梅花结」（致敬科大梅花精神与科研星轨，同时寓意东西南北高新五区相连，以及闲置、跑腿、小店、项目四大校园市集汇聚）；
  3. 视觉正中心悬浮一颗璀璨立体的「鎏金四角北极星芒（North Star）」。
- 注意事项：图片内**不要包含任何汉字或英文字母**（因为网页图标右侧已经有“校集 CampusHub”文字，图标本身应为纯粹、高辨识度的图形徽章），确保在 40×40 小尺寸下线条不粘连、轮廓一眼可辨。
```

---

## 版本二：纯图像生成模型提示词（Image Generation Prompt · 中文版）

> 适用场景：如果通过 GPT-4o / DALL·E / Midjourney / Flux / Ideogram 等生图工具直接生成单张图片。

```text
一枚极高品质的现代学院风 App 品牌图标徽章（App Icon Emblem），正方形 1:1 构图，居中展示圆角矩形徽章（Squircle）。
主题是中国科学技术大学（USTC）校园生活平台“校集”。
风格融合“中式书院人文美学”与“现代精致拟物微立体工艺（Tactile Enamel & Metallic Pin Badge）”。
配色方案：深海墨蓝与普鲁士蓝丝绒磨砂质感底板（#0b1626 到 #16325b），搭配温润的香槟鎏金（#c6922c 到 #f7e3a3）与宣纸暖白（#f7f5f0）立体浮雕线条。
中心图案：下半部分是简洁几何化的翻开书卷与瀚海波浪弧线，上半部分是优雅交错的双环原子星轨，星轨交汇成一朵对称的四瓣科大梅花轮廓，正中央镶嵌一颗闪耀的鎏金四角星芒。
徽章边缘带有细腻的鎏金内嵌细线边框与柔和的顶部环境高光。
画面干净纯粹，图标内部不含任何文字或字母，无杂乱背景，极高辨识度，4K 超清渲染，光线柔和考究，兼具学府庄重感与温暖生活气息。
```

---

## 版本三：纯图像生成模型提示词（Image Generation Prompt · 英文版）

> 适用场景：对英文理解更精准的图像生成模型。

```text
A masterfully crafted modern academic app icon emblem, 1:1 square composition, centered squircle badge design for "CampusHub (USTC Campus Community)".
Aesthetic style: Warm Editorial Academy meets Tactile Craft Enamel & Brushed Gold Badge, inspired by Apple Design Award icons and classic university heraldry.
Color palette: Deep USTC ink-navy matte background (#0b1626 to #16325b gradient) with warm champagne gold (#c6922c to #f7e3a3) and warm parchment ivory (#f7f5f0) sculpted metallic lines.
Central motif: A clean, iconic geometric composition combining an open book / ocean wave crest at the base ("Hanhai" sea of knowledge), intertwined with dual orbital rings that form a subtle four-petal plum blossom silhouette, crowned with a radiant four-point gold North Star at the exact center.
Framed by a delicate double-line gold seal border inside the squircle with soft top-left studio rim lighting and subtle depth shadows.
Strictly NO text, NO letters, NO typography inside the icon. Clean, minimal, bold geometry that remains crystal clear at 40x40 pixels, ultra-detailed 4k vector-like render.
```
