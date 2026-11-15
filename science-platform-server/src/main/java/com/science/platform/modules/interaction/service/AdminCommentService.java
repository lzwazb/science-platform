package com.science.platform.modules.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.interaction.entity.InteractionComment;
import com.science.platform.modules.interaction.mapper.InteractionCommentMapper;
import com.science.platform.modules.interaction.vo.AdminCommentVO;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台评论管理。
 *
 * <p>与前台 {@link CommentService} 的分工：前台按「某篇文章/某个作品」查可见评论；
 * 这里跨全站检索，可按状态、目标类型、关键词筛选，已删除评论也可见，
 * 供管理员处理违规内容与追溯删除记录。</p>
 *
 * <p>标题与昵称都做批量查询，避免一页 20 条触发 40 次回表。</p>
 */
@Service
public class AdminCommentService {

    private static final String TYPE_ARTICLE = "ARTICLE";
    private static final String TYPE_WORK = "WORK";

    private final InteractionCommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;
    private final ScienceArticleMapper articleMapper;
    private final CreatorWorkMapper workMapper;

    public AdminCommentService(InteractionCommentMapper commentMapper,
                               SysUserMapper sysUserMapper,
                               ScienceArticleMapper articleMapper,
                               CreatorWorkMapper workMapper) {
        this.commentMapper = commentMapper;
        this.sysUserMapper = sysUserMapper;
        this.articleMapper = articleMapper;
        this.workMapper = workMapper;
    }

    /**
     * 后台评论分页。
     *
     * @param targetType 目标类型，ARTICLE / WORK，为空不筛选
     * @param targetId   目标 ID，为空不筛选
     * @param status     状态，VISIBLE / HIDDEN / DELETED，为空返回全部
     * @param keyword    评论内容关键词，为空不筛选
     */
    public IPage<AdminCommentVO> pageAll(long page, long size, String targetType, Long targetId,
                                         String status, String keyword) {
        LambdaQueryWrapper<InteractionComment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(targetType)) {
            String type = targetType.trim().toUpperCase(Locale.ROOT);
            if (!TYPE_ARTICLE.equals(type) && !TYPE_WORK.equals(type)) {
                throw new BusinessException("不支持的目标类型：" + targetType);
            }
            wrapper.eq(InteractionComment::getTargetType, type);
        }
        if (targetId != null) {
            wrapper.eq(InteractionComment::getTargetId, targetId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(InteractionComment::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(InteractionComment::getContent, keyword.trim());
        }
        // 新评论在前，方便发现最新违规内容
        wrapper.orderByDesc(InteractionComment::getCreatedAt)
                .orderByDesc(InteractionComment::getId);

        Page<InteractionComment> result = commentMapper.selectPage(new Page<>(page, size), wrapper);
        Page<AdminCommentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVOs(result.getRecords()));
        return voPage;
    }

    private List<AdminCommentVO> toVOs(List<InteractionComment> comments) {
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
                        .collect(Collectors.toMap(SysUser::getId, user -> user));

        Map<Long, String> articleTitles = titlesOfArticles(comments);
        Map<Long, String> workTitles = titlesOfWork(comments);

        return comments.stream().map(comment -> {
            AdminCommentVO vo = new AdminCommentVO();
            vo.setId(comment.getId());
            vo.setUserId(comment.getUserId());
            SysUser author = users.get(comment.getUserId());
            vo.setUserName(author == null ? "已注销用户" : author.getNickname());
            vo.setUserAvatar(author == null ? null : author.getAvatar());
            vo.setTargetType(comment.getTargetType());
            vo.setTargetId(comment.getTargetId());
            vo.setTargetTitle(resolveTitle(comment, articleTitles, workTitles));
            vo.setParentId(comment.getParentId());
            vo.setContent(comment.getContent());
            vo.setStatus(comment.getStatus());
            vo.setCreatedAt(comment.getCreatedAt());
            return vo;
        }).toList();
    }

    private String resolveTitle(InteractionComment comment,
                                Map<Long, String> articleTitles,
                                Map<Long, String> workTitles) {
        if (TYPE_ARTICLE.equals(comment.getTargetType())) {
            return articleTitles.getOrDefault(comment.getTargetId(), "已删除的文章");
        }
        if (TYPE_WORK.equals(comment.getTargetType())) {
            return workTitles.getOrDefault(comment.getTargetId(), "已删除的作品");
        }
        return "未知内容";
    }

    private Map<Long, String> titlesOfArticles(List<InteractionComment> comments) {
        List<Long> ids = comments.stream()
                .filter(comment -> TYPE_ARTICLE.equals(comment.getTargetType()))
                .map(InteractionComment::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return articleMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ScienceArticle::getId, ScienceArticle::getTitle));
    }

    private Map<Long, String> titlesOfWork(List<InteractionComment> comments) {
        List<Long> ids = comments.stream()
                .filter(comment -> TYPE_WORK.equals(comment.getTargetType()))
                .map(InteractionComment::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return workMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CreatorWork::getId, CreatorWork::getTitle));
    }
}
