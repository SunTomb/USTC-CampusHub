package com.campushub.shop;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-items")
public class ServiceItemController {

    private final ServiceItemRepository serviceItemRepository;

    public ServiceItemController(ServiceItemRepository serviceItemRepository) {
        this.serviceItemRepository = serviceItemRepository;
    }

    @GetMapping
    public ApiResponse<List<ServiceItemSummary>> listItems() {
        List<ServiceItemSummary> items = serviceItemRepository.findByStatusOrderByCreatedAtDesc("AVAILABLE").stream()
                .map(ServiceItemSummary::from)
                .toList();
        return ApiResponse.ok(items);
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceItemSummary> getItem(@PathVariable Long id) {
        ServiceItem item = serviceItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("service item not found"));
        return ApiResponse.ok(ServiceItemSummary.from(item));
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<ServiceItemSummary>> listShopItems(@PathVariable Long shopId) {
        List<ServiceItemSummary> items = serviceItemRepository
                .findByShopIdAndStatusOrderByCreatedAtDesc(shopId, "AVAILABLE").stream()
                .map(ServiceItemSummary::from)
                .toList();
        return ApiResponse.ok(items);
    }
}
