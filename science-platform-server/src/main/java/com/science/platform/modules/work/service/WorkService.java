package com.science.platform.modules.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.knowledge.entity.ScienceCategory;
import com.science.platform.modules.knowledge.mapper.ScienceCategoryMapper;
import com.science.platform.modules.work.dto.WorkSaveRequest;
import com.science.platform.modules.work.entity.CreatorWork;
import com.science.platform.modules.work.mapper.CreatorWorkMapper;
import com.science.platform.modules.work.vo.WorkDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 创作者的作品写操作。
 *
 * <p>所有方法都要求传入「当前登录用户 ID」，并在修改前校验作品归属。
 * 这是开发档案第 3 章「不能修改他人作品」的落地点——权限判断在 Service，
 * 而不是靠前端隐藏按钮。</p>
 */
@Service
public class WorkService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_OFFLINE = "OFFLINE";

    private final CreatorWorkMapper workMapper;
    private final ScienceCategoryMapper categoryMapper;
    private final WorkQueryService workQueryService;

    public WorkService(CreatorWorkMapper workMapper,
                                             ScienceCategoryMapper categoryMapper,
                       WorkQueryService workQueryService) {
        this.workMapper = workMapper;
        this.categoryMapper = categoryMapper;
        this.workQueryService = workQueryService;
    }

    /**
     * 创建作品，初始状态为草稿。
     *
     * @param request   作品内容
     * @param creatorId 当前登录用户 ID
     */
    @Transactional
    public WorkDetailVO create(WorkSaveRequest request, Long creatorId) {
        validateCategory(request.getCategoryId());
        validateKnowledgeCategory(request.getKnowledgeCategoryId());

        CreatorWork work = new CreatorWork();
        work.setCreatorId(creatorId);
        work.setTitle(request.getTitle().trim());
        work.setDescription(request.getDescription());
        work.setContent(request.getContent());
        work.setCoverUrl(request.getCoverUrl());
        work.setCategoryId(request.getCategoryId());
        work.setKnowledgeCategoryId(request.getKnowledgeCategoryId());
        work.setStatus(STATUS_DRAFT);
        work.setViewCount(0);

        workMapper.insert(work);

        return workQueryService.getDetailAnyStatus(work.getId());
    }

    /**
     * 编辑作品。
     *
     * <p>状态规则（开发档案 5.2）：已发布的作品被编辑后自动回到待审核，
     * 避免未经审核的重大修改直接公开；驳回状态的作品编辑后仍为驳回，
     * 需要作者显式「提交审核」。</p>
     */
    @Transactional
    public WorkDetailVO update(Long id, WorkSaveRequest request, Long operatorId) {
        CreatorWork work = requireOwnedWork(id, operatorId);
        validateCategory(request.getCategoryId());
        validateKnowledgeCategory(request.getKnowledgeCategoryId());

        // 逐列 set，不用 updateById(entity)：
        // 整行回写会把读出来的旧 view_count 一起写回去（覆盖这期间的浏览量），
        // 且 NOT_NULL 策略会让「清空描述 / 清空封面」变成无效操作。
        LambdaUpdateWrapper<CreatorWork> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CreatorWork::getId, id)
                .set(CreatorWork::getTitle, request.getTitle().trim())
                .set(CreatorWork::getDescription, request.getDescription())
                .set(CreatorWork::getContent, request.getContent())
                .set(CreatorWork::getCoverUrl, request.getCoverUrl())
                .set(CreatorWork::getCategoryId, request.getCategoryId())
                .set(CreatorWork::getUpdatedAt, LocalDateTime.now());
        workMapper.update(null, wrapper);

        boolean needsReReview = STATUS_PUBLISHED.equals(work.getStatus());
        if (needsReReview) {
            // 重新送审：状态与上一轮的审核结论都要清空
            updateReviewFields(id, STATUS_PENDING, null, null, null);
        }

        return workQueryService.getDetailAnyStatus(id);
    }

    /**
     * 提交审核：草稿或被驳回的作品变为待审核。
     */
    @Transactional
    public WorkDetailVO submit(Long id, Long operatorId) {
        CreatorWork work = requireOwnedWork(id, operatorId);

        if (STATUS_PENDING.equals(work.getStatus())) {
            throw new BusinessException("作品已在审核中，请勿重复提交");
        }
        if (STATUS_PUBLISHED.equals(work.getStatus())) {
            throw new BusinessException("作品已发布，如需修改请编辑后重新提交审核");
        }

        updateReviewFields(id, STATUS_PENDING, null, null, null);
        return workQueryService.getDetailAnyStatus(id);
    }

    /**
     * 删除作品（逻辑删除）。
     *
     * <p>已被活动投稿引用的作品不允许删除，否则投稿记录会指向不存在的作品。</p>
     */
    @Transactional
    public void delete(Long id, Long operatorId) {
        requireOwnedWork(id, operatorId);

        long submissions = workMapper.countEventSubmissions(id);
        if (submissions > 0) {
            throw new BusinessException("该作品已被 " + submissions + " 个活动投稿引用，不能删除");
        }



        workMapper.deleteById(id);
    }

    /**
     * 更新作品所属的知识分类（作者本人操作）。
     *
     * <p>传 null 表示不关联任何学科，这是允许的：作品可以先发出来，之后再归类。</p>
     */
    @Transactional
    public WorkDetailVO updateKnowledgeCategory(Long id, Long knowledgeCategoryId, Long operatorId) {
        CreatorWork work = requireOwnedWork(id, operatorId);
        validateKnowledgeCategory(knowledgeCategoryId);

        LambdaUpdateWrapper<CreatorWork> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CreatorWork::getId, id)
                .set(CreatorWork::getKnowledgeCategoryId, knowledgeCategoryId)
                .set(CreatorWork::getUpdatedAt, LocalDateTime.now());
        workMapper.update(null, wrapper);

        // 已发布作品改归属同样需要重新送审，保持「公开内容必经过审核」的一致性
        if (STATUS_PUBLISHED.equals(work.getStatus())) {
            updateReviewFields(id, STATUS_PENDING, null, null, null);
        }

        return workQueryService.getDetailAnyStatus(id);
    }

    /**
     * 显式更新审核相关字段。
     *
     * <p>不能直接用 {@code updateById(entity)}：MyBatis-Plus 默认的字段策略是
     * {@code NOT_NULL}，实体里设为 null 的字段不会出现在 SET 子句里，
     * 导致「重新送审」时旧的审核意见清不掉。这里用 {@code LambdaUpdateWrapper}
     * 逐列 set，null 才会真正写成 SQL NULL。</p>
     */
    private void updateReviewFields(Long id, String status, Long reviewerId, String comment,
                                    LocalDateTime reviewedAt) {
        LambdaUpdateWrapper<CreatorWork> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CreatorWork::getId, id)
                .set(CreatorWork::getStatus, status)
                .set(CreatorWork::getReviewerId, reviewerId)
                .set(CreatorWork::getReviewComment, comment)
                .set(CreatorWork::getReviewedAt, reviewedAt)
                .set(CreatorWork::getUpdatedAt, LocalDateTime.now());
        workMapper.update(null, wrapper);
    }

    /**
     * 校验作品归属，返回作品实体。
     *
     * @throws BusinessException 作品不存在（404）或不属于当前用户（403）
     */
    private CreatorWork requireOwnedWork(Long id, Long operatorId) {
        CreatorWork work = workMapper.selectById(id);
        if (work == null) {
            throw BusinessException.notFound("作品不存在");
        }
        if (!Objects.equals(work.getCreatorId(), operatorId)) {
            throw BusinessException.forbidden("只能操作自己的作品");
        }
        return work;
    }

    /**
     * 校验所选的「知识分类」。
     *
     * <p>允许为空（作品不归属任何学科）；非空时必须是知识库分类（type = ARTICLE），
     * 不能拿作品形态分类（图文 / 海报 / 漫画 / 短视频）混进来。</p>
     */
    private void validateKnowledgeCategory(Long knowledgeCategoryId) {
        if (knowledgeCategoryId == null) {
            return;
        }
        ScienceCategory category = categoryMapper.selectById(knowledgeCategoryId);
        if (category == null) {
            throw new BusinessException("所选知识分类不存在");
        }
        if (!"ARTICLE".equals(category.getType())) {
            throw new BusinessException("请选择知识库中的学科分类");
        }
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        ScienceCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (!"WORK".equals(category.getType())) {
            throw new BusinessException("所选分类不是作品分类");
        }
    }

    /**
     * 供其他 Service 使用的空列表常量，避免重复创建。
     */
    static List<Long> emptyIds() {
        return Collections.emptyList();
    }
}
