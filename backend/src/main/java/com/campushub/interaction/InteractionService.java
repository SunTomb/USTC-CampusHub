package com.campushub.interaction;

import com.campushub.common.BusinessException;
import com.campushub.goods.GoodsListingAccess;
import com.campushub.goods.GoodsRepository;
import com.campushub.projectad.ProjectAdRepository;
import com.campushub.shop.ShopRepository;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InteractionService {

    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final GoodsRepository goodsRepository;
    private final ProjectAdRepository projectAdRepository;
    private final ShopRepository shopRepository;

    public InteractionService(CommentRepository commentRepository, FavoriteRepository favoriteRepository,
                              UserRepository userRepository, GoodsRepository goodsRepository,
                              ProjectAdRepository projectAdRepository, ShopRepository shopRepository) {
        this.commentRepository = commentRepository;
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.goodsRepository = goodsRepository;
        this.projectAdRepository = projectAdRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentSummary> listComments(String type, Long id, Long actorId, boolean admin) {
        requireTargetVisible(type, id, actorId, admin);
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(type, id).stream()
                .map(CommentSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FavoriteSummary> listFavorites(String type, Long id, Long actorId, boolean admin) {
        requireTargetOwnerOrAdmin(type, id, actorId, admin);
        return favoriteRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, id).stream()
                .map(FavoriteSummary::from).toList();
    }

    @Transactional
    public CommentSummary comment(Long userId, CommentRequest request, boolean admin) {
        requireTargetVisible(request.targetType(), request.targetId(), userId, admin);
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        Comment parent = request.parentId() == null ? null : commentRepository.findById(request.parentId())
                .orElseThrow(() -> new BusinessException("父评论不存在"));
        if (parent != null && (!request.targetType().equals(parent.getTargetType()) || !request.targetId().equals(parent.getTargetId()))) {
            throw new BusinessException("父评论不属于当前内容");
        }
        Comment comment = new Comment(user, request.targetType(), request.targetId(), parent, request.content().trim());
        return CommentSummary.from(commentRepository.save(comment));
    }

    @Transactional
    public void favorite(Long userId, FavoriteRequest request, boolean admin) {
        requireTargetVisible(request.targetType(), request.targetId(), userId, admin);
        if (favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId())) {
            return;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        favoriteRepository.save(new Favorite(user, request.targetType(), request.targetId()));
    }

    @Transactional
    public void unfavorite(Long userId, FavoriteRequest request) {
        TargetType.parse(request.targetType());
        favoriteRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId());
    }

    private void requireTargetVisible(String type, Long id, Long actorId, boolean admin) {
        Target target = loadTarget(type, id);
        if (!target.publiclyVisible() && !admin && !target.ownerId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前内容暂不可查看");
        }
    }

    private void requireTargetOwnerOrAdmin(String type, Long id, Long actorId, boolean admin) {
        Target target = loadTarget(type, id);
        if (!admin && !target.ownerId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        }
    }

    private Target loadTarget(String type, Long id) {
        return (switch (TargetType.parse(type)) {
            case GOODS -> goodsRepository.findById(id).map(goods ->
                    new Target(goods.getSeller().getId(), GoodsListingAccess.isPublic(goods.getStatus())));
            case PROJECT_AD -> projectAdRepository.findById(id).map(ad ->
                    new Target(ad.getPublisher().getId(), "APPROVED".equals(ad.getStatus())));
            case SHOP -> shopRepository.findById(id).map(shop ->
                    new Target(shop.getOwner().getId(), "APPROVED".equals(shop.getStatus())));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "互动目标不存在"));
    }

    private record Target(Long ownerId, boolean publiclyVisible) {}

    private enum TargetType {
        GOODS, PROJECT_AD, SHOP;

        static TargetType parse(String type) {
            for (TargetType candidate : values()) {
                if (candidate.name().equals(type)) return candidate;
            }
            throw new BusinessException("不支持的互动目标");
        }
    }
}
