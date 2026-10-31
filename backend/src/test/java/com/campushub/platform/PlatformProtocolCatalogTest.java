package com.campushub.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class PlatformProtocolCatalogTest {
    private final PlatformProtocolCatalog catalog = new PlatformProtocolCatalog();

    @Test
    void mapsEachBusinessRoleToItsOwnAgreement() {
        assertThat(catalog.currentForRole("RUNNER").code()).isEqualTo("RUNNER_SERVICE");
        assertThat(catalog.currentForRole("GOODS_PUBLISHER").title()).contains("闲置");
        assertThat(catalog.currentForRole("SHOP_MERCHANT").title()).contains("店主理人");
        assertThat(catalog.currentForRole("USER_SERVICE").title()).contains("蜗壳 CampusHub");
    }

    @Test
    void doesNotInventAgreementsForAdministratorRoles() {
        assertThatThrownBy(() -> catalog.currentForRole("TRADE_ADMIN")).isInstanceOf(IllegalArgumentException.class);
    }
}
