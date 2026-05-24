package com.campushub.wallet;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campushub.wallet.recharge")
public class WalletRechargeProperties {

    private final Wechat wechat = new Wechat();

    public Wechat getWechat() {
        return wechat;
    }

    public static class Wechat {
        private String manualQrUrl;
        private String manualNote;

        public String getManualQrUrl() {
            return manualQrUrl;
        }

        public void setManualQrUrl(String manualQrUrl) {
            this.manualQrUrl = manualQrUrl;
        }

        public String getManualNote() {
            return manualNote;
        }

        public void setManualNote(String manualNote) {
            this.manualNote = manualNote;
        }
    }
}
