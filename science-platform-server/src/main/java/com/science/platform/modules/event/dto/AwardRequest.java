package com.science.platform.modules.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 确认优秀作品请求（管理员）。
 *
 * <p>只接受投稿 ID 与获奖等级，不接受「按分数自动排序」——
 * 开发档案 FR-EV-05 要求「不得把未评审作品自动标为优秀」，
 * 由管理员显式确认才能保证每个入选作品都经过人工评审。</p>
 */
@Data
public class AwardRequest {

    @NotEmpty(message = "请选择要确认的优秀作品")
    private List<Item> awards;

    @Data
    public static class Item {

        @jakarta.validation.constraints.NotNull(message = "请指定投稿 ID")
        private Long submissionId;

        @NotBlank(message = "请选择获奖等级")
        @Pattern(regexp = "FIRST|SECOND|THIRD|EXCELLENT",
                message = "获奖等级只能是一等奖、二等奖、三等奖或优秀奖")
        private String awardLevel;
    }
}
