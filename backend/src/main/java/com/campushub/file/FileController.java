package com.campushub.file;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileResourceRepository fileResourceRepository;
    private final FileBindingRepository fileBindingRepository;

    public FileController(FileResourceRepository fileResourceRepository, FileBindingRepository fileBindingRepository) {
        this.fileResourceRepository = fileResourceRepository;
        this.fileBindingRepository = fileBindingRepository;
    }

    @GetMapping
    public ApiResponse<List<FileResourceSummary>> listFiles() {
        List<FileResourceSummary> files = fileResourceRepository.findAll().stream()
                .map(FileResourceSummary::from)
                .toList();
        return ApiResponse.ok(files);
    }

    @GetMapping("/{id}")
    public ApiResponse<FileResourceSummary> getFile(@PathVariable Long id) {
        FileResource file = fileResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("file not found"));
        return ApiResponse.ok(FileResourceSummary.from(file));
    }

    @GetMapping("/{targetType}/{targetId}/bindings")
    public ApiResponse<List<FileBindingSummary>> listBindings(@PathVariable String targetType, @PathVariable Long targetId) {
        List<FileBindingSummary> bindings = fileBindingRepository
                .findByTargetTypeAndTargetIdOrderBySortOrderAsc(targetType, targetId)
                .stream()
                .map(FileBindingSummary::from)
                .toList();
        return ApiResponse.ok(bindings);
    }
}
