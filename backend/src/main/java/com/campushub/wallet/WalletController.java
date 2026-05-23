package com.campushub.wallet;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;
    private final WalletOperationService walletOperationService;
    private final CurrentUserService currentUserService;

    public WalletController(
            WalletAccountRepository walletAccountRepository,
            WalletFlowRepository walletFlowRepository,
            WalletOperationService walletOperationService,
            CurrentUserService currentUserService) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
        this.walletOperationService = walletOperationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/accounts")
    public ApiResponse<List<WalletAccountSummary>> listAccounts() {
        List<WalletAccountSummary> accounts = walletAccountRepository.findAll().stream()
                .map(WalletAccountSummary::from)
                .toList();
        return ApiResponse.ok(accounts);
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<WalletAccountSummary> getUserWallet(@PathVariable Long userId) {
        Long effectiveUserId = currentUserService.requireSameUser(userId);
        WalletAccount account = walletAccountRepository.findByUserId(effectiveUserId)
                .orElseThrow(() -> new BusinessException("wallet account not found"));
        return ApiResponse.ok(WalletAccountSummary.from(account));
    }

    @GetMapping("/users/{userId}/flows")
    public ApiResponse<List<WalletFlowSummary>> listUserFlows(@PathVariable Long userId) {
        Long effectiveUserId = currentUserService.requireSameUser(userId);
        List<WalletFlowSummary> flows = walletFlowRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId).stream()
                .map(WalletFlowSummary::from)
                .toList();
        return ApiResponse.ok(flows);
    }

    @PostMapping("/users/{userId}/recharges")
    public ApiResponse<WalletRechargeSummary> createRecharge(@PathVariable Long userId, @Valid @RequestBody WalletRechargeRequest request) {
        return ApiResponse.ok(walletOperationService.createRecharge(currentUserService.requireSameUser(userId), request));
    }

    @GetMapping("/users/{userId}/recharges")
    public ApiResponse<List<WalletRechargeSummary>> listRecharges(@PathVariable Long userId) {
        return ApiResponse.ok(walletOperationService.listUserRecharges(currentUserService.requireSameUser(userId)));
    }

    @PostMapping("/users/{userId}/withdrawals")
    public ApiResponse<WalletWithdrawalSummary> createWithdrawal(@PathVariable Long userId, @Valid @RequestBody CreateWithdrawalRequest request) {
        return ApiResponse.ok(walletOperationService.createWithdrawal(currentUserService.requireSameUser(userId), request));
    }

    @GetMapping("/users/{userId}/withdrawals")
    public ApiResponse<List<WalletWithdrawalSummary>> listWithdrawals(@PathVariable Long userId) {
        return ApiResponse.ok(walletOperationService.listUserWithdrawals(currentUserService.requireSameUser(userId)));
    }

    @GetMapping("/users/{userId}/frozen-items")
    public ApiResponse<List<WalletFrozenRecordSummary>> listUserFrozenItems(@PathVariable Long userId) {
        return ApiResponse.ok(walletOperationService.listUserFrozenRecords(currentUserService.requireSameUser(userId)));
    }
}
