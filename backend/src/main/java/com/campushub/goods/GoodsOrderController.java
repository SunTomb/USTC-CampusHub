package com.campushub.goods;

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

    public GoodsOrderController(GoodsOrderRepository goodsOrderRepository) {
        this.goodsOrderRepository = goodsOrderRepository;
    }

    @GetMapping
    public ApiResponse<List<GoodsOrderSummary>> listOrders() {
        List<GoodsOrderSummary> orders = goodsOrderRepository.findAll().stream()
                .map(GoodsOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsOrderSummary> getOrder(@PathVariable Long id) {
        GoodsOrder order = goodsOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("order not found"));
        return ApiResponse.ok(GoodsOrderSummary.from(order));
    }

    @GetMapping("/buyer/{buyerId}")
    public ApiResponse<List<GoodsOrderSummary>> listBuyerOrders(@PathVariable Long buyerId) {
        List<GoodsOrderSummary> orders = goodsOrderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(GoodsOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }

    @GetMapping("/seller/{sellerId}")
    public ApiResponse<List<GoodsOrderSummary>> listSellerOrders(@PathVariable Long sellerId) {
        List<GoodsOrderSummary> orders = goodsOrderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(GoodsOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }
}
