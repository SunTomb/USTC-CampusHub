package com.campushub.shop;

import com.campushub.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-items")
public class ServiceItemController {

    private final ShopService shopService;

    public ServiceItemController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ApiResponse<List<ServiceItemSummary>> listItems() {
        return ApiResponse.ok(shopService.listPublishedItems());
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceItemSummary> getItem(@PathVariable Long id) {
        return ApiResponse.ok(shopService.getItem(id));
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<ServiceItemSummary>> listShopItems(@PathVariable Long shopId, @RequestParam(defaultValue = "false") boolean includeAll) {
        return ApiResponse.ok(shopService.listShopItems(shopId, includeAll));
    }

    @PostMapping("/shop/{shopId}")
    public ApiResponse<ServiceItemSummary> createItem(@PathVariable Long shopId, @RequestParam Long ownerId, @Valid @RequestBody CreateServiceItemRequest request) {
        return ApiResponse.ok(shopService.createItem(shopId, ownerId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceItemSummary> updateItem(@PathVariable Long id, @RequestParam Long ownerId, @Valid @RequestBody UpdateServiceItemRequest request) {
        return ApiResponse.ok(shopService.updateItem(id, ownerId, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ServiceItemSummary> publishItem(@PathVariable Long id, @RequestBody ShopActionRequest request) {
        return ApiResponse.ok(shopService.publishItem(id, request.userId()));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<ServiceItemSummary> pauseItem(@PathVariable Long id, @RequestBody ShopActionRequest request) {
        return ApiResponse.ok(shopService.pauseItem(id, request.userId()));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<ServiceItemSummary> offShelfItem(@PathVariable Long id, @RequestBody ShopActionRequest request) {
        return ApiResponse.ok(shopService.offShelfItem(id, request.userId()));
    }

    @PostMapping("/{id}/orders")
    public ApiResponse<ServiceOrderSummary> createOrder(@PathVariable Long id, @RequestParam Long customerId, @Valid @RequestBody CreateServiceOrderRequest request) {
        return ApiResponse.ok(shopService.createOrder(id, customerId, request));
    }
}
