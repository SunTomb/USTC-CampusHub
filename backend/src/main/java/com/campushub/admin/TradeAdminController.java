package com.campushub.admin;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.goods.GoodsService;
import com.campushub.goods.GoodsOrderRepository;
import com.campushub.goods.GoodsOrderSummary;
import com.campushub.goods.GoodsRepository;
import com.campushub.goods.GoodsSummary;
import com.campushub.task.RewardTaskRepository;
import com.campushub.task.RewardTaskSummary;
import com.campushub.task.TaskIssueRepository;
import com.campushub.task.TaskIssueSummary;
import com.campushub.task.RunnerTaskService;
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
    private final RunnerTaskService runnerTaskService;
    private final TaskIssueRepository taskIssueRepository;
    private final GoodsService goodsService;
    private final GoodsRepository goodsRepository;
    private final GoodsOrderRepository goodsOrderRepository;

    public TradeAdminController(
            CurrentUserService currentUserService,
            RewardTaskRepository rewardTaskRepository,
            RunnerTaskService runnerTaskService,
            TaskIssueRepository taskIssueRepository,
            GoodsService goodsService, GoodsRepository goodsRepository,
            GoodsOrderRepository goodsOrderRepository) {
        this.currentUserService = currentUserService;
        this.rewardTaskRepository = rewardTaskRepository;
        this.runnerTaskService = runnerTaskService;
        this.taskIssueRepository = taskIssueRepository;
        this.goodsService=goodsService;
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
        Long adminId = currentUserService.requireTradeAdminId();
        return ApiResponse.ok(runnerTaskService.cancelByAdmin(taskId, adminId, request == null ? null : request.note()));
    }

    @GetMapping("/goods")
    public ApiResponse<List<GoodsSummary>> listGoods() {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(goodsRepository.findAll().stream().map(GoodsSummary::from).toList());
    }

    @PostMapping("/goods/{goodsId}/off-shelf")
    public ApiResponse<GoodsSummary> offShelfGoods(@PathVariable Long goodsId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireTradeAdminId();
        return ApiResponse.ok(goodsService.offShelfByAdmin(goodsId));
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
