package com.science.platform.modules.work.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import com.science.platform.modules.work.vo.WorkDetailVO;
import com.science.platform.modules.work.vo.WorkListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 作品审核（评审员 / 管理员）。
 *
 * <p>审核对象是「他人提交的作品」；谁能进入这里由角色决定
 * （见 SecurityConfig 与 Controller 上的 {@code @PreAuthorize}）。</p>
 */
@Service
public class WorkReviewService {

    private final CreatorWorkMapper workMapper;
    private final WorkQueryService workQueryService;

    public WorkReviewService(CreatorWorkMapper workMapper, WorkQueryService workQueryService) {
        this.workMapper = workMapper;
        this.workQueryService = workQueryService;
    }

    /**
     * 作品列表，默认只看待审核。
     */
    public IPage<WorkListItemVO> pageWorks(long page, long size, String status, String keyword) {
        String effectiveStatus = StringUtils.hasText(status) ? status : WorkService.STATUS_PENDING;
        return workQueryService.pageForReview(page, size, effectiveStatus, keyword);
    }

    /**
     * 审核通过：作品可被公开访问。
     *
     * @param id         作品 ID
     * @param comment    审核意见，可为空
     * @param reviewerId 当前登录的评审员 ID
     */
    @Transactional
    public WorkDetailVO approve(Long id, String comment, Long reviewerId) {
        requirePendingWork(id);
        applyReviewResult(id, WorkService.STATUS_PUBLISHED, reviewerId, comment);
        return workQueryService.getDetailAnyStatus(id);
    }

    /**
     * 审核驳回：必须填写意见，供作者修改后重新提交。
     */
    @Transactional
    public WorkDetailVO reject(Long id, String comment, Long reviewerId) {
        if (!StringUtils.hasText(comment)) {
            throw new BusinessException("驳回时必须填写审核意见");
        }
        requirePendingWork(id);
        applyReviewResult(id, WorkService.STATUS_REJECTED, reviewerId, comment);
        return workQueryService.getDetailAnyStatus(id);
    }

    /**
     * 下架已发布作品（处理违规内容时使用）。
     */
    @Transactional
    public WorkDetailVO offline(Long id, String comment, Long operatorId) {
        CreatorWork work = workMapper.selectById(id);
        if (work == null) {
            throw BusinessException.notFound("作品不存在");
        }
        if (!WorkService.STATUS_PUBLISHED.equals(work.getStatus())) {
            throw new BusinessException("只有已发布的作品可以下架");
        }
        applyReviewResult(id, WorkService.STATUS_OFFLINE, operatorId, comment);
        return workQueryService.getDetailAnyStatus(id);
    }

    /**
     * 只允许处理待审核状态的作品。
     *
     * <p>这是防止「重复审批」的关键：如果审核接口可以作用于任意状态，
     * 两个评审员同时点通过就会产生状态错乱。要求操作前状态必须是 PENDING。</p>
     */
    private void requirePendingWork(Long id) {
        CreatorWork work = workMapper.selectById(id);
        if (work == null) {
            throw BusinessException.notFound("作品不存在");
        }
        if (!WorkService.STATUS_PENDING.equals(work.getStatus())) {
            throw new BusinessException("该作品当前状态为 " + statusText(work.getStatus()) + "，不能进行审核");
        }
    }

    /**
     * 写入审核结论。
     *
     * <p>用 {@code LambdaUpdateWrapper} 逐列 set 而不是 {@code updateById(entity)}：
     * MyBatis-Plus 默认 {@code NOT_NULL} 字段策略会跳过 null 字段，
     * 「通过但未填写意见」时旧意见就清不掉。</p>
     */
    private void applyReviewResult(Long id, String status, Long reviewerId, String comment) {
        LambdaUpdateWrapper<CreatorWork> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CreatorWork::getId, id)
                .set(CreatorWork::getStatus, status)
                .set(CreatorWork::getReviewerId, reviewerId)
                .set(CreatorWork::getReviewComment, StringUtils.hasText(comment) ? comment.trim() : null)
                .set(CreatorWork::getReviewedAt, LocalDateTime.now())
                .set(CreatorWork::getUpdatedAt, LocalDateTime.now());
        workMapper.update(null, wrapper);
    }

    private String statusText(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "草稿";
            case "PUBLISHED" -> "已发布";
            case "REJECTED" -> "已驳回";
            case "OFFLINE" -> "已下架";
            default -> status;
        };
    }
}
