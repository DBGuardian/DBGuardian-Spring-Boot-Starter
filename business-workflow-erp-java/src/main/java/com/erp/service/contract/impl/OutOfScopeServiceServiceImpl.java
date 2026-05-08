package com.erp.service.contract.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO;
import com.erp.entity.contract.OutOfScopeService;
import com.erp.mapper.contract.OutOfScopeServiceMapper;
import com.erp.service.contract.OutOfScopeServiceService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 价外服务管理服务实现类
 */
@Slf4j
@Service
public class OutOfScopeServiceServiceImpl implements OutOfScopeServiceService {

    @Autowired
    private OutOfScopeServiceMapper outOfScopeServiceMapper;

    @Autowired
    private ILogRecordService logRecordService;

    private static final String BUSINESS_TYPE_QUOTATION = "QUOTATION";
    private static final String BUSINESS_TYPE_CONTRACT = "CONTRACT";

    @Override
    public List<OutOfScopeService> listByQuotation(Integer quotationId) {
        QueryWrapper<OutOfScopeService> wrapper = new QueryWrapper<>();
        wrapper.eq("关联业务类型", BUSINESS_TYPE_QUOTATION).eq("关联业务单号", quotationId);
        return outOfScopeServiceMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OutOfScopeService> createForQuotation(Integer quotationId, List<OutOfScopeServiceCreateDTO> services, Integer createdBy) {
        if (services == null || services.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请求体不能为空");
        }

        List<OutOfScopeService> created = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (OutOfScopeServiceCreateDTO dto : services) {
            OutOfScopeService entity = new OutOfScopeService();
            entity.setBusinessType(BUSINESS_TYPE_QUOTATION);
            entity.setBusinessId(quotationId);
            String projectValue = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
            entity.setProject(projectValue != null ? projectValue : "");
            entity.setSpec(dto.getSpec());
            entity.setUnit(dto.getUnit());
            entity.setPlannedQuantity(dto.getPlannedQuantity());
            entity.setContractUnitPrice(dto.getContractUnitPrice());
            entity.setStatus("ACTIVE");
            entity.setCreatedAt(now);
            entity.setCreatedBy(createdBy);

            outOfScopeServiceMapper.insert(entity);
            created.add(entity);

            // 记录数据变更日志（新增）
            logRecordService.recordDataChangeLog("价外服务", "OUT_OF_SCOPE_SERVICE", String.valueOf(entity.getOutOfScopeServiceId()),
                    "新增", "为报价单新增价外服务，报价单ID=" + quotationId,
                    null, entity, null, null, true, null);
        }

        return created;
    }

    @Override
    public List<OutOfScopeService> listByContract(Integer contractId) {
        QueryWrapper<OutOfScopeService> wrapper = new QueryWrapper<>();
        wrapper.eq("关联业务类型", BUSINESS_TYPE_CONTRACT).eq("关联业务单号", contractId);
        return outOfScopeServiceMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OutOfScopeService> createForContract(Integer contractId, List<OutOfScopeServiceCreateDTO> services, Integer createdBy) {
        if (services == null || services.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请求体不能为空");
        }

        List<OutOfScopeService> created = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (OutOfScopeServiceCreateDTO dto : services) {
            OutOfScopeService entity = new OutOfScopeService();
            entity.setBusinessType(BUSINESS_TYPE_CONTRACT);
            entity.setBusinessId(contractId);
            String projectValue = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
            entity.setProject(projectValue != null ? projectValue : "");
            entity.setSpec(dto.getSpec());
            entity.setUnit(dto.getUnit());
            entity.setPlannedQuantity(dto.getPlannedQuantity());
            entity.setContractUnitPrice(dto.getContractUnitPrice());
            entity.setStatus("ACTIVE");
            entity.setCreatedAt(now);
            entity.setCreatedBy(createdBy);

            outOfScopeServiceMapper.insert(entity);
            created.add(entity);

            // 记录数据变更日志（新增）
            logRecordService.recordDataChangeLog("价外服务", "OUT_OF_SCOPE_SERVICE", String.valueOf(entity.getOutOfScopeServiceId()),
                    "新增", "为合同新增价外服务，合同ID=" + contractId,
                    null, entity, null, null, true, null);
        }

        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutOfScopeService update(Integer id, OutOfScopeServiceCreateDTO dto, Integer updatedBy) {
        OutOfScopeService existing = outOfScopeServiceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "价外服务不存在");
        }

        String updatedProject = dto.getProject() != null && !dto.getProject().isEmpty() ? dto.getProject() : dto.getServiceType();
        existing.setProject(updatedProject != null ? updatedProject : existing.getProject());
        existing.setSpec(dto.getSpec() != null ? dto.getSpec() : existing.getSpec());
        existing.setUnit(dto.getUnit() != null ? dto.getUnit() : existing.getUnit());
        existing.setPlannedQuantity(dto.getPlannedQuantity() != null ? dto.getPlannedQuantity() : existing.getPlannedQuantity());
        existing.setContractUnitPrice(dto.getContractUnitPrice() != null ? dto.getContractUnitPrice() : existing.getContractUnitPrice());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);

        outOfScopeServiceMapper.updateById(existing);

        // 获取更新后的数据并记录数据变更日志
        OutOfScopeService afterUpdate = outOfScopeServiceMapper.selectById(id);
        logRecordService.recordDataChangeLog("价外服务", "OUT_OF_SCOPE_SERVICE", String.valueOf(id),
                "更新", "更新价外服务，项目=" + existing.getProject(),
                existing, afterUpdate, null, null, true, null);

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        // 获取删除前的数据
        OutOfScopeService existing = outOfScopeServiceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "价外服务不存在");
        }

        int deleted = outOfScopeServiceMapper.deleteById(id);
        if (deleted == 0) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "价外服务不存在");
        }

        // 记录数据变更日志（删除）
        logRecordService.recordDataChangeLog("价外服务", "OUT_OF_SCOPE_SERVICE", String.valueOf(id),
                "删除", "删除价外服务，项目=" + existing.getProject(),
                existing, null, null, null, true, null);
    }
}
