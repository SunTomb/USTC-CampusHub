package com.campushub.goods;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = "parent")
    List<Category> findByEnabledTrueOrderBySortOrderAscIdAsc();
}
