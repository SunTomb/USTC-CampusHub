package com.campushub.shop;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final ServiceOrderRepository serviceOrderRepository;

    public ServiceOrderController(ServiceOrderRepository serviceOrderRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
    }

    @GetMapping
    public ApiResponse<List<ServiceOrderSummary>> listOrders() {
        List<ServiceOrderSummary> orders = serviceOrderRepository.findAll().stream()
                .map(ServiceOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceOrderSummary> getOrder(@PathVariable Long id) {
        ServiceOrder order = serviceOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("service order not found"));
        return ApiResponse.ok(ServiceOrderSummary.from(order));
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<ServiceOrderSummary>> listCustomerOrders(@PathVariable Long customerId) {
        List<ServiceOrderSummary> orders = serviceOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(ServiceOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }

    @GetMapping("/provider/{providerId}")
    public ApiResponse<List<ServiceOrderSummary>> listProviderOrders(@PathVariable Long providerId) {
        List<ServiceOrderSummary> orders = serviceOrderRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(ServiceOrderSummary::from)
                .toList();
        return ApiResponse.ok(orders);
    }
}
