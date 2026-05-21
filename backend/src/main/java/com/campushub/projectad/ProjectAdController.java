package com.campushub.projectad;

import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-ads")
public class ProjectAdController {

    private final ProjectAdService projectAdService;

    public ProjectAdController(ProjectAdService projectAdService) {
        this.projectAdService = projectAdService;
    }

    @GetMapping
    public ApiResponse<List<ProjectAdSummary>> listProjectAds(
            @RequestParam(required = false) String adType,
            @RequestParam(required = false) String campusZone,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean featured) {
        return ApiResponse.ok(projectAdService.listPublic(adType, campusZone, keyword, featured));
    }

    @GetMapping("/featured")
    public ApiResponse<List<ProjectAdSummary>> featuredProjectAds() {
        return ApiResponse.ok(projectAdService.listFeatured());
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<ProjectAdSummary>> userProjectAds(@PathVariable Long userId) {
        return ApiResponse.ok(projectAdService.listByPublisher(userId));
    }

    @PostMapping
    public ApiResponse<ProjectAdSummary> createProjectAd(@RequestParam Long publisherId, @Valid @RequestBody ProjectAdRequest request) {
        return ApiResponse.ok(projectAdService.create(publisherId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectAdDetailSummary> getProjectAd(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        return ApiResponse.ok(projectAdService.getDetail(id, viewerId));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectAdSummary> updateProjectAd(
            @PathVariable Long id,
            @RequestParam Long publisherId,
            @Valid @RequestBody ProjectAdRequest request) {
        return ApiResponse.ok(projectAdService.update(id, publisherId, request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ProjectAdSummary> submitProjectAd(@PathVariable Long id, @RequestParam Long publisherId) {
        return ApiResponse.ok(projectAdService.submit(id, publisherId));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ProjectAdSummary> closeProjectAd(@PathVariable Long id, @RequestParam Long publisherId) {
        return ApiResponse.ok(projectAdService.close(id, publisherId));
    }
}
