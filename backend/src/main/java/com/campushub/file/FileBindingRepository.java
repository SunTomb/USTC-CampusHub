package com.campushub.file;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileBindingRepository extends JpaRepository<FileBinding, Long> {

    List<FileBinding> findByTargetTypeAndTargetIdOrderBySortOrderAsc(String targetType, Long targetId);
}
