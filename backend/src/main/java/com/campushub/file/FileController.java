package com.campushub.file;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileResourceRepository fileResourceRepository;
    private final FileBindingRepository fileBindingRepository;
    private final FileUploadService fileUploadService;
    private final CurrentUserService currentUserService;
    private final FileTargetAuthorizationService fileTargetAuthorizationService;

    public FileController(
            FileResourceRepository fileResourceRepository,
            FileBindingRepository fileBindingRepository,
            FileUploadService fileUploadService,
            CurrentUserService currentUserService,
            FileTargetAuthorizationService fileTargetAuthorizationService) {
        this.fileResourceRepository = fileResourceRepository;
        this.fileBindingRepository = fileBindingRepository;
        this.fileUploadService = fileUploadService;
        this.currentUserService = currentUserService;
        this.fileTargetAuthorizationService = fileTargetAuthorizationService;
    }

    @GetMapping
    public ApiResponse<List<FileResourceSummary>> listFiles() {
        currentUserService.requireAdminId();
        List<FileResourceSummary> files = fileResourceRepository.findAll().stream()
                .map(FileResourceSummary::from)
                .toList();
        return ApiResponse.ok(files);
    }

    @GetMapping("/{id}")
    public ApiResponse<FileResourceSummary> getFile(@PathVariable Long id) {
        FileResource file = fileResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("file not found"));
        if (!file.getUploader().getId().equals(currentUserService.requireUserId()) && !currentUserService.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        }
        return ApiResponse.ok(FileResourceSummary.from(file));
    }

    @GetMapping("/{targetType}/{targetId}/bindings")
    public ApiResponse<List<FileBindingSummary>> listBindings(@PathVariable String targetType, @PathVariable Long targetId) {
        fileTargetAuthorizationService.requireCanReadBindings(
                targetType, targetId, currentUserService.requireUserId(), currentUserService.isAdmin());
        List<FileBindingSummary> bindings = fileBindingRepository
                .findByTargetTypeAndTargetIdOrderBySortOrderAsc(targetType, targetId)
                .stream()
                .map(FileBindingSummary::from)
                .toList();
        return ApiResponse.ok(bindings);
    }

    @PostMapping("/bindings")
    public ApiResponse<FileBindingSummary> bind(@Valid @RequestBody BindFileRequest request) {
        return ApiResponse.ok(fileUploadService.bindExisting(request, currentUserService.requireUserId(), currentUserService.isAdmin()));
    }
}
