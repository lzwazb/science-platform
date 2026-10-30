package com.science.platform.modules.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.interaction.dto.FeedbackCreateRequest;
import com.science.platform.modules.interaction.dto.FeedbackHandleRequest;
import com.science.platform.modules.interaction.entity.ContentFeedback;
import com.science.platform.modules.interaction.mapper.ContentFeedbackMapper;
import com.science.platform.modules.interaction.vo.FeedbackVO;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.modules.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内容反馈：用户提交、管理员处理。
 */
@Service
public class FeedbackService {

    private static final String STATUS_PENDING = "PENDING";

    private final ContentFeedbackMapper feedbackMapper;
    private final SysUserMapper sysUserMapper;
    private final InteractionTargetResolver targetResolver;
    private final OperationLogService operationLogService;

    public FeedbackService(ContentFeedbackMapper feedbackMapper,
                           SysUserMapper sysUserMapper,
                           InteractionTargetResolver targetResolver,
                           OperationLogService operationLogService) {
        this.feedbackMapper = feedbackMapper;
        this.sysUserMapper = sysUserMapper;
        this.targetResolver = targetResolver;
        this.operationLogService = operationLogService;
    }

    /**
     * 提交内容反馈。
     *
     * <p>只校验目标存在，不要求目标必须是已发布状态——反馈本身可能就是
     * 「这条已下架的内容仍有问题」，或者针对某条评论。</p>
     */
    @Transactional
    public FeedbackVO create(FeedbackCreateRequest request, Long userId) {
        ContentFeedback feedback = new ContentFeedback();
        feedback.setUserId(userId);
        feedback.setTargetType(request.getTargetType());
        feedback.setTargetId(request.getTargetId());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setDescription(request.getDescription().trim());
        feedback.setStatus(STATUS_PENDING);
        feedbackMapper.insert(feedback);

        return toVO(feedback);
    }

    /**
     * 后台反馈列表。
     *
     * @param status 状态筛选，可为空
     */
    public IPage<FeedbackVO> pageAll(long page, long size, String status) {
        LambdaQueryWrapper<ContentFeedback> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(ContentFeedback::getStatus, status);
        }
        // 待处理的排前面，其余按时间倒序
        wrapper.orderByAsc(ContentFeedback::getStatus).orderByDesc(ContentFeedback::getId);

        Page<ContentFeedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);
        Page<FeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVOs(result.getRecords()));
        return voPage;
    }

    /**
     * 处理反馈。
     *
     * @param id        反馈 ID
     * @param request   处理结果与说明
     * @param handlerId 处理人 ID
     */
    @Transactional
    public FeedbackVO handle(Long id, FeedbackHandleRequest request, Long handlerId) {
        ContentFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw BusinessException.notFound("反馈不存在");
        }
        if (!STATUS_PENDING.equals(feedback.getStatus())
                && !"PROCESSING".equals(feedback.getStatus())) {
            throw new BusinessException("该反馈已处理完毕，不能再次处理");
        }

        LambdaUpdateWrapper<ContentFeedback> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ContentFeedback::getId, id)
                .set(ContentFeedback::getStatus, request.getStatus())
                .set(ContentFeedback::getHandlerId, handlerId)
                .set(ContentFeedback::getHandleComment,
                        StringUtils.hasText(request.getHandleComment())
                                ? request.getHandleComment().trim() : null)
                .set(ContentFeedback::getHandledAt, LocalDateTime.now())
                .set(ContentFeedback::getUpdatedAt, LocalDateTime.now());
        feedbackMapper.update(null, wrapper);

        operationLogService.record(handlerId, "FEEDBACK_HANDLE", feedback.getTargetType(),
                feedback.getTargetId(),
                "处理内容反馈 #" + id + "（" + feedback.getTargetType() + "#" + feedback.getTargetId() + "）");

        // 用 toVOs 统一补齐提交人与处理人昵称，避免返回的 VO 缺 handlerName
        return toVOs(List.of(feedbackMapper.selectById(id))).get(0);
    }

    /**
     * 我的反馈记录。
     */
    public IPage<FeedbackVO> pageMine(Long userId, long page, long size) {
        LambdaQueryWrapper<ContentFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentFeedback::getUserId, userId)
                .orderByDesc(ContentFeedback::getId);

        Page<ContentFeedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);
        Page<FeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(toVOs(result.getRecords()));
        return voPage;
    }

    private List<FeedbackVO> toVOs(List<ContentFeedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = feedbacks.stream()
                .flatMap(f -> java.util.stream.Stream.of(f.getUserId(), f.getHandlerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        return feedbacks.stream().map(feedback -> {
            FeedbackVO vo = toVO(feedback);
            vo.setUserName(names.get(feedback.getUserId()));
            vo.setHandlerName(names.get(feedback.getHandlerId()));
            return vo;
        }).toList();
    }

    private FeedbackVO toVO(ContentFeedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setTargetType(feedback.getTargetType());
        vo.setTargetId(feedback.getTargetId());
        vo.setTargetTitle(targetResolver.titleOf(feedback.getTargetType(), feedback.getTargetId()));
        vo.setFeedbackType(feedback.getFeedbackType());
        vo.setDescription(feedback.getDescription());
        vo.setStatus(feedback.getStatus());
        vo.setHandlerId(feedback.getHandlerId());
        vo.setHandleComment(feedback.getHandleComment());
        vo.setCreatedAt(feedback.getCreatedAt());
        vo.setHandledAt(feedback.getHandledAt());
        return vo;
    }
}
