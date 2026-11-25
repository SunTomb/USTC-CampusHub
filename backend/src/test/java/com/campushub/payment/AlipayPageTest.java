package com.campushub.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AlipayPageTest {
    @Test void dormantNeedsNoKeysAndCannotSign() throws Exception {
        var provider=new AlipayPageProvider(new ObjectMapper(),"1234567890123456","",false,"","");
        assertThatThrownBy(()->provider.createWebPayment(null)).hasMessageContaining("未启用");
    }
    @Test void syntheticCheckoutSignsActualDecodedQuery() throws Exception {
        var keys=KeyPairGenerator.getInstance("RSA").generateKeyPair();
        var provider=new AlipayPageProvider(new ObjectMapper(),"1234567890123456","",false,"","");
        var req=new PaymentRequest("TEST-1","SERVICE_FEE",1L,2L,"S1",new BigDecimal("1.23"),"测试 & +","ignored",15);
        var order=provider.checkout(req,LocalDateTime.of(2026,11,24,17,0),keys.getPrivate());
        var p=new TreeMap<String,String>();for(String item:java.net.URI.create(order.payUrl()).getRawQuery().split("&")){String[] kv=item.split("=",2);p.put(kv[0],URLDecoder.decode(kv[1],StandardCharsets.UTF_8));}
        var sig=p.remove("sign");var verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(keys.getPublic());var parts=new ArrayList<String>();p.forEach((k,v)->parts.add(k+"="+v));verifier.update(String.join("&",parts).getBytes(StandardCharsets.UTF_8));assertThat(verifier.verify(Base64.getDecoder().decode(sig))).isTrue();
        assertThat(p.get("method")).isEqualTo("alipay.trade.page.pay");assertThat(p.get("biz_content")).contains("1.23","测试 & +","FAST_INSTANT_TRADE_PAY");assertThat(p.get("notify_url")).endsWith("/api/payment/alipay/notify");
    }
    @Test void validatesPlatformSignatureAmountAndMerchantBinding() throws Exception {
        var keys=KeyPairGenerator.getInstance("RSA").generateKeyPair();var f=new TreeMap<String,String>();f.put("app_id","app");f.put("seller_id","seller");f.put("total_amount","1.23");var sig=Signature.getInstance("SHA256withRSA");sig.initSign(keys.getPrivate());sig.update("app_id=app&seller_id=seller&total_amount=1.23".getBytes(StandardCharsets.UTF_8));f.put("sign",Base64.getEncoder().encodeToString(sig.sign()));f.put("sign_type","RSA2");assertThat(AlipayPageProvider.verify(f,keys.getPublic(),"app","seller")).isTrue();assertThat(AlipayPageProvider.verify(f,keys.getPublic(),"app","different")).isFalse();f.put("total_amount","2.00");assertThat(AlipayPageProvider.verify(f,keys.getPublic(),"app","seller")).isFalse();
    }
}
