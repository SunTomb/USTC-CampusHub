package com.campushub.wallet;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;
    private final UserRepository userRepository;

    public WalletService(WalletAccountRepository walletAccountRepository, WalletFlowRepository walletFlowRepository, UserRepository userRepository) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WalletAccount credit(Long userId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.credit(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "IN", "RECHARGE", amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public WalletAccount debit(Long userId, BigDecimal amount, String flowType, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.debit(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "OUT", flowType, amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public WalletAccount freeze(Long userId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.freeze(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "OUT", "FREEZE", amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public WalletAccount unfreeze(Long userId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return findAccount(userId);
        }
        WalletAccount account = findAccountForUpdate(userId);
        account.unfreeze(amount);
        walletAccountRepository.save(account);
        saveFlow(account, "IN", "UNFREEZE", amount, businessType, businessId, idempotencyKey, null, createdBy, operatorId, remark);
        return account;
    }

    @Transactional
    public void transferFrozen(Long fromUserId, Long toUserId, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, String createdBy, Long operatorId, String remark) {
        if (walletFlowRepository.findByIdempotencyKey(idempotencyKey + ":OUT").isPresent()) {
            return;
        }
        WalletAccount from = findAccountForUpdate(fromUserId);
        WalletAccount to = findAccountForUpdate(toUserId);
        from.debitFrozen(amount);
        to.credit(amount);
        walletAccountRepository.save(from);
        walletAccountRepository.save(to);
        saveFlow(from, "OUT", "ESCROW_TRANSFER_OUT", amount, businessType, businessId, idempotencyKey + ":OUT", toUserId, createdBy, operatorId, remark);
        saveFlow(to, "IN", "ESCROW_TRANSFER_IN", amount, businessType, businessId, idempotencyKey + ":IN", fromUserId, createdBy, operatorId, remark);
    }

    private WalletAccount findAccount(Long userId) {
        return walletAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new com.campushub.common.BusinessException("钱包账户不存在"));
    }

    private WalletAccount findAccountForUpdate(Long userId) {
        return walletAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new com.campushub.common.BusinessException("钱包账户不存在"));
    }

    private void saveFlow(WalletAccount account, String direction, String flowType, BigDecimal amount, String businessType, Long businessId, String idempotencyKey, Long counterpartyUserId, String createdBy, Long operatorId, String remark) {
        User counterpartyUser = counterpartyUserId == null ? null : userRepository.findById(counterpartyUserId).orElse(null);
        User operator = operatorId == null ? null : userRepository.findById(operatorId).orElse(null);
        walletFlowRepository.save(new WalletFlow(
                account,
                account.getUser(),
                "WF-" + System.currentTimeMillis() + "-" + account.getUser().getId(),
                direction,
                flowType,
                amount,
                account.getBalance(),
                account.getFrozenBalance(),
                businessType,
                businessId,
                idempotencyKey,
                counterpartyUser,
                createdBy,
                operator,
                remark));
    }
}
