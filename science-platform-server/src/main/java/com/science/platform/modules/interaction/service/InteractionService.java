package com.science.platform.modules.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.modules.interaction.entity.InteractionComment;
import com.science.platform.modules.interaction.entity.InteractionFavorite;
import com.science.platform.modules.interaction.entity.InteractionLike;
import com.science.platform.modules.interaction.mapper.InteractionCommentMapper;
import com.science.platform.modules.interaction.mapper.InteractionFavoriteMapper;
import com.science.platform.modules.interaction.mapper.InteractionLikeMapper;
import com.science.platform.modules.interaction.vo.FavoriteItemVO;
import com.science.platform.modules.interaction.vo.InteractionStatusVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞与收藏。
 *
 * <p>防重复靠数据库唯一约束而不是「先查后插」：并发下先查后插会漏判。
 * 重复点赞时捕获 {@link DuplicateKeyException} 并当作「已点赞」返回，
 * 而不是把唯一键冲突抛给用户。</p>
 */
@Service
public class InteractionService {

    private static final String STATUS_VISIBLE = "VISIBLE";

    private final InteractionLikeMapper likeMapper;
    private final InteractionFavoriteMapper favoriteMapper;
    private final InteractionCommentMapper commentMapper;
    private final InteractionTargetResolver targetResolver;

    public InteractionService(InteractionLikeMapper likeMapper,
                              InteractionFavoriteMapper favoriteMapper,
                              InteractionCommentMapper commentMapper,
                              InteractionTargetResolver targetResolver) {
        this.likeMapper = likeMapper;
        this.favoriteMapper = favoriteMapper;
        this.commentMapper = commentMapper;
        this.targetResolver = targetResolver;
    }

    /**
     * 点赞。已点过时保持幂等（不报错、不产生第二条记录）。
     *
     * @return 点赞后的总数
     */
    @Transactional
    public long like(Long userId, String targetType, Long targetId) {
        targetResolver.requirePublicTarget(targetType, targetId);

        if (!existsLike(userId, targetType, targetId)) {
            InteractionLike like = new InteractionLike();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            try {
                likeMapper.insert(like);
            } catch (DuplicateKeyException ex) {
                // 并发重复点击：唯一索引拦下，视为已点赞
            }
        }
        return countLikes(targetType, targetId);
    }

    /**
     * 取消点赞。未点赞时同样幂等。
     *
     * @return 取消后的总数
     */
    @Transactional
    public long unlike(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionLike::getUserId, userId)
                .eq(InteractionLike::getTargetType, targetType)
                .eq(InteractionLike::getTargetId, targetId);
        likeMapper.delete(wrapper);
        return countLikes(targetType, targetId);
    }

    /**
     * 收藏。已收藏时保持幂等。
     */
    @Transactional
    public long favorite(Long userId, String targetType, Long targetId) {
        targetResolver.requirePublicTarget(targetType, targetId);

        if (!existsFavorite(userId, targetType, targetId)) {
            InteractionFavorite favorite = new InteractionFavorite();
            favorite.setUserId(userId);
            favorite.setTargetType(targetType);
            favorite.setTargetId(targetId);
            try {
                favoriteMapper.insert(favorite);
            } catch (DuplicateKeyException ex) {
                // 并发重复点击：唯一索引拦下，视为已收藏
            }
        }
        return countFavorites(targetType, targetId);
    }

    /**
     * 取消收藏。
     */
    @Transactional
    public long unfavorite(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionFavorite::getUserId, userId)
                .eq(InteractionFavorite::getTargetType, targetType)
                .eq(InteractionFavorite::getTargetId, targetId);
        favoriteMapper.delete(wrapper);
        return countFavorites(targetType, targetId);
    }

    /**
     * 查询某目标的互动状态与计数。
     *
     * @param userId 当前登录用户 ID，未登录传 null（此时 liked/favorited 恒为 false）
     */
    public InteractionStatusVO status(Long userId, String targetType, Long targetId) {
        InteractionStatusVO vo = new InteractionStatusVO();
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        vo.setLikeCount(countLikes(targetType, targetId));
        vo.setFavoriteCount(countFavorites(targetType, targetId));
        vo.setCommentCount(countComments(targetType, targetId));

        if (userId != null) {
            vo.setLiked(existsLike(userId, targetType, targetId));
            vo.setFavorited(existsFavorite(userId, targetType, targetId));
        }
        return vo;
    }

    /**
     * 我的收藏，按收藏时间倒序，文章与作品混排。
     */
    public IPage<FavoriteItemVO> pageMyFavorites(Long userId, long page, long size) {
        LambdaQueryWrapper<InteractionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionFavorite::getUserId, userId)
                .orderByDesc(InteractionFavorite::getCreatedAt)
                .orderByDesc(InteractionFavorite::getId);

        Page<InteractionFavorite> result = favoriteMapper.selectPage(new Page<>(page, size), wrapper);
        Page<FavoriteItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(favorite -> {
            FavoriteItemVO vo = new FavoriteItemVO();
            vo.setTargetType(favorite.getTargetType());
            vo.setTargetId(favorite.getTargetId());
            vo.setTitle(targetResolver.titleOf(favorite.getTargetType(), favorite.getTargetId()));
            vo.setSummary(targetResolver.summaryOf(favorite.getTargetType(), favorite.getTargetId()));
            vo.setCoverUrl(targetResolver.coverOf(favorite.getTargetType(), favorite.getTargetId()));
            vo.setAvailable(targetResolver.isPubliclyVisible(favorite.getTargetType(), favorite.getTargetId()));
            vo.setCreatedAt(favorite.getCreatedAt());
            return vo;
        }).toList());
        return voPage;
    }

    long countLikes(String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionLike::getTargetType, targetType)
                .eq(InteractionLike::getTargetId, targetId);
        return likeMapper.selectCount(wrapper);
    }

    long countFavorites(String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionFavorite::getTargetType, targetType)
                .eq(InteractionFavorite::getTargetId, targetId);
        return favoriteMapper.selectCount(wrapper);
    }

    long countComments(String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionComment::getTargetType, targetType)
                .eq(InteractionComment::getTargetId, targetId)
                .eq(InteractionComment::getStatus, STATUS_VISIBLE);
        return commentMapper.selectCount(wrapper);
    }

    private boolean existsLike(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionLike::getUserId, userId)
                .eq(InteractionLike::getTargetType, targetType)
                .eq(InteractionLike::getTargetId, targetId);
        return likeMapper.selectCount(wrapper) > 0;
    }

    private boolean existsFavorite(Long userId, String targetType, Long targetId) {
        LambdaQueryWrapper<InteractionFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionFavorite::getUserId, userId)
                .eq(InteractionFavorite::getTargetType, targetType)
                .eq(InteractionFavorite::getTargetId, targetId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
