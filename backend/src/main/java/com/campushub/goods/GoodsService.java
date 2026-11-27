package com.campushub.goods;

import com.campushub.common.BusinessException;
import com.campushub.auth.UserRoleService;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.interaction.CommentRepository;
import com.campushub.moderation.GovernanceService;
import com.campushub.interaction.CommentSummary;
import com.campushub.interaction.FavoriteRepository;
import com.campushub.notification.NotificationService;
import com.campushub.platform.FinancialWriteGuard;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.FeePolicyService;
import com.campushub.wallet.WalletFrozenRecord;
import com.campushub.wallet.WalletFrozenRecordRepository;
import com.campushub.wallet.WalletService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final CategoryRepository categories;
    private final GoodsListingAccess listingAccess;
    private final JdbcTemplate db;
    private final GoodsIntentRepository goodsIntentRepository;
    private final UserRepository userRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final UserRoleService userRoleService;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationService notificationService;
    private final GoodsOrderRepository goodsOrderRepository;
    private final GovernanceService governanceService;
    private final WalletService walletService;
    private final FeePolicyService feePolicyService;
    private final WalletFrozenRecordRepository walletFrozenRecordRepository;
    private final FinancialWriteGuard financialWriteGuard;

    public GoodsService(
            GoodsRepository goodsRepository,
            CategoryRepository categories,
            GoodsListingAccess listingAccess,
            JdbcTemplate db,
            GoodsIntentRepository goodsIntentRepository,
            UserRepository userRepository,
            RoleApplicationRepository roleApplicationRepository,
            UserRoleService userRoleService,
            CommentRepository commentRepository,
            FavoriteRepository favoriteRepository,
            NotificationService notificationService,
            GoodsOrderRepository goodsOrderRepository,
            GovernanceService governanceService,
            WalletService walletService,
            FeePolicyService feePolicyService,
            WalletFrozenRecordRepository walletFrozenRecordRepository,
            FinancialWriteGuard financialWriteGuard) {
        this.goodsRepository = goodsRepository;
        this.categories = categories;
        this.listingAccess = listingAccess;
        this.db = db;
        this.goodsIntentRepository = goodsIntentRepository;
        this.userRepository = userRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.userRoleService = userRoleService;
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
        this.notificationService = notificationService;
        this.goodsOrderRepository = goodsOrderRepository;
        this.governanceService = governanceService;
        this.walletService = walletService;
        this.feePolicyService = feePolicyService;
        this.walletFrozenRecordRepository = walletFrozenRecordRepository;
        this.financialWriteGuard = financialWriteGuard;
    }

    @Transactional(readOnly = true)
    public List<GoodsSummary> listPublished() {
        Map<Long, String> names = categories.findAll().stream().collect(Collectors.toMap(Category::getId, Category::getName));
        return goodsRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .map(goods -> GoodsSummary.from(goods, null, names.get(goods.getCategoryId())))
                .toList();
    }

    @Transactional
    public GoodsDetailSummary publish(Long sellerId, CreateGoodsRequest request) {
        User seller = userRepository.findById(sellerId).orElseThrow(() -> new BusinessException("用户不存在"));
        governanceService.ensureCanPost(sellerId);
        ensureGoodsPublisher(sellerId);
        ensureCategory(request.categoryId());
        Goods goods = goodsRepository.save(new Goods(seller, request));
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsDetailSummary getDetail(Long goodsId, Long viewerId) {
        // Update only the counter: a concurrent view must not restore a stale listing status.
        goodsRepository.incrementViews(goodsId);
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(GoodsService::notVisible);
        listingAccess.requireReadable(goodsId, goods.getSeller().getId(), goods.getStatus(), viewerId);
        return detailFor(goods, viewerId);
    }

    @Transactional
    public GoodsDetailSummary update(Long goodsId, Long sellerId, UpdateGoodsRequest request) {
        Goods goods = locked(goodsId);
        ensureSeller(goods, sellerId);
        ensureNotDeleted(goods);
        if (!GoodsListingAccess.canEdit(goods.getStatus())) throw new BusinessException("当前商品状态不可编辑");
        governanceService.ensureCanPost(sellerId);
        ensureCategory(request.categoryId());
        goods.update(request);
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsDetailSummary offShelf(Long goodsId, Long sellerId) {
        Goods goods = locked(goodsId);
        ensureSeller(goods, sellerId);
        if (!"OFF_SHELF".equals(goods.getStatus())) {
            ensureListed(goods);
            takeOffShelf(goods, "卖家已下架闲置");
        }
        return detailFor(goods, sellerId);
    }

    @Transactional
    public void deleteListing(Long goodsId, Long sellerId) {
        Goods goods = locked(goodsId);
        ensureSeller(goods, sellerId);
        if (GoodsListingAccess.DELETED.equals(goods.getStatus())) return; // safe retry
        if (goodsOrderRepository.hasUnfinishedOrder(goodsId)) throw new BusinessException("这件商品还有未结束的订单，暂不能删除");
        for (GoodsIntent intent : goodsIntentRepository.findByGoodsIdOrderByCreatedAtDesc(goodsId)) {
            closeIntent(intent, "卖家已删除闲置");
        }
        // Same parent lock as gallery updates. Discard current image bytes, not the order/feedback rows.
        db.update("delete from catalog_images where kind='GOODS' and object_id=?", goodsId);
        goods.deleteListing();
    }

    // The admin controller checks trade-admin authority; use the same row lock as seller actions.
    @Transactional
    public GoodsSummary offShelfByAdmin(Long goodsId) {
        Goods goods = locked(goodsId);
        ensureNotDeleted(goods);
        if (!"OFF_SHELF".equals(goods.getStatus())) {
            if ("SOLD".equals(goods.getStatus())) throw new BusinessException("不能更改已成交的商品记录");
            takeOffShelf(goods, "闲置已由站务下架");
        }
        return GoodsSummary.from(goods);
    }

    private void takeOffShelf(Goods goods, String notice) {
        goods.offShelf();
        for (GoodsIntent intent : goodsIntentRepository.findByGoodsIdOrderByCreatedAtDesc(goods.getId())) {
            closeIntent(intent, notice);
        }
    }

    @Transactional
    public GoodsIntentSummary createIntent(Long goodsId, Long buyerId, GoodsIntentRequest request) {
        Goods goods = locked(goodsId);
        ensureNotDeleted(goods);
        if (goods.getSeller().getId().equals(buyerId)) throw new BusinessException("不能对自己的商品提交购买意向");
        var existing = goodsIntentRepository.findByGoodsIdAndBuyerId(goodsId, buyerId);
        if (existing.isPresent()) return GoodsIntentSummary.from(existing.get());
        ensureListed(goods);
        User buyer = userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("用户不存在"));
        String contact = buildContactSnapshot(goods.getSeller());
        if (contact == null) throw new BusinessException("卖家联系方式缺失");
        GoodsIntent intent = new GoodsIntent(goods, buyer, request.message(), contact);
        GoodsIntent saved = goodsIntentRepository.save(intent);
        notificationService.notify(goods.getSeller(), "收到二手购买意向", buyer.getNickname() + " 想要购买你的商品：" + goods.getTitle(), "GOODS", goods.getId());
        return GoodsIntentSummary.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GoodsIntentSummary> listIntents(Long goodsId, Long sellerId) {
        Goods goods = goodsRepository.findById(goodsId).orElseThrow(() -> new BusinessException("商品不存在"));
        ensureSeller(goods, sellerId);
        ensureNotDeleted(goods);
        return goodsIntentRepository.findByGoodsIdOrderByCreatedAtDesc(goodsId).stream()
                .map(GoodsIntentSummary::from)
                .toList();
    }

    @Transactional
    public GoodsDetailSummary markSold(Long goodsId, Long sellerId, Long buyerId) {
        Goods goods = locked(goodsId);
        ensureSeller(goods, sellerId);
        ensureNotDeleted(goods);
        if (buyerId == null || sellerId.equals(buyerId)) throw new BusinessException("请从意向列表选择实际买家");
        var intents = goodsIntentRepository.findByGoodsIdOrderByCreatedAtDesc(goodsId);
        var winner = intents.stream().filter(i -> i.getBuyer().getId().equals(buyerId)).findFirst()
                .orElseThrow(() -> new BusinessException("对方未对此商品提交意向"));
        if ("SOLD".equals(goods.getStatus()) && goods.getSoldToUser() != null
                && buyerId.equals(goods.getSoldToUser().getId()) && "COMPLETED".equals(winner.getStatus())) {
            return detailFor(goods, sellerId);
        }
        ensureListed(goods);
        if (!"OPEN".equals(winner.getStatus())) throw new BusinessException("该意向已结束");
        if (goodsOrderRepository.hasCommittedOrder(goodsId)) throw new BusinessException("商品已有交易记录，不可重复成交");
        goods.markSold(winner.getBuyer());
        winner.complete();
        notificationService.notify(winner.getBuyer(), "二手交接已确认", "卖家确认与你成交：「" + goods.getTitle() + "」", "GOODS", goodsId);
        for (GoodsIntent intent : intents) closeIntent(intent, "闲置已与其他买家成交");
        return detailFor(goods, sellerId);
    }

    @Transactional
    public GoodsOrderSummary createOnlineEscrowOrder(Long goodsId, Long buyerId) {
        financialWriteGuard.assertEnabled();
        Goods goods = locked(goodsId);
        User buyer = userRepository.findById(buyerId).orElseThrow(() -> new BusinessException("买家不存在"));
        if (!"PUBLISHED".equals(goods.getStatus())) {
            throw new BusinessException("商品不可交易");
        }
        if (goods.getSeller().getId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的商品");
        }
        String contact = buildContactSnapshot(goods.getSeller());
        if (contact == null) throw new BusinessException("卖家联系方式缺失");
        BigDecimal amount = goods.getPrice();
        BigDecimal fee = feePolicyService.calculateOnlineEscrowFee(amount);
        GoodsOrder order = new GoodsOrder("GO" + Instant.now().toEpochMilli(), goods, buyer, goods.getSeller(), amount, fee, contact);
        order.enableOnlineEscrow(amount, fee);
        return GoodsOrderSummary.from(goodsOrderRepository.save(order));
    }

    @Transactional
    public GoodsOrderSummary freezeGoodsEscrow(Long orderId, Long buyerId) {
        financialWriteGuard.assertEnabled();
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
        financialWriteGuard.assertEnabled();
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
        financialWriteGuard.assertEnabled();
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
        financialWriteGuard.assertEnabled();
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
        if (userRoleService.hasRole(userId, "ROLE_GOODS_PUBLISHER")) {
            return;
        }
        roleApplicationRepository.findByUserIdAndRoleType(userId, "GOODS_PUBLISHER")
                .filter(application -> "PAID".equals(application.getDepositStatus()))
                .filter(application -> "APPROVED".equals(application.getReviewStatus()))
                .orElseThrow(() -> new BusinessException("请先开通二手发布者身份"));
    }

    private void ensureSeller(Goods goods, Long sellerId) {
        if (!goods.getSeller().getId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能操作自己的商品");
        }
    }

    private void ensureEscrowBuyer(GoodsOrder order, Long buyerId) {
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException("只有买家可以操作托管交易");
        }
    }

    private GoodsDetailSummary detailFor(Goods goods, Long viewerId) {
        boolean owner = goods.getSeller().getId().equals(viewerId);
        boolean listed = "PUBLISHED".equals(goods.getStatus());
        GoodsIntent mine = viewerId == null || owner ? null
                : goodsIntentRepository.findByGoodsIdAndBuyerId(goods.getId(), viewerId).orElse(null);
        String contactSnapshot = owner ? buildContactSnapshot(goods.getSeller()) : null;
        if (mine != null && mine.contactAvailable()) contactSnapshot = mine.getContactSnapshot();
        // Listing history grants access to the goods, not to a hidden listing's comment thread.
        boolean canReadComments = owner || GoodsListingAccess.isPublic(goods.getStatus());
        List<CommentSummary> comments = canReadComments
                ? commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc("GOODS", goods.getId()).stream()
                        .map(CommentSummary::from).toList()
                : List.of();
        long favoriteCount = favoriteRepository.countByTargetTypeAndTargetId("GOODS", goods.getId());
        boolean favorited = viewerId != null && favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(viewerId, "GOODS", goods.getId());
        String categoryName = categories.findById(goods.getCategoryId()).map(Category::getName).orElse(null);
        boolean canManage = owner && listed;
        boolean canDelete = owner && !goodsOrderRepository.hasUnfinishedOrder(goods.getId());
        return GoodsDetailSummary.from(goods, categoryName, contactSnapshot != null, contactSnapshot,
                mine == null ? null : mine.getStatus(), owner && GoodsListingAccess.canEdit(goods.getStatus()), canDelete, canManage,
                canManage && goodsIntentRepository.existsByGoodsIdAndStatus(goods.getId(), "OPEN")
                    && !goodsOrderRepository.hasCommittedOrder(goods.getId()),
                List.of(), comments, List.of(), favoriteCount, favorited);
    }

    private Goods locked(Long id) {
        return goodsRepository.findByIdForUpdate(id).orElseThrow(GoodsService::notVisible);
    }

    private static ResponseStatusException notVisible() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在或暂不可查看");
    }

    private static void ensureNotDeleted(Goods goods) {
        if (GoodsListingAccess.DELETED.equals(goods.getStatus())) throw new ResponseStatusException(HttpStatus.GONE, GoodsListingAccess.DELETED_MESSAGE);
    }

    private void ensureListed(Goods goods) {
        ensureNotDeleted(goods);
        if (!"PUBLISHED".equals(goods.getStatus())) throw new BusinessException("商品不在售，不能执行此操作");
    }

    private void ensureCategory(Long id) {
        if (id == null || !categories.existsByIdAndEnabledTrue(id)) throw new BusinessException("请选择有效的商品分类");
    }

    private void closeIntent(GoodsIntent intent, String title) {
        if (!"OPEN".equals(intent.getStatus())) return;
        intent.close();
        notificationService.notify(intent.getBuyer(), title, "「" + intent.getGoods().getTitle() + "」的购买意向已结束", "GOODS", intent.getGoods().getId());
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
        return builder.isEmpty() ? null : builder.toString();
    }
}
