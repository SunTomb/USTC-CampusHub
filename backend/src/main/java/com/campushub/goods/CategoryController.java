package com.campushub.goods;

import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ApiResponse<List<CategorySummary>> listCategories() {
        List<CategorySummary> categories = categoryRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream()
                .map(CategorySummary::from)
                .toList();
        return ApiResponse.ok(categories);
    }
}
