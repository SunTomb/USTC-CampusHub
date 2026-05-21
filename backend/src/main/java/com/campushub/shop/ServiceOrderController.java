package com.campushub.shop;

import com.campushub.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final ShopService shopService;

    public ServiceOrderController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ApiResponse<List<ServiceOrderSummary>> listOrders() {
        return ApiResponse.ok(shopService.listAllOrders());
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<ServiceOrderSummary>> listCustomerOrders(@PathVariable Long customerId) {
        return ApiResponse.ok(shopService.listCustomerOrders(customerId));
    }

    @GetMapping("/provider/{providerId}")
    public ApiResponse<List<ServiceOrderSummary>> listProviderOrders(@PathVariable Long providerId) {
        return ApiResponse.ok(shopService.listProviderOrders(providerId));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ServiceOrderSummary> acceptOrder(@PathVariable Long id, @RequestBody ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.acceptOrder(id, request.actorId()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ServiceOrderSummary> rejectOrder(@PathVariable Long id, @RequestBody ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.rejectOrder(id, request.actorId(), request.cancelReason()));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<ServiceOrderSummary> startOrder(@PathVariable Long id, @RequestBody ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.startOrder(id, request.actorId()));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ServiceOrderSummary> completeOrder(@PathVariable Long id, @RequestBody ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.completeOrder(id, request.actorId()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ServiceOrderSummary> cancelOrder(@PathVariable Long id, @RequestBody ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.cancelOrder(id, request.actorId(), request.cancelReason()));
    }
}
