package com.campushub.payment;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.MultiValueMap;
import java.util.*;

@RestController
@ConditionalOnProperty(name="campushub.payment.provider",havingValue="alipay-page")
class AlipayNotificationController {
    private final AlipayPageProvider provider; private final PaymentService service;
    AlipayNotificationController(AlipayPageProvider provider,PaymentService service){this.provider=provider;this.service=service;}
    @PostMapping(path="/api/payment/alipay/notify",consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE,produces=MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> notify(@RequestParam MultiValueMap<String,String> form){
        if(!provider.enabled)return ResponseEntity.status(503).body("failure");
        if(form.values().stream().anyMatch(v->v.size()!=1))return ResponseEntity.badRequest().body("failure");
        Map<String,String> f=form.toSingleValueMap();
        if(!provider.authentic(f))return ResponseEntity.status(403).body("failure");
        try {service.settleAlipay(f);return ResponseEntity.ok("success");}catch(RuntimeException ex){return ResponseEntity.badRequest().body("failure");}
    }
}
