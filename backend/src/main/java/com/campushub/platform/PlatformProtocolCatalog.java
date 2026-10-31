package com.campushub.platform;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlatformProtocolCatalog {

    private final Map<String, PlatformProtocolDefinition> definitions = Map.of(
            "USER_SERVICE", new PlatformProtocolDefinition("USER_SERVICE", "蜗壳 CampusHub 用户服务协议"),
            "RUNNER", new PlatformProtocolDefinition("RUNNER_SERVICE", "顺路跑腿服务协议"),
            "GOODS_PUBLISHER", new PlatformProtocolDefinition("GOODS_PUBLISHER_SERVICE", "闲置发布与交易规范"),
            "SHOP_MERCHANT", new PlatformProtocolDefinition("SHOP_MERCHANT_SERVICE", "技能店主理人服务协议"));

    public PlatformProtocolDefinition currentForRole(String roleType) {
        String key = roleType == null ? "" : roleType.trim().toUpperCase(Locale.ROOT);
        PlatformProtocolDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("不支持的协议身份：" + roleType);
        }
        return definition;
    }
}
