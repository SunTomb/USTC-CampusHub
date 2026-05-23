package com.campushub.wallet;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/wallet")
public class AdminWalletController {

    private final WalletOperationService walletOperationService;
    private final CurrentUserService currentUserService;

    public AdminWalletController(WalletOperationService walletOperationService, CurrentUserService currentUserService) {
        this.walletOperationService = walletOperationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/recharges")
    public ApiResponse<List<WalletRechargeSummary>> listRecharges(@RequestParam(required = false) String status) {
        return ApiResponse.ok(walletOperationService.listAdminRecharges(status));
    }

    @PostMapping("/recharges/{id}/approve")
    public ApiResponse<WalletRechargeSummary> approveRecharge(@PathVariable Long id, @RequestParam(required = false) Long adminId, @RequestParam(defaultValue = "微信充值审核通过") String note) {
        return ApiResponse.ok(walletOperationService.approveWechatRecharge(id, currentUserService.requireAdminId(), note));
    }

    @PostMapping("/recharges/{id}/reject")
    public ApiResponse<WalletRechargeSummary> rejectRecharge(@PathVariable Long id, @RequestParam(required = false) Long adminId, @RequestParam(defaultValue = "微信充值审核拒绝") String note) {
        return ApiResponse.ok(walletOperationService.rejectWechatRecharge(id, currentUserService.requireAdminId(), note));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<List<WalletWithdrawalSummary>> listWithdrawals(@RequestParam(required = false) String status) {
        return ApiResponse.ok(walletOperationService.listAdminWithdrawals(status));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public ApiResponse<WalletWithdrawalSummary> approveWithdrawal(@PathVariable Long id, @RequestParam(required = false) Long adminId, @RequestParam(defaultValue = "提现审核通过") String note) {
        return ApiResponse.ok(walletOperationService.approveWithdrawal(id, currentUserService.requireAdminId(), note));
    }

    @PostMapping("/withdrawals/{id}/complete")
    public ApiResponse<WalletWithdrawalSummary> completeWithdrawal(@PathVariable Long id, @RequestParam(required = false) Long adminId, @RequestParam(defaultValue = "提现已人工打款") String note) {
        return ApiResponse.ok(walletOperationService.completeWithdrawal(id, currentUserService.requireAdminId(), note));
    }

    @PostMapping("/withdrawals/{id}/reject")
    public ApiResponse<WalletWithdrawalSummary> rejectWithdrawal(@PathVariable Long id, @RequestParam(required = false) Long adminId, @RequestParam(defaultValue = "提现审核拒绝") String note) {
        return ApiResponse.ok(walletOperationService.rejectWithdrawal(id, currentUserService.requireAdminId(), note));
    }

    @GetMapping("/flows")
    public ApiResponse<List<WalletFlowSummary>> listFlows() {
        return ApiResponse.ok(walletOperationService.listAdminFlows());
    }

    @GetMapping("/frozen-records")
    public ApiResponse<List<WalletFrozenRecordSummary>> listFrozenRecords() {
        return ApiResponse.ok(walletOperationService.listAdminFrozenRecords());
    }
}
