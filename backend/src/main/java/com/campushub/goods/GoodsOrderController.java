package com.campushub.goods;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods/orders")
public class GoodsOrderController {

    private final GoodsOrderRepository goodsOrderRepository;
    private final CurrentUserService currentUserService;

    public GoodsOrderController(GoodsOrderRepository goodsOrderRepository, CurrentUserService currentUserService) {
        this.goodsOrderRepository = goodsOrderRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<GoodsOrderSummary>> listOrders() {
        currentUserService.requireAdminId();
        List<GoodsOrderSummary> orders = goodsOrderRepository.findAll().stream()
                .map(GoodsOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsOrderSummary> getOrder(@PathVariable Long id) {
        GoodsOrder order = goodsOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("order not found"));
        Long currentUserId = currentUserService.requireUserId();
        if (!currentUserService.isAdmin()
                && !order.getBuyer().getId().equals(currentUserId)
                && !order.getSeller().getId().equals(currentUserId)) {
            throw new BusinessException("无权查看该订单");
        }
        return ApiResponse.ok(GoodsOrderSummary.from(order));
    }

    @GetMapping("/buyer/{buyerId}")
    public ApiResponse<List<GoodsOrderSummary>> listBuyerOrders(@PathVariable Long buyerId) {
        Long effectiveBuyerId = currentUserService.requireSameUser(buyerId);
        List<GoodsOrderSummary> orders = goodsOrderRepository.findByBuyerIdOrderByCreatedAtDesc(effectiveBuyerId).stream()
                .map(GoodsOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }

    @GetMapping("/seller/{sellerId}")
    public ApiResponse<List<GoodsOrderSummary>> listSellerOrders(@PathVariable Long sellerId) {
        Long effectiveSellerId = currentUserService.requireSameUser(sellerId);
        List<GoodsOrderSummary> orders = goodsOrderRepository.findBySellerIdOrderByCreatedAtDesc(effectiveSellerId).stream()
                .map(GoodsOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }
}
