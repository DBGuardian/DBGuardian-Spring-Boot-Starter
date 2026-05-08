package com.erp.service.oa.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.entity.oa.OaApprovalRecord;
import com.erp.mapper.oa.OaApprovalRecordMapper;
import com.erp.service.oa.OaApprovalRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * OA审核记录服务实现
 *
 * @author ERP System
 * @date 2026-04-06
 */
@Slf4j
@Service
public class OaApprovalRecordServiceImpl implements OaApprovalRecordService {

    @Autowired
    private OaApprovalRecordMapper oaApprovalRecordMapper;

    private static final DateTimeFormatter APPROVAL_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public OaApprovalStatistics getStatistics(String viewScope, Integer submitterId, Integer approverId) {
        OaApprovalStatistics statistics = new OaApprovalStatistics();

        // 总数量
        Long totalCount = oaApprovalRecordMapper.selectCount(null, null, null, null, null);
        statistics.setTotalCount(totalCount);

        // 待审核数量
        Long pendingCount = oaApprovalRecordMapper.selectCount("pending", null, approverId, "待审核", null);
        statistics.setPendingCount(pendingCount);

        // 我发起的数量
        Long submittedCount = oaApprovalRecordMapper.selectCount("submitted", submitterId, null, null, null);
        statistics.setSubmittedCount(submittedCount);

        // 已通过数量
        Long passedCount = oaApprovalRecordMapper.selectCount(null, null, null, "已通过", null);
        statistics.setPassedCount(passedCount);

        // 已驳回数量
        Long rejectedCount = oaApprovalRecordMapper.selectCount(null, null, null, "已驳回", null);
        statistics.setRejectedCount(rejectedCount);

        // 已撤回数量
        Long withdrawnCount = oaApprovalRecordMapper.selectCount(null, null, null, "已撤回", null);
        statistics.setWithdrawnCount(withdrawnCount);

        // 超时未审数量（超过3天未审核）
        Long longPendingCount = oaApprovalRecordMapper.selectCount(null, null, null, "待审核", LocalDateTime.now());
        statistics.setLongPendingCount(longPendingCount);

        return statistics;
    }

    @Override
    public IPage<OaApprovalRecord> getApprovalPage(
            Integer page,
            Integer pageSize,
            String keyword,
            String businessType,
            String approvalStatus,
            String viewScope,
            Integer submitterId,
            Integer approverId,
            String startDate,
            String endDate,
            Integer unapprovedDays
    ) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        Page<OaApprovalRecord> pageParam = new Page<>(page, pageSize);
        return oaApprovalRecordMapper.selectPageList(
                pageParam,
                keyword,
                businessType,
                approvalStatus,
                viewScope,
                submitterId,
                approverId,
                startDate,
                endDate,
                unapprovedDays
        );
    }

    @Override
    public OaApprovalRecord getApprovalDetail(Integer approvalRecordId) {
        if (approvalRecordId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核记录ID不能为空");
        }

        OaApprovalRecord record = oaApprovalRecordMapper.selectById(approvalRecordId);
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "审核记录不存在");
        }

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaApprovalRecord approve(
            Integer approvalRecordId,
            String sourceTable,
            Integer sourceId,
            String result,
            String opinion,
            Integer approverId,
            String approverName
    ) {
        if (approvalRecordId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核记录ID不能为空");
        }

        OaApprovalRecord record = oaApprovalRecordMapper.selectById(approvalRecordId);
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "审核记录不存在");
        }

        if (!"待审核".equals(record.getApprovalStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "该记录已审核，无法重复审核");
        }

        // 更新审核状态
        String newStatus = "通过".equals(result) ? "已通过" : "已驳回";
        record.setApprovalStatus(newStatus);
        record.setApprovalTime(LocalDateTime.now());
        record.setApproverId(approverId);
        record.setApproverName(approverName);

        oaApprovalRecordMapper.updateById(record);

        log.info("OA审核操作完成：approvalRecordId={}, result={}, approverId={}", approvalRecordId, result, approverId);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaApprovalRecord submit(
            String sourceTable,
            Integer sourceId,
            String sourceTableName,
            String sourceNo,
            String title,
            Integer submitterId,
            String submitterName
    ) {
        if (!StringUtils.hasText(sourceTable)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "来源表名不能为空");
        }
        if (sourceId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "来源记录ID不能为空");
        }

        // 检查是否已有待审核的记录
        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaApprovalRecord::getSourceTable, sourceTable)
                .eq(OaApprovalRecord::getSourceId, sourceId)
                .eq(OaApprovalRecord::getApprovalStatus, "待审核")
                .eq(OaApprovalRecord::getDeleted, 0);

        List<OaApprovalRecord> existingRecords = oaApprovalRecordMapper.selectList(wrapper);
        if (!existingRecords.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "该记录已在审核中，请勿重复提交");
        }

        // 生成审核编号
        String approvalNo = generateApprovalNo();

        // 获取当前审核次数
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaApprovalRecord::getSourceTable, sourceTable)
                .eq(OaApprovalRecord::getSourceId, sourceId)
                .eq(OaApprovalRecord::getDeleted, 0)
                .orderByDesc(OaApprovalRecord::getApprovalCount);
        List<OaApprovalRecord> allRecords = oaApprovalRecordMapper.selectList(wrapper);
        int approvalCount = allRecords.isEmpty() ? 1 : allRecords.get(0).getApprovalCount() + 1;

        // 创建新的审核记录
        OaApprovalRecord record = new OaApprovalRecord();
        record.setApprovalNo(approvalNo);
        record.setSourceTable(sourceTable);
        record.setSourceTableName(sourceTableName);
        record.setSourceId(sourceId);
        record.setSourceNo(sourceNo != null ? sourceNo : "");
        record.setTitle(title != null ? title : sourceTableName + "审核");
        record.setSubmitterId(submitterId);
        record.setSubmitterName(submitterName);
        record.setApprovalStatus("待审核");
        record.setApprovalCount(approvalCount);
        record.setSubmitTime(LocalDateTime.now());
        record.setDeleted(0);

        oaApprovalRecordMapper.insert(record);

        log.info("OA审核提交成功：approvalNo={}, sourceTable={}, sourceId={}, submitterId={}",
                approvalNo, sourceTable, sourceId, submitterId);

        return record;
    }

    @Override
    public OaApprovalRecord findPendingBySource(String sourceTable, Integer sourceId) {
        if (!StringUtils.hasText(sourceTable) || sourceId == null) {
            return null;
        }

        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaApprovalRecord::getSourceTable, sourceTable)
                .eq(OaApprovalRecord::getSourceId, sourceId)
                .eq(OaApprovalRecord::getApprovalStatus, "待审核")
                .eq(OaApprovalRecord::getDeleted, 0)
                .orderByDesc(OaApprovalRecord::getApprovalRecordId)
                .last("LIMIT 1");

        List<OaApprovalRecord> records = oaApprovalRecordMapper.selectList(wrapper);
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    public OaApprovalRecord findLatestBySource(String sourceTable, Integer sourceId) {
        if (!StringUtils.hasText(sourceTable) || sourceId == null) {
            return null;
        }

        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaApprovalRecord::getSourceTable, sourceTable)
                .eq(OaApprovalRecord::getSourceId, sourceId)
                .eq(OaApprovalRecord::getDeleted, 0)
                .orderByDesc(OaApprovalRecord::getApprovalCount)
                .last("LIMIT 1");

        List<OaApprovalRecord> records = oaApprovalRecordMapper.selectList(wrapper);
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaApprovalRecord reactivateRejectedRecord(
            String sourceTable,
            Integer sourceId,
            Integer submitterId,
            String submitterName
    ) {
        if (!StringUtils.hasText(sourceTable) || sourceId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "来源表名或来源记录ID不能为空");
        }

        // 查询最新的OA审批记录
        OaApprovalRecord record = findLatestBySource(sourceTable, sourceId);
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "未找到对应的OA审批记录");
        }

        // 支持已驳回和已撤回状态的重新激活
        if (!"已驳回".equals(record.getApprovalStatus()) && !"已撤回".equals(record.getApprovalStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "只有已驳回或已撤回的记录才能重新提交，当前状态为：" + record.getApprovalStatus());
        }

        // 检查是否已有待审核的记录（防止重复提交）
        OaApprovalRecord pendingRecord = findPendingBySource(sourceTable, sourceId);
        if (pendingRecord != null) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "该记录已在审核中，请勿重复提交");
        }

        // 重新激活：审核次数+1，状态改为待审核，提交时间更新，提交人信息更新
        record.setApprovalCount(record.getApprovalCount() + 1);
        record.setApprovalStatus("待审核");
        record.setSubmitTime(LocalDateTime.now());
        record.setSubmitterId(submitterId);
        record.setSubmitterName(submitterName);
        record.setApproverId(null);
        record.setApproverName(null);
        record.setApprovalTime(null);

        oaApprovalRecordMapper.updateById(record);

        log.info("OA审批记录重新激活成功：approvalRecordId={}, sourceTable={}, sourceId={}, newApprovalCount={}, submitterId={}",
                record.getApprovalRecordId(), sourceTable, sourceId, record.getApprovalCount(), submitterId);

        return record;
    }

    @Override
    public OaApprovalRecord findWithdrawnBySource(String sourceTable, Integer sourceId) {
        if (!StringUtils.hasText(sourceTable) || sourceId == null) {
            return null;
        }
        return oaApprovalRecordMapper.selectOne(
                new LambdaQueryWrapper<OaApprovalRecord>()
                        .eq(OaApprovalRecord::getSourceTable, sourceTable)
                        .eq(OaApprovalRecord::getSourceId, sourceId)
                        .eq(OaApprovalRecord::getApprovalStatus, "已撤回")
                        .orderByDesc(OaApprovalRecord::getApprovalCount)
                        .last("LIMIT 1")
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaApprovalRecord reactivateWithdrawnRecord(
            String sourceTable,
            Integer sourceId,
            Integer submitterId,
            String submitterName
    ) {
        if (!StringUtils.hasText(sourceTable) || sourceId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "来源表名或来源记录ID不能为空");
        }

        // 查询已撤回状态的OA审批记录
        OaApprovalRecord record = findWithdrawnBySource(sourceTable, sourceId);
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "未找到已撤回的OA审批记录");
        }

        // 检查是否已有待审核的记录（防止重复提交）
        OaApprovalRecord pendingRecord = findPendingBySource(sourceTable, sourceId);
        if (pendingRecord != null) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "该记录已在审核中，请勿重复提交");
        }

        // 重新激活：审核次数+1，状态改为待审核，提交时间更新，提交人信息更新
        record.setApprovalCount(record.getApprovalCount() + 1);
        record.setApprovalStatus("待审核");
        record.setSubmitTime(LocalDateTime.now());
        record.setSubmitterId(submitterId);
        record.setSubmitterName(submitterName);
        record.setApproverId(null);
        record.setApproverName(null);
        record.setApprovalTime(null);

        oaApprovalRecordMapper.updateById(record);

        log.info("OA审批记录重新激活（已撤回）：approvalRecordId={}, sourceTable={}, sourceId={}, newApprovalCount={}, submitterId={}",
                record.getApprovalRecordId(), sourceTable, sourceId, record.getApprovalCount(), submitterId);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OaApprovalRecord resubmit(
            Integer originalRecordId,
            String sourceTable,
            Integer sourceId,
            Integer submitterId,
            String submitterName
    ) {
        if (originalRecordId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "原审核记录ID不能为空");
        }

        OaApprovalRecord originalRecord = oaApprovalRecordMapper.selectById(originalRecordId);
        if (originalRecord == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "原审核记录不存在");
        }

        if (!"已驳回".equals(originalRecord.getApprovalStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "只有已驳回的记录才能重新提交");
        }

        // 提交新的审核记录（审核次数+1）
        return submit(
                sourceTable,
                sourceId,
                originalRecord.getSourceTableName(),
                originalRecord.getSourceNo(),
                originalRecord.getTitle(),
                submitterId,
                submitterName
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer approvalRecordId, String sourceTable, Integer sourceId, Integer operatorId, String reason) {
        if (approvalRecordId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "审核记录ID不能为空");
        }

        OaApprovalRecord record = oaApprovalRecordMapper.selectById(approvalRecordId);
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "审核记录不存在");
        }

        // 验证状态：只有待审核状态的记录才能撤回
        if (!"待审核".equals(record.getApprovalStatus())) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(),
                    "只有待审核状态的记录才能撤回，当前状态为：" + record.getApprovalStatus());
        }

        // 保留该记录，审核次数减1后最小为0，状态改为已撤回
        Integer currentApprovalCount = record.getApprovalCount() == null ? 0 : record.getApprovalCount();
        record.setApprovalCount(Math.max(currentApprovalCount - 1, 0));
        record.setApprovalStatus("已撤回");
        record.setApprovalTime(LocalDateTime.now());
        oaApprovalRecordMapper.updateById(record);
        log.info("OA审批记录撤回：approvalRecordId={}, sourceTable={}, sourceId={}, operatorId={}, newApprovalCount={}, reason={}",
                approvalRecordId, sourceTable, sourceId, operatorId, record.getApprovalCount(), reason);
    }

    /**
     * 生成审核编号
     * 格式：OA-YYYYMMDD-XXXX
     */
    private String generateApprovalNo() {
        String dateStr = LocalDateTime.now().format(APPROVAL_NO_DATE_FORMAT);

        // 查询当天最大的序号
        LambdaQueryWrapper<OaApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(OaApprovalRecord::getApprovalNo, "OA-" + dateStr)
                .orderByDesc(OaApprovalRecord::getApprovalRecordId)
                .last("LIMIT 1");

        OaApprovalRecord lastRecord = oaApprovalRecordMapper.selectOne(wrapper);
        int sequence = 1;

        if (lastRecord != null && lastRecord.getApprovalNo() != null) {
            String lastNo = lastRecord.getApprovalNo();
            String[] parts = lastNo.split("-");
            if (parts.length == 3) {
                try {
                    sequence = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                    // 解析失败，使用默认序号
                }
            }
        }

        return String.format("OA-%s-%04d", dateStr, sequence);
    }
}
