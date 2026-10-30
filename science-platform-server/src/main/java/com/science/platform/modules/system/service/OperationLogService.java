package com.science.platform.modules.system.service;

import com.science.platform.modules.system.entity.SysOperationLog;
import com.science.platform.modules.system.mapper.SysOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 重要操作日志写入。
 *
 * <p>与业务写操作处在同一事务内，保证「操作成功即留痕」。</p>
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final SysOperationLogMapper operationLogMapper;

    public OperationLogService(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录一条操作日志。
     *
     * @param operatorId 操作人 ID，系统操作为 null
     * @param action     动作编码
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param detail     中文描述
     */
    public void record(Long operatorId, String action, String targetType, Long targetId, String detail) {
        SysOperationLog entity = new SysOperationLog();
        entity.setOperatorId(operatorId);
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setDetail(detail);
        entity.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(entity);

        log.debug("操作日志: {} {}#{} by {}", action, targetType, targetId, operatorId);
    }
}
