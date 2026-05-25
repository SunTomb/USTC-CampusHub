package com.campushub.admin;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.goods.Goods;
import com.campushub.goods.GoodsOrderRepository;
import com.campushub.goods.GoodsOrderSummary;
import com.campushub.goods.GoodsRepository;
import com.campushub.goods.GoodsSummary;
import com.campushub.task.RewardTask;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.RewardTaskSummary;
import com.campushub.task.TaskIssueRepository;
import com.campushub.task.TaskIssueSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trade")
public class TradeAdminController {

    private final CurrentUserService currentUserService;
    private final RewardTaskRepository rewardTaskRepository;
    private final TaskIssueRepository taskIssueRepository;
    private final GoodsRepository goodsRepository;
    private final GoodsOrderRepository goodsOrderRepository;

    public TradeAdminController(
            CurrentUserService currentUserService,
            RewardTaskRepository rewardTaskRepository,
            TaskIssueRepository taskIssueRepository,
            GoodsRepository goodsRepository,
            GoodsOrderRepository goodsOrderRepository) {
        this.currentUserService = currentUserService;
        this.rewardTaskRepository = rewardTaskRepository;
        this.taskIssueRepository = taskIssueRepository;
        this.goodsRepository = goodsRepository;
        this.goodsOrderRepository = goodsOrderRepository;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<RewardTaskSummary>> listTasks() {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(rewardTaskRepository.findAll().stream().map(RewardTaskSummary::from).toList());
    }

    @PostMapping("/tasks/{taskId}/close")
    public ApiResponse<RewardTaskSummary> closeTask(@PathVariable Long taskId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireTradeAdminId();
        RewardTask task = rewardTaskRepository.findById(taskId).orElseThrow();
        task.moveTo("CANCELLED");
        return ApiResponse.ok(RewardTaskSummary.from(rewardTaskRepository.save(task)));
    }

    @GetMapping("/goods")
    public ApiResponse<List<GoodsSummary>> listGoods() {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(goodsRepository.findAll().stream().map(GoodsSummary::from).toList());
    }

    @PostMapping("/goods/{goodsId}/off-shelf")
    public ApiResponse<GoodsSummary> offShelfGoods(@PathVariable Long goodsId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireTradeAdminId();
        Goods goods = goodsRepository.findById(goodsId).orElseThrow();
        goods.offShelf();
        return ApiResponse.ok(GoodsSummary.from(goodsRepository.save(goods)));
    }

    @GetMapping("/goods-orders")
    public ApiResponse<List<GoodsOrderSummary>> listGoodsOrders() {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(goodsOrderRepository.findAll().stream().map(GoodsOrderSummary::from).toList());
    }

    @GetMapping("/task-issues")
    public ApiResponse<List<TaskIssueSummary>> listTaskIssues() {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(taskIssueRepository.findByStatusOrderByCreatedAtAsc("OPEN").stream().map(TaskIssueSummary::from).toList());
    }
}
