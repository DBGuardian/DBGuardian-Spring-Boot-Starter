package com.erp.service.contract.impl;

import com.erp.entity.contract.ContractApprovalFlow;
import com.erp.mapper.contract.ContractApprovalFlowMapper;
import com.erp.service.contract.ContractApprovalFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 合同审批流服务实现
 */
@Slf4j
@Service
public class ContractApprovalFlowServiceImpl implements ContractApprovalFlowService {

    @Autowired
    private ContractApprovalFlowMapper contractApprovalFlowMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createContractCreationFlow(Integer contractId, Integer creatorId) {
        if (contractId == null || creatorId == null) {
            log.warn("创建审批流记录失败：参数为空，contractId={}, creatorId={}", contractId, creatorId);
            return;
        }

        // 检查是否已存在"合同创建"记录
        ContractApprovalFlow existingFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "合同创建");
        
        if (existingFlow != null) {
            log.info("合同审批流记录已存在，跳过创建：contractId={}, nodeName=合同创建", contractId);
            return;
        }

        // 创建审批流记录
        ContractApprovalFlow flow = new ContractApprovalFlow();
        flow.setContractId(contractId);
        flow.setNodeName("合同创建");
        flow.setApproverId(creatorId);
        flow.setApprovalResult("APPROVED"); // 合同创建即表示已完成
        flow.setApprovalTime(LocalDateTime.now());
        flow.setCreateTime(LocalDateTime.now());

        contractApprovalFlowMapper.insert(flow);
        log.info("创建合同审批流记录成功：contractId={}, nodeName=合同创建, approverId={}", 
                contractId, creatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToContractAuditFlow(Integer contractId, Integer approverId, String approvalOpinion) {
        if (contractId == null || approverId == null) {
            log.warn("更新审批流记录失败：参数为空，contractId={}, approverId={}", contractId, approverId);
            return;
        }

        // 检查是否已存在"合同审核"记录
        ContractApprovalFlow existingAuditFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "合同审核");
        
        if (existingAuditFlow != null) {
            // 如果已存在"合同审核"记录，更新它
            existingAuditFlow.setApproverId(approverId);
            existingAuditFlow.setApprovalResult("APPROVED");
            existingAuditFlow.setApprovalOpinion(approvalOpinion);
            existingAuditFlow.setApprovalTime(LocalDateTime.now());

            int rows = contractApprovalFlowMapper.updateById(existingAuditFlow);
            if (rows == 0) {
                log.warn("更新合同审批流失败（乐观锁冲突），contractId={}", contractId);
            }
            log.info("更新合同审核审批流记录成功：contractId={}, approverId={}", contractId, approverId);
        } else {
            // 创建新的"合同审核"记录（不更新"合同创建"记录，保持两个记录独立）
            ContractApprovalFlow newFlow = new ContractApprovalFlow();
            newFlow.setContractId(contractId);
            newFlow.setNodeName("合同审核");
            newFlow.setApproverId(approverId);
            newFlow.setApprovalResult("APPROVED");
            newFlow.setApprovalOpinion(approvalOpinion);
            newFlow.setApprovalTime(LocalDateTime.now());
            newFlow.setCreateTime(LocalDateTime.now());
            contractApprovalFlowMapper.insert(newFlow);
            log.info("创建合同审核审批流记录成功：contractId={}, approverId={}", contractId, approverId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPickupNoticeFlow(Integer contractId, Integer creatorId) {
        if (contractId == null || creatorId == null) {
            log.warn("创建收运通知审批流记录失败：参数为空，contractId={}, creatorId={}", contractId, creatorId);
            return;
        }

        // 检查是否已存在"收运通知"记录
        ContractApprovalFlow existingFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "收运通知");
        
        if (existingFlow != null) {
            log.info("收运通知审批流记录已存在，跳过创建：contractId={}, nodeName=收运通知", contractId);
            return;
        }

        // 创建审批流记录
        ContractApprovalFlow flow = new ContractApprovalFlow();
        flow.setContractId(contractId);
        flow.setNodeName("收运通知");
        flow.setApproverId(creatorId);
        flow.setApprovalResult("APPROVED"); // 收运通知创建即表示已完成
        flow.setApprovalTime(LocalDateTime.now());
        flow.setCreateTime(LocalDateTime.now());

        contractApprovalFlowMapper.insert(flow);
        log.info("创建收运通知审批流记录成功：contractId={}, nodeName=收运通知, creatorId={}", 
                contractId, creatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWarehousingFlow(Integer contractId, Integer auditorId) {
        if (contractId == null || auditorId == null) {
            log.warn("创建入库完成审批流记录失败：参数为空，contractId={}, auditorId={}", contractId, auditorId);
            return;
        }

        // 检查是否已存在"入库完成"记录
        ContractApprovalFlow existingFlow = contractApprovalFlowMapper.selectByContractIdAndNodeName(
                contractId, "入库完成");
        
        if (existingFlow != null) {
            log.info("入库完成审批流记录已存在，跳过创建：contractId={}, nodeName=入库完成", contractId);
            return;
        }

        // 创建审批流记录
        ContractApprovalFlow flow = new ContractApprovalFlow();
        flow.setContractId(contractId);
        flow.setNodeName("入库完成");
        flow.setApproverId(auditorId);
        flow.setApprovalResult("APPROVED"); // 入库单审核通过即表示入库完成
        flow.setApprovalTime(LocalDateTime.now());
        flow.setCreateTime(LocalDateTime.now());

        contractApprovalFlowMapper.insert(flow);
        log.info("创建入库完成审批流记录成功：contractId={}, nodeName=入库完成, auditorId={}", 
                contractId, auditorId);
    }
}
















