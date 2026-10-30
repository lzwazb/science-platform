package com.science.platform.modules.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.interaction.dto.CommentCreateRequest;
import com.science.platform.modules.interaction.entity.InteractionComment;
import com.science.platform.modules.interaction.mapper.InteractionCommentMapper;
import com.science.platform.modules.interaction.vo.CommentVO;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论。
 *
 * <p>只做一层回复：父评论的父评论视为同一层，避免无限嵌套带来的排序与渲染复杂度。</p>
 */
@Service
public class CommentService {

    private static final String STATUS_VISIBLE = "VISIBLE";
    private static final String STATUS_DELETED = "DELETED";

    private final InteractionCommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;

    public CommentService(InteractionCommentMapper commentMapper, SysUserMapper sysUserMapper) {
        this.commentMapper = commentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询某个目标下的评论（一级评论 + 各自的回复）。
     *
     * @param currentUserId 当前登录用户 ID，可为 null，用于标记「我的评论」
     */
    public IPage<CommentVO> pageComments(String targetType, Long targetId, long page, long size,
                                         Long currentUserId) {
        LambdaQueryWrapper<InteractionComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InteractionComment::getTargetType, targetType)
                .eq(InteractionComment::getTargetId, targetId)
                .eq(InteractionComment::getStatus, STATUS_VISIBLE)
                .isNull(InteractionComment::getParentId)
                .orderByDesc(InteractionComment::getCreatedAt)
                .orderByDesc(InteractionComment::getId);

        Page<InteractionComment> result = commentMapper.selectPage(new Page<>(page, size), wrapper);
        List<CommentVO> roots = toVOs(result.getRecords(), currentUserId);

        // 一次性取回这些一级评论的所有回复
        if (!roots.isEmpty()) {
            List<Long> rootIds = roots.stream().map(CommentVO::getId).toList();
            LambdaQueryWrapper<InteractionComment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(InteractionComment::getTargetType, targetType)
                    .eq(InteractionComment::getTargetId, targetId)
                    .eq(InteractionComment::getStatus, STATUS_VISIBLE)
                    .in(InteractionComment::getParentId, rootIds)
                    .orderByAsc(InteractionComment::getCreatedAt);

            List<InteractionComment> replies = commentMapper.selectList(replyWrapper);
            Map<Long, List<CommentVO>> replyMap = toVOs(replies, currentUserId).stream()
                    .collect(Collectors.groupingBy(CommentVO::getParentId));
            roots.forEach(root -> root.setReplies(
                    replyMap.getOrDefault(root.getId(), Collections.emptyList())));
        }

        Page<CommentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(roots);
        return voPage;
    }

    /**
     * 发表评论或回复。
     */
    @Transactional
    public CommentVO create(CommentCreateRequest request, Long userId,
                            InteractionTargetResolver targetResolver) {
        targetResolver.requirePublicTarget(request.getTargetType(), request.getTargetId());

        Long parentId = request.getParentId();
        if (parentId != null) {
            InteractionComment parent = commentMapper.selectById(parentId);
            if (parent == null || !STATUS_VISIBLE.equals(parent.getStatus())) {
                throw BusinessException.notFound("要回复的评论不存在");
            }
            if (!Objects.equals(parent.getTargetType(), request.getTargetType())
                    || !Objects.equals(parent.getTargetId(), request.getTargetId())) {
                throw new BusinessException("回复的评论不属于当前内容");
            }
            // 只保留一层：回复的回复挂到同一个根评论下
            if (parent.getParentId() != null) {
                parentId = parent.getParentId();
            }
        }

        InteractionComment comment = new InteractionComment();
        comment.setUserId(userId);
        comment.setTargetType(request.getTargetType());
        comment.setTargetId(request.getTargetId());
        comment.setParentId(parentId);
        comment.setContent(request.getContent().trim());
        comment.setStatus(STATUS_VISIBLE);
        commentMapper.insert(comment);

        return toVOs(List.of(comment), userId).get(0);
    }

    /**
     * 删除评论：作者本人可删，管理员可删任意评论。
     *
     * <p>用状态位标记为 DELETED 而不是物理删除，保留楼层结构，
     * 也便于后续追查违规内容。</p>
     *
     * @param operatorId 操作人 ID
     * @param isAdmin    操作人是否具备管理权限
     */
    @Transactional
    public void delete(Long id, Long operatorId, boolean isAdmin) {
        InteractionComment comment = commentMapper.selectById(id);
        if (comment == null || STATUS_DELETED.equals(comment.getStatus())) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!isAdmin && !Objects.equals(comment.getUserId(), operatorId)) {
            throw BusinessException.forbidden("只能删除自己的评论");
        }

        LambdaUpdateWrapper<InteractionComment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InteractionComment::getId, id)
                .set(InteractionComment::getStatus, STATUS_DELETED)
                .set(InteractionComment::getUpdatedAt, LocalDateTime.now());
        commentMapper.update(null, wrapper);
    }

    private List<CommentVO> toVOs(List<InteractionComment> comments, Long currentUserId) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = comments.stream()
                .map(InteractionComment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> users = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        return comments.stream().map(comment -> {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setUserId(comment.getUserId());
            vo.setParentId(comment.getParentId());
            vo.setContent(comment.getContent());
            vo.setStatus(comment.getStatus());
            vo.setCreatedAt(comment.getCreatedAt());
            vo.setMine(currentUserId != null && Objects.equals(currentUserId, comment.getUserId()));
            SysUser user = users.get(comment.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname());
                vo.setUserAvatar(user.getAvatar());
            }
            return vo;
        }).toList();
    }
}
