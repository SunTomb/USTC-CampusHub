package com.campushub.projectad;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-ads")
public class ProjectAdController {

    private final ProjectAdRepository projectAdRepository;

    public ProjectAdController(ProjectAdRepository projectAdRepository) {
        this.projectAdRepository = projectAdRepository;
    }

    @GetMapping
    public ApiResponse<List<ProjectAdSummary>> listProjectAds() {
        List<ProjectAdSummary> projectAds = projectAdRepository.findByStatusOrderByCreatedAtDesc("APPROVED").stream()
                .map(ProjectAdSummary::from)
                .toList();
        return ApiResponse.ok(projectAds);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectAdSummary> getProjectAd(@PathVariable Long id) {
        ProjectAd projectAd = projectAdRepository.findById(id)
                .orElseThrow(() -> new BusinessException("project ad not found"));
        return ApiResponse.ok(ProjectAdSummary.from(projectAd));
    }
}
