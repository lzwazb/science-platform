package com.science.platform.modules.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.event.dto.AwardRequest;
import com.science.platform.modules.event.dto.SubmissionReviewRequest;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.entity.EventReview;
import com.science.platform.modules.event.entity.EventSubmission;
import com.science.platform.modules.event.mapper.CreativeEventMapper;
import com.science.platform.modules.event.mapper.EventReviewMapper;
import com.science.platform.modules.event.mapper.EventSubmissionMapper;
import com.science.platform.modules.event.vo.SubmissionListItemVO;
import com.science.platform.modules.system.service.OperationLogService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 活动评审与优秀作品确认。
 */
@Service
public class EventReviewService {

    private final EventReviewMapper reviewMapper;
    private final EventSubmissionMapper submissionMapper;
    private final CreativeEventMapper eventMapper;
    private final EventSubmissionService submissionService;
    private final OperationLogService operationLogService;

    public EventReviewService(EventReviewMapper reviewMapper,
                              EventSubmissionMapper submissionMapper,
                              CreativeEventMapper eventMapper,
                              EventSubmissionService submissionService,
                              OperationLogService operationLogService) {
        this.reviewMapper = reviewMapper;
        this.submissionMapper = submissionMapper;
        this.eventMapper = eventMapper;
        this.submissionService = submissionService;
        this.operationLogService = operationLogService;
    }

    /**
     * 对投稿打分并写评语。
     *
     * <p>同一评审员对同一投稿只保留一份有效评审：已评过则更新，不新增记录。
     * 唯一索引 {@code uk_submission_reviewer} 兜底并发。</p>
     */
    @Transactional
    public SubmissionListItemVO review(Long submissionId, SubmissionReviewRequest request, Long reviewerId) {
        EventSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw BusinessException.notFound("投稿记录不存在");
        }

        CreativeEvent event = eventMapper.selectById(submission.getEventId());
        if (event == null) {
            throw BusinessException.notFound("活动不存在");
        }
        if (EventService.STATUS_DRAFT.equals(event.getStatus())) {
            throw new BusinessException("活动尚未发布，不能评审");
        }

        LambdaQueryWrapper<EventReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventReview::getSubmissionId, submissionId)
                .eq(EventReview::getReviewerId, reviewerId);
        EventReview existing = reviewMapper.selectOne(wrapper);

        if (existing == null) {
            EventReview review = new EventReview();
            review.setSubmissionId(submissionId);
            review.setReviewerId(reviewerId);
            review.setScore(request.getScore());
            review.setComment(request.getComment().trim());
            try {
                reviewMapper.insert(review);
            } catch (DuplicateKeyException ex) {
                // 并发同评：改为更新已存在的那条
                EventReview conflict = reviewMapper.selectOne(wrapper);
                if (conflict != null) {
                    updateReview(conflict.getId(), request);
                }
            }
        } else {
            updateReview(existing.getId(), request);
        }

        // 投稿状态：至少被评过一次就标为 REVIEWED
        if (EventSubmissionService.STATUS_SUBMITTED.equals(submission.getStatus())) {
            LambdaUpdateWrapper<EventSubmission> statusWrapper = new LambdaUpdateWrapper<>();
            statusWrapper.eq(EventSubmission::getId, submissionId)
                    .set(EventSubmission::getStatus, "REVIEWED")
                    .set(EventSubmission::getUpdatedAt, LocalDateTime.now());
            submissionMapper.update(null, statusWrapper);
        }

        List<SubmissionListItemVO> items = submissionService.toListItems(List.of(submission), reviewerId);
        return items.isEmpty() ? null : items.get(0);
    }

    private void updateReview(Long id, SubmissionReviewRequest request) {
        LambdaUpdateWrapper<EventReview> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EventReview::getId, id)
                .set(EventReview::getScore, request.getScore())
                .set(EventReview::getComment, request.getComment().trim())
                .set(EventReview::getUpdatedAt, LocalDateTime.now());
        reviewMapper.update(null, wrapper);
    }

    /**
     * 确认优秀作品。
     *
     * <p>开发档案 FR-EV-05：「不得把未评审作品自动标为优秀」。
     * 因此这里要求每个被选中的投稿**至少有一条评审记录**，否则整体拒绝。</p>
     *
     * @param eventId  活动 ID
     * @param request  获奖名单
     * @param operatorId 操作人（管理员）
     */
    @Transactional
    public void confirmAwards(Long eventId, AwardRequest request, Long operatorId) {
        CreativeEvent event = eventMapper.selectById(eventId);
        if (event == null) {
            throw BusinessException.notFound("活动不存在");
        }

        Set<Long> seen = new HashSet<>();
        for (AwardRequest.Item item : request.getAwards()) {
            if (!seen.add(item.getSubmissionId())) {
                throw new BusinessException("同一投稿不能重复获奖");
            }

            EventSubmission submission = submissionMapper.selectById(item.getSubmissionId());
            if (submission == null || !eventId.equals(submission.getEventId())) {
                throw new BusinessException("投稿 " + item.getSubmissionId() + " 不属于本活动");
            }

            LambdaQueryWrapper<EventReview> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(EventReview::getSubmissionId, submission.getId());
            if (reviewMapper.selectCount(reviewWrapper) == 0) {
                throw new BusinessException("投稿 #" + submission.getId() + " 还没有任何评审记录，不能直接标为优秀");
            }

            LambdaUpdateWrapper<EventSubmission> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(EventSubmission::getId, submission.getId())
                    .set(EventSubmission::getAwardLevel, item.getAwardLevel())
                    .set(EventSubmission::getStatus, EventSubmissionService.STATUS_SELECTED)
                    .set(EventSubmission::getUpdatedAt, LocalDateTime.now());
            submissionMapper.update(null, updateWrapper);
        }

        operationLogService.record(operatorId, "EVENT_AWARD_CONFIRM", "EVENT", eventId,
                "确认活动《" + event.getTitle() + "》的优秀作品共 " + request.getAwards().size() + " 件");
    }

    /**
     * 撤销某件投稿的获奖标记（写错名单时用）。
     */
    @Transactional
    public void revokeAward(Long submissionId, Long operatorId) {
        EventSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw BusinessException.notFound("投稿记录不存在");
        }
        LambdaUpdateWrapper<EventSubmission> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EventSubmission::getId, submissionId)
                .set(EventSubmission::getAwardLevel, null)
                .set(EventSubmission::getStatus, "REVIEWED")
                .set(EventSubmission::getUpdatedAt, LocalDateTime.now());
        submissionMapper.update(null, wrapper);

        operationLogService.record(operatorId, "EVENT_AWARD_REVOKE", "EVENT", submission.getEventId(),
                "撤销投稿 #" + submissionId + " 的获奖标记");
    }

    /**
     * 取活动下指定投稿的评审详情（可选，用于详情页展示评语）。
     */
    public List<EventReview> listReviews(Long submissionId) {
        LambdaQueryWrapper<EventReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventReview::getSubmissionId, submissionId)
                .orderByAsc(EventReview::getId);
        return reviewMapper.selectList(wrapper);
    }

    /**
     * 校验条件：活动是否允许评审（草稿与已取消不允许）。
     */
    static boolean allowsReview(CreativeEvent event) {
        return event != null && StringUtils.hasText(event.getStatus())
                && !EventService.STATUS_DRAFT.equals(event.getStatus())
                && !EventService.STATUS_CANCELLED.equals(event.getStatus());
    }
}
