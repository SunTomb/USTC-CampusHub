package com.campushub.identity;

import com.campushub.auth.UserRoleService;
import com.campushub.common.BusinessException;
import com.campushub.platform.PlatformProtocolConsent;
import com.campushub.platform.PlatformProtocolService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityService {

    private final RoleApplicationRepository roleApplicationRepository;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PlatformProtocolService platformProtocolService;

    public IdentityService(RoleApplicationRepository roleApplicationRepository, UserRepository userRepository,
            UserRoleService userRoleService, PlatformProtocolService platformProtocolService) {
        this.roleApplicationRepository = roleApplicationRepository;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.platformProtocolService = platformProtocolService;
    }

    @Transactional
    public RoleApplicationSummary apply(Long userId, ApplyRoleRequest request) {
        PlatformRoleType roleType = parseRoleType(request.roleType());
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        var existing = roleApplicationRepository.findByUserIdAndRoleType(userId, roleType.name());
        if (userRoleService.hasRole(userId, roleType.grantedRoleCode())) {
            if (existing.isPresent()) return RoleApplicationSummary.from(existing.get());
            throw new BusinessException("该身份已开通，无需重新同意协议");
        }
        if (existing.isPresent() && "APPROVED".equals(existing.get().getReviewStatus())
                && eligibleForActivation(existing.get(), roleType)) {
            assignRoleAfterDeposit(existing.get());
            return RoleApplicationSummary.from(existing.get());
        }
        if (existing.isPresent() && "PENDING_REVIEW".equals(existing.get().getReviewStatus())) {
            return RoleApplicationSummary.from(existing.get());
        }
        if (!roleType.requiresAgreement()) {
            if (existing.isPresent()) throw new BusinessException("该身份已申请，当前状态不可重复申请");
            return RoleApplicationSummary.from(roleApplicationRepository.save(new RoleApplication(user, roleType, request.applyNote())));
        }
        if (!request.agreementAccepted() || request.agreementVersion() == null || request.agreementVersion().isBlank()) {
            throw new BusinessException("请先阅读并同意当前身份协议");
        }
        if (existing.isPresent() && !existing.get().isRecoverableUnpaid()
                && !"REJECTED".equals(existing.get().getReviewStatus())) {
            throw new BusinessException("该身份已申请，当前状态不可重复申请");
        }
        PlatformProtocolConsent consent = platformProtocolService.acceptConsent(userId, roleType.name(), request.agreementVersion());
        RoleApplication application;
        if (existing.isPresent()) {
            application = existing.get();
            application.convertToAgreement(roleType, request.applyNote(), consent.getAgreementVersion(), consent);
        } else {
            application = RoleApplication.agreementBased(user, roleType, request.applyNote(), consent.getAgreementVersion(), consent);
        }
        application = roleApplicationRepository.save(application);
        assignRoleAfterDeposit(application);
        return RoleApplicationSummary.from(application);
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationSummary> listUserApplications(Long userId) {
        return roleApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RoleApplicationSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationSummary> listPendingShopMerchantApplications() {
        return roleApplicationRepository.findByRoleTypeInAndReviewStatusOrderByCreatedAtAsc(
                List.of(PlatformRoleType.SHOP_MERCHANT.name()), "PENDING_REVIEW").stream()
                .map(RoleApplicationSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationSummary> listPendingAdminApplications() {
        return roleApplicationRepository.findByRoleTypeInAndReviewStatusOrderByCreatedAtAsc(
                List.of(PlatformRoleType.TRADE_ADMIN.name(), PlatformRoleType.SHOWCASE_ADMIN.name()), "PENDING_REVIEW")
                .stream().map(RoleApplicationSummary::from).toList();
    }

    @Transactional
    public RoleApplicationSummary approve(Long applicationId, Long reviewerId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        PlatformRoleType roleType = parseRoleType(application.getRoleType());
        ensureReviewerScope(roleType, reviewerId);
        if (!"PENDING_REVIEW".equals(application.getReviewStatus()) || !eligibleForActivation(application, roleType)) {
            throw new BusinessException("该申请尚未完成协议或历史准入条件，或不处于待审核状态");
        }
        User reviewer = userRepository.findById(reviewerId).orElseThrow(() -> new BusinessException("审核人不存在"));
        application.markApproved(reviewer);
        assignRoleAfterDeposit(application);
        return RoleApplicationSummary.from(application);
    }

    @Transactional
    public RoleApplicationSummary reject(Long applicationId, Long reviewerId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        ensureReviewerScope(parseRoleType(application.getRoleType()), reviewerId);
        if (!"PENDING_REVIEW".equals(application.getReviewStatus())) {
            throw new BusinessException("只能处理待审核申请");
        }
        User reviewer = userRepository.findById(reviewerId).orElseThrow(() -> new BusinessException("审核人不存在"));
        application.markRejected(reviewer);
        return RoleApplicationSummary.from(application);
    }

    @Transactional
    public void assignRoleAfterDeposit(RoleApplication application) {
        PlatformRoleType roleType = parseRoleType(application.getRoleType());
        if ("APPROVED".equals(application.getReviewStatus()) && eligibleForActivation(application, roleType)) {
            userRoleService.assignRole(application.getUser().getId(), roleType.grantedRoleCode());
        }
    }

    private boolean eligibleForActivation(RoleApplication application, PlatformRoleType roleType) {
        if (!roleType.requiresAgreement()) return "NOT_REQUIRED".equals(application.getDepositStatus());
        if (application.isAgreementBased()) {
            return "CONSENTED".equals(application.getAgreementStatus())
                    && application.getAgreementConsent() != null && application.getAgreementVersion() != null;
        }
        return "PAID".equals(application.getDepositStatus());
    }

    private void ensureReviewerScope(PlatformRoleType roleType, Long reviewerId) {
        List<String> roles = userRoleService.findRoleCodes(reviewerId);
        boolean master = roles.contains("ROLE_MASTER_ADMIN");
        boolean allowed = roleType == PlatformRoleType.SHOP_MERCHANT
                ? master || roles.contains("ROLE_SHOWCASE_ADMIN") || roles.contains("ROLE_ADMIN")
                : (roleType == PlatformRoleType.TRADE_ADMIN || roleType == PlatformRoleType.SHOWCASE_ADMIN) && master;
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权审核此身份");
    }

    private PlatformRoleType parseRoleType(String roleType) {
        try {
            return PlatformRoleType.valueOf(roleType == null ? "" : roleType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("不支持的身份类型");
        }
    }
}
