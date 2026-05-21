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
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ApiResponse<List<ShopSummary>> listShops() {
        return ApiResponse.ok(shopService.listApprovedShops());
    }

    @PostMapping
    public ApiResponse<ShopDetailSummary> createShop(@RequestParam Long ownerId, @Valid @RequestBody CreateShopRequest request) {
        return ApiResponse.ok(shopService.createShop(ownerId, request));
    }

    @GetMapping("/mine")
    public ApiResponse<ShopDetailSummary> getMyShop(@RequestParam Long ownerId) {
        return ApiResponse.ok(shopService.getMyShop(ownerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShopDetailSummary> getShop(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        return ApiResponse.ok(shopService.getShop(id, viewerId));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShopDetailSummary> updateShop(@PathVariable Long id, @RequestParam Long ownerId, @Valid @RequestBody UpdateShopRequest request) {
        return ApiResponse.ok(shopService.updateShop(id, ownerId, request));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<ShopDetailSummary> pauseShop(@PathVariable Long id, @RequestBody ShopActionRequest request) {
        return ApiResponse.ok(shopService.pauseShop(id, request.userId()));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<ShopDetailSummary> resumeShop(@PathVariable Long id, @RequestBody ShopActionRequest request) {
        return ApiResponse.ok(shopService.resumeShop(id, request.userId()));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ShopDetailSummary> closeShop(@PathVariable Long id, @RequestBody ShopActionRequest request) {
        return ApiResponse.ok(shopService.closeShop(id, request.userId()));
    }

    @GetMapping("/{id}/orders")
    public ApiResponse<List<ServiceOrderSummary>> listShopOrders(@PathVariable Long id, @RequestParam Long ownerId) {
        return ApiResponse.ok(shopService.listShopOrders(id, ownerId));
    }
}
