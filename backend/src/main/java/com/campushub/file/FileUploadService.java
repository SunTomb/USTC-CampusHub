package com.campushub.file;

import com.campushub.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileUploadService {

    private final FileResourceRepository fileResourceRepository;
    private final FileBindingRepository fileBindingRepository;

    public FileUploadService(FileResourceRepository fileResourceRepository, FileBindingRepository fileBindingRepository) {
        this.fileResourceRepository = fileResourceRepository;
        this.fileBindingRepository = fileBindingRepository;
    }

    @Transactional
    public FileBindingSummary bindExisting(BindFileRequest request) {
        FileResource file = fileResourceRepository.findById(request.fileId())
                .orElseThrow(() -> new BusinessException("文件不存在"));
        long existingCount = fileBindingRepository.countByTargetTypeAndTargetId(request.targetType(), request.targetId());
        if ("GOODS".equals(request.targetType()) && existingCount >= 9) {
            throw new BusinessException("每个商品最多上传 9 张图片");
        }
        FileBinding binding = new FileBinding(file, request.targetType(), request.targetId(), request.usageType(), request.sortOrder());
        return FileBindingSummary.from(fileBindingRepository.save(binding));
    }
}
