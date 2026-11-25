package com.campushub.payment;

import com.campushub.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Desktop checkout with an operator-managed JCA signing handle, never a private key file. */
@Component
@ConditionalOnProperty(name="campushub.payment.provider", havingValue="alipay-page")
public class AlipayPageProvider implements PaymentProvider {
    static final String PROVIDER="ALIPAY_PAGE", GATEWAY="https://openapi.alipay.com/gateway.do";
    static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    final String appId, seller; final boolean enabled;
    private final ObjectMapper json;
    private PrivateKey signingKey; private PublicKey verifyKey;
    public AlipayPageProvider(ObjectMapper json,
            @Value("${campushub.alipay.app-id:}") String appId, @Value("${campushub.alipay.seller-id:}") String seller,
            @Value("${campushub.alipay.enabled:false}") boolean enabled,
            @Value("${campushub.alipay.key-alias:}") String alias,
            @Value("${campushub.alipay.platform-public-key:}") String publicKey) throws GeneralSecurityException,java.io.IOException {
        this.json=json; this.enabled=enabled; this.appId=appId; this.seller=seller;
        if (!appId.matches("[0-9]{16}")) throw new IllegalArgumentException("Invalid Alipay AppID");
        if (enabled) {
            if (!seller.matches("[0-9]{16}") || alias.isBlank()) throw new IllegalArgumentException("Alipay merchant and signing handle required");
            KeyStore store=KeyStore.getInstance("PKCS11"); store.load(null,null);
            signingKey=(PrivateKey)store.getKey(alias,null);
            verifyKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            if(signingKey==null || !(verifyKey instanceof java.security.interfaces.RSAPublicKey rsa) || rsa.getModulus().bitLength()<2048) throw new InvalidKeyException("Alipay verification/signing handle unavailable");
        }
    }
    public String providerName(){return PROVIDER;}
    public PaymentCreation createWebPayment(PaymentRequest r){
        if(!enabled) throw new BusinessException("支付宝收款未启用");
        try { return checkout(r,LocalDateTime.now(ZONE),signingKey); }
        catch(Exception ex){throw new BusinessException("支付宝订单签名失败");}
    }
    PaymentCreation checkout(PaymentRequest r, LocalDateTime now, PrivateKey key) throws Exception {
        var p=new TreeMap<String,String>(); p.put("app_id",appId);p.put("method","alipay.trade.page.pay"); p.put("format","JSON");p.put("charset","utf-8");p.put("sign_type","RSA2");p.put("timestamp",TIME.format(now));p.put("version","1.0");
        p.put("notify_url","https://ustc.suntomb.cn/api/payment/alipay/notify");p.put("return_url","https://ustc.suntomb.cn/wallet");
        p.put("biz_content",json.writeValueAsString(Map.of("out_trade_no",r.orderNo(),"total_amount",r.amount().setScale(2,RoundingMode.UNNECESSARY).toPlainString(),"subject",r.subject(),"product_code","FAST_INSTANT_TRADE_PAY","time_expire",TIME.format(now.plusMinutes(r.expireMinutes())))));
        var sig=Signature.getInstance("SHA256withRSA");sig.initSign(key);sig.update(canonical(p));p.put("sign",Base64.getEncoder().encodeToString(sig.sign()));
        String url=GATEWAY+"?"+p.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return new PaymentCreation(PROVIDER,r.orderNo(),r.orderNo(),url,"PENDING",now.plusMinutes(r.expireMinutes()),"电脑网站支付");
    }
    static byte[] canonical(Map<String,String> fields){return new TreeMap<>(fields).entrySet().stream().filter(e->!e.getValue().isEmpty()).map(e->e.getKey()+"="+e.getValue()).collect(Collectors.joining("&")).getBytes(StandardCharsets.UTF_8);}
    boolean authentic(Map<String,String> fields){return enabled && verify(fields,verifyKey,appId,seller);}
    static boolean verify(Map<String,String> fields, PublicKey platform, String app, String merchant){
        if(!app.equals(fields.get("app_id")) || !merchant.equals(fields.get("seller_id")) || !"RSA2".equals(fields.get("sign_type")))return false;
        try { var p=new TreeMap<>(fields);var signature=p.remove("sign");p.remove("sign_type");var v=Signature.getInstance("SHA256withRSA");v.initVerify(platform);v.update(canonical(p));return v.verify(Base64.getDecoder().decode(signature)); } catch(Exception ex){return false;}
    }
    public PaymentStatus queryPaymentStatus(String orderNo){throw new BusinessException("以支付宝已验签的异步通知为准");}
}
