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
@RequestMapping("/api/service-items")
public class ServiceItemController {

    private final ShopService shopService;
    private final CurrentUserService currentUserService;

    public ServiceItemController(ShopService shopService, CurrentUserService currentUserService) {
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ServiceItemSummary>> listItems() {
        return ApiResponse.ok(shopService.listPublishedItems());
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceItemSummary> getItem(@PathVariable Long id) {
        return ApiResponse.ok(shopService.getItem(id,currentUserService.optionalUserId().orElse(null)));
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<ServiceItemSummary>> listShopItems(@PathVariable Long shopId, @RequestParam(defaultValue = "false") boolean includeAll) {
        var detail=shopService.getShop(shopId,currentUserService.optionalUserId().orElse(null));
        if(includeAll&&!detail.ownerId().equals(currentUserService.optionalUserId().orElse(null)))throw new com.campushub.common.BusinessException("只能查看自己店内的未上架服务");
        return ApiResponse.ok(detail.serviceItems().stream().filter(i->includeAll||"PUBLISHED".equals(i.status())).toList());
    }

    @PostMapping("/shop/{shopId}")
    public ApiResponse<ServiceItemSummary> createItem(@PathVariable Long shopId, @RequestParam(required = false) Long ownerId, @Valid @RequestBody CreateServiceItemRequest request) {
        Long effectiveOwnerId = ownerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(ownerId);
        return ApiResponse.ok(shopService.createItem(shopId, effectiveOwnerId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ServiceItemSummary> updateItem(@PathVariable Long id, @RequestParam(required = false) Long ownerId, @Valid @RequestBody UpdateServiceItemRequest request) {
        Long effectiveOwnerId = ownerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(ownerId);
        return ApiResponse.ok(shopService.updateItem(id, effectiveOwnerId, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ServiceItemSummary> publishItem(@PathVariable Long id, @RequestBody(required = false) ShopActionRequest request) {
        return ApiResponse.ok(shopService.publishItem(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<ServiceItemSummary> pauseItem(@PathVariable Long id, @RequestBody(required = false) ShopActionRequest request) {
        return ApiResponse.ok(shopService.pauseItem(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<ServiceItemSummary> offShelfItem(@PathVariable Long id, @RequestBody(required = false) ShopActionRequest request) {
        return ApiResponse.ok(shopService.offShelfItem(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/orders")
    public ApiResponse<ServiceOrderSummary> createOrder(@PathVariable Long id, @RequestParam(required = false) Long customerId, @Valid @RequestBody CreateServiceOrderRequest request) {
        Long effectiveCustomerId = customerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(customerId);
        return ApiResponse.ok(shopService.createOrder(id, effectiveCustomerId, request));
    }
}
