package com.campushub.goods;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleService;
import com.campushub.common.BusinessException;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:goods-offline;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "campushub.secondhand.service-fee.enabled=true", "campushub.secondhand.service-fee.intent-amount=9.00",
        "campushub.financial.enabled=true"}) // Even legacy fee switches must not charge for an offline intent.
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoodsOfflineIntegrationTest {
    @Autowired GoodsService service;
    @Autowired UserRoleService roles;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;
    @BeforeEach void setup() { roles.assignRole(1L, "ROLE_GOODS_PUBLISHER"); }
    long publish() {
        return service.publish(1L,new CreateGoodsRequest(1L,"书","有笔记",BigDecimal.TEN,null,"九成新","WEST","图书馆","OFFLINE_MEETUP","INTENT_ONLY")).id();
    }
    GoodsIntentSummary want(long id,long buyer) { return service.createIntent(id,buyer,new GoodsIntentRequest("下午方便")); }
    int count(String sql,Object... p) { return db.queryForObject(sql,Integer.class,p); }
    @Test void retriesAndSettlementLeaveOneWinnerWithoutMovingMoney() {
        long id=publish(); int fees=count("select count(*) from service_fee_records"), flows=count("select count(*) from wallet_flows"), orders=count("select count(*) from goods_orders");
        assertThat(service.getDetail(id,null).contactVisible()).isFalse();
        assertThat(service.getDetail(id,1L).canMarkSold()).isFalse();
        assertThatThrownBy(()->want(id,1L)).isInstanceOf(BusinessException.class);
        var first=want(id,2L); assertThat(want(id,2L).id()).isEqualTo(first.id()); want(id,3L);
        assertThat(service.getDetail(id,2L).myIntentStatus()).isEqualTo("OPEN");
        assertThat(service.getDetail(id,1L).canMarkSold()).isTrue();
        assertThatThrownBy(()->service.markSold(id,1L,null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.markSold(id,1L,4L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.markSold(id,2L,3L)).isInstanceOf(ResponseStatusException.class);
        var sold=service.markSold(id,1L,2L); assertThat(sold.status()).isEqualTo("SOLD");
        service.markSold(id,1L,2L); // A lost success response must never emit another notification.
        assertThatThrownBy(()->service.markSold(id,1L,3L)).isInstanceOf(BusinessException.class);
        assertThat(service.getDetail(id,2L).myIntentStatus()).isEqualTo("COMPLETED");
        assertThat(service.getDetail(id,3L).myIntentStatus()).isEqualTo("CLOSED");
        assertThat(service.getDetail(id,3L).contactVisible()).isFalse();
        var losingRetry=want(id,3L); assertThat(losingRetry.status()).isEqualTo("CLOSED");
        assertThat(losingRetry.contactSnapshot()).isNull();
        assertThat(want(id,2L).contactSnapshot()).isEqualTo(first.contactSnapshot());
        assertThat(count("select count(*) from station_notifications where target_type='GOODS' and target_id=?",id)).isEqualTo(4);
        assertThat(count("select count(*) from goods_orders")).isEqualTo(orders);
        assertThat(count("select count(*) from wallet_flows")).isEqualTo(flows);
        assertThat(count("select count(*) from service_fee_records")).isEqualTo(fees);
    }
    @Test void simultaneousIntentsAndCompetingSellersDecisionsAreSerialized() throws Exception {
        long id=publish(); var pool=Executors.newFixedThreadPool(2);
        try {
            var start=new CountDownLatch(1);
            var a=pool.submit(()->{start.await();return want(id,2L).id();});
            var b=pool.submit(()->{start.await();return want(id,2L).id();});
            start.countDown(); assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));
            want(id,3L); var select=new CountDownLatch(1);
            var x=pool.submit(()->{select.await();return select(id,2L);});
            var y=pool.submit(()->{select.await();return select(id,3L);});
            select.countDown(); assertThat(x.get(10,TimeUnit.SECONDS)+y.get(10,TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(count("select count(*) from goods_intents where goods_id=? and status='COMPLETED'",id)).isEqualTo(1);
            assertThat(count("select count(*) from goods_intents where goods_id=? and status='CLOSED'",id)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    int select(long id,long buyer) { try {service.markSold(id,1L,buyer);return 1;} catch(BusinessException rejected) {return 0;} }
    @Test void withdrawnListingKeepsHistoryAndPrivateIntentCollectionUsesOnlyTheJwtActor() throws Exception {
        long id=publish(); want(id,2L);
        mvc.perform(get("/api/goods/{id}/intents?sellerId=1",id).header("Authorization","Bearer "+tokens.issueToken(4L,"test"))).andExpect(status().isForbidden());
        service.offShelf(id,1L); service.offShelf(id,1L);
        assertThat(service.listPublished()).noneMatch(g->g.id()==id);
        assertThat(service.listIntents(id,1L)).singleElement().satisfies(i->assertThat(i.status()).isEqualTo("CLOSED"));
        assertThatThrownBy(()->service.markSold(id,1L,2L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->want(id,4L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.getDetail(id,null)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.getDetail(id,2L).canEdit()).isFalse();
        assertThat(want(id,2L).contactSnapshot()).isNull();
    }
    @Test void adminRemovalUsesSameTerminalRulesAsSellerAndNotifiesOpenBuyers() {
        long id=publish(); want(id,2L); service.offShelfByAdmin(id);
        assertThat(service.getDetail(id,2L).myIntentStatus()).isEqualTo("CLOSED");
        assertThat(count("select count(*) from station_notifications where target_type='GOODS' and target_id=? and recipient_id=2",id)).isOne();
        long sold=publish(); want(sold,3L); service.markSold(sold,1L,3L);
        assertThatThrownBy(()->service.offShelfByAdmin(sold)).isInstanceOf(BusinessException.class);
        assertThat(service.getDetail(sold,1L).status()).isEqualTo("SOLD");
    }

    @Test void historicallySoldWithoutABuyerIsNeverBackfilled() {
        long id=publish();want(id,2L);
        db.update("update goods set status='SOLD',sold_to_user_id=null where id=?",id);
        assertThatThrownBy(()->service.markSold(id,1L,2L)).isInstanceOf(BusinessException.class);
        assertThat(count("select count(*) from goods_intents where goods_id=? and status='COMPLETED'",id)).isZero();
        assertThat(want(id,2L).contactSnapshot()).isNull(); // historical OPEN row on SOLD without a verified winner
    }
}
