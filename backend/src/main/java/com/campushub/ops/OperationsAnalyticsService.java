package com.campushub.ops;

import com.campushub.goods.Goods;
import com.campushub.goods.GoodsIntent;
import com.campushub.goods.GoodsIntentRepository;
import com.campushub.goods.GoodsRepository;
import com.campushub.identity.PlatformRoleType;
import com.campushub.identity.RoleApplication;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.interaction.Comment;
import com.campushub.interaction.CommentRepository;
import com.campushub.interaction.Favorite;
import com.campushub.interaction.FavoriteRepository;
import com.campushub.moderation.AdminActionLog;
import com.campushub.moderation.AdminActionLogRepository;
import com.campushub.moderation.CreditAdjustmentRecord;
import com.campushub.moderation.CreditAdjustmentRecordRepository;
import com.campushub.moderation.ReportRecord;
import com.campushub.moderation.ReportRecordRepository;
import com.campushub.moderation.UserRestriction;
import com.campushub.moderation.UserRestrictionRepository;
import com.campushub.moderation.ViolationRecord;
import com.campushub.moderation.ViolationRecordRepository;
import com.campushub.payment.ServiceFeeRecord;
import com.campushub.payment.ServiceFeeRecordRepository;
import com.campushub.projectad.ProjectAd;
import com.campushub.projectad.ProjectAdRepository;
import com.campushub.shop.ServiceItem;
import com.campushub.shop.ServiceItemRepository;
import com.campushub.shop.ServiceOrder;
import com.campushub.shop.ServiceOrderRepository;
import com.campushub.shop.Shop;
import com.campushub.shop.ShopRepository;
import com.campushub.task.RewardTask;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.TaskApplication;
import com.campushub.task.TaskApplicationRepository;
import com.campushub.task.TaskIssue;
import com.campushub.task.TaskIssueRepository;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperationsAnalyticsService {

    private final UserRepository userRepository;
    private final RewardTaskRepository rewardTaskRepository;
    private final TaskApplicationRepository taskApplicationRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final GoodsRepository goodsRepository;
    private final GoodsIntentRepository goodsIntentRepository;
    private final ShopRepository shopRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ProjectAdRepository projectAdRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final ReportRecordRepository reportRecordRepository;
    private final ServiceFeeRecordRepository serviceFeeRecordRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final ViolationRecordRepository violationRecordRepository;
    private final CreditAdjustmentRecordRepository creditAdjustmentRecordRepository;
    private final UserRestrictionRepository userRestrictionRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    public OperationsAnalyticsService(
            UserRepository userRepository,
            RewardTaskRepository rewardTaskRepository,
            TaskApplicationRepository taskApplicationRepository,
            TaskIssueRepository taskIssueRepository,
            GoodsRepository goodsRepository,
            GoodsIntentRepository goodsIntentRepository,
            ShopRepository shopRepository,
            ServiceItemRepository serviceItemRepository,
            ServiceOrderRepository serviceOrderRepository,
            ProjectAdRepository projectAdRepository,
            FavoriteRepository favoriteRepository,
            CommentRepository commentRepository,
            ReportRecordRepository reportRecordRepository,
            ServiceFeeRecordRepository serviceFeeRecordRepository,
            RoleApplicationRepository roleApplicationRepository,
            ViolationRecordRepository violationRecordRepository,
            CreditAdjustmentRecordRepository creditAdjustmentRecordRepository,
            UserRestrictionRepository userRestrictionRepository,
            AdminActionLogRepository adminActionLogRepository) {
        this.userRepository = userRepository;
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskApplicationRepository = taskApplicationRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.goodsRepository = goodsRepository;
        this.goodsIntentRepository = goodsIntentRepository;
        this.shopRepository = shopRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.projectAdRepository = projectAdRepository;
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.reportRecordRepository = reportRecordRepository;
        this.serviceFeeRecordRepository = serviceFeeRecordRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.violationRecordRepository = violationRecordRepository;
        this.creditAdjustmentRecordRepository = creditAdjustmentRecordRepository;
        this.userRestrictionRepository = userRestrictionRepository;
        this.adminActionLogRepository = adminActionLogRepository;
    }

    public OperationsAnalyticsOverview overview(AnalyticsDateRange range) {
        List<RewardTask> tasks = rewardTaskRepository.findAll();
        List<TaskIssue> taskIssues = taskIssueRepository.findAll();
        List<Goods> goods = goodsRepository.findAll();
        List<GoodsIntent> goodsIntents = goodsIntentRepository.findAll();
        List<ServiceOrder> serviceOrders = serviceOrderRepository.findAll();
        List<ProjectAd> projectAds = projectAdRepository.findAll();
        List<ReportRecord> reports = reportRecordRepository.findAll();
        List<ServiceFeeRecord> serviceFees = serviceFeeRecordRepository.findAll();
        List<RoleApplication> roleApplications = roleApplicationRepository.findAll();

        long newUsers = userRepository.findAll().stream().filter(user -> inRange(user.getCreatedAt(), range)).count();
        long newTasks = tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).count();
        long completedTasks = tasks.stream()
                .filter(task -> inRange(task.getCreatedAt(), range))
                .filter(task -> "COMPLETED".equals(task.getWorkflowStatus()))
                .count();
        long taskIssueCount = taskIssues.stream().filter(issue -> inRange(issue.getCreatedAt(), range)).count();
        long newGoods = goods.stream().filter(item -> inRange(businessTime(item.getPublishedAt(), item.getCreatedAt()), range)).count();
        long activeGoods = goods.stream()
                .filter(item -> inRange(businessTime(item.getPublishedAt(), item.getCreatedAt()), range))
                .filter(item -> "PUBLISHED".equals(item.getStatus()))
                .count();
        long goodsIntentCount = goodsIntents.stream().filter(intent -> inRange(intent.getCreatedAt(), range)).count();
        long newShopOrders = serviceOrders.stream().filter(order -> inRange(order.getCreatedAt(), range)).count();
        long completedShopOrders = serviceOrders.stream()
                .filter(order -> inRange(prefer(order.getCompletedAt(), order.getCreatedAt()), range))
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .count();
        long canceledShopOrders = serviceOrders.stream()
                .filter(order -> inRange(prefer(order.getCanceledAt(), order.getCreatedAt()), range))
                .filter(order -> "CANCELED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus()))
                .count();
        long newProjectAds = projectAds.stream().filter(ad -> inRange(ad.getCreatedAt(), range)).count();
        long approvedProjectAds = projectAds.stream()
                .filter(ad -> inRange(projectAdApprovedTime(ad), range))
                .filter(ad -> "APPROVED".equals(ad.getStatus()))
                .count();
        long projectAdViews = projectAds.stream()
                .map(ProjectAd::getViewCount)
                .filter(count -> count != null)
                .mapToLong(Integer::longValue)
                .sum();
        long openReports = reports.stream()
                .filter(report -> inRange(report.getCreatedAt(), range))
                .filter(report -> "OPEN".equals(report.getStatus()) || "IN_REVIEW".equals(report.getStatus()) || "ESCALATED".equals(report.getStatus()))
                .count();
        long pendingRoleApplications = roleApplications.stream()
                .filter(application -> inRange(application.getCreatedAt(), range))
                .filter(application -> "PENDING_REVIEW".equals(application.getReviewStatus()))
                .count();
        long pendingProjectAds = projectAds.stream()
                .filter(ad -> inRange(ad.getCreatedAt(), range))
                .filter(ad -> "PENDING_REVIEW".equals(ad.getStatus()))
                .count();
        BigDecimal paidServiceFeeAmount = sumAmounts(serviceFees.stream()
                .filter(fee -> "PAID".equals(fee.getStatus()))
                .filter(fee -> inRange(serviceFeeBusinessTime(fee), range))
                .toList());
        BigDecimal roleDepositAmount = sumRoleDeposits(roleApplications.stream()
                .filter(application -> inRange(application.getCreatedAt(), range))
                .filter(this::isPaidRoleDeposit)
                .toList());
        long activeUsers = collectActiveUserIds(tasks, goods, goodsIntents, serviceOrders, projectAds, reports, range).size();

        List<MetricCardSummary> cards = List.of(
                card("newUsers", "新增用户", newUsers),
                card("activeUsers", "活跃用户", activeUsers),
                card("newTasks", "新增跑腿任务", newTasks),
                card("newGoods", "新增二手商品", newGoods),
                card("openReports", "待处理举报", openReports),
                moneyCard("paidServiceFeeAmount", "已付服务费", paidServiceFeeAmount),
                moneyCard("roleDepositAmount", "身份保证金", roleDepositAmount)
        );

        return new OperationsAnalyticsOverview(
                range.startDate(),
                range.endDate(),
                newUsers,
                activeUsers,
                newTasks,
                completedTasks,
                taskIssueCount,
                newGoods,
                activeGoods,
                goodsIntentCount,
                newShopOrders,
                completedShopOrders,
                canceledShopOrders,
                newProjectAds,
                approvedProjectAds,
                projectAdViews,
                openReports,
                pendingRoleApplications,
                pendingProjectAds,
                paidServiceFeeAmount,
                roleDepositAmount,
                cards
        );
    }

    public OperationsFunnelSummary funnels(AnalyticsDateRange range) {
        List<RewardTask> tasks = rewardTaskRepository.findAll().stream()
                .filter(task -> inRange(task.getCreatedAt(), range))
                .toList();
        List<TaskApplication> applications = taskApplicationRepository.findAll().stream()
                .filter(application -> inRange(application.getCreatedAt(), range))
                .toList();
        List<TaskIssue> issues = taskIssueRepository.findAll().stream()
                .filter(issue -> inRange(issue.getCreatedAt(), range))
                .toList();
        List<Goods> goods = goodsRepository.findAll().stream()
                .filter(item -> inRange(businessTime(item.getPublishedAt(), item.getCreatedAt()), range))
                .toList();
        List<GoodsIntent> intents = goodsIntentRepository.findAll().stream()
                .filter(intent -> inRange(intent.getCreatedAt(), range))
                .toList();
        List<Shop> shops = shopRepository.findAll().stream()
                .filter(shop -> inRange(shop.getCreatedAt(), range))
                .toList();
        List<ServiceItem> items = serviceItemRepository.findAll().stream()
                .filter(item -> inRange(item.getCreatedAt(), range))
                .toList();
        List<ServiceOrder> orders = serviceOrderRepository.findAll().stream()
                .filter(order -> inRange(order.getCreatedAt(), range))
                .toList();
        List<ProjectAd> projectAds = projectAdRepository.findAll().stream()
                .filter(ad -> inRange(ad.getCreatedAt(), range))
                .toList();
        List<Favorite> favorites = favoriteRepository.findAll().stream()
                .filter(favorite -> inRange(favorite.getCreatedAt(), range))
                .toList();
        List<Comment> comments = commentRepository.findAll().stream()
                .filter(comment -> inRange(comment.getCreatedAt(), range))
                .toList();

        BusinessFunnelSummary taskFunnel = new BusinessFunnelSummary("tasks", "跑腿任务", List.of(
                card("published", "发布", tasks.size()),
                card("grabMode", "抢单模式", count(tasks, task -> "GRAB".equals(task.getAcceptanceMode()))),
                card("applicationMode", "申请模式", count(tasks, task -> "APPLICATION".equals(task.getAcceptanceMode()))),
                card("applications", "报名/接单", applications.size()),
                card("accepted", "已接单", count(tasks, task -> isAcceptedOrLater(task.getWorkflowStatus()))),
                card("completed", "已完成", count(tasks, task -> "COMPLETED".equals(task.getWorkflowStatus()))),
                card("issues", "异常", issues.size())
        ));
        BusinessFunnelSummary goodsFunnel = new BusinessFunnelSummary("goods", "二手交易", List.of(
                card("published", "发布", goods.size()),
                card("onSale", "在售", count(goods, item -> "PUBLISHED".equals(item.getStatus()))),
                card("favorites", "收藏", count(favorites, favorite -> "GOODS".equals(favorite.getTargetType()))),
                card("comments", "留言", count(comments, comment -> "GOODS".equals(comment.getTargetType()))),
                card("intents", "购买意向", intents.size()),
                card("closed", "成交/下架", count(goods, item -> "SOLD".equals(item.getStatus()) || "OFF_SHELF".equals(item.getStatus())))
        ));
        BusinessFunnelSummary shopFunnel = new BusinessFunnelSummary("shops", "学生店铺", List.of(
                card("shops", "店铺", shops.size()),
                card("items", "服务项目", items.size()),
                card("requested", "预约请求", count(orders, order -> "REQUESTED".equals(order.getStatus()))),
                card("accepted", "已接受", count(orders, order -> "ACCEPTED".equals(order.getStatus()))),
                card("inService", "服务中", count(orders, order -> "IN_SERVICE".equals(order.getStatus()))),
                card("completed", "已完成", count(orders, order -> "COMPLETED".equals(order.getStatus()))),
                card("canceled", "取消/拒绝", count(orders, order -> "CANCELED".equals(order.getStatus()) || "REJECTED".equals(order.getStatus())))
        ));
        BusinessFunnelSummary projectAdFunnel = new BusinessFunnelSummary("projectAds", "项目广告", List.of(
                card("created", "创建", projectAds.size()),
                card("pending", "待审核", count(projectAds, ad -> "PENDING_REVIEW".equals(ad.getStatus()))),
                card("approved", "已通过", count(projectAds, ad -> "APPROVED".equals(ad.getStatus()))),
                card("closed", "已关闭", count(projectAds, ad -> "CLOSED".equals(ad.getStatus()) || "BLOCKED".equals(ad.getStatus()))),
                card("views", "项目广告累计浏览", projectAds.stream().map(ProjectAd::getViewCount).filter(value -> value != null).mapToLong(Integer::longValue).sum()),
                card("favorites", "收藏", count(favorites, favorite -> "PROJECT_AD".equals(favorite.getTargetType()))),
                card("comments", "评论", count(comments, comment -> "PROJECT_AD".equals(comment.getTargetType())))
        ));

        return new OperationsFunnelSummary(range.startDate(), range.endDate(), List.of(taskFunnel, goodsFunnel, shopFunnel, projectAdFunnel));
    }

    public FeeAnalyticsSummary fees(AnalyticsDateRange range) {
        List<ServiceFeeRecord> serviceFees = serviceFeeRecordRepository.findAll().stream()
                .filter(fee -> inRange(serviceFeeBusinessTime(fee), range))
                .toList();
        List<RoleApplication> roleApplications = roleApplicationRepository.findAll().stream()
                .filter(application -> inRange(application.getCreatedAt(), range))
                .toList();

        BigDecimal paidServiceFeeAmount = sumAmounts(serviceFees.stream()
                .filter(fee -> "PAID".equals(fee.getStatus()))
                .toList());
        BigDecimal pendingServiceFeeAmount = sumAmounts(serviceFees.stream()
                .filter(fee -> "PENDING".equals(fee.getStatus()))
                .toList());
        BigDecimal roleDepositAmount = sumRoleDeposits(roleApplications.stream()
                .filter(this::isPaidRoleDeposit)
                .toList());

        Map<String, BigDecimal> feesByTargetType = new LinkedHashMap<>();
        serviceFees.stream()
                .filter(fee -> "PAID".equals(fee.getStatus()))
                .forEach(fee -> feesByTargetType.merge(fee.getTargetType(), nullSafe(fee.getAmount()), BigDecimal::add));
        List<MetricCardSummary> serviceFeesByTargetType = feesByTargetType.entrySet().stream()
                .map(entry -> moneyCard(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();

        Map<String, BigDecimal> depositsByType = new LinkedHashMap<>();
        for (PlatformRoleType roleType : PlatformRoleType.values()) {
            depositsByType.put(roleType.name(), BigDecimal.ZERO);
        }
        roleApplications.stream()
                .filter(this::isPaidRoleDeposit)
                .forEach(application -> depositsByType.merge(application.getRoleType(), nullSafe(application.getDepositAmount()), BigDecimal::add));
        List<MetricCardSummary> roleDepositsByType = depositsByType.entrySet().stream()
                .map(entry -> moneyCard(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();

        return new FeeAnalyticsSummary(
                range.startDate(),
                range.endDate(),
                serviceFees.size(),
                paidServiceFeeAmount,
                pendingServiceFeeAmount,
                serviceFeesByTargetType,
                roleApplications.size(),
                roleDepositAmount,
                roleDepositsByType
        );
    }

    public OperationsZoneSummary zones(AnalyticsDateRange range) {
        List<RewardTask> tasks = rewardTaskRepository.findAll().stream()
                .filter(task -> inRange(task.getCreatedAt(), range))
                .toList();
        List<Goods> goods = goodsRepository.findAll().stream()
                .filter(item -> inRange(businessTime(item.getPublishedAt(), item.getCreatedAt()), range))
                .toList();
        List<Shop> shops = shopRepository.findAll().stream()
                .filter(shop -> inRange(shop.getCreatedAt(), range))
                .toList();
        List<ProjectAd> projectAds = projectAdRepository.findAll().stream()
                .filter(ad -> inRange(projectAdApprovedTime(ad), range))
                .toList();

        return new OperationsZoneSummary(
                range.startDate(),
                range.endDate(),
                topZoneMetrics(tasks, RewardTask::getOriginZone),
                topZoneMetrics(tasks, RewardTask::getDestinationZone),
                topZoneMetrics(tasks, task -> text(task.getOriginZone()) + " -> " + text(task.getDestinationZone())),
                topZoneMetrics(goods, Goods::getCampusZone),
                topZoneMetrics(shops, Shop::getCampusZone),
                topZoneMetrics(projectAds, ProjectAd::getCampusZone)
        );
    }

    public CsvExport exportTasks(AnalyticsDateRange range) {
        List<String> rows = csvRows(row -> {
            row.accept(csv("任务ID", "标题", "发布者昵称", "赏金", "保证金", "发布状态", "流程状态", "接单模式", "起点校区", "终点校区", "截止时间", "创建时间"));
            rewardTaskRepository.findAll().stream()
                    .filter(task -> inRange(task.getCreatedAt(), range))
                    .forEach(task -> row.accept(csv(
                            task.getId(),
                            task.getTitle(),
                            nickname(task.getPublisher()),
                            task.getRewardAmount(),
                            task.getDepositAmount(),
                            task.getStatus(),
                            task.getWorkflowStatus(),
                            task.getAcceptanceMode(),
                            task.getOriginZone(),
                            task.getDestinationZone(),
                            task.getDeadline(),
                            task.getCreatedAt())));
        });
        return CsvExport.of(fileName("tasks", range), String.join("\n", rows));
    }

    public CsvExport exportGoods(AnalyticsDateRange range) {
        List<String> rows = csvRows(row -> {
            row.accept(csv("商品ID", "标题", "卖家昵称", "价格", "原价", "成色", "校区", "交易地点", "配送方式", "状态", "浏览量", "联系方式开放", "发布时间", "创建时间"));
            goodsRepository.findAll().stream()
                    .filter(item -> inRange(businessTime(item.getPublishedAt(), item.getCreatedAt()), range))
                    .forEach(item -> row.accept(csv(
                            item.getId(),
                            item.getTitle(),
                            nickname(item.getSeller()),
                            item.getPrice(),
                            item.getOriginalPrice(),
                            item.getConditionLevel(),
                            item.getCampusZone(),
                            item.getTradeLocation(),
                            item.getDeliveryMethod(),
                            item.getStatus(),
                            item.getViewCount(),
                            contactVisibilityLabel(item.getContactVisibility()),
                            item.getPublishedAt(),
                            item.getCreatedAt())));
        });
        return CsvExport.of(fileName("goods", range), String.join("\n", rows));
    }

    public CsvExport exportShopOrders(AnalyticsDateRange range) {
        List<String> rows = csvRows(row -> {
            row.accept(csv("预约ID", "订单号", "服务项目", "店铺", "客户昵称", "服务者昵称", "预约金额", "服务费", "状态", "预约时间", "完成时间", "取消时间", "创建时间"));
            serviceOrderRepository.findAll().stream()
                    .filter(order -> inRange(order.getCreatedAt(), range))
                    .forEach(order -> row.accept(csv(
                            order.getId(),
                            order.getOrderNo(),
                            order.getServiceItem() == null ? null : order.getServiceItem().getTitle(),
                            order.getServiceItem() == null || order.getServiceItem().getShop() == null ? null : order.getServiceItem().getShop().getName(),
                            nickname(order.getCustomer()),
                            nickname(order.getProvider()),
                            order.getAmount(),
                            order.getServiceFee(),
                            order.getStatus(),
                            order.getAppointmentTime(),
                            order.getCompletedAt(),
                            order.getCanceledAt(),
                            order.getCreatedAt())));
        });
        return CsvExport.of(fileName("shop-orders", range), String.join("\n", rows));
    }

    public CsvExport exportProjectAds(AnalyticsDateRange range) {
        List<String> rows = csvRows(row -> {
            row.accept(csv("项目ID", "标题", "类型", "发布者昵称", "校区", "标签", "状态", "浏览量", "精选", "精选优先级", "联系方式开放", "过期时间", "发布时间", "创建时间"));
            projectAdRepository.findAll().stream()
                    .filter(ad -> inRange(projectAdApprovedTime(ad), range))
                    .forEach(ad -> row.accept(csv(
                            ad.getId(),
                            ad.getTitle(),
                            ad.getAdType(),
                            nickname(ad.getPublisher()),
                            ad.getCampusZone(),
                            ad.getTags(),
                            ad.getStatus(),
                            ad.getViewCount(),
                            yesNo(Boolean.TRUE.equals(ad.getFeatured())),
                            ad.getFeaturedPriority(),
                            contactVisibilityLabel(ad.getContactVisibility()),
                            ad.getExpiresAt(),
                            ad.getPublishedAt(),
                            ad.getCreatedAt())));
        });
        return CsvExport.of(fileName("project-ads", range), String.join("\n", rows));
    }

    public CsvExport exportGovernance(AnalyticsDateRange range) {
        List<String> rows = csvRows(row -> {
            row.accept(csv("记录类型", "记录ID", "用户昵称", "目标类型", "目标ID", "状态/等级", "原因/动作", "信用变化", "管理员昵称", "时间"));
            reportRecordRepository.findAll().stream()
                    .filter(report -> inRange(report.getCreatedAt(), range))
                    .forEach(report -> row.accept(csv(
                            "举报",
                            report.getId(),
                            nickname(report.getReporter()),
                            report.getTargetType(),
                            report.getTargetId(),
                            report.getStatus(),
                            report.getReason(),
                            null,
                            nickname(report.getHandler()),
                            report.getCreatedAt())));
            violationRecordRepository.findAll().stream()
                    .filter(violation -> inRange(violation.getCreatedAt(), range))
                    .forEach(violation -> row.accept(csv(
                            "违规",
                            violation.getId(),
                            nickname(violation.getUser()),
                            violation.getTargetType(),
                            violation.getTargetId(),
                            violation.getSeverity(),
                            violation.getViolationType(),
                            violation.getCreditDelta(),
                            nickname(violation.getAdmin()),
                            violation.getCreatedAt())));
            creditAdjustmentRecordRepository.findAll().stream()
                    .filter(adjustment -> inRange(adjustment.getCreatedAt(), range))
                    .forEach(adjustment -> row.accept(csv(
                            "信用调整",
                            adjustment.getId(),
                            nickname(adjustment.getUser()),
                            "USER",
                            adjustment.getUser() == null ? null : adjustment.getUser().getId(),
                            text(adjustment.getBeforeScore()) + " -> " + text(adjustment.getAfterScore()),
                            adjustment.getReason(),
                            adjustment.getDeltaScore(),
                            nickname(adjustment.getAdmin()),
                            adjustment.getCreatedAt())));
            userRestrictionRepository.findAll().stream()
                    .filter(restriction -> inRange(restriction.getCreatedAt(), range))
                    .forEach(restriction -> row.accept(csv(
                            "限制",
                            restriction.getId(),
                            nickname(restriction.getUser()),
                            "USER",
                            restriction.getUser() == null ? null : restriction.getUser().getId(),
                            restriction.getRestrictionType(),
                            restriction.getReason(),
                            null,
                            nickname(restriction.getAdmin()),
                            restriction.getCreatedAt())));
            adminActionLogRepository.findAll().stream()
                    .filter(log -> inRange(log.getCreatedAt(), range))
                    .forEach(log -> row.accept(csv(
                            "管理员动作",
                            log.getId(),
                            null,
                            log.getTargetType(),
                            log.getTargetId(),
                            log.getActionType(),
                            log.getNote(),
                            null,
                            nickname(log.getAdmin()),
                            log.getCreatedAt())));
        });
        return CsvExport.of(fileName("governance", range), String.join("\n", rows));
    }

    public CsvExport exportFees(AnalyticsDateRange range) {
        List<String> rows = csvRows(row -> {
            row.accept(csv("费用类型", "记录ID", "编号/角色", "用户昵称", "目标类型", "目标ID", "金额", "状态", "支付时间", "创建时间"));
            serviceFeeRecordRepository.findAll().stream()
                    .filter(fee -> inRange(serviceFeeBusinessTime(fee), range))
                    .forEach(fee -> row.accept(csv(
                            "服务费",
                            fee.getId(),
                            fee.getFeeNo(),
                            nickname(fee.getPayer()),
                            fee.getTargetType(),
                            fee.getTargetId(),
                            fee.getAmount(),
                            fee.getStatus(),
                            fee.getPaidAt(),
                            fee.getCreatedAt())));
            roleApplicationRepository.findAll().stream()
                    .filter(application -> inRange(application.getCreatedAt(), range))
                    .forEach(application -> row.accept(csv(
                            "角色保证金",
                            application.getId(),
                            application.getRoleType(),
                            nickname(application.getUser()),
                            "ROLE_APPLICATION",
                            application.getId(),
                            application.getDepositAmount(),
                            application.getDepositStatus() + "/" + application.getReviewStatus(),
                            application.getReviewedAt(),
                            application.getCreatedAt())));
        });
        return CsvExport.of(fileName("fees", range), String.join("\n", rows));
    }

    boolean inRange(LocalDateTime value, AnalyticsDateRange range) {
        return value != null && !value.isBefore(range.startInclusive()) && value.isBefore(range.endExclusive());
    }

    MetricCardSummary card(String key, String label, long value) {
        return new MetricCardSummary(key, label, BigDecimal.valueOf(value), "count");
    }

    MetricCardSummary moneyCard(String key, String label, BigDecimal value) {
        return new MetricCardSummary(key, label, nullSafe(value), "CNY");
    }

    BigDecimal sumAmounts(List<ServiceFeeRecord> records) {
        return records.stream()
                .map(ServiceFeeRecord::getAmount)
                .map(this::nullSafe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRoleDeposits(List<RoleApplication> applications) {
        return applications.stream()
                .map(RoleApplication::getDepositAmount)
                .map(this::nullSafe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Set<Long> collectActiveUserIds(
            List<RewardTask> tasks,
            List<Goods> goods,
            List<GoodsIntent> goodsIntents,
            List<ServiceOrder> serviceOrders,
            List<ProjectAd> projectAds,
            List<ReportRecord> reports,
            AnalyticsDateRange range) {
        Set<Long> activeUserIds = new LinkedHashSet<>();
        tasks.stream().filter(task -> inRange(task.getCreatedAt(), range)).map(RewardTask::getPublisher).forEach(user -> addUserId(activeUserIds, user));
        goods.stream().filter(item -> inRange(businessTime(item.getPublishedAt(), item.getCreatedAt()), range)).map(Goods::getSeller).forEach(user -> addUserId(activeUserIds, user));
        goodsIntents.stream().filter(intent -> inRange(intent.getCreatedAt(), range)).map(GoodsIntent::getBuyer).forEach(user -> addUserId(activeUserIds, user));
        serviceOrders.stream().filter(order -> inRange(order.getCreatedAt(), range)).map(ServiceOrder::getCustomer).forEach(user -> addUserId(activeUserIds, user));
        projectAds.stream().filter(ad -> inRange(ad.getCreatedAt(), range)).map(ProjectAd::getPublisher).forEach(user -> addUserId(activeUserIds, user));
        reports.stream().filter(report -> inRange(report.getCreatedAt(), range)).map(ReportRecord::getReporter).forEach(user -> addUserId(activeUserIds, user));
        return activeUserIds;
    }

    private void addUserId(Set<Long> userIds, User user) {
        addUserId(userIds, user == null ? null : user.getId());
    }

    private void addUserId(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private LocalDateTime prefer(LocalDateTime primary, LocalDateTime fallback) {
        return primary == null ? fallback : primary;
    }

    private LocalDateTime businessTime(LocalDateTime preferred, LocalDateTime fallback) {
        return preferred == null ? fallback : preferred;
    }

    private LocalDateTime projectAdApprovedTime(ProjectAd ad) {
        return businessTime(ad.getPublishedAt(), businessTime(ad.getReviewedAt(), ad.getCreatedAt()));
    }

    private LocalDateTime serviceFeeBusinessTime(ServiceFeeRecord fee) {
        if ("PAID".equals(fee.getStatus())) {
            return businessTime(fee.getPaidAt(), fee.getCreatedAt());
        }
        return fee.getCreatedAt();
    }

    private boolean isPaidRoleDeposit(RoleApplication application) {
        return "PAID".equals(application.getDepositStatus());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isAcceptedOrLater(String workflowStatus) {
        return "ACCEPTED".equals(workflowStatus)
                || "HEADING_TO_PICKUP".equals(workflowStatus)
                || "PICKED_UP".equals(workflowStatus)
                || "DELIVERING".equals(workflowStatus)
                || "DELIVERED".equals(workflowStatus)
                || "PENDING_CONFIRMATION".equals(workflowStatus)
                || "COMPLETED".equals(workflowStatus)
                || "ISSUE_HANDLING".equals(workflowStatus)
                || "DISPUTE_HANDLING".equals(workflowStatus);
    }

    private <T> List<ZoneMetricSummary> topZoneMetrics(List<T> items, java.util.function.Function<T, String> keyExtractor) {
        return items.stream()
                .map(keyExtractor)
                .map(this::normalizeZoneKey)
                .collect(Collectors.groupingBy(key -> key, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Comparator
                        .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(20)
                .map(entry -> new ZoneMetricSummary(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();
    }

    private String normalizeZoneKey(String value) {
        String normalized = text(value).trim();
        return normalized.isEmpty() ? "UNKNOWN" : normalized;
    }

    private List<String> csvRows(Consumer<Consumer<String>> writer) {
        List<String> rows = new java.util.ArrayList<>();
        writer.accept(rows::add);
        return rows;
    }

    String csv(Object... values) {
        return java.util.Arrays.stream(values)
                .map(this::text)
                .map(this::escapeCsv)
                .collect(Collectors.joining(","));
    }

    String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        boolean neutralizedFormula = startsWithSpreadsheetFormulaTrigger(safeValue);
        if (neutralizedFormula) {
            safeValue = "'" + safeValue;
        }
        if (neutralizedFormula || safeValue.contains("\"") || safeValue.contains(",") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private boolean startsWithSpreadsheetFormulaTrigger(String value) {
        if (value.isEmpty()) {
            return false;
        }
        return switch (value.charAt(0)) {
            case '=', '+', '-', '@', '\t', '\r' -> true;
            default -> false;
        };
    }

    String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    String yesNo(boolean value) {
        return value ? "是" : "否";
    }

    private String contactVisibilityLabel(String contactVisibility) {
        return switch (text(contactVisibility)) {
            case "PUBLIC" -> "公开";
            case "LOGIN_ONLY" -> "登录可见";
            case "INTENT_ONLY" -> "意向后可见";
            default -> text(contactVisibility);
        };
    }

    private String nickname(User user) {
        return user == null ? "" : user.getNickname();
    }

    private String fileName(String prefix, AnalyticsDateRange range) {
        return "campushub-" + prefix + "-" + range.startDate() + "-" + range.endDate() + ".csv";
    }

    private <T> long count(List<T> items, java.util.function.Predicate<T> predicate) {
        return items.stream().filter(predicate).count();
    }
}
