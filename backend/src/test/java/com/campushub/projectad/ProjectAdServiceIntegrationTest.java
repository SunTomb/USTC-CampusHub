package com.campushub.projectad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class ProjectAdServiceIntegrationTest {

    @Autowired
    private ProjectAdService projectAdService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createdProjectAdIsPendingReviewAndHiddenFromPublicList() {
        User publisher = userRepository.findByEmail("alice@campus.example").orElseThrow();

        ProjectAdSummary created = projectAdService.create(publisher.getId(), request("LOGIN_ONLY", LocalDateTime.now().plusDays(7)));

        assertThat(created.status()).isEqualTo("PENDING_REVIEW");
        assertThat(projectAdService.listPublic(null, null, "招募", null))
                .extracting(ProjectAdSummary::id)
                .doesNotContain(created.id());
    }

    @Test
    void approvedProjectAdAppearsInPublicList() {
        User publisher = userRepository.findByEmail("alice@campus.example").orElseThrow();
        User admin = userRepository.findByEmail("admin@campus.example").orElseThrow();
        ProjectAdSummary created = projectAdService.create(publisher.getId(), request("LOGIN_ONLY", LocalDateTime.now().plusDays(7)));

        ProjectAdSummary approved = projectAdService.approve(created.id(), admin.getId(), new ProjectAdReviewRequest("内容完整", null));

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.publishedAt()).isNotNull();
        assertThat(projectAdService.listPublic("TEAM_UP", null, "招募", null))
                .extracting(ProjectAdSummary::id)
                .contains(approved.id());
    }

    @Test
    void expiredProjectAdIsHiddenFromPublicList() {
        User publisher = userRepository.findByEmail("alice@campus.example").orElseThrow();
        User admin = userRepository.findByEmail("admin@campus.example").orElseThrow();
        ProjectAdSummary created = projectAdService.create(publisher.getId(), request("LOGIN_ONLY", LocalDateTime.now().plusDays(1)));
        projectAdService.approve(created.id(), admin.getId(), new ProjectAdReviewRequest("短期活动", null));

        assertThat(projectAdService.listPublic(null, null, "招募", null))
                .extracting(ProjectAdSummary::id)
                .contains(created.id());
    }

    @Test
    void loginOnlyContactIsVisibleOnlyToLoggedInViewer() {
        User publisher = userRepository.findByEmail("alice@campus.example").orElseThrow();
        User admin = userRepository.findByEmail("admin@campus.example").orElseThrow();
        User viewer = userRepository.findByEmail("bob@campus.example").orElseThrow();
        ProjectAdSummary created = projectAdService.create(publisher.getId(), request("LOGIN_ONLY", LocalDateTime.now().plusDays(7)));
        projectAdService.approve(created.id(), admin.getId(), new ProjectAdReviewRequest("通过", null));

        ProjectAdDetailSummary anonymousDetail = projectAdService.getDetail(created.id(), null);
        ProjectAdDetailSummary viewerDetail = projectAdService.getDetail(created.id(), viewer.getId());

        assertThat(anonymousDetail.contactVisible()).isFalse();
        assertThat(anonymousDetail.contactInfo()).isNull();
        assertThat(viewerDetail.contactVisible()).isTrue();
        assertThat(viewerDetail.contactInfo()).isEqualTo("project-wechat");
        assertThat(viewerDetail.viewCount()).isGreaterThan(anonymousDetail.viewCount());
    }

    @Test
    void publisherCannotCloseAnotherUsersProjectAd() {
        User publisher = userRepository.findByEmail("alice@campus.example").orElseThrow();
        User another = userRepository.findByEmail("bob@campus.example").orElseThrow();
        ProjectAdSummary created = projectAdService.create(publisher.getId(), request("PUBLIC", LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> projectAdService.close(created.id(), another.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只能操作自己的项目广告");
    }

    private ProjectAdRequest request(String contactVisibility, LocalDateTime expiresAt) {
        return new ProjectAdRequest(
                "CampusHub 体验项目招募",
                "TEAM_UP",
                "招募前后端同学一起打磨校园项目",
                "我们正在完善校园服务平台，需要同学参与产品体验、前端开发和运营测试。",
                "Vue,SpringBoot,校园服务",
                "EAST",
                null,
                "https://example.com/project",
                "project-wechat",
                contactVisibility,
                expiresAt);
    }
}
