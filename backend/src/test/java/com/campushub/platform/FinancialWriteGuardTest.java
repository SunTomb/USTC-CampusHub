package com.campushub.platform;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.campushub.common.BusinessException;
import org.junit.jupiter.api.Test;

class FinancialWriteGuardTest {

    @Test
    void rejectsFinancialWritesWhenThePlatformFlagIsOff() {
        FinancialWriteGuard guard = new FinancialWriteGuard(false);

        assertThatThrownBy(guard::assertEnabled)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("资金与线上担保功能暂未开放");
    }

    @Test
    void allowsFinancialWritesWhenThePlatformFlagIsOn() {
        FinancialWriteGuard guard = new FinancialWriteGuard(true);

        assertThatCode(guard::assertEnabled).doesNotThrowAnyException();
    }
}
