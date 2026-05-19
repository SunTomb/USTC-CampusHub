package com.campushub.task;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class RewardTaskController {

    private final RewardTaskRepository rewardTaskRepository;

    public RewardTaskController(RewardTaskRepository rewardTaskRepository) {
        this.rewardTaskRepository = rewardTaskRepository;
    }

    @GetMapping
    public ApiResponse<List<RewardTaskSummary>> listTasks() {
        List<RewardTaskSummary> tasks = rewardTaskRepository.findByStatusOrderByDeadlineAsc("PUBLISHED").stream()
                .map(RewardTaskSummary::from)
                .toList();
        return ApiResponse.ok(tasks);
    }

    @GetMapping("/{id}")
    public ApiResponse<RewardTaskSummary> getTask(@PathVariable Long id) {
        RewardTask task = rewardTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("task not found"));
        return ApiResponse.ok(RewardTaskSummary.from(task));
    }
}
