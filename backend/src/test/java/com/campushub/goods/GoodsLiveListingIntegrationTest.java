package com.campushub.goods;

import com.campushub.auth.JwtTokenService;
import com.campushub.auth.UserRoleService;
import com.campushub.orders.OrderDeskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:goods-live-listing;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class GoodsLiveListingIntegrationTest {
    @Autowired GoodsService goods;
    @Autowired UserRoleService roles;
    @Autowired OrderDeskService orders;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired JwtTokenService jwt;
    @Autowired ObjectMapper json;
    @BeforeEach void grantRole(){roles.assignRole(1L,"ROLE_GOODS_PUBLISHER");}
    String auth(long id){return "Bearer "+jwt.issueToken(id,"fixture");}
    long listing(){return goods.publish(1L,new CreateGoodsRequest(1L,"原商品","最初说明",BigDecimal.TEN,null,"九成新","WEST","图书馆","OFFLINE_MEETUP","INTENT_ONLY")).id();}
    GoodsIntentSummary want(long id,long buyer){return goods.createIntent(id,buyer,new GoodsIntentRequest("想自提"));}
    long photo(String path,String key,int color) throws Exception {
        var out=new ByteArrayOutputStream();var img=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);img.setRGB(0,0,color);ImageIO.write(img,"png",out);
        var result=mvc.perform(multipart(path).file(new MockMultipartFile("file","sample.png","image/png",out.toByteArray())).param("clientRequestId",key).header("Authorization",auth(1)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).path("data").path("id").longValue();
    }
    @Test void orderOpensSameListingAndCurrentPhotosWithoutChangingTheSale() throws Exception {
        long id=listing();String path="/api/goods/"+id,album="/api/catalog-media/GOODS/"+id;
        long old=photo(album,"first",0x23561a);long intent=want(id,2).id();want(id,3);goods.markSold(id,1L,2L);
        assertThat(orders.get(2L,"GOODS_INTENT",intent).objectId()).isEqualTo(id);
        mvc.perform(get(path).header("Authorization",auth(1))).andExpect(jsonPath("$.data.canEdit").value(true));
        goods.update(id,1L,new UpdateGoodsRequest(1L,"修改后的标题","新的说明",new BigDecimal("25.50"),null,"八成新","EAST","南门","SELF_PICKUP","INTENT_ONLY"));
        mvc.perform(multipart(album).file(new MockMultipartFile("file","sample.png","image/png",new byte[]{1})).param("clientRequestId","not-owner").header("Authorization",auth(2)))
                .andExpect(status().isForbidden());
        mvc.perform(delete(album+"/"+old).header("Authorization",auth(2))).andExpect(status().isForbidden());
        long replacement=photo(album,"second",0x7142a0);
        mvc.perform(delete(album+"/"+old).header("Authorization",auth(1))).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get(path).header("Authorization",auth(2))).andExpect(header().string(HttpHeaders.CACHE_CONTROL,"private, no-store")).andExpect(jsonPath("$.data.id").value(id)).andExpect(jsonPath("$.data.title").value("修改后的标题")).andExpect(jsonPath("$.data.price").value(25.5)).andExpect(jsonPath("$.data.status").value("SOLD"));
        mvc.perform(get(album).header("Authorization",auth(2))).andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].id").value(replacement)).andExpect(jsonPath("$.data[0].published").value(false));
        mvc.perform(get(album+"/"+replacement).header("Authorization",auth(2))).andExpect(content().contentType("image/png"));
        mvc.perform(get(album+"/"+old).header("Authorization",auth(2))).andExpect(status().isNotFound());
        mvc.perform(get(album+"/"+replacement)).andExpect(status().isNotFound());mvc.perform(get(album).header("Authorization",auth(4))).andExpect(status().isNotFound());
        assertThat(orders.get(2L,"GOODS_INTENT",intent).amount()).isEqualByComparingTo("10.00");assertThat(orders.get(2L,"GOODS_INTENT",intent).canReview()).isTrue();
        assertThat(goods.getDetail(id,3L).contactVisible()).isFalse();assertThatThrownBy(()->want(id,4)).isInstanceOf(RuntimeException.class);
    }
    @Test void deletionFreesImagesKeepsOrderAndHasADistinctGoneResponse() throws Exception {
        long id=listing();String path="/api/goods/"+id,album="/api/catalog-media/GOODS/"+id;long intent=want(id,2).id();long image=photo(album,"gone",0x882211);goods.markSold(id,1L,2L);
        mvc.perform(delete(path).header("Authorization",auth(2))).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(delete(path).header("Authorization",auth(1))).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get(path).header("Authorization",auth(2))).andExpect(status().isGone()).andExpect(jsonPath("$.message").value("该商品已删除")).andExpect(jsonPath("$.data").doesNotExist());
        mvc.perform(get(album+"/"+image).header("Authorization",auth(1))).andExpect(status().isNotFound());
        assertThat(db.queryForObject("select count(*) from catalog_images where kind='GOODS' and object_id=?",Integer.class,id)).isZero();
        var row=orders.get(2L,"GOODS_INTENT",intent);assertThat(row.objectId()).isEqualTo(id);assertThat(row.status()).isEqualTo("COMPLETED");assertThat(row.canComplain()).isTrue();assertThat(row.title()).isEqualTo("该商品已删除");
        mvc.perform(post("/api/orders/GOODS_INTENT/"+intent+"/review").header("Authorization",auth(2)).contentType("application/json").content("{\"rating\":4,\"content\":\"fixture\"}"))
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/orders/GOODS_INTENT/"+intent+"/complaint").header("Authorization",auth(2)).contentType("application/json").content("{\"reason\":\"fixture\"}"))
                .andExpect(jsonPath("$.success").value(true));
        assertThat(goods.listPublished()).noneMatch(g->g.id()==id);assertThatThrownBy(()->want(id,2)).isInstanceOf(RuntimeException.class);
        mvc.perform(get("/api/goods/999999999")).andExpect(status().isNotFound());
    }
    @Test void offShelfIsNotDeletedAndAccessTracksTheListingNotAnotherProduct() throws Exception {
        long id=listing(),other=listing();String path="/api/goods/"+id,album="/api/catalog-media/GOODS/"+id;photo(album,"shelf",0x331122);want(id,2);goods.offShelf(id,1L);
        mvc.perform(get(path).header("Authorization",auth(2))).andExpect(jsonPath("$.data.status").value("OFF_SHELF"));
        mvc.perform(get(album).header("Authorization",auth(2))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(delete(path).header("Authorization",auth(1))).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/api/goods/"+other)).andExpect(jsonPath("$.data.id").value(other));
    }
    @Test void openIntentEndsWhenSellerDeletesAndDifferentListingDoesNotReplaceIt() throws Exception {
        long id=listing(), intent=want(id,2).id();
        mvc.perform(delete("/api/goods/"+id).header("Authorization",auth(1))).andExpect(jsonPath("$.success").value(true));
        long another=listing();assertThat(another).isNotEqualTo(id);
        var row=orders.get(2L,"GOODS_INTENT",intent);assertThat(row.objectId()).isEqualTo(id);assertThat(row.status()).isEqualTo("CLOSED");assertThat(row.canReview()).isFalse();
        mvc.perform(get("/api/goods/"+row.objectId()).header("Authorization",auth(2))).andExpect(status().isGone());
        mvc.perform(get("/api/goods/"+another)).andExpect(status().isOk());
    }
    @Test void historicalOrderWithADifferentIdKeepsItsOwnAmountAndOnlyUnfinishedOrdersPreventDeletion() throws Exception {
        long id=listing();String path="/api/goods/"+id,album="/api/catalog-media/GOODS/"+id;
        db.update("insert into goods_orders(order_no,goods_id,buyer_id,seller_id,amount,status,contact_snapshot) values('HISTORICAL-TEST',?,2,1,7.80,'PAID','fixture')",id);
        long order=db.queryForObject("select id from goods_orders where order_no='HISTORICAL-TEST'",Long.class);
        mvc.perform(delete(path).header("Authorization",auth(1))).andExpect(jsonPath("$.success").value(false));
        db.update("update goods_orders set status='COMPLETED' where id=?",order);
        db.update("update goods set status='SOLD',sold_to_user_id=2 where id=?",id);
        long image=photo(album,"history",0x118822);
        var row=orders.get(2L,"GOODS_ORDER",order);assertThat(row.objectId()).isEqualTo(id);assertThat(row.amount()).isEqualByComparingTo("7.80");
        mvc.perform(get(album+"/"+image).header("Authorization",auth(2))).andExpect(status().isOk());
        mvc.perform(delete(path).header("Authorization",auth(1))).andExpect(jsonPath("$.success").value(true));
        assertThat(orders.get(2L,"GOODS_ORDER",order).amount()).isEqualByComparingTo("7.80");
        mvc.perform(get(path).header("Authorization",auth(2))).andExpect(status().isGone());
    }
}
