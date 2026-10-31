package com.campushub.platform;

import com.campushub.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class FinancialWriteGuard {

    public static final String UNAVAILABLE_MESSAGE = "资金与线上担保功能暂未开放，历史记录仅供查看";

    private final boolean enabled;

    @Autowired
    public FinancialWriteGuard(FinancialFeatureProperties properties) {
        this(properties.isEnabled());
    }

    FinancialWriteGuard(boolean enabled) {
        this.enabled = enabled;
    }

    public void assertEnabled() {
        if (!enabled) {
            throw new BusinessException(UNAVAILABLE_MESSAGE);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
