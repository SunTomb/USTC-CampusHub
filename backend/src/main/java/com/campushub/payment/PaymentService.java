package com.campushub.payment;

import com.campushub.common.BusinessException;
import com.campushub.identity.RoleApplication;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.wallet.WalletAccount;
import com.campushub.wallet.WalletAccountRepository;
import com.campushub.wallet.WalletFlow;
import com.campushub.wallet.WalletFlowRepository;
import com.campushub.wallet.WalletOperationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentProvider paymentProvider;
    private final ServiceFeeRecordRepository serviceFeeRecordRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentCallbackEventRepository paymentCallbackEventRepository;
    private final PaymentCenterProperties paymentCenterProperties;
    private final RoleApplicationRepository roleApplicationRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;
    private final WalletOperationService walletOperationService;

    public PaymentService(
            PaymentProvider paymentProvider,
            ServiceFeeRecordRepository serviceFeeRecordRepository,
            PaymentOrderRepository paymentOrderRepository,
            PaymentCallbackEventRepository paymentCallbackEventRepository,
            PaymentCenterProperties paymentCenterProperties,
            RoleApplicationRepository roleApplicationRepository,
            WalletAccountRepository walletAccountRepository,
            WalletFlowRepository walletFlowRepository,
            WalletOperationService walletOperationService) {
        this.paymentProvider = paymentProvider;
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentCallbackEventRepository = paymentCallbackEventRepository;
        this.paymentCenterProperties = paymentCenterProperties;
        this.roleApplicationRepository = roleApplicationRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
        this.walletOperationService = walletOperationService;
    }

    @Transactional
    public PaymentCreation createServiceFeePayment(Long serviceFeeId) {
        ServiceFeeRecord fee = findServiceFee(serviceFeeId);
        if ("PAID".equals(fee.getStatus())) {
            throw new BusinessException("服务费已支付");
        }
        PaymentOrder order = paymentOrderRepository.findByBusinessTypeAndBusinessIdAndStatus("SERVICE_FEE", fee.getId(), "PENDING")
                .orElseGet(() -> paymentOrderRepository.save(new PaymentOrder(
                        nextOrderNo("SF", fee.getId()),
                        "SERVICE_FEE",
                        fee.getId(),
                        fee.getPayer(),
                        fee.getAmount(),
                        paymentProvider.providerName(),
                        LocalDateTime.now().plusMinutes(paymentCenterProperties.getExpireMinutes()))));
        if (order.getProviderOrderNo() == null) {
            PaymentCreation creation = paymentProvider.createWebPayment(new PaymentRequest(
                    order.getOrderNo(),
                    order.getBusinessType(),
                    order.getBusinessId(),
                    fee.getPayer().getId(),
                    fee.getFeeNo(),
                    fee.getAmount(),
                    "CampusHub 服务费 " + fee.getFeeNo(),
                    paymentCenterProperties.getCallbackUrl(),
                    paymentCenterProperties.getExpireMinutes()));
            order.attachProviderOrder(creation.providerOrderNo(), creation.payUrl());
            fee.attachPaymentOrder(order.getOrderNo(), order.getProvider(), creation.providerOrderNo(), creation.payUrl(), creation.expiresAt());
            paymentOrderRepository.save(order);
            serviceFeeRecordRepository.save(fee);
            return creation;
        }
        return PaymentOrderSummary.toCreation(order);
    }

    @Transactional(readOnly = true)
    public PaymentOrderSummary getOrder(String orderNo) {
        return paymentOrderRepository.findByOrderNo(orderNo)
                .map(PaymentOrderSummary::from)
                .orElseThrow(() -> new BusinessException("支付订单不存在"));
    }

    @Transactional
    public PaymentCreation createRoleDepositPayment(Long applicationId) {
        RoleApplication application = roleApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        if ("PAID".equals(application.getDepositStatus())) {
            throw new BusinessException("身份保证金已支付");
        }
        PaymentOrder order = paymentOrderRepository.findByBusinessTypeAndBusinessIdAndStatus("ROLE_DEPOSIT", application.getId(), "PENDING")
                .orElseGet(() -> paymentOrderRepository.save(new PaymentOrder(
                        nextOrderNo("RD", application.getId()),
                        "ROLE_DEPOSIT",
                        application.getId(),
                        application.getUser(),
                        application.getDepositAmount(),
                        paymentProvider.providerName(),
                        LocalDateTime.now().plusMinutes(paymentCenterProperties.getExpireMinutes()))));
        if (order.getProviderOrderNo() == null) {
            PaymentCreation creation = paymentProvider.createWebPayment(new PaymentRequest(
                    order.getOrderNo(),
                    order.getBusinessType(),
                    order.getBusinessId(),
                    application.getUser().getId(),
                    application.getRoleType(),
                    application.getDepositAmount(),
                    "CampusHub 身份保证金 " + application.getRoleType(),
                    paymentCenterProperties.getCallbackUrl(),
                    paymentCenterProperties.getExpireMinutes()));
            order.attachProviderOrder(creation.providerOrderNo(), creation.payUrl());
            application.attachDepositPaymentOrder(order.getOrderNo());
            paymentOrderRepository.save(order);
            roleApplicationRepository.save(application);
            return creation;
        }
        return PaymentOrderSummary.toCreation(order);
    }

    @Transactional
    public PaymentStatus handlePaymentCenterCallback(PaymentCenterCallbackRequest request, PaymentCallbackHeaders headers, String expectedToken, String signingSecret) {
        if (expectedToken == null || expectedToken.isBlank() || !expectedToken.equals(headers.token())) {
            paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), false, false, "内部 token 校验失败"));
            throw new BusinessException("支付回调鉴权失败");
        }
        validateCallbackSignature(request, headers, signingSecret);
        if (paymentCallbackEventRepository.findByEventId(request.eventId()).isPresent()) {
            return new PaymentStatus(paymentProvider.providerName(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.paidAt(), null, "重复回调已忽略");
        }
        PaymentOrder order = paymentOrderRepository.findByOrderNo(request.orderNo())
                .orElseThrow(() -> new BusinessException("支付订单不存在"));
        validateCallback(request, order);
        if ("PAID".equals(order.getStatus()) && "PAID".equals(request.status())) {
            paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), true, true, null));
            return new PaymentStatus(order.getProvider(), order.getOrderNo(), order.getProviderOrderNo(), order.getStatus(), order.getPaidAt(), null, "支付订单已处理");
        }
        if (!"PENDING".equals(order.getStatus())) {
            paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), true, false, "订单状态不允许流转"));
            throw new BusinessException("订单状态不允许流转");
        }
        applyBusinessPaymentResult(order, request);
        paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), true, true, null));
        return new PaymentStatus(order.getProvider(), order.getOrderNo(), order.getProviderOrderNo(), order.getStatus(), order.getPaidAt(), order.getFailureReason(), "支付回调已处理");
    }

    @Transactional
    public PaymentStatus markMockServiceFeeSuccess(Long serviceFeeId) {
        PaymentCreation creation = createServiceFeePayment(serviceFeeId);
        if (paymentProvider instanceof MockPaymentProvider mockPaymentProvider) {
            mockPaymentProvider.markSuccess(creation.orderNo());
        }
        PaymentOrder order = paymentOrderRepository.findByOrderNo(creation.orderNo())
                .orElseThrow(() -> new BusinessException("支付订单不存在"));
        if (!"PAID".equals(order.getStatus())) {
            order.markPaid(LocalDateTime.now());
            markServiceFeePaid(order);
            paymentCallbackEventRepository.findByEventId("mock-" + creation.orderNo())
                    .orElseGet(() -> paymentCallbackEventRepository.save(new PaymentCallbackEvent(
                            "mock-" + creation.orderNo(),
                            creation.orderNo(),
                            creation.providerOrderNo(),
                            "PAID",
                            order.getAmount(),
                            true,
                            true,
                            null)));
        }
        return new PaymentStatus(order.getProvider(), order.getOrderNo(), order.getProviderOrderNo(), order.getStatus(), order.getPaidAt(), null, "服务费已标记为本地支付成功。 ");
    }

    private void validateCallbackSignature(PaymentCenterCallbackRequest request, PaymentCallbackHeaders headers, String signingSecret) {
        if (signingSecret == null || signingSecret.isBlank()) {
            return;
        }
        if (headers.timestamp() == null || headers.timestamp().isBlank() || headers.signature() == null || headers.signature().isBlank()) {
            paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), false, false, "签名头缺失"));
            throw new BusinessException("支付回调签名失败");
        }
        String expected = hmacSha256Hex(headers.timestamp() + "." + headers.rawBody(), signingSecret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), headers.signature().getBytes(StandardCharsets.UTF_8))) {
            paymentCallbackEventRepository.save(new PaymentCallbackEvent(request.eventId(), request.orderNo(), request.paymentCenterOrderNo(), request.status(), request.amount(), false, false, "签名不匹配"));
            throw new BusinessException("支付回调签名失败");
        }
    }

    private String hmacSha256Hex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new BusinessException("支付回调签名失败");
        }
    }

    private void validateCallback(PaymentCenterCallbackRequest request, PaymentOrder order) {
        if (!order.getBusinessType().equals(request.businessType()) || !order.getBusinessId().equals(request.businessId())) {
            throw new BusinessException("支付业务对象不匹配");
        }
        if (order.getProviderOrderNo() != null && !order.getProviderOrderNo().equals(request.paymentCenterOrderNo())) {
            throw new BusinessException("支付中心订单号不匹配");
        }
        if (order.getAmount().compareTo(request.amount()) != 0) {
            throw new BusinessException("支付金额不匹配");
        }
    }

    private void applyBusinessPaymentResult(PaymentOrder order, PaymentCenterCallbackRequest request) {
        if ("PAID".equals(request.status())) {
            order.markPaid(request.paidAt() != null ? request.paidAt() : LocalDateTime.now());
            if ("SERVICE_FEE".equals(order.getBusinessType())) {
                markServiceFeePaid(order);
            }
            if ("ROLE_DEPOSIT".equals(order.getBusinessType())) {
                markRoleDepositPaid(order);
            }
            if ("WALLET_RECHARGE".equals(order.getBusinessType())) {
                walletOperationService.markRechargePaidByPaymentOrder(order.getOrderNo());
            }
            return;
        }
        if ("FAILED".equals(request.status())) {
            order.markFailed(LocalDateTime.now(), request.failureReason());
            if ("SERVICE_FEE".equals(order.getBusinessType())) {
                findServiceFee(order.getBusinessId()).markFailed(LocalDateTime.now(), request.failureReason());
            }
            if ("ROLE_DEPOSIT".equals(order.getBusinessType())) {
                markRoleDepositFailed(order, request.failureReason());
            }
            return;
        }
        if ("EXPIRED".equals(request.status())) {
            order.markExpired(LocalDateTime.now());
            if ("SERVICE_FEE".equals(order.getBusinessType())) {
                findServiceFee(order.getBusinessId()).markExpired(LocalDateTime.now());
            }
            if ("ROLE_DEPOSIT".equals(order.getBusinessType())) {
                markRoleDepositExpired(order);
            }
            return;
        }
        throw new BusinessException("未知支付状态");
    }

    private void markServiceFeePaid(PaymentOrder order) {
        ServiceFeeRecord fee = findServiceFee(order.getBusinessId());
        if (!"PAID".equals(fee.getStatus())) {
            fee.markPaid(order.getPaidAt());
            serviceFeeRecordRepository.save(fee);
            WalletAccount wallet = walletAccountRepository.findByUserId(fee.getPayer().getId())
                    .orElseThrow(() -> new BusinessException("付款用户钱包不存在"));
            walletFlowRepository.save(new WalletFlow(
                    wallet,
                    fee.getPayer(),
                    "WF-FEE-" + order.getOrderNo(),
                    "OUT",
                    fee.getAmount(),
                    wallet.getBalance(),
                    "SERVICE_FEE",
                    fee.getId(),
                    "支付平台服务费"));
        }
    }

    private void markRoleDepositPaid(PaymentOrder order) {
        RoleApplication application = roleApplicationRepository.findById(order.getBusinessId())
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        if (!order.getOrderNo().equals(application.getDepositPaymentOrderNo())) {
            return;
        }
        if (!"PAID".equals(application.getDepositStatus())) {
            application.markDepositPaid();
            roleApplicationRepository.save(application);
        }
    }

    private void markRoleDepositFailed(PaymentOrder order, String failureReason) {
        RoleApplication application = roleApplicationRepository.findById(order.getBusinessId())
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        if (!order.getOrderNo().equals(application.getDepositPaymentOrderNo())) {
            return;
        }
        application.markDepositFailed(failureReason);
        roleApplicationRepository.save(application);
    }

    private void markRoleDepositExpired(PaymentOrder order) {
        RoleApplication application = roleApplicationRepository.findById(order.getBusinessId())
                .orElseThrow(() -> new BusinessException("身份申请不存在"));
        if (!order.getOrderNo().equals(application.getDepositPaymentOrderNo())) {
            return;
        }
        application.markDepositExpired();
        roleApplicationRepository.save(application);
    }

    private String nextOrderNo(String prefix, Long id) {
        return "CHP-" + prefix + "-" + id + "-" + System.currentTimeMillis();
    }

    private ServiceFeeRecord findServiceFee(Long serviceFeeId) {
        return serviceFeeRecordRepository.findById(serviceFeeId)
                .orElseThrow(() -> new BusinessException("服务费记录不存在"));
    }
}
