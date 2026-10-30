package com.science.platform.modules.event;

import com.science.platform.common.ApiResponse;
import com.science.platform.modules.event.dto.SubmitWorkRequest;
import com.science.platform.modules.event.entity.EventSubmission;
import com.science.platform.modules.event.service.EventSubmissionService;
import com.science.platform.modules.event.vo.SubmissionListItemVO;
import com.science.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动投稿接口（创作者）。
 */
@RestController
@RequestMapping("/api/v1/events")
@PreAuthorize("hasAnyRole('CREATOR', 'ADMIN', 'SUPER_ADMIN')")
public class EventSubmissionController {

    private final EventSubmissionService submissionService;

    public EventSubmissionController(EventSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * 提交作品参加活动。
     */
    @PostMapping("/{id}/submissions")
    public ApiResponse<SubmissionListItemVO> submit(@PathVariable Long id,
                                                    @Valid @RequestBody SubmitWorkRequest request) {
        EventSubmission submission = submissionService.submit(
                id, request.getWorkId(), SecurityUtils.currentUserId());

        List<SubmissionListItemVO> items = submissionService.toListItems(
                List.of(submission), null);
        return ApiResponse.success("投稿成功，等待评审", items.isEmpty() ? null : items.get(0));
    }

    /**
     * 我在某个活动中的投稿。
     */
    @GetMapping("/{id}/submissions/mine")
    public ApiResponse<List<SubmissionListItemVO>> mine(@PathVariable Long id) {
        return ApiResponse.success(submissionService.listMine(
                SecurityUtils.currentUserId(), id, null));
    }

    /**
     * 我的全部投稿（跨活动）。
     */
    @GetMapping("/submissions/mine")
    public ApiResponse<List<SubmissionListItemVO>> mineAll() {
        return ApiResponse.success(submissionService.listMine(
                SecurityUtils.currentUserId(), null, null));
    }
}
