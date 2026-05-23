package com.campushub.file;

import com.campushub.common.BusinessException;
import com.campushub.goods.GoodsRepository;
import com.campushub.projectad.ProjectAdRepository;
import com.campushub.shop.ServiceItemRepository;
import com.campushub.shop.ShopRepository;
import com.campushub.task.RewardTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class FileTargetAuthorizationService {

    private final GoodsRepository goodsRepository;
    private final ProjectAdRepository projectAdRepository;
    private final RewardTaskRepository rewardTaskRepository;
    private final ShopRepository shopRepository;
    private final ServiceItemRepository serviceItemRepository;

    public FileTargetAuthorizationService(
            GoodsRepository goodsRepository,
            ProjectAdRepository projectAdRepository,
            RewardTaskRepository rewardTaskRepository,
            ShopRepository shopRepository,
            ServiceItemRepository serviceItemRepository) {
        this.goodsRepository = goodsRepository;
        this.projectAdRepository = projectAdRepository;
        this.rewardTaskRepository = rewardTaskRepository;
        this.shopRepository = shopRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public void requireCanBind(String targetType, Long targetId, Long userId, boolean admin) {
        if (admin) {
            return;
        }
        switch (targetType) {
            case "GOODS" -> requireOwner(goodsRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("绑定目标不存在"))
                    .getSeller().getId(), userId);
            case "PROJECT_AD" -> requireOwner(projectAdRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("绑定目标不存在"))
                    .getPublisher().getId(), userId);
            case "REWARD_TASK" -> requireOwner(rewardTaskRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("绑定目标不存在"))
                    .getPublisher().getId(), userId);
            case "SHOP" -> requireOwner(shopRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("绑定目标不存在"))
                    .getOwner().getId(), userId);
            case "SERVICE_ITEM" -> requireOwner(serviceItemRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException("绑定目标不存在"))
                    .getShop().getOwner().getId(), userId);
            default -> throw new BusinessException("不支持的文件绑定目标");
        }
    }

    private void requireOwner(Long ownerId, Long userId) {
        if (!ownerId.equals(userId)) {
            throw new BusinessException("无权绑定该目标");
        }
    }
}
