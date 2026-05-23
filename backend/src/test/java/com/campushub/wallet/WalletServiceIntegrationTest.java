package com.campushub.wallet;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class WalletServiceIntegrationTest {

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private WalletFlowRepository walletFlowRepository;

    @Test
    void walletAccountAndFlowExposePhase9LedgerFields() {
        WalletAccount wallet = walletAccountRepository.findByUserId(1L).orElseThrow();

        wallet.credit(new BigDecimal("25.50"));
        wallet.freeze(new BigDecimal("10.00"));
        wallet = walletAccountRepository.saveAndFlush(wallet);

        WalletFlow flow = walletFlowRepository.saveAndFlush(new WalletFlow(
                wallet,
                wallet.getUser(),
                "WF-P9-LEDGER-001",
                "OUT",
                "ESCROW_FREEZE",
                new BigDecimal("10.00"),
                wallet.getBalance(),
                wallet.getFrozenBalance(),
                "GOODS_ESCROW",
                1L,
                "P9-LEDGER-001",
                null,
                "SYSTEM",
                null,
                "Phase 9 ledger field smoke test"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("215.50"));
        assertThat(wallet.getFrozenBalance()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(wallet.getUpdatedAt()).isNotNull();
        assertThat(flow.getFlowType()).isEqualTo("ESCROW_FREEZE");
        assertThat(flow.getAvailableBalanceAfter()).isEqualByComparingTo(new BigDecimal("215.50"));
        assertThat(flow.getFrozenBalanceAfter()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(flow.getIdempotencyKey()).isEqualTo("P9-LEDGER-001");
    }
}
