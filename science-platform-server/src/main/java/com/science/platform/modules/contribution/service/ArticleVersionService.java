package com.science.platform.modules.contribution.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.science.platform.common.exception.BusinessException;
import com.science.platform.modules.contribution.entity.ArticleVersion;
import com.science.platform.modules.contribution.mapper.ArticleVersionMapper;
import com.science.platform.modules.contribution.vo.ArticleVersionDetailVO;
import com.science.platform.modules.contribution.vo.ArticleVersionListItemVO;
import com.science.platform.modules.knowledge.entity.ScienceArticle;
import com.science.platform.modules.knowledge.mapper.ScienceArticleMapper;
import com.science.platform.modules.user.entity.SysUser;
import com.science.platform.modules.user.mapper.SysUserMapper;
import com.science.platform.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章历史版本查询。
 *
 * <p>版本列表按版本号倒序返回（最新在前），不包含文章当前版本本身——
 * 当前版本应该看文章详情，本表只存「已被覆盖的旧内容」。</p>
 */
@Service
public class ArticleVersionService {

    private static final Set<String> PRIVILEGED_ROLES =
            Set.of("ROLE_REVIEWER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    private final ArticleVersionMapper versionMapper;
    private final ScienceArticleMapper articleMapper;
    private final SysUserMapper sysUserMapper;

    public ArticleVersionService(ArticleVersionMapper versionMapper,
                                 ScienceArticleMapper articleMapper,
                                 SysUserMapper sysUserMapper) {
        this.versionMapper = versionMapper;
        this.articleMapper = articleMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询某篇文章的历史版本列表。
     *
     * @param articleId 文章 ID
     */
    public List<ArticleVersionListItemVO> listVersions(Long articleId) {
        ScienceArticle article = requireVisibleArticle(articleId);
        return listVersionsOf(article);
    }

    private List<ArticleVersionListItemVO> listVersionsOf(ScienceArticle article) {
        LambdaQueryWrapper<ArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleVersion::getArticleId, article.getId())
                .orderByDesc(ArticleVersion::getVersionNo);

        List<ArticleVersion> versions = versionMapper.selectList(wrapper);
        if (versions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> operatorIds = versions.stream()
                .map(ArticleVersion::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> operatorNames = operatorIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(operatorIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        return versions.stream().map(version -> {
            ArticleVersionListItemVO vo = new ArticleVersionListItemVO();
            vo.setId(version.getId());
            vo.setArticleId(version.getArticleId());
            vo.setVersionNo(version.getVersionNo());
            vo.setChangeNote(version.getChangeNote());
            vo.setOperatorId(version.getOperatorId());
            vo.setOperatorName(operatorNames.get(version.getOperatorId()));
            vo.setSourceContributionId(version.getSourceContributionId());
            vo.setCreatedAt(version.getCreatedAt());
            return vo;
        }).toList();
    }

    /**
     * 查询某个具体版本的内容快照。
     *
     * @param articleId 文章 ID
     * @param versionNo 版本号
     */
    public ArticleVersionDetailVO getVersion(Long articleId, Integer versionNo) {
        ScienceArticle article = requireVisibleArticle(articleId);

        LambdaQueryWrapper<ArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleVersion::getArticleId, articleId)
                .eq(ArticleVersion::getVersionNo, versionNo);
        ArticleVersion version = versionMapper.selectOne(wrapper);
        if (version == null) {
            throw BusinessException.notFound("该版本不存在");
        }

        ArticleVersionDetailVO vo = new ArticleVersionDetailVO();
        vo.setId(version.getId());
        vo.setArticleId(version.getArticleId());
        vo.setArticleTitle(article.getTitle());
        vo.setVersionNo(version.getVersionNo());
        vo.setTitleSnapshot(version.getTitleSnapshot());
        vo.setSummarySnapshot(version.getSummarySnapshot());
        vo.setContentSnapshot(version.getContentSnapshot());
        vo.setChangeNote(version.getChangeNote());
        vo.setOperatorId(version.getOperatorId());
        vo.setSourceContributionId(version.getSourceContributionId());
        vo.setCreatedAt(version.getCreatedAt());

        if (version.getOperatorId() != null) {
            SysUser operator = sysUserMapper.selectById(version.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getNickname());
            }
        }
        return vo;
    }

    /**
     * 校验文章可访问：已发布文章对所有人开放版本历史；
     * 未发布文章只对评审员及以上开放，避免草稿内容提前泄漏。
     */
    private ScienceArticle requireVisibleArticle(Long articleId) {
        ScienceArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        if ("PUBLISHED".equals(article.getStatus())) {
            return article;
        }
        if (!currentUserHasReviewPrivilege()) {
            throw BusinessException.notFound("文章不存在");
        }
        return article;
    }

    private boolean currentUserHasReviewPrivilege() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (PRIVILEGED_ROLES.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
