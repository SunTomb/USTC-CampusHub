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
import com.campushub.moderation.ReportRecord;
import com.campushub.moderation.ReportRecordRepository;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            RoleApplicationRepository roleApplicationRepository) {
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

    private <T> long count(List<T> items, java.util.function.Predicate<T> predicate) {
        return items.stream().filter(predicate).count();
    }
}
