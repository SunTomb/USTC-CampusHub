package com.campushub.file;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileBindingRepository extends JpaRepository<FileBinding, Long> {

    @EntityGraph(attributePaths = {"file", "file.uploader"})
    List<FileBinding> findByTargetTypeAndTargetIdOrderBySortOrderAsc(String targetType, Long targetId);

    long countByTargetTypeAndTargetId(String targetType, Long targetId);
}
