package com.campushub.shop;

import com.campushub.auth.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    public ShopController(ShopService shopService, CurrentUserService currentUserService) {
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ShopSummary>> listShops() {
        return ApiResponse.ok(shopService.listApprovedShops());
    }

    @PostMapping
    public ApiResponse<ShopDetailSummary> createShop(@RequestParam(required = false) Long ownerId, @Valid @RequestBody CreateShopRequest request) {
        Long effectiveOwnerId = ownerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(ownerId);
        return ApiResponse.ok(shopService.createShop(effectiveOwnerId, request));
    }

    @GetMapping("/mine")
    public ApiResponse<ShopDetailSummary> getMyShop(@RequestParam(required = false) Long ownerId) {
        Long effectiveOwnerId = ownerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(ownerId);
        return ApiResponse.ok(shopService.getMyShop(effectiveOwnerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShopDetailSummary> getShop(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        Long effectiveViewerId = currentUserService.optionalUserId().orElse(viewerId);
        return ApiResponse.ok(shopService.getShop(id, effectiveViewerId));
    }

    @PutMapping("/{id}")
    public ApiResponse<ShopDetailSummary> updateShop(@PathVariable Long id, @RequestParam(required = false) Long ownerId, @Valid @RequestBody UpdateShopRequest request) {
        Long effectiveOwnerId = ownerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(ownerId);
        return ApiResponse.ok(shopService.updateShop(id, effectiveOwnerId, request));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<ShopDetailSummary> pauseShop(@PathVariable Long id, @RequestBody(required = false) ShopActionRequest request) {
        return ApiResponse.ok(shopService.pauseShop(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<ShopDetailSummary> resumeShop(@PathVariable Long id, @RequestBody(required = false) ShopActionRequest request) {
        return ApiResponse.ok(shopService.resumeShop(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ShopDetailSummary> closeShop(@PathVariable Long id, @RequestBody(required = false) ShopActionRequest request) {
        return ApiResponse.ok(shopService.closeShop(id, currentUserService.requireUserId()));
    }

    @GetMapping("/{id}/orders")
    public ApiResponse<List<ServiceOrderSummary>> listShopOrders(@PathVariable Long id, @RequestParam(required = false) Long ownerId) {
        Long effectiveOwnerId = ownerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(ownerId);
        return ApiResponse.ok(shopService.listShopOrders(id, effectiveOwnerId));
    }
}
