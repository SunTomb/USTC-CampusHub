package com.campushub.shop;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopRepository shopRepository;

    public ShopController(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @GetMapping
    public ApiResponse<List<ShopSummary>> listShops() {
        List<ShopSummary> shops = shopRepository.findByStatusOrderByRatingDesc("APPROVED").stream()
                .map(ShopSummary::from)
                .toList();
        return ApiResponse.ok(shops);
    }

    @GetMapping("/{id}")
    public ApiResponse<ShopSummary> getShop(@PathVariable Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new BusinessException("shop not found"));
        return ApiResponse.ok(ShopSummary.from(shop));
    }
}
