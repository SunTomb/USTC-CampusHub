package com.campushub.projectad;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAdRepository extends JpaRepository<ProjectAd, Long> {

    List<ProjectAd> findByStatusOrderByCreatedAtDesc(String status);
}
