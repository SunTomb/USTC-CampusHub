package com.campushub.goods;

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

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public ApiResponse<List<GoodsSummary>> listGoods() {
        return ApiResponse.ok(goodsService.listPublished());
    }

    @PostMapping
    public ApiResponse<GoodsDetailSummary> publish(@RequestParam Long sellerId, @Valid @RequestBody CreateGoodsRequest request) {
        return ApiResponse.ok(goodsService.publish(sellerId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsDetailSummary> getGoods(@PathVariable Long id, @RequestParam(required = false) Long viewerId) {
        return ApiResponse.ok(goodsService.getDetail(id, viewerId));
    }

    @PutMapping("/{id}")
    public ApiResponse<GoodsDetailSummary> update(@PathVariable Long id, @RequestParam Long sellerId, @Valid @RequestBody UpdateGoodsRequest request) {
        return ApiResponse.ok(goodsService.update(id, sellerId, request));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<GoodsDetailSummary> offShelf(@PathVariable Long id, @RequestBody GoodsActionRequest request) {
        return ApiResponse.ok(goodsService.offShelf(id, request.userId()));
    }

    @PostMapping("/{id}/intents")
    public ApiResponse<GoodsIntentSummary> createIntent(@PathVariable Long id, @RequestParam Long buyerId, @Valid @RequestBody GoodsIntentRequest request) {
        return ApiResponse.ok(goodsService.createIntent(id, buyerId, request));
    }

    @GetMapping("/{id}/intents")
    public ApiResponse<List<GoodsIntentSummary>> listIntents(@PathVariable Long id, @RequestParam Long sellerId) {
        return ApiResponse.ok(goodsService.listIntents(id, sellerId));
    }

    @PostMapping("/{id}/mark-sold")
    public ApiResponse<GoodsDetailSummary> markSold(@PathVariable Long id, @RequestBody GoodsActionRequest request) {
        return ApiResponse.ok(goodsService.markSold(id, request.userId(), request.buyerId()));
    }

    @PostMapping("/{id}/orders/escrow")
    public ApiResponse<GoodsOrderSummary> createOnlineEscrowOrder(@PathVariable Long id, @RequestParam Long buyerId) {
        return ApiResponse.ok(goodsService.createOnlineEscrowOrder(id, buyerId));
    }

    @PostMapping("/orders/{orderId}/escrow/freeze")
    public ApiResponse<GoodsOrderSummary> freezeEscrow(@PathVariable Long orderId, @RequestParam Long buyerId) {
        return ApiResponse.ok(goodsService.freezeGoodsEscrow(orderId, buyerId));
    }

    @PostMapping("/orders/{orderId}/escrow/confirm")
    public ApiResponse<GoodsOrderSummary> confirmEscrow(@PathVariable Long orderId, @RequestParam Long buyerId) {
        return ApiResponse.ok(goodsService.confirmGoodsEscrow(orderId, buyerId));
    }

    @PostMapping("/orders/{orderId}/escrow/cancel")
    public ApiResponse<GoodsOrderSummary> cancelEscrow(@PathVariable Long orderId, @RequestParam Long buyerId, @RequestParam(defaultValue = "买家取消线上托管交易") String reason) {
        return ApiResponse.ok(goodsService.cancelGoodsEscrow(orderId, buyerId, reason));
    }

    @PostMapping("/orders/{orderId}/escrow/dispute")
    public ApiResponse<GoodsOrderSummary> disputeEscrow(@PathVariable Long orderId, @RequestParam Long userId, @RequestParam String reason) {
        return ApiResponse.ok(goodsService.disputeGoodsEscrow(orderId, userId, reason));
    }
}
