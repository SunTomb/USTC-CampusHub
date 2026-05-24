package com.campushub.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletRechargeSummaryTest {

    @Test
    void withPaymentInfoCopiesAlipayPaymentMetadata() {
        WalletRechargeSummary summary = new WalletRechargeSummary(
                1L,
                "WR-1",
                2L,
                "学生",
                "ALIPAY",
                new BigDecimal("10.00"),
                new BigDecimal("0.06"),
                new BigDecimal("10.06"),
                "PENDING_PAYMENT",
                "CHP-WR-1",
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        WalletRechargeSummary updated = summary.withPaymentInfo("PAYMENT_CENTER", "https://pay.example.com/order/1");

        assertThat(updated.paymentProvider()).isEqualTo("PAYMENT_CENTER");
        assertThat(updated.paymentPayUrl()).isEqualTo("https://pay.example.com/order/1");
        assertThat(updated.wechatQrUrl()).isNull();
    }

    @Test
    void withWechatManualInfoCopiesQrAndNote() {
        WalletRechargeSummary summary = new WalletRechargeSummary(
                1L,
                "WR-1",
                2L,
                "学生",
                "WECHAT",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                new BigDecimal("10.00"),
                "PENDING_REVIEW",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        WalletRechargeSummary updated = summary.withWechatManualInfo("https://assets.example.com/wechat.png", "扫码后备注订单号");

        assertThat(updated.wechatQrUrl()).isEqualTo("https://assets.example.com/wechat.png");
        assertThat(updated.wechatNote()).isEqualTo("扫码后备注订单号");
        assertThat(updated.paymentPayUrl()).isNull();
    }
}
