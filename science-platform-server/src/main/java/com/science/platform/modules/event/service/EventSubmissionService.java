package com.science.platform.modules.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.event.entity.CreativeEvent;
import com.science.platform.modules.event.entity.EventReview;
import com.science.platform.modules.event.entity.EventSubmission;
import com.science.platform.modules.event.mapper.CreativeEventMapper;
import com.science.platform.modules.event.mapper.EventReviewMapper;
import com.science.platform.modules.event.mapper.EventSubmissionMapper;
import com.science.platform.modules.event.vo.AwardVO;
import com.science.platform.modules.event.vo.SubmissionListItemVO;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import org.springframework.dao.DuplicateKeyException;
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
 * 活动投稿：提交、查询、组装展示对象。
 */
@Service
public class EventSubmissionService {

    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_SELECTED = "SELECTED";

    private final EventSubmissionMapper submissionMapper;
    private final EventReviewMapper reviewMapper;
    private final CreativeEventMapper eventMapper;
    private final CreatorWorkMapper workMapper;
    private final SysUserMapper sysUserMapper;

    public EventSubmissionService(EventSubmissionMapper submissionMapper,
                                  EventReviewMapper reviewMapper,
                                  CreativeEventMapper eventMapper,
                                  CreatorWorkMapper workMapper,
                                  SysUserMapper sysUserMapper) {
        this.submissionMapper = submissionMapper;
        this.reviewMapper = reviewMapper;
        this.eventMapper = eventMapper;
        this.workMapper = workMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 提交作品参加活动。
     *
     * <p>四道校验：活动必须进行中、作品必须存在且已发布、作品必须属于投稿人、
     * 同一作品在同一活动只能投一次。最后一条同时靠唯一索引兜底。</p>
     *
     * @param eventId     活动 ID
     * @param workId      作品 ID
     * @param submitterId 当前登录创作者 ID
     */
    @Transactional
    public EventSubmission submit(Long eventId, Long workId, Long submitterId) {
        CreativeEvent event = eventMapper.selectById(eventId);
        if (event == null) {
            throw BusinessException.notFound("活动不存在");
        }
        // T11：活动时间校验
        String timeStatus = EventService.timeStatusOf(event, LocalDateTime.now());
        if (!EventService.TIME_ONGOING.equals(timeStatus)) {
            throw new BusinessException(switch (timeStatus) {
                case EventService.TIME_NOT_STARTED -> "活动尚未开始，暂不能投稿";
                case EventService.TIME_ENDED -> "活动已结束，不能再投稿";
                default -> "活动未发布，不能投稿";
            });
        }

        CreatorWork work = workMapper.selectById(workId);
        if (work == null) {
            throw BusinessException.notFound("作品不存在");
        }
        if (!Objects.equals(work.getCreatorId(), submitterId)) {
            throw BusinessException.forbidden("只能提交自己的作品");
        }
        if (!"PUBLISHED".equals(work.getStatus())) {
            throw new BusinessException("只有已发布的作品可以投稿");
        }

        LambdaQueryWrapper<EventSubmission> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(EventSubmission::getEventId, eventId)
                .eq(EventSubmission::getWorkId, workId);
        if (submissionMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException("该作品已报名本活动，不能重复投稿");
        }

        EventSubmission submission = new EventSubmission();
        submission.setEventId(eventId);
        submission.setWorkId(workId);
        submission.setSubmitterId(submitterId);
        submission.setStatus(STATUS_SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());

        try {
            submissionMapper.insert(submission);
        } catch (DuplicateKeyException ex) {
            // 并发重复点击：唯一索引 uk_event_work 兜底
            throw new BusinessException("该作品已报名本活动，不能重复投稿");
        }

        return submission;
    }

    /**
     * 我的投稿（可指定活动）。
     */
    public List<SubmissionListItemVO> listMine(Long submitterId, Long eventId, Long reviewerId) {
        LambdaQueryWrapper<EventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubmission::getSubmitterId, submitterId);
        if (eventId != null) {
            wrapper.eq(EventSubmission::getEventId, eventId);
        }
        wrapper.orderByDesc(EventSubmission::getId);
        return toListItems(submissionMapper.selectList(wrapper), reviewerId);
    }

    /**
     * 某活动的全部投稿，供评审工作台使用。
     */
    public List<SubmissionListItemVO> listForReview(Long eventId, Long reviewerId) {
        LambdaQueryWrapper<EventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubmission::getEventId, eventId)
                .orderByAsc(EventSubmission::getSubmittedAt)
                .orderByAsc(EventSubmission::getId);
        return toListItems(submissionMapper.selectList(wrapper), reviewerId);
    }

    /**
     * 统计某活动的投稿数。
     */
    public long countByEvent(Long eventId) {
        LambdaQueryWrapper<EventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubmission::getEventId, eventId);
        return submissionMapper.selectCount(wrapper);
    }

    /**
     * 活动已确认的优秀作品。
     */
    public List<AwardVO> listAwards(Long eventId) {
        LambdaQueryWrapper<EventSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubmission::getEventId, eventId)
                .isNotNull(EventSubmission::getAwardLevel)
                .orderByAsc(EventSubmission::getAwardLevel)
                .orderByAsc(EventSubmission::getId);

        List<EventSubmission> submissions = submissionMapper.selectList(wrapper);
        if (submissions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CreatorWork> works = worksOf(submissions);
        Map<Long, Double> averageScores = averageScoresOf(
                submissions.stream().map(EventSubmission::getId).toList());

        return submissions.stream()
                .filter(s -> works.containsKey(s.getWorkId()))
                .map(s -> {
                    CreatorWork work = works.get(s.getWorkId());
                    AwardVO vo = new AwardVO();
                    vo.setSubmissionId(s.getId());
                    vo.setWorkId(work.getId());
                    vo.setWorkTitle(work.getTitle());
                    vo.setWorkDescription(work.getDescription());
                    vo.setWorkCover(work.getCoverUrl());
                    vo.setAwardLevel(s.getAwardLevel());
                    vo.setAverageScore(averageScores.get(s.getId()));
                    return vo;
                }).toList();
    }

    /**
     * 批量转列表项：作品信息、提交人昵称、评审统计一次补齐。
     *
     * @param reviewerId 当前评审员 ID，用于标记「我是否已评过」
     */
    public List<SubmissionListItemVO> toListItems(List<EventSubmission> submissions, Long reviewerId) {
        if (submissions == null || submissions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CreatorWork> works = worksOf(submissions);
        Set<Long> eventIds = submissions.stream()
                .map(EventSubmission::getEventId)
                .collect(Collectors.toSet());
        Map<Long, String> eventTitles = eventIds.isEmpty()
                ? Collections.emptyMap()
                : eventMapper.selectBatchIds(eventIds).stream()
                        .collect(Collectors.toMap(CreativeEvent::getId, CreativeEvent::getTitle));

        Set<Long> submitterIds = submissions.stream()
                .map(EventSubmission::getSubmitterId)
                .collect(Collectors.toSet());
        Map<Long, String> submitterNames = submitterIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(submitterIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        List<Long> submissionIds = submissions.stream().map(EventSubmission::getId).toList();
        Map<Long, Double> averageScores = averageScoresOf(submissionIds);
        Map<Long, Long> reviewCounts = reviewCountsOf(submissionIds);
        Set<Long> reviewedByMe = reviewerId == null
                ? Collections.emptySet()
                : reviewedSubmissionIds(reviewerId, submissionIds);

        return submissions.stream().map(submission -> {
            SubmissionListItemVO vo = new SubmissionListItemVO();
            vo.setId(submission.getId());
            vo.setEventId(submission.getEventId());
            vo.setEventTitle(eventTitles.get(submission.getEventId()));
            vo.setWorkId(submission.getWorkId());
            vo.setSubmitterId(submission.getSubmitterId());
            vo.setSubmitterName(submitterNames.get(submission.getSubmitterId()));
            vo.setStatus(submission.getStatus());
            vo.setAwardLevel(submission.getAwardLevel());
            vo.setSubmittedAt(submission.getSubmittedAt());
            vo.setAverageScore(averageScores.get(submission.getId()));
            vo.setReviewCount(reviewCounts.getOrDefault(submission.getId(), 0L));
            vo.setReviewedByMe(reviewedByMe.contains(submission.getId()));

            CreatorWork work = works.get(submission.getWorkId());
            if (work != null) {
                vo.setWorkTitle(work.getTitle());
                vo.setWorkCover(work.getCoverUrl());
                vo.setWorkDescription(work.getDescription());
            }
            return vo;
        }).toList();
    }

    private Map<Long, CreatorWork> worksOf(List<EventSubmission> submissions) {
        Set<Long> workIds = submissions.stream()
                .map(EventSubmission::getWorkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (workIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workMapper.selectBatchIds(workIds).stream()
                .collect(Collectors.toMap(CreatorWork::getId, w -> w, (a, b) -> a));
    }

    private Map<Long, Double> averageScoresOf(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<EventReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EventReview::getSubmissionId, submissionIds);
        List<EventReview> reviews = reviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return Collections.emptyMap();
        }
        return reviews.stream()
                .collect(Collectors.groupingBy(
                        EventReview::getSubmissionId,
                        Collectors.averagingInt(EventReview::getScore)));
    }

    private Map<Long, Long> reviewCountsOf(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<EventReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EventReview::getSubmissionId, submissionIds);
        return reviewMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(EventReview::getSubmissionId, Collectors.counting()));
    }

    private Set<Long> reviewedSubmissionIds(Long reviewerId, List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<EventReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventReview::getReviewerId, reviewerId)
                .in(EventReview::getSubmissionId, submissionIds);
        return reviewMapper.selectList(wrapper).stream()
                .map(EventReview::getSubmissionId)
                .collect(Collectors.toSet());
    }
}
