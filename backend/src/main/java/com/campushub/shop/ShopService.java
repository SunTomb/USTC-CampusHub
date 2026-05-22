package com.campushub.shop;

import com.campushub.common.BusinessException;
import com.campushub.identity.RoleApplicationRepository;
import com.campushub.moderation.GovernanceService;
import com.campushub.notification.NotificationService;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final RoleApplicationRepository roleApplicationRepository;
    private final NotificationService notificationService;
    private final GovernanceService governanceService;

    public ShopService(
            ShopRepository shopRepository,
            ServiceItemRepository serviceItemRepository,
            ServiceOrderRepository serviceOrderRepository,
            UserRepository userRepository,
            RoleApplicationRepository roleApplicationRepository,
            NotificationService notificationService,
            GovernanceService governanceService) {
        this.shopRepository = shopRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.serviceOrderRepository = serviceOrderRepository;
        this.userRepository = userRepository;
        this.roleApplicationRepository = roleApplicationRepository;
        this.notificationService = notificationService;
        this.governanceService = governanceService;
    }

    @Transactional(readOnly = true)
    public List<ShopSummary> listApprovedShops() {
        return shopRepository.findByStatusOrderByRatingDesc("APPROVED").stream()
                .map(ShopSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopDetailSummary getShop(Long shopId, Long viewerId) {
        Shop shop = findShop(shopId);
        return detailFor(shop, viewerId, false);
    }

    @Transactional(readOnly = true)
    public ShopDetailSummary getMyShop(Long ownerId) {
        Shop shop = shopRepository.findByOwnerId(ownerId).orElseThrow(() -> new BusinessException("尚未创建店铺"));
        return detailFor(shop, ownerId, true);
    }

    @Transactional
    public ShopDetailSummary createShop(Long ownerId, CreateShopRequest request) {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new BusinessException("用户不存在"));
        governanceService.ensureCanPost(ownerId);
        ensureShopMerchant(ownerId);
        shopRepository.findByOwnerId(ownerId).ifPresent(existing -> {
            throw new BusinessException("已创建店铺");
        });
        Shop shop = shopRepository.save(new Shop(owner, request));
        return detailFor(shop, ownerId, true);
    }

    @Transactional
    public ShopDetailSummary updateShop(Long shopId, Long ownerId, UpdateShopRequest request) {
        Shop shop = findShop(shopId);
        ensureOwner(shop, ownerId);
        shop.update(request);
        return detailFor(shop, ownerId, true);
    }

    @Transactional
    public ShopDetailSummary pauseShop(Long shopId, Long ownerId) {
        Shop shop = findShop(shopId);
        ensureOwner(shop, ownerId);
        shop.pause();
        return detailFor(shop, ownerId, true);
    }

    @Transactional
    public ShopDetailSummary resumeShop(Long shopId, Long ownerId) {
        Shop shop = findShop(shopId);
        ensureOwner(shop, ownerId);
        shop.resume();
        return detailFor(shop, ownerId, true);
    }

    @Transactional
    public ShopDetailSummary closeShop(Long shopId, Long ownerId) {
        Shop shop = findShop(shopId);
        ensureOwner(shop, ownerId);
        shop.close();
        return detailFor(shop, ownerId, true);
    }

    @Transactional(readOnly = true)
    public List<ServiceItemSummary> listPublishedItems() {
        return serviceItemRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .map(ServiceItemSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceItemSummary> listShopItems(Long shopId, boolean includeAll) {
        List<ServiceItem> items = includeAll
                ? serviceItemRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                : serviceItemRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shopId, "PUBLISHED");
        return items.stream().map(ServiceItemSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceItemSummary getItem(Long itemId) {
        return ServiceItemSummary.from(findItem(itemId));
    }

    @Transactional
    public ServiceItemSummary createItem(Long shopId, Long ownerId, CreateServiceItemRequest request) {
        Shop shop = findShop(shopId);
        ensureOwner(shop, ownerId);
        governanceService.ensureCanPost(ownerId);
        ensureShopActive(shop);
        return ServiceItemSummary.from(serviceItemRepository.save(new ServiceItem(shop, request)));
    }

    @Transactional
    public ServiceItemSummary updateItem(Long itemId, Long ownerId, UpdateServiceItemRequest request) {
        ServiceItem item = findItem(itemId);
        ensureOwner(item.getShop(), ownerId);
        item.update(request);
        return ServiceItemSummary.from(item);
    }

    @Transactional
    public ServiceItemSummary publishItem(Long itemId, Long ownerId) {
        ServiceItem item = findItem(itemId);
        ensureOwner(item.getShop(), ownerId);
        ensureShopActive(item.getShop());
        item.publish();
        return ServiceItemSummary.from(item);
    }

    @Transactional
    public ServiceItemSummary pauseItem(Long itemId, Long ownerId) {
        ServiceItem item = findItem(itemId);
        ensureOwner(item.getShop(), ownerId);
        item.pause();
        return ServiceItemSummary.from(item);
    }

    @Transactional
    public ServiceItemSummary offShelfItem(Long itemId, Long ownerId) {
        ServiceItem item = findItem(itemId);
        ensureOwner(item.getShop(), ownerId);
        item.offShelf();
        return ServiceItemSummary.from(item);
    }

    @Transactional
    public ServiceOrderSummary createOrder(Long itemId, Long customerId, CreateServiceOrderRequest request) {
        ServiceItem item = findItem(itemId);
        if (!"PUBLISHED".equals(item.getStatus()) || !"APPROVED".equals(item.getShop().getStatus())) {
            throw new BusinessException("服务暂不可预约");
        }
        if (item.getShop().getOwner().getId().equals(customerId)) {
            throw new BusinessException("不能预约自己的服务");
        }
        User customer = userRepository.findById(customerId).orElseThrow(() -> new BusinessException("用户不存在"));
        ServiceOrder order = serviceOrderRepository.save(new ServiceOrder(item, customer, request, buildContactSnapshot(item.getShop().getOwner())));
        notificationService.notify(item.getShop().getOwner(), "收到服务预约", customer.getNickname() + " 预约了你的服务：" + item.getTitle(), "SERVICE_ORDER", order.getId());
        return ServiceOrderSummary.from(order);
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderSummary> listShopOrders(Long shopId, Long ownerId) {
        Shop shop = findShop(shopId);
        ensureOwner(shop, ownerId);
        return serviceOrderRepository.findByServiceItemShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(ServiceOrderSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderSummary> listCustomerOrders(Long customerId) {
        return serviceOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(ServiceOrderSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderSummary> listProviderOrders(Long providerId) {
        return serviceOrderRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(ServiceOrderSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderSummary> listAllOrders() {
        return serviceOrderRepository.findAll().stream()
                .map(ServiceOrderSummary::from)
                .toList();
    }

    @Transactional
    public ServiceOrderSummary acceptOrder(Long orderId, Long actorId) {
        ServiceOrder order = findOrder(orderId);
        ensureProvider(order, actorId);
        governanceService.ensureCanProvideService(actorId);
        order.accept();
        notificationService.notify(order.getCustomer(), "服务预约已接受", order.getProvider().getNickname() + " 已接受你的预约", "SERVICE_ORDER", order.getId());
        return ServiceOrderSummary.from(order);
    }

    @Transactional
    public ServiceOrderSummary rejectOrder(Long orderId, Long actorId, String reason) {
        ServiceOrder order = findOrder(orderId);
        ensureProvider(order, actorId);
        order.reject(reason);
        notificationService.notify(order.getCustomer(), "服务预约已拒绝", order.getProvider().getNickname() + " 拒绝了你的预约", "SERVICE_ORDER", order.getId());
        return ServiceOrderSummary.from(order);
    }

    @Transactional
    public ServiceOrderSummary startOrder(Long orderId, Long actorId) {
        ServiceOrder order = findOrder(orderId);
        ensureProvider(order, actorId);
        governanceService.ensureCanProvideService(actorId);
        order.start();
        notificationService.notify(order.getCustomer(), "服务已开始", order.getServiceItem().getTitle() + " 已开始服务", "SERVICE_ORDER", order.getId());
        return ServiceOrderSummary.from(order);
    }

    @Transactional
    public ServiceOrderSummary completeOrder(Long orderId, Long actorId) {
        ServiceOrder order = findOrder(orderId);
        ensureParticipant(order, actorId);
        order.complete();
        User recipient = order.getProvider().getId().equals(actorId) ? order.getCustomer() : order.getProvider();
        notificationService.notify(recipient, "服务预约已完成", order.getServiceItem().getTitle() + " 已标记完成", "SERVICE_ORDER", order.getId());
        return ServiceOrderSummary.from(order);
    }

    @Transactional
    public ServiceOrderSummary cancelOrder(Long orderId, Long actorId, String reason) {
        ServiceOrder order = findOrder(orderId);
        ensureParticipant(order, actorId);
        order.cancel(reason);
        User recipient = order.getProvider().getId().equals(actorId) ? order.getCustomer() : order.getProvider();
        notificationService.notify(recipient, "服务预约已取消", order.getServiceItem().getTitle() + " 已取消", "SERVICE_ORDER", order.getId());
        return ServiceOrderSummary.from(order);
    }

    private ShopDetailSummary detailFor(Shop shop, Long viewerId, boolean includeAllItems) {
        List<ServiceItemSummary> serviceItems = listShopItems(shop.getId(), includeAllItems);
        boolean owner = viewerId != null && shop.getOwner().getId().equals(viewerId);
        boolean hasOrder = viewerId != null && serviceOrderRepository.existsByServiceItemShopIdAndCustomerId(shop.getId(), viewerId);
        boolean contactVisible = owner || hasOrder;
        String contactSnapshot = contactVisible ? buildContactSnapshot(shop.getOwner()) : null;
        return ShopDetailSummary.from(shop, contactVisible, contactSnapshot, serviceItems);
    }

    private void ensureShopMerchant(Long userId) {
        roleApplicationRepository.findByUserIdAndRoleType(userId, "SHOP_MERCHANT")
                .filter(application -> "PAID".equals(application.getDepositStatus()))
                .filter(application -> "APPROVED".equals(application.getReviewStatus()))
                .orElseThrow(() -> new BusinessException("请先开通店铺商家身份"));
    }

    private void ensureOwner(Shop shop, Long userId) {
        if (!shop.getOwner().getId().equals(userId)) {
            throw new BusinessException("只能操作自己的店铺");
        }
    }

    private void ensureProvider(ServiceOrder order, Long userId) {
        if (!order.getProvider().getId().equals(userId)) {
            throw new BusinessException("只能由服务商家处理预约");
        }
    }

    private void ensureParticipant(ServiceOrder order, Long userId) {
        if (!order.getProvider().getId().equals(userId) && !order.getCustomer().getId().equals(userId)) {
            throw new BusinessException("只能由预约参与方操作");
        }
    }

    private void ensureShopActive(Shop shop) {
        if (!"APPROVED".equals(shop.getStatus())) {
            throw new BusinessException("店铺当前不可发布服务");
        }
    }

    private Shop findShop(Long shopId) {
        return shopRepository.findById(shopId).orElseThrow(() -> new BusinessException("店铺不存在"));
    }

    private ServiceItem findItem(Long itemId) {
        return serviceItemRepository.findById(itemId).orElseThrow(() -> new BusinessException("服务项目不存在"));
    }

    private ServiceOrder findOrder(Long orderId) {
        return serviceOrderRepository.findById(orderId).orElseThrow(() -> new BusinessException("预约不存在"));
    }

    private String buildContactSnapshot(User user) {
        String wechat = user.getWechatContact() == null ? "" : "微信:" + user.getWechatContact();
        String qq = user.getQqContact() == null ? "" : "QQ:" + user.getQqContact();
        String joined = (wechat + " " + qq).trim();
        if (joined.isEmpty()) {
            throw new BusinessException("用户联系方式缺失");
        }
        return joined;
    }
}
