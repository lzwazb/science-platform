package com.science.platform.modules.event.vo;

import lombok.Data;

/**
 * 优秀作品展示对象。
 */
@Data
public class AwardVO {

    private Long submissionId;

    private Long workId;

    private String workTitle;

    private String workDescription;

    private String workCover;

    private String creatorName;

    /** FIRST / SECOND / THIRD / EXCELLENT */
    private String awardLevel;

    /** 平均分，便于答辩演示时展示评审依据 */
    private Double averageScore;
}
