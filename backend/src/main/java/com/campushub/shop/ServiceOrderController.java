package com.campushub.shop;

import com.campushub.auth.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    public ServiceOrderController(ShopService shopService, CurrentUserService currentUserService) {
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ServiceOrderSummary>> listOrders() {
        return ApiResponse.ok(shopService.listAllOrders());
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<ServiceOrderSummary>> listCustomerOrders(@PathVariable Long customerId) {
        return ApiResponse.ok(shopService.listCustomerOrders(currentUserService.requireSameUser(customerId)));
    }

    @GetMapping("/provider/{providerId}")
    public ApiResponse<List<ServiceOrderSummary>> listProviderOrders(@PathVariable Long providerId) {
        return ApiResponse.ok(shopService.listProviderOrders(currentUserService.requireSameUser(providerId)));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ServiceOrderSummary> acceptOrder(@PathVariable Long id, @RequestBody(required = false) ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.acceptOrder(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ServiceOrderSummary> rejectOrder(@PathVariable Long id, @RequestBody(required = false) ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.rejectOrder(id, currentUserService.requireUserId(), request == null ? null : request.cancelReason()));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<ServiceOrderSummary> startOrder(@PathVariable Long id, @RequestBody(required = false) ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.startOrder(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ServiceOrderSummary> completeOrder(@PathVariable Long id, @RequestBody(required = false) ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.completeOrder(id, currentUserService.requireUserId()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ServiceOrderSummary> cancelOrder(@PathVariable Long id, @RequestBody(required = false) ServiceOrderActionRequest request) {
        return ApiResponse.ok(shopService.cancelOrder(id, currentUserService.requireUserId(), request == null ? null : request.cancelReason()));
    }
}
