package com.campushub.identity;

import com.campushub.auth.UserRoleService;
import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

    private final RoleApplicationRepository roleApplicationRepository;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    public IdentityService(
            RoleApplicationRepository roleApplicationRepository,
            UserRepository userRepository,
            UserRoleService userRoleService) {
        this.roleApplicationRepository = roleApplicationRepository;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
    }

    @Transactional
    public RoleApplicationSummary apply(Long userId, ApplyRoleRequest request) {
        PlatformRoleType roleType = parseRoleType(request.roleType());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        var existingApplication = roleApplicationRepository.findByUserIdAndRoleType(userId, roleType.name());
        if (existingApplication.isPresent()) {
            RoleApplication existing = existingApplication.get();
            if (existing.isRecoverableUnpaid()) {
                existing.resetForPayment(request.applyNote());
                return RoleApplicationSummary.from(existing);
            }
            throw new BusinessException("该身份已申请，当前状态不可重复申请");
        }

        RoleApplication application = new RoleApplication(user, roleType, request.applyNote());
        return RoleApplicationSummary.from(roleApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationSummary> listUserApplications(Long userId) {
        return roleApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RoleApplicationSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationSummary> listPendingShopMerchantApplications() {
        return roleApplicationRepository.findByReviewStatusOrderByCreatedAtAsc("PENDING_REVIEW").stream()
                .filter(application -> PlatformRoleType.SHOP_MERCHANT.name().equals(application.getRoleType()))
                .map(RoleApplicationSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleApplicationSummary> listPendingAdminApplications() {
        return roleApplicationRepository.findByRoleTypeInAndReviewStatusOrderByCreatedAtAsc(
                        List.of(PlatformRoleType.TRADE_ADMIN.name(), PlatformRoleType.SHOWCASE_ADMIN.name()),
                        "PENDING_REVIEW")
                .stream()
                .map(RoleApplicationSummary::from)
                .toList();
    }

    @Transactional
    public RoleApplicationSummary approve(Long applicationId, Long reviewerId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new BusinessException("审核人不存在"));

        application.markApproved(reviewer);
        assignRoleAfterDeposit(application);
        return RoleApplicationSummary.from(application);
    }

    @Transactional
    public RoleApplicationSummary reject(Long applicationId, Long reviewerId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new BusinessException("审核人不存在"));

        application.markRejected(reviewer);
        return RoleApplicationSummary.from(application);
    }

    @Transactional
    public void assignRoleAfterDeposit(RoleApplication application) {
        if (!"APPROVED".equals(application.getReviewStatus())) {
            return;
        }
        PlatformRoleType roleType = parseRoleType(application.getRoleType());
        userRoleService.assignRole(application.getUser().getId(), roleType.grantedRoleCode());
    }

    private PlatformRoleType parseRoleType(String roleType) {
        try {
            return PlatformRoleType.valueOf(roleType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("不支持的身份类型");
        }
    }
}
