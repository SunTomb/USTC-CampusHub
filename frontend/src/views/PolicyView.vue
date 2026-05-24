<template>
  <section class="page policy-page">
    <div class="page-header">
      <div>
        <p class="eyebrow">Beta readiness</p>
        <h2>校集 CampusHub 使用说明与风险提示</h2>
        <p class="page-subtitle">本页面说明平台服务边界、隐私规则、交易风险和钱包资金规则，适用于内测和 Beta 使用。</p>
      </div>
    </div>

    <el-card class="content-card">
      <h3>服务协议摘要</h3>
      <p>CampusHub 是校园二手交易与学生微服务信息平台，提供跑腿任务、二手商品、学生店铺、项目广告、站内通知、信用治理和钱包服务费能力。</p>
      <p>平台用于校园内测与课程设计演示，不承诺覆盖所有真实商业场景。管理员可对违规内容、异常交易和风险账户进行审核、限制或下架。</p>
    </el-card>

    <el-card class="content-card">
      <h3>隐私与联系方式</h3>
      <p>注册和业务流程可能收集邮箱、昵称、学号占位、微信或 QQ 联系方式。联系方式用于交易达成后的沟通，并按业务规则向相关交易方展示。</p>
      <p>请不要在留言、举报、审核备注或公开内容中填写密码、验证码、支付 token、身份证号、银行卡号或支付宝密钥等敏感信息。</p>
    </el-card>

    <el-card class="content-card">
      <h3>交易风险提示</h3>
      <p>线下微信/QQ 沟通和交易需要用户自行核验对方身份、商品状态、服务范围和交付结果。平台治理能力可以处理举报、信用和限制，但不能完全消除线下交易风险。</p>
      <p>遇到欺诈、骚扰、虚假宣传或安全问题，请保留记录并通过举报或管理员渠道反馈。</p>
    </el-card>

    <el-card class="content-card">
      <h3>钱包与支付边界</h3>
      <p>CampusHub 钱包用于余额、冻结余额、角色保证金、服务费、充值、提现和二手线上托管演示。真实支付宝收款由外部 API-Transfer-Station 支付中心处理。</p>
      <p>CampusHub 不读取、不复制、不保存支付宝私钥/公钥正文。提现和部分微信充值仍需要管理员人工审核。</p>
    </el-card>

    <el-card class="content-card">
      <h3>Beta 数据边界</h3>
      <p>内测期间可能存在标记为 Beta 或 Demo 的账号、订单、钱包记录和审核记录。这些数据用于验收矩阵和演示，不代表真实用户交易。</p>
      <p>真实用户数据修复、删除或生产恢复必须经过单独确认，不能用宽泛删除或重置命令处理。</p>
    </el-card>
  </section>
</template>
