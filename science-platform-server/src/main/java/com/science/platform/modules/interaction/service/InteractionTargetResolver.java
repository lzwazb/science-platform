package com.science.platform.modules.interaction.service;

import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.interaction.entity.InteractionComment;
import com.science.platform.modules.interaction.mapper.InteractionCommentMapper;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import org.springframework.stereotype.Component;

/**
 * 互动目标解析：把 {@code targetType + targetId} 这个多态引用落到具体的表上。
 *
 * <p>评论、点赞、收藏、反馈都通过这两个字段指向文章或作品，数据库层没有外键，
 * 所以「目标是否存在、是否还公开可见」必须在这里统一校验，避免出现
 * 给已下架文章点赞、给已删除作品评论这类悬空数据。</p>
 */
@Component
public class InteractionTargetResolver {

    public static final String TYPE_ARTICLE = "ARTICLE";
    public static final String TYPE_WORK = "WORK";
    public static final String TYPE_COMMENT = "COMMENT";

    private final ScienceArticleMapper articleMapper;
    private final CreatorWorkMapper workMapper;
    private final InteractionCommentMapper commentMapper;

    public InteractionTargetResolver(ScienceArticleMapper articleMapper,
                                     CreatorWorkMapper workMapper,
                                     InteractionCommentMapper commentMapper) {
        this.articleMapper = articleMapper;
        this.workMapper = workMapper;
        this.commentMapper = commentMapper;
    }

    /**
     * 校验目标存在且公开可见（文章 PUBLISHED、作品 PUBLISHED）。
     *
     * @throws BusinessException 目标不存在或不可见时抛出 404
     */
    public void requirePublicTarget(String targetType, Long targetId) {
        if (TYPE_ARTICLE.equals(targetType)) {
            ScienceArticle article = articleMapper.selectById(targetId);
            if (article == null || !"PUBLISHED".equals(article.getStatus())) {
                throw BusinessException.notFound("文章不存在或未发布");
            }
            return;
        }
        if (TYPE_WORK.equals(targetType)) {
            CreatorWork work = workMapper.selectById(targetId);
            if (work == null || !"PUBLISHED".equals(work.getStatus())) {
                throw BusinessException.notFound("作品不存在或未发布");
            }
            return;
        }
        throw new BusinessException("不支持的目标类型：" + targetType);
    }

    /**
     * 取目标标题，用于后台展示与通知文案。
     *
     * @return 标题；目标已不存在时返回「已删除内容」
     */
    public String titleOf(String targetType, Long targetId) {
        if (TYPE_ARTICLE.equals(targetType)) {
            ScienceArticle article = articleMapper.selectById(targetId);
            return article == null ? "已删除的文章" : article.getTitle();
        }
        if (TYPE_WORK.equals(targetType)) {
            CreatorWork work = workMapper.selectById(targetId);
            return work == null ? "已删除的作品" : work.getTitle();
        }
        if (TYPE_COMMENT.equals(targetType)) {
            // 针对评论的反馈（如违规内容举报），用评论内容前若干字作为标题
            InteractionComment comment = commentMapper.selectById(targetId);
            if (comment == null) {
                return "已删除的评论";
            }
            String text = comment.getContent();
            return text.length() > 30 ? "评论：" + text.substring(0, 30) + "…" : "评论：" + text;
        }
        return "未知内容";
    }

    /**
     * 取「可跳转的目标」。
     *
     * <p>反馈可能指向文章、作品，也可能指向某条评论。评论本身没有独立页面，
     * 但用户点进反馈列表时希望能看到被反馈的那条内容，所以这里把评论回溯到
     * 它所属的文章或作品上，返回一个前端能直接拼路由的引用。</p>
     *
     * @return 可跳转的引用；目标已删除或类型不支持时返回 {@code null}
     */
    public TargetLink linkOf(String targetType, Long targetId) {
        if (TYPE_ARTICLE.equals(targetType) || TYPE_WORK.equals(targetType)) {
            return new TargetLink(targetType, targetId);
        }
        if (TYPE_COMMENT.equals(targetType)) {
            InteractionComment comment = commentMapper.selectById(targetId);
            if (comment == null) {
                return null;
            }
            // 评论挂在文章或作品下，把引用提升到父内容
            if (TYPE_ARTICLE.equals(comment.getTargetType()) || TYPE_WORK.equals(comment.getTargetType())) {
                return new TargetLink(comment.getTargetType(), comment.getTargetId());
            }
            return null;
        }
        return null;
    }

    /**
     * 可跳转的目标引用。
     *
     * @param type ARTICLE 或 WORK
     * @param id   对应内容的 ID
     */
    public record TargetLink(String type, Long id) {
    }

    /**
     * 取目标摘要，用于「我的收藏」列表。
     */
    public String summaryOf(String targetType, Long targetId) {        if (TYPE_ARTICLE.equals(targetType)) {
            ScienceArticle article = articleMapper.selectById(targetId);
            return article == null ? null : article.getSummary();
        }
        if (TYPE_WORK.equals(targetType)) {
            CreatorWork work = workMapper.selectById(targetId);
            return work == null ? null : work.getDescription();
        }
        return null;
    }

    /**
     * 取目标封面。
     */
    public String coverOf(String targetType, Long targetId) {
        if (TYPE_ARTICLE.equals(targetType)) {
            ScienceArticle article = articleMapper.selectById(targetId);
            return article == null ? null : article.getCoverUrl();
        }
        if (TYPE_WORK.equals(targetType)) {
            CreatorWork work = workMapper.selectById(targetId);
            return work == null ? null : work.getCoverUrl();
        }
        return null;
    }

    /**
     * 目标是否仍公开可见，用于收藏列表标记「内容已下架」。
     */
    public boolean isPubliclyVisible(String targetType, Long targetId) {
        try {
            requirePublicTarget(targetType, targetId);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }
}
