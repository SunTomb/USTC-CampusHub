package com.campushub.orders;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @ActiveProfiles("test") @Transactional
class OrderDeskIntegrationTest {
    @Autowired OrderDeskService desk;
    @Autowired OrderFeedback feedback;
    @Autowired CatalogRatings ratings;
    @Autowired JdbcTemplate sql;

    @Test void twoBookingParticipantsNotAnUnrelatedUser() {
        assertThat(desk.detail(1L,"BOOKING",1).record().role()).isEqualTo("CUSTOMER");
        assertThat(desk.detail(3L,"BOOKING",1).record().role()).isEqualTo("MERCHANT");
        assertThatThrownBy(()->desk.detail(2L,"BOOKING",1)).isInstanceOf(RuntimeException.class);
        assertThat(desk.list(1L,"BOOKING","ALL",0,1).items()).hasSize(1);
    }
    @Test void onlyCompletedParticipantMayReviewAndComplainOnce() {
        assertThatThrownBy(()->feedback.review(1L,"BOOKING",1,4,"方便")).isInstanceOf(RuntimeException.class);
        sql.update("update service_orders set status='COMPLETED' where id=1");
        feedback.review(1L,"BOOKING",1,4,"方便"); feedback.review(3L,"BOOKING",1,1,"准时");
        assertThat(ratings.forItem(1).score()).isEqualByComparingTo("4");
        assertThat(ratings.forItem(1).count()).isEqualTo(1);
        assertThatThrownBy(()->feedback.review(1L,"BOOKING",1,5,"重复")).isInstanceOf(RuntimeException.class);
        feedback.complain(1L,"BOOKING",1,"物品损坏","外壳划伤");
        assertThat(desk.detail(1L,"BOOKING",1).record().complained()).isTrue();
        assertThatThrownBy(()->feedback.complain(1L,"BOOKING",1,"重复反馈",null)).isInstanceOf(RuntimeException.class);
        assertThat(sql.queryForObject("select count(*) from report_records where target_type='DESK_BOOKING' and reporter_id=1 and status='OPEN'",Integer.class)).isOne();
    }

    @Test void shopWeightsRatedServicesEquallyAndIgnoresUnratedAndLegacy() {
        assertThat(ratings.shops().get(2L)).isNull();
        for(int i=0;i<4;i++) sql.update("insert into reviews(reviewer_id,target_user_id,target_type,target_id,rating,rated_item_id) values(1,2,'DESK_BOOKING',?,?,?)",900+i,i<3?5:1,i<3?2:3);
        assertThat(ratings.shops().get(2L)).isEqualByComparingTo("3.0");
        assertThat(ratings.read(2,0).entries()).hasSize(3);
        assertThat(ratings.forItem(1).score()).isNull();
    }

    @Test void sellerSeesOneListingBeforeAnyIntentAndApplicantsGetNoWorkspaceEligibility(){
        var p=desk.list(1,"GOODS","ALL",0,20);
        assertThat(p.items()).anyMatch(i->i.kind().equals("GOODS_LISTING")&&i.id()==1);
        sql.update("insert into goods_intents(goods_id,buyer_id,seller_id,contact_snapshot,status) values(1,2,1,'private','OPEN'),(1,3,1,'private','OPEN')");
        assertThat(desk.list(1,"GOODS","ALL",0,20).items()).filteredOn(i->i.objectId()==1).singleElement().satisfies(i->assertThat(i.interested()).isEqualTo(2));
        var applicant=desk.detail(2,"TASK",1).record();assertThat(applicant.role()).isEqualTo("APPLICANT");assertThat(applicant.canComplain()).isFalse();
        assertThat(desk.list(1,"PROJECT","REVIEW",0,20).total()).isZero();
    }

    @Autowired com.campushub.moderation.ModerationService moderation;
    @Autowired com.campushub.review.ReviewService legacyReviews;
    @Test void publicContentReportsAndOrderFeedbackAreSeparate(){
        var shop=new com.campushub.moderation.ReportRequest("SHOP",1L,"冒用资料",null);
        var report=moderation.report(1L,shop);assertThat(report.status()).isEqualTo("OPEN");
        assertThatThrownBy(()->moderation.report(1L,shop)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->moderation.report(3L,shop)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->moderation.report(1L,new com.campushub.moderation.ReportRequest("DESK_BOOKING",1L,"测试",null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->legacyReviews.create(1L,new com.campushub.review.ReviewRequest(2L,"GOODS",1L,5,null))).isInstanceOf(RuntimeException.class);
        sql.update("update project_ads set status='PENDING_REVIEW' where id=2");
        assertThatThrownBy(()->moderation.report(1L,new com.campushub.moderation.ReportRequest("PROJECT_AD",2L,"测试",null))).isInstanceOf(RuntimeException.class);
    }
}
