package com.campushub.goods;

import com.campushub.common.BusinessException;
import com.campushub.file.FileBindingRepository;
import com.campushub.file.FileBindingSummary;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.interaction.CommentRepository;
import com.campushub.moderation.GovernanceService;
import com.campushub.interaction.CommentSummary;
import com.campushub.interaction.FavoriteRepository;
import com.campushub.notification.NotificationService;
import com.campushub.payment.ServiceFeeRecord;
import com.campushub.payment.ServiceFeeRecordRepository;
import com.campushub.review.ReviewRepository;
import com.campushub.review.ReviewSummary;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.FeePolicyService;
import com.campushub.wallet.WalletFrozenRecord;
import com.campushub.wallet.WalletFrozenRecordRepository;
import com.campushub.wallet.WalletService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final GoodsIntentRepository goodsIntentRepository;
    private final UserRepository userRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final FileBindingRepository fileBindingRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final ServiceFeeRecordRepository serviceFeeRecordRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final GovernanceService governanceService;
    private final WalletService walletService;
    private final FeePolicyService feePolicyService;
    private final WalletFrozenRecordRepository walletFrozenRecordRepository;

    @Value("${campushub.secondhand.service-fee.enabled:false}")
    private boolean secondhandServiceFeeEnabled;

    @Value("${campushub.secondhand.service-fee.intent-amount:0.00}")
    private BigDecimal secondhandIntentFeeAmount;

    public GoodsService(
            GoodsRepository goodsRepository,
            GoodsIntentRepository goodsIntentRepository,
            UserRepository userRepository,
            RoleApplicationRepository roleApplicationRepository,
            FileBindingRepository fileBindingRepository,
            CommentRepository commentRepository,
            FavoriteRepository favoriteRepository,
            ReviewRepository reviewRepository,
            NotificationService notificationService,
            ServiceFeeRecordRepository serviceFeeRecordRepository,
            GoodsOrderRepository goodsOrderRepository,
            GovernanceService governanceService,
            WalletService walletService,
            FeePolicyService feePolicyService,
            WalletFrozenRecordRepository walletFrozenRecordRepository) {
        this.goodsRepository = goodsRepository;
        this.goodsIntentRepository = goodsIntentRepository;
        this.userRepository = userRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.fileBindingRepository = fileBindingRepository;
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.goodsOrderRepository = goodsOrderRepository;
        this.governanceService = governanceService;
        this.walletService = walletService;
        this.feePolicyService = feePolicyService;
        this.walletFrozenRecordRepository = walletFrozenRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<GoodsSummary> listPublished() {
        return goodsRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .map(goods -> GoodsSummary.from(goods, coverUrl(goods.getId())))
                .toList();
    }

    @Transactional
    public GoodsDetailSummary publish(Long sellerId, CreateGoodsRequest request) {
        User seller = userRepository.findById(sellerId).orElseThrow(() -> new BusinessException("用户不存在"));
        governanceService.ensureCanPost(sellerId);
        ensureGoodsPublisher(sellerId);
        Goods goods = goodsRepository.save(new Goods(seller, request));
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsDetailSummary getDetail(Long goodsId, Long viewerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        goods.increaseViewCount();
        return detailFor(goods, viewerId);
    }

    @Transactional
    public GoodsDetailSummary update(Long goodsId, Long sellerId, UpdateGoodsRequest request) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        goods.update(request);
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsDetailSummary offShelf(Long goodsId, Long sellerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        goods.offShelf();
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsIntentSummary createIntent(Long goodsId, Long buyerId, GoodsIntentRequest request) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        User buyer = userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!"PUBLISHED".equals(goods.getStatus())) {
            throw new BusinessException("商品不可交易");
        }
        if (goods.getSeller().getId().equals(buyerId)) {
            throw new BusinessException("不能对自己的商品提交购买意向");
        }
        goodsIntentRepository.findByGoodsIdAndBuyerId(goodsId, buyerId).ifPresent(existing -> {
            throw new BusinessException("已提交过购买意向");
        });
        GoodsIntent intent = new GoodsIntent(goods, buyer, request.message(), buildContactSnapshot(goods.getSeller()));
        if (secondhandServiceFeeEnabled && secondhandIntentFeeAmount.compareTo(BigDecimal.ZERO) > 0) {
            intent.attachServiceFee(serviceFeeRecordRepository.save(new ServiceFeeRecord(
                    "SH" + Instant.now().toEpochMilli(), buyer, "GOODS_INTENT", goodsId, secondhandIntentFeeAmount)));
        }
        GoodsIntent saved = goodsIntentRepository.save(intent);
        notificationService.notify(goods.getSeller(), "收到二手购买意向", buyer.getNickname() + " 想要购买你的商品：" + goods.getTitle(), "GOODS", goods.getId());
        return GoodsIntentSummary.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GoodsIntentSummary> listIntents(Long goodsId, Long sellerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        return goodsIntentRepository.findByGoodsIdOrderByCreatedAtDesc(goodsId).stream()
                .map(GoodsIntentSummary::from)
                .toList();
    }

    @Transactional
    public GoodsDetailSummary markSold(Long goodsId, Long sellerId, Long buyerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        User buyer = buyerId == null ? null : userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("买家不存在"));
        goods.markSold(buyer);
        if (buyer != null) {
            goodsIntentRepository.findByGoodsIdAndBuyerId(goodsId, buyerId).ifPresent(GoodsIntent::complete);
            notificationService.notify(buyer, "二手商品已标记成交", goods.getTitle() + " 已由卖家标记成交", "GOODS", goods.getId());
        }
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsOrderSummary createOnlineEscrowOrder(Long goodsId, Long buyerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        User buyer = userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("买家不存在"));
        if (!"PUBLISHED".equals(goods.getStatus())) {
            throw new BusinessException("商品不可交易");
        }
        if (goods.getSeller().getId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的商品");
        }
        BigDecimal amount = goods.getPrice();
        BigDecimal fee = feePolicyService.calculateOnlineEscrowFee(amount);
        GoodsOrder order = new GoodsOrder("GO" + Instant.now().toEpochMilli(), goods, buyer, goods.getSeller(), amount, fee, buildContactSnapshot(goods.getSeller()));
        order.enableOnlineEscrow(amount, fee);
        return GoodsOrderSummary.from(goodsOrderRepository.save(order));
    }

    @Transactional
    public GoodsOrderSummary freezeGoodsEscrow(Long orderId, Long buyerId) {
        GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
        ensureEscrowBuyer(order, buyerId);
        if (!"PENDING_FREEZE".equals(order.getEscrowStatus())) {
            throw new BusinessException("订单状态不可冻结");
        }
        BigDecimal total = order.getEscrowAmount().add(order.getPlatformServiceFee());
        walletService.freeze(buyerId, total, "GOODS_ESCROW", order.getId(), "goods-escrow-freeze:" + order.getId(), "USER", buyerId, "二手线上交易托管冻结");
        walletFrozenRecordRepository.save(new WalletFrozenRecord("FRZ-GOODS-" + order.getId(), order.getBuyer(), "GOODS_ESCROW", order.getId(), total, "二手交易托管冻结"));
        order.markEscrowFrozen();
        return GoodsOrderSummary.from(order);
    }

    @Transactional
    public GoodsOrderSummary confirmGoodsEscrow(Long orderId, Long buyerId) {
        GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
        ensureEscrowBuyer(order, buyerId);
        if (!"FROZEN".equals(order.getEscrowStatus())) {
            throw new BusinessException("订单状态不可确认完成");
        }
        walletService.transferFrozen(order.getBuyer().getId(), order.getSeller().getId(), order.getEscrowAmount(), "GOODS_ESCROW", order.getId(), "goods-escrow-release:" + order.getId(), "USER", buyerId, "二手托管本金划转给卖家");
        walletService.debit(order.getBuyer().getId(), order.getPlatformServiceFee(), "SERVICE_FEE", "GOODS_ESCROW", order.getId(), "goods-escrow-fee:" + order.getId(), "SYSTEM", null, "二手线上托管服务费");
        walletFrozenRecordRepository.findByBusinessTypeAndBusinessIdAndStatus("GOODS_ESCROW", order.getId(), "FROZEN").ifPresent(WalletFrozenRecord::markReleased);
        order.markEscrowReleased();
        order.getGoods().markSold(order.getBuyer());
        return GoodsOrderSummary.from(order);
    }

    @Transactional
    public GoodsOrderSummary cancelGoodsEscrow(Long orderId, Long buyerId, String reason) {
        GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
        ensureEscrowBuyer(order, buyerId);
        if (!"FROZEN".equals(order.getEscrowStatus()) && !"PENDING_FREEZE".equals(order.getEscrowStatus())) {
            throw new BusinessException("订单状态不可取消");
        }
        if ("FROZEN".equals(order.getEscrowStatus())) {
            walletService.unfreeze(order.getBuyer().getId(), order.getEscrowAmount().add(order.getPlatformServiceFee()), "GOODS_ESCROW", order.getId(), "goods-escrow-cancel:" + order.getId(), "USER", buyerId, "二手托管取消解冻");
        }
        walletFrozenRecordRepository.findByBusinessTypeAndBusinessIdAndStatus("GOODS_ESCROW", order.getId(), "FROZEN").ifPresent(WalletFrozenRecord::markUnfrozen);
        order.markEscrowCanceled(reason);
        return GoodsOrderSummary.from(order);
    }

    @Transactional
    public GoodsOrderSummary disputeGoodsEscrow(Long orderId, Long userId, String reason) {
        GoodsOrder order = goodsOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("交易订单不存在"));
        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new BusinessException("只有交易双方可以发起争议");
        }
        if (!"FROZEN".equals(order.getEscrowStatus())) {
            throw new BusinessException("订单状态不可发起争议");
        }
        order.markEscrowDisputed(reason);
        return GoodsOrderSummary.from(order);
    }

    private void ensureGoodsPublisher(Long userId) {
        roleApplicationRepository.findByUserIdAndRoleType(userId, "GOODS_PUBLISHER")
                .filter(application -> "PAID".equals(application.getDepositStatus()))
                .filter(application -> "APPROVED".equals(application.getReviewStatus()))
                .orElseThrow(() -> new BusinessException("请先开通二手发布者身份"));
    }

    private void ensureSeller(Goods goods, Long sellerId) {
        if (!goods.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("只能操作自己的商品");
        }
    }

    private void ensureEscrowBuyer(GoodsOrder order, Long buyerId) {
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException("只有买家可以操作托管交易");
        }
    }

    private GoodsDetailSummary detailFor(Goods goods, Long viewerId) {
        boolean owner = viewerId != null && goods.getSeller().getId().equals(viewerId);
        String contactSnapshot = null;
        boolean contactVisible = owner;
        if (owner) {
            contactSnapshot = buildContactSnapshot(goods.getSeller());
        } else if (viewerId != null) {
            contactSnapshot = goodsIntentRepository.findByGoodsIdAndBuyerId(goods.getId(), viewerId)
                    .map(GoodsIntent::getContactSnapshot)
                    .orElse(null);
            contactVisible = contactSnapshot != null;
        }
        List<FileBindingSummary> images = fileBindingRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc("GOODS", goods.getId()).stream()
                .map(FileBindingSummary::from)
                .toList();
        List<CommentSummary> comments = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("GOODS", goods.getId()).stream()
                .map(CommentSummary::from)
                .toList();
        List<ReviewSummary> reviews = reviewRepository.findTop5ByTargetUserIdOrderByCreatedAtDesc(goods.getSeller().getId()).stream()
                .map(ReviewSummary::from)
                .toList();
        long favoriteCount = favoriteRepository.countByTargetTypeAndTargetId("GOODS", goods.getId());
        boolean favorited = viewerId != null && favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(viewerId, "GOODS", goods.getId());
        return GoodsDetailSummary.from(goods, contactVisible, contactSnapshot, images, comments, reviews, favoriteCount, favorited);
    }

    private String coverUrl(Long goodsId) {
        return fileBindingRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc("GOODS", goodsId).stream()
                .findFirst()
                .map(binding -> binding.getFile().getStoragePath())
                .orElse(null);
    }

    private String buildContactSnapshot(User seller) {
        StringBuilder builder = new StringBuilder();
        if (seller.getWechatContact() != null && !seller.getWechatContact().isBlank()) {
            builder.append("微信:").append(seller.getWechatContact().trim());
        }
        if (seller.getQqContact() != null && !seller.getQqContact().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append("QQ:").append(seller.getQqContact().trim());
        }
        if (builder.isEmpty()) {
            throw new BusinessException("卖家联系方式缺失");
        }
        return builder.toString();
    }
}
