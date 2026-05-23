package com.campushub.goods;

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
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;
    private final CurrentUserService currentUserService;

    public GoodsController(GoodsService goodsService, CurrentUserService currentUserService) {
        this.goodsService = goodsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<GoodsSummary>> listGoods() {
        return ApiResponse.ok(goodsService.listPublished());
    }

    @PostMapping
    public ApiResponse<GoodsDetailSummary> publish(@Valid @RequestBody CreateGoodsRequest request) {
        return ApiResponse.ok(goodsService.publish(currentUserService.requireUserId(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsDetailSummary> getGoods(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        Long effectiveViewerId = currentUserService.optionalUserId().orElse(viewerId);
        return ApiResponse.ok(goodsService.getDetail(id, effectiveViewerId));
    }

    @PutMapping("/{id}")
    public ApiResponse<GoodsDetailSummary> update(@PathVariable Long id, @Valid @RequestBody UpdateGoodsRequest request) {
        return ApiResponse.ok(goodsService.update(id, currentUserService.requireUserId(), request));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<GoodsDetailSummary> offShelf(@PathVariable Long id, @RequestBody(required = false) GoodsActionRequest request) {
        return ApiResponse.ok(goodsService.offShelf(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/intents")
    public ApiResponse<GoodsIntentSummary> createIntent(@PathVariable Long id, @Valid @RequestBody GoodsIntentRequest request) {
        return ApiResponse.ok(goodsService.createIntent(id, currentUserService.requireUserId(), request));
    }

    @GetMapping("/{id}/intents")
    public ApiResponse<List<GoodsIntentSummary>> listIntents(@PathVariable Long id, @RequestParam(required = false) Long sellerId) {
        Long effectiveSellerId = sellerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(sellerId);
        return ApiResponse.ok(goodsService.listIntents(id, effectiveSellerId));
    }

    @PostMapping("/{id}/mark-sold")
    public ApiResponse<GoodsDetailSummary> markSold(@PathVariable Long id, @RequestBody(required = false) GoodsActionRequest request) {
        Long buyerId = request == null ? null : request.buyerId();
        return ApiResponse.ok(goodsService.markSold(id, currentUserService.requireUserId(), buyerId));
    }

    @PostMapping("/{id}/orders/escrow")
    public ApiResponse<GoodsOrderSummary> createOnlineEscrowOrder(@PathVariable Long id, @RequestParam(required = false) Long buyerId) {
        Long effectiveBuyerId = buyerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(buyerId);
        return ApiResponse.ok(goodsService.createOnlineEscrowOrder(id, effectiveBuyerId));
    }

    @PostMapping("/orders/{orderId}/escrow/freeze")
    public ApiResponse<GoodsOrderSummary> freezeEscrow(@PathVariable Long orderId, @RequestParam(required = false) Long buyerId) {
        Long effectiveBuyerId = buyerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(buyerId);
        return ApiResponse.ok(goodsService.freezeGoodsEscrow(orderId, effectiveBuyerId));
    }

    @PostMapping("/orders/{orderId}/escrow/confirm")
    public ApiResponse<GoodsOrderSummary> confirmEscrow(@PathVariable Long orderId, @RequestParam(required = false) Long buyerId) {
        Long effectiveBuyerId = buyerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(buyerId);
        return ApiResponse.ok(goodsService.confirmGoodsEscrow(orderId, effectiveBuyerId));
    }

    @PostMapping("/orders/{orderId}/escrow/cancel")
    public ApiResponse<GoodsOrderSummary> cancelEscrow(@PathVariable Long orderId, @RequestParam(required = false) Long buyerId, @RequestParam(defaultValue = "买家取消线上托管交易") String reason) {
        Long effectiveBuyerId = buyerId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(buyerId);
        return ApiResponse.ok(goodsService.cancelGoodsEscrow(orderId, effectiveBuyerId, reason));
    }

    @PostMapping("/orders/{orderId}/escrow/dispute")
    public ApiResponse<GoodsOrderSummary> disputeEscrow(@PathVariable Long orderId, @RequestParam(required = false) Long userId, @RequestParam String reason) {
        Long effectiveUserId = userId == null ? currentUserService.requireUserId() : currentUserService.requireSameUser(userId);
        return ApiResponse.ok(goodsService.disputeGoodsEscrow(orderId, effectiveUserId, reason));
    }
}
