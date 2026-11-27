package com.campushub.interaction;

import com.campushub.auth.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:interaction-security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Sql(statements = {
        "insert into goods(id, seller_id, category_id, title, description, price, condition_level, trade_location, status) values (8100, 1, 1, 'fixture', 'fixture', 5, 'GOOD', 'WEST', 'ON_SALE'), (8101, 1, 1, 'other', 'other', 5, 'GOOD', 'WEST', 'PUBLISHED')",
        "insert into project_ads(id, publisher_id, title, description, contact_info, status) values (8100, 1, 'fixture', 'fixture', 'fixture', 'APPROVED')",
        "insert into shops(id, owner_id, name, description, service_area, status) values (8100, 1, 'fixture', 'fixture', 'WEST', 'APPROVED')"
})
class InteractionSecurityIntegrationTest {
    private static final String BASE = "/api/interactions";
    private static final long ID = 8100L, OWNER = 1L, STUDENT = 2L, ADMIN = 4L;

    @Autowired MockMvc mvc;
    @Autowired JwtTokenService jwt;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;

    @ParameterizedTest
    @ValueSource(strings = {"GOODS", "PROJECT_AD", "SHOP"})
    void publicTargetsAllowInteractionsButNotEnumeration(String type) throws Exception {
        String path = BASE + "/" + type + "/" + ID;
        mvc.perform(get(path + "/comments")).andExpect(status().isUnauthorized());
        withUser(comment(type, ID, null), STUDENT).andExpect(jsonPath("$.success").value(true));
        withUser(get(path + "/comments"), STUDENT).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("fixture comment"));
        withUser(favorite(post(BASE + "/favorites"), type), STUDENT).andExpect(jsonPath("$.success").value(true));
        withUser(get(path + "/favorites"), STUDENT).andExpect(status().isForbidden());
        for (long reader : new long[]{OWNER, ADMIN}) {
            withUser(get(path + "/favorites"), reader).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].userId").value((int) STUDENT));
        }
        withUser(favorite(delete(BASE + "/favorites"), type).param("userId", Long.toString(STUDENT)), OWNER)
                .andExpect(status().isForbidden());
        withUser(favorite(delete(BASE + "/favorites"), type), STUDENT).andExpect(jsonPath("$.success").value(true));
        withUser(get(path + "/favorites"), OWNER).andExpect(jsonPath("$.data.length()").value(0));
    }

    @ParameterizedTest
    @CsvSource({"GOODS,OFF_SHELF", "PROJECT_AD,PENDING_REVIEW", "PROJECT_AD,REJECTED", "SHOP,CLOSED"})
    void hiddenTargetsLimitReadsAndWritesButAllowRemovingOwnFavorite(String type, String targetStatus) throws Exception {
        switch (type) {
            case "GOODS" -> db.update("update goods set status=? where id=?", targetStatus, ID);
            case "PROJECT_AD" -> db.update("update project_ads set status=? where id=?", targetStatus, ID);
            case "SHOP" -> db.update("update shops set status=? where id=?", targetStatus, ID);
            default -> throw new AssertionError(type);
        }
        db.update("insert into favorites(user_id, target_type, target_id) values (?, ?, ?)", STUDENT, type, ID);
        String path = BASE + "/" + type + "/" + ID;
        withUser(get(path + "/comments"), STUDENT).andExpect(status().isForbidden());
        withUser(comment(type, ID, null), STUDENT).andExpect(status().isForbidden());
        withUser(favorite(post(BASE + "/favorites"), type), STUDENT).andExpect(status().isForbidden());
        for (long reader : new long[]{OWNER, ADMIN}) {
            withUser(get(path + "/comments"), reader).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
        withUser(favorite(delete(BASE + "/favorites"), type), STUDENT).andExpect(jsonPath("$.success").value(true));
        withUser(get(path + "/favorites"), OWNER).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void replyRequiresBothTheOriginalTargetTypeAndId() throws Exception {
        byte[] response = withUser(comment("GOODS", ID, null), STUDENT).andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsByteArray();
        long parent = json.readTree(response).path("data").required("id").longValue();
        withUser(comment("PROJECT_AD", ID, parent), STUDENT).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("父评论不属于当前内容"));
        withUser(comment("GOODS", ID + 1, parent), STUDENT).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("父评论不属于当前内容"));
        withUser(comment("GOODS", ID, parent), STUDENT).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parentId").value((int) parent));
    }

    @Test
    void invalidTypesAreRejectedEvenForAdminAndUnfavorite() throws Exception {
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[]{
                get(BASE + "/goods/8100/comments"), get(BASE + "/goods/8100/favorites"), comment("goods", ID, null),
                favorite(post(BASE + "/favorites"), "goods"), favorite(delete(BASE + "/favorites"), "goods")}) {
            withUser(request, ADMIN).andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("不支持的互动目标"));
        }
        withUser(get(BASE + "/GOODS/99999999/comments"), ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void goodsDetailShowsPublicCommentsToGuestsAndRestrictsHiddenCommentsToSeller() throws Exception {
        db.update("insert into comments(user_id, target_type, target_id, content, status) values (1, 'GOODS', ?, 'public thread', 'VISIBLE')", ID);
        mvc.perform(get("/api/goods/{id}", ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments[0].content").value("public thread"));
        db.update("insert into goods_orders(order_no, goods_id, buyer_id, seller_id, amount, status, contact_snapshot) values ('interaction-audience', ?, 2, 1, 5, 'COMPLETED', 'fixture')", ID + 1);
        db.update("insert into comments(user_id, target_type, target_id, content, status) values (1, 'GOODS', ?, 'private thread', 'VISIBLE')", ID + 1);
        db.update("update goods set status='OFF_SHELF' where id=?", ID + 1);
        mvc.perform(get("/api/goods/{id}", ID + 1)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.comments").doesNotExist());
        withUser(get("/api/goods/{id}", ID + 1), STUDENT).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comments.length()").value(0));
        withUser(get("/api/goods/{id}", ID + 1), OWNER).andExpect(jsonPath("$.data.comments[0].content").value("private thread"));
    }

    private ResultActions withUser(MockHttpServletRequestBuilder request, long userId) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.issueToken(userId, "fixture")));
    }

    private MockHttpServletRequestBuilder comment(String type, long id, Long parent) throws Exception {
        return post(BASE + "/comments").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(new CommentRequest(type, id, parent, "fixture comment")));
    }

    private MockHttpServletRequestBuilder favorite(MockHttpServletRequestBuilder request, String type) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(new FavoriteRequest(type, ID)));
    }
}
