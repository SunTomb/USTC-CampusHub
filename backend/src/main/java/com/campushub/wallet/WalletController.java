package com.campushub.wallet;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;

    public WalletController(WalletAccountRepository walletAccountRepository, WalletFlowRepository walletFlowRepository) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
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
        WalletAccount account = walletAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("wallet account not found"));
        return ApiResponse.ok(WalletAccountSummary.from(account));
    }

    @GetMapping("/users/{userId}/flows")
    public ApiResponse<List<WalletFlowSummary>> listUserFlows(@PathVariable Long userId) {
        List<WalletFlowSummary> flows = walletFlowRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WalletFlowSummary::from)
                .toList();
        return ApiResponse.ok(flows);
    }
}
