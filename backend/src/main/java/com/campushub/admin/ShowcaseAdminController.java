package com.campushub.admin;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.identity.IdentityService;
import com.campushub.identity.RoleApplicationSummary;
import com.campushub.projectad.ProjectAdReviewRequest;
import com.campushub.projectad.ProjectAdService;
import com.campushub.projectad.ProjectAdSummary;
import com.campushub.shop.ServiceItem;
import com.campushub.shop.ServiceItemRepository;
import com.campushub.shop.ServiceItemSummary;
import com.campushub.shop.ServiceOrderRepository;
import com.campushub.shop.ServiceOrderSummary;
import com.campushub.shop.Shop;
import com.campushub.shop.ShopRepository;
import com.campushub.shop.ShopSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/showcase")
public class ShowcaseAdminController {

    private final CurrentUserService currentUserService;
    private final ProjectAdService projectAdService;
    private final ShopRepository shopRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final IdentityService identityService;

    public ShowcaseAdminController(
            CurrentUserService currentUserService,
            ProjectAdService projectAdService,
            ShopRepository shopRepository,
            ServiceItemRepository serviceItemRepository,
            ServiceOrderRepository serviceOrderRepository,
            IdentityService identityService) {
        this.currentUserService = currentUserService;
        this.projectAdService = projectAdService;
        this.shopRepository = shopRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.identityService = identityService;
    }

    @GetMapping("/project-ads")
    public ApiResponse<List<ProjectAdSummary>> listProjectAds(@RequestParam(required = false) String status) {
        currentUserService.requireShowcaseAdminId();
        return ApiResponse.ok(projectAdService.listForAdmin(status));
    }

    @PostMapping("/project-ads/{id}/approve")
    public ApiResponse<ProjectAdSummary> approveProjectAd(@PathVariable Long id, @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.approve(id, currentUserService.requireShowcaseAdminId(), request));
    }

    @PostMapping("/project-ads/{id}/reject")
    public ApiResponse<ProjectAdSummary> rejectProjectAd(@PathVariable Long id, @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.reject(id, currentUserService.requireShowcaseAdminId(), request));
    }

    @PostMapping("/project-ads/{id}/block")
    public ApiResponse<ProjectAdSummary> blockProjectAd(@PathVariable Long id, @RequestBody(required = false) ProjectAdReviewRequest request) {
        return ApiResponse.ok(projectAdService.block(id, currentUserService.requireShowcaseAdminId(), request));
    }

    @GetMapping("/shops")
    public ApiResponse<List<ShopSummary>> listShops() {
        currentUserService.requireShowcaseAdminId();
        return ApiResponse.ok(shopRepository.findAll().stream().map(ShopSummary::from).toList());
    }

    @PostMapping("/shops/{shopId}/pause")
    public ApiResponse<ShopSummary> pauseShop(@PathVariable Long shopId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireShowcaseAdminId();
        Shop shop = shopRepository.findById(shopId).orElseThrow();
        shop.pause();
        return ApiResponse.ok(ShopSummary.from(shopRepository.save(shop)));
    }

    @PostMapping("/shops/{shopId}/resume")
    public ApiResponse<ShopSummary> resumeShop(@PathVariable Long shopId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireShowcaseAdminId();
        Shop shop = shopRepository.findById(shopId).orElseThrow();
        shop.resume();
        return ApiResponse.ok(ShopSummary.from(shopRepository.save(shop)));
    }

    @PostMapping("/shops/{shopId}/block")
    public ApiResponse<ShopSummary> blockShop(@PathVariable Long shopId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireShowcaseAdminId();
        Shop shop = shopRepository.findById(shopId).orElseThrow();
        shop.block();
        return ApiResponse.ok(ShopSummary.from(shopRepository.save(shop)));
    }

    @GetMapping("/service-items")
    public ApiResponse<List<ServiceItemSummary>> listServiceItems() {
        currentUserService.requireShowcaseAdminId();
        return ApiResponse.ok(serviceItemRepository.findAll().stream().map(ServiceItemSummary::from).toList());
    }

    @PostMapping("/service-items/{itemId}/off-shelf")
    public ApiResponse<ServiceItemSummary> offShelfServiceItem(@PathVariable Long itemId, @RequestBody(required = false) AdminActionRequest request) {
        currentUserService.requireShowcaseAdminId();
        ServiceItem item = serviceItemRepository.findById(itemId).orElseThrow();
        item.offShelf();
        return ApiResponse.ok(ServiceItemSummary.from(serviceItemRepository.save(item)));
    }

    @GetMapping("/shop-orders")
    public ApiResponse<List<ServiceOrderSummary>> listShopOrders() {
        currentUserService.requireShowcaseAdminId();
        return ApiResponse.ok(serviceOrderRepository.findAll().stream().map(ServiceOrderSummary::from).toList());
    }

    @GetMapping("/shop-merchant-applications")
    public ApiResponse<List<RoleApplicationSummary>> listShopMerchantApplications() {
        currentUserService.requireShowcaseAdminId();
        return ApiResponse.ok(identityService.listPendingShopMerchantApplications());
    }

    @PostMapping("/shop-merchant-applications/{applicationId}/approve")
    public ApiResponse<RoleApplicationSummary> approveShopMerchantApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(identityService.approve(applicationId, currentUserService.requireShowcaseAdminId()));
    }

    @PostMapping("/shop-merchant-applications/{applicationId}/reject")
    public ApiResponse<RoleApplicationSummary> rejectShopMerchantApplication(@PathVariable Long applicationId) {
        return ApiResponse.ok(identityService.reject(applicationId, currentUserService.requireShowcaseAdminId()));
    }
}
