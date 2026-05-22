package com.campushub.moderation;

import com.campushub.common.BusinessException;
import com.campushub.notification.NotificationService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceService {

    private final ReportRecordRepository reportRecordRepository;
    private final ViolationRecordRepository violationRecordRepository;
    private final CreditAdjustmentRecordRepository creditAdjustmentRecordRepository;
    private final UserRestrictionRepository userRestrictionRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public GovernanceService(
            ReportRecordRepository reportRecordRepository,
            ViolationRecordRepository violationRecordRepository,
            CreditAdjustmentRecordRepository creditAdjustmentRecordRepository,
            UserRestrictionRepository userRestrictionRepository,
            AdminActionLogRepository adminActionLogRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.reportRecordRepository = reportRecordRepository;
        this.violationRecordRepository = violationRecordRepository;
        this.creditAdjustmentRecordRepository = creditAdjustmentRecordRepository;
        this.userRestrictionRepository = userRestrictionRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public GovernanceDashboardSummary dashboard() {
        return new GovernanceDashboardSummary(
                reportRecordRepository.countByStatus("OPEN"),
                reportRecordRepository.countByStatus("IN_REVIEW"),
                reportRecordRepository.countByStatus("RESOLVED") + reportRecordRepository.countByStatus("REJECTED"),
                violationRecordRepository.countBySeverityIn(List.of("HIGH", "CRITICAL")),
                userRestrictionRepository.countByActiveTrue());
    }

    @Transactional(readOnly = true)
    public List<ReportRecordSummary> listReports(String status, String targetType) {
        if (status != null && !status.isBlank()) {
            return reportRecordRepository.findByStatusOrderByCreatedAtAsc(status).stream().map(ReportRecordSummary::from).toList();
        }
        if (targetType != null && !targetType.isBlank()) {
            return reportRecordRepository.findByTargetTypeOrderByCreatedAtDesc(targetType).stream().map(ReportRecordSummary::from).toList();
        }
        return reportRecordRepository.findAll().stream().map(ReportRecordSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportRecordSummary> reportsByReporter(Long userId) {
        return reportRecordRepository.findByReporterIdOrderByCreatedAtDesc(userId).stream().map(ReportRecordSummary::from).toList();
    }

    @Transactional
    public ReportRecordSummary startReview(Long reportId, Long adminId, GovernanceActionRequest request) {
        User admin = user(adminId, "管理员不存在");
        ReportRecord report = report(reportId);
        report.startReview(admin, note(request));
        audit(admin, "REPORT_START_REVIEW", "REPORT", reportId, note(request));
        notificationService.notify(report.getReporter(), "举报已受理", "你的举报已进入处理流程", "REPORT", report.getId());
        return ReportRecordSummary.from(report);
    }

    @Transactional
    public ReportRecordSummary reject(Long reportId, Long adminId, GovernanceActionRequest request) {
        User admin = user(adminId, "管理员不存在");
        ReportRecord report = report(reportId);
        report.reject(admin, note(request));
        audit(admin, "REPORT_REJECT", "REPORT", reportId, note(request));
        notificationService.notify(report.getReporter(), "举报处理结果", "你的举报未被采纳：" + note(request), "REPORT", report.getId());
        return ReportRecordSummary.from(report);
    }

    @Transactional
    public ReportRecordSummary resolve(Long reportId, Long adminId, GovernanceActionRequest request) {
        User admin = user(adminId, "管理员不存在");
        ReportRecord report = report(reportId);
        report.resolve(admin, defaultString(request.resolutionType(), "CONTENT_REMOVED"), note(request));
        audit(admin, "REPORT_RESOLVE", "REPORT", reportId, note(request));
        notificationService.notify(report.getReporter(), "举报处理完成", "你的举报已处理：" + note(request), "REPORT", report.getId());
        return ReportRecordSummary.from(report);
    }

    @Transactional
    public ReportRecordSummary escalate(Long reportId, Long adminId, GovernanceActionRequest request) {
        User admin = user(adminId, "管理员不存在");
        ReportRecord report = report(reportId);
        report.escalate(admin, note(request));
        audit(admin, "REPORT_ESCALATE", "REPORT", reportId, note(request));
        notificationService.notify(report.getReporter(), "举报已升级处理", "你的举报需要进一步人工处理：" + note(request), "REPORT", report.getId());
        return ReportRecordSummary.from(report);
    }

    @Transactional
    public ViolationRecordSummary createViolation(Long adminId, CreateViolationRequest request) {
        User admin = user(adminId, "管理员不存在");
        User targetUser = user(request.userId(), "被处理用户不存在");
        ReportRecord report = request.reportId() == null ? null : report(request.reportId());
        ViolationRecord violation = violationRecordRepository.save(new ViolationRecord(
                targetUser,
                report,
                request.targetType(),
                request.targetId(),
                defaultString(request.violationType(), "GENERAL"),
                defaultString(request.severity(), "LOW"),
                defaultString(request.penaltyType(), "CREDIT_ONLY"),
                defaultString(request.description(), "平台治理处理"),
                request.creditDelta() == null ? 0 : request.creditDelta(),
                admin,
                request.depositImpactNote()));
        if (report != null && !isTerminal(report.getStatus())) {
            report.resolve(admin, defaultString(request.penaltyType(), "CREDIT_ONLY"), defaultString(request.description(), "平台治理处理"));
        }
        if (violation.getCreditDelta() != 0) {
            applyCreditDelta(targetUser, violation, violation.getCreditDelta(), violation.getDescription(), admin);
        }
        if (request.restrictionType() != null && !request.restrictionType().isBlank() && !"WARNING".equals(request.restrictionType())) {
            LocalDateTime endsAt = request.restrictionDays() == null ? null : LocalDateTime.now().plusDays(request.restrictionDays());
            userRestrictionRepository.save(new UserRestriction(targetUser, violation, request.restrictionType(), violation.getDescription(), LocalDateTime.now(), endsAt, admin));
        }
        audit(admin, "VIOLATION_CREATE", "VIOLATION", violation.getId(), violation.getDescription());
        notificationService.notify(targetUser, "信用与治理通知", "平台已记录违规处理：" + violation.getDescription(), "VIOLATION", violation.getId());
        return ViolationRecordSummary.from(violation);
    }

    @Transactional
    public CreditAdjustmentSummary adjustCredit(Long userId, Long adminId, CreditAdjustmentRequest request) {
        User admin = user(adminId, "管理员不存在");
        User targetUser = user(userId, "用户不存在");
        CreditAdjustmentRecord record = applyCreditDelta(targetUser, null, request.deltaScore(), defaultString(request.reason(), "平台信用调整"), admin);
        audit(admin, "CREDIT_ADJUST", "USER", userId, record.getReason());
        notificationService.notify(targetUser, "信用分调整", "你的信用分已调整：" + record.getDeltaScore(), "USER", userId);
        return CreditAdjustmentSummary.from(record);
    }

    @Transactional
    public UserRestrictionSummary restrictUser(Long userId, Long adminId, UserRestrictionRequest request) {
        User admin = user(adminId, "管理员不存在");
        User targetUser = user(userId, "用户不存在");
        LocalDateTime endsAt = request.days() == null ? null : LocalDateTime.now().plusDays(request.days());
        UserRestriction restriction = userRestrictionRepository.save(new UserRestriction(
                targetUser,
                null,
                defaultString(request.restrictionType(), "WARNING"),
                defaultString(request.reason(), "平台治理限制"),
                LocalDateTime.now(),
                endsAt,
                admin));
        audit(admin, "RESTRICTION_CREATE", "USER_RESTRICTION", restriction.getId(), restriction.getReason());
        notificationService.notify(targetUser, "账号限制通知", "平台已更新你的账号限制状态：" + restriction.getRestrictionType(), "USER_RESTRICTION", restriction.getId());
        return UserRestrictionSummary.from(restriction);
    }

    @Transactional(readOnly = true)
    public List<AdminActionLogSummary> auditLogs() {
        return adminActionLogRepository.findTop100ByOrderByCreatedAtDesc().stream().map(AdminActionLogSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public CreditCenterSummary creditCenter(Long userId) {
        User targetUser = user(userId, "用户不存在");
        return new CreditCenterSummary(
                targetUser.getId(),
                targetUser.getNickname(),
                targetUser.getCreditScore(),
                userRestrictionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId).stream().map(UserRestrictionSummary::from).toList(),
                violationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(ViolationRecordSummary::from).toList(),
                creditAdjustmentRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(CreditAdjustmentSummary::from).toList(),
                reportRecordRepository.findByReporterIdOrderByCreatedAtDesc(userId).stream().map(ReportRecordSummary::from).toList());
    }

    @Transactional(readOnly = true)
    public void ensureCanPost(Long userId) {
        if (userRestrictionRepository.existsByUserIdAndActiveTrueAndRestrictionTypeIn(userId, List.of("POSTING_FREEZE", "ACCOUNT_DISABLED"))) {
            throw new BusinessException("当前账号存在发布限制，请查看信用中心");
        }
    }

    @Transactional(readOnly = true)
    public void ensureCanProvideService(Long userId) {
        if (userRestrictionRepository.existsByUserIdAndActiveTrueAndRestrictionTypeIn(userId, List.of("SERVICE_FREEZE", "ACCOUNT_DISABLED"))) {
            throw new BusinessException("当前账号存在服务限制，请查看信用中心");
        }
    }

    private CreditAdjustmentRecord applyCreditDelta(User targetUser, ViolationRecord violation, Integer deltaScore, String reason, User admin) {
        int delta = deltaScore == null ? 0 : deltaScore;
        int before = targetUser.getCreditScore();
        int after = clampCredit(before + delta);
        targetUser.setCreditScore(after);
        return creditAdjustmentRecordRepository.save(new CreditAdjustmentRecord(targetUser, violation, before, delta, after, reason, admin));
    }

    private int clampCredit(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private User user(Long id, String message) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException(message));
    }

    private ReportRecord report(Long id) {
        return reportRecordRepository.findById(id).orElseThrow(() -> new BusinessException("举报不存在"));
    }

    private void audit(User admin, String actionType, String targetType, Long targetId, String note) {
        adminActionLogRepository.save(new AdminActionLog(admin, actionType, targetType, targetId, note));
    }

    private boolean isTerminal(String status) {
        return List.of("RESOLVED", "REJECTED", "ESCALATED").contains(status);
    }

    private String note(GovernanceActionRequest request) {
        return request == null ? "" : defaultString(request.note(), "");
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
