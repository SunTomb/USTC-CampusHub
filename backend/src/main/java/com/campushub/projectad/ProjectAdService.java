package com.campushub.projectad;

import com.campushub.common.BusinessException;
import com.campushub.file.FileBindingRepository;
import com.campushub.file.FileBindingSummary;
import com.campushub.interaction.CommentRepository;
import com.campushub.interaction.FavoriteRepository;
import com.campushub.notification.NotificationService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectAdService {

    private static final String TARGET_TYPE = "PROJECT_AD";

    private final ProjectAdRepository projectAdRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final FileBindingRepository fileBindingRepository;
    private final NotificationService notificationService;

    public ProjectAdService(
            ProjectAdRepository projectAdRepository,
            UserRepository userRepository,
            FavoriteRepository favoriteRepository,
            CommentRepository commentRepository,
            FileBindingRepository fileBindingRepository,
            NotificationService notificationService) {
        this.projectAdRepository = projectAdRepository;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.fileBindingRepository = fileBindingRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ProjectAdSummary> listPublic(String adType, String campusZone, String keyword, Boolean featured) {
        LocalDateTime now = LocalDateTime.now();
        return projectAdRepository.findByStatusOrderByFeaturedDescFeaturedPriorityDescCreatedAtDesc("APPROVED").stream()
                .filter(ad -> ad.isPubliclyVisible(now))
                .filter(ad -> matches(ad.getAdType(), adType))
                .filter(ad -> matches(ad.getCampusZone(), campusZone))
                .filter(ad -> featured == null || featured.equals(ad.getFeatured()))
                .filter(ad -> matchesKeyword(ad, keyword))
                .map(ProjectAdSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectAdSummary> listFeatured() {
        LocalDateTime now = LocalDateTime.now();
        return projectAdRepository.findByFeaturedTrueAndStatusOrderByFeaturedPriorityDescCreatedAtDesc("APPROVED").stream()
                .filter(ad -> ad.isPubliclyVisible(now))
                .map(ProjectAdSummary::from)
                .toList();
    }

    @Transactional
    public ProjectAdDetailSummary getDetail(Long id, Long viewerId) {
        ProjectAd projectAd = getProjectAd(id);
        if (!projectAd.isPubliclyVisible(LocalDateTime.now()) && (viewerId == null || !projectAd.getPublisher().getId().equals(viewerId))) {
            throw new BusinessException("项目广告暂不可查看");
        }
        projectAd.increaseViewCount();
        return toDetail(projectAd, viewerId);
    }

    @Transactional(readOnly = true)
    public List<ProjectAdSummary> listByPublisher(Long publisherId) {
        return projectAdRepository.findByPublisherIdOrderByCreatedAtDesc(publisherId).stream()
                .map(ProjectAdSummary::from)
                .toList();
    }

    @Transactional
    public ProjectAdSummary create(Long publisherId, ProjectAdRequest request) {
        User publisher = getUser(publisherId);
        ProjectAd projectAd = projectAdRepository.save(new ProjectAd(publisher, request));
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary update(Long id, Long publisherId, ProjectAdRequest request) {
        ProjectAd projectAd = getProjectAd(id);
        requirePublisher(projectAd, publisherId);
        projectAd.update(request);
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary submit(Long id, Long publisherId) {
        ProjectAd projectAd = getProjectAd(id);
        requirePublisher(projectAd, publisherId);
        projectAd.submit();
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary close(Long id, Long publisherId) {
        ProjectAd projectAd = getProjectAd(id);
        requirePublisher(projectAd, publisherId);
        projectAd.closeByPublisher();
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional(readOnly = true)
    public List<ProjectAdSummary> listForAdmin(String status) {
        return projectAdRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(ad -> status == null || status.isBlank() || status.equals(ad.getStatus()))
                .map(ProjectAdSummary::from)
                .toList();
    }

    @Transactional
    public ProjectAdSummary approve(Long id, Long adminId, ProjectAdReviewRequest request) {
        ProjectAd projectAd = getProjectAd(id);
        User admin = getUser(adminId);
        projectAd.approve(admin, request == null ? null : request.note());
        notificationService.notify(projectAd.getPublisher(), "项目广告审核通过", projectAd.getTitle(), TARGET_TYPE, projectAd.getId());
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary reject(Long id, Long adminId, ProjectAdReviewRequest request) {
        ProjectAd projectAd = getProjectAd(id);
        User admin = getUser(adminId);
        projectAd.reject(admin, request == null ? null : request.note());
        notificationService.notify(projectAd.getPublisher(), "项目广告审核未通过", projectAd.getTitle(), TARGET_TYPE, projectAd.getId());
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary feature(Long id, Long adminId, ProjectAdReviewRequest request) {
        ProjectAd projectAd = getProjectAd(id);
        getUser(adminId);
        if (!"APPROVED".equals(projectAd.getStatus())) {
            throw new BusinessException("只有已通过审核的项目广告可以精选");
        }
        int priority = request == null || request.featuredPriority() == null ? 0 : request.featuredPriority();
        projectAd.feature(priority);
        notificationService.notify(projectAd.getPublisher(), "项目广告已被设为精选", projectAd.getTitle(), TARGET_TYPE, projectAd.getId());
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary unfeature(Long id, Long adminId) {
        ProjectAd projectAd = getProjectAd(id);
        getUser(adminId);
        projectAd.unfeature();
        return ProjectAdSummary.from(projectAd);
    }

    @Transactional
    public ProjectAdSummary block(Long id, Long adminId, ProjectAdReviewRequest request) {
        ProjectAd projectAd = getProjectAd(id);
        User admin = getUser(adminId);
        projectAd.block(admin, request == null ? null : request.note());
        notificationService.notify(projectAd.getPublisher(), "项目广告已被平台下架", projectAd.getTitle(), TARGET_TYPE, projectAd.getId());
        return ProjectAdSummary.from(projectAd);
    }

    private ProjectAdDetailSummary toDetail(ProjectAd projectAd, Long viewerId) {
        long favoriteCount = favoriteRepository.countByTargetTypeAndTargetId(TARGET_TYPE, projectAd.getId());
        long commentCount = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TARGET_TYPE, projectAd.getId()).size();
        boolean favorited = viewerId != null && favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(viewerId, TARGET_TYPE, projectAd.getId());
        boolean commented = viewerId != null && commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TARGET_TYPE, projectAd.getId()).stream()
                .anyMatch(comment -> comment.getUser().getId().equals(viewerId));
        boolean contactVisible = canViewContact(projectAd, viewerId, favorited || commented);
        List<FileBindingSummary> attachments = fileBindingRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(TARGET_TYPE, projectAd.getId()).stream()
                .map(FileBindingSummary::from)
                .toList();
        return ProjectAdDetailSummary.from(projectAd, contactVisible, favoriteCount, commentCount, favorited, attachments);
    }

    private boolean canViewContact(ProjectAd projectAd, Long viewerId, boolean interacted) {
        if (viewerId != null && projectAd.getPublisher().getId().equals(viewerId)) {
            return true;
        }
        return switch (projectAd.getContactVisibility()) {
            case "PUBLIC" -> true;
            case "LOGIN_ONLY" -> viewerId != null;
            case "INTERACTION_ONLY" -> viewerId != null && interacted;
            default -> false;
        };
    }

    private ProjectAd getProjectAd(Long id) {
        return projectAdRepository.findById(id)
                .orElseThrow(() -> new BusinessException("项目广告不存在"));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private void requirePublisher(ProjectAd projectAd, Long publisherId) {
        if (!projectAd.getPublisher().getId().equals(publisherId)) {
            throw new BusinessException("只能操作自己的项目广告");
        }
    }

    private boolean matches(String actual, String expected) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private boolean matchesKeyword(ProjectAd ad, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return contains(ad.getTitle(), normalized)
                || contains(ad.getSummary(), normalized)
                || contains(ad.getDescription(), normalized)
                || contains(ad.getTags(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
