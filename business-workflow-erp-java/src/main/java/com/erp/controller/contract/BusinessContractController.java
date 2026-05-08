package com.erp.controller.contract;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.BusinessContractPageRequest;
import com.erp.controller.contract.dto.BusinessContractPageResponse;
import com.erp.service.contract.BusinessContractService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 业务合作合同控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/business-contract")
@Api(tags = "业务合作合同管理")
public class BusinessContractController {

    @Autowired
    private BusinessContractService businessContractService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 查询各状态合同数量统计
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "查询业务合同各状态统计数量")
    public Result<java.util.Map<String, Long>> getStatistics() {
        try {
            return Result.success("查询成功", businessContractService.getStatistics());
        } catch (Exception e) {
            log.error("查询业务合同统计数量失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据危废合同ID查询关联业务合同
     */
    @GetMapping("/by-hazardous-contract/{hazardousContractId}")
    @ApiOperation(value = "根据危废合同ID查询关联业务合同",
            notes = "危废合同与业务合同一对一，若不存在则返回空")
    public Result<com.erp.controller.contract.dto.BusinessContractDetailResponse> getByHazardousContractId(
            @PathVariable Integer hazardousContractId) {
        try {
            com.erp.controller.contract.dto.BusinessContractDetailResponse detail =
                    businessContractService.getByHazardousContractId(hazardousContractId);
            return Result.success("查询成功", detail);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("根据危废合同ID查询关联业务合同失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 搜索汇款用业务合同选项（专用接口，一次返回含收款卡的完整数据）
     */
    @GetMapping("/remittance-options")
    @ApiOperation(value = "搜索汇款用业务合同选项",
            notes = "按关键词模糊匹配合同单号/甲方名称/业务员姓名，仅返回执行中合同，携带完整收款卡信息，支持viewScope参数")
    public Result<java.util.List<com.erp.controller.contract.dto.BusinessContractRemittanceOptionDTO>> searchRemittanceOptions(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String viewScope) {
        try {
            return Result.success("查询成功", businessContractService.searchRemittanceOptions(keyword, viewScope));
        } catch (Exception e) {
            log.error("搜索汇款用业务合同选项失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 业务费结算专用合同列表查询（不分页）
     * 返回状态为"执行中"或"已完结"的业务合同，包含BUSINESS_CONTRACT的合同编号、合同单号、业务员姓名
     * 以及JOIN CONTRACT获取的合同编号、合同号、甲方名称
     */
    @GetMapping("/settlement-list")
    @ApiOperation(value = "业务费结算专用合同列表",
            notes = "查询执行中和已完结的业务合同，返回业务合同编号、合同单号、业务员姓名及关联合同编号、合同号、甲方名称，不分页")
    public Result<com.erp.controller.contract.dto.BusinessSettlementContractListResponse> getSettlementList() {
        try {
            return Result.success("查询成功", businessContractService.getSettlementList());
        } catch (Exception e) {
            log.error("业务费结算合同列表查询失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询业务合作合同列表
     */
    @RequirePagePermission("合同管理:业务合作合同:业务合同:页面")
    @GetMapping("/list")
    @ApiOperation(value = "业务合作合同分页查询",
            notes = "支持按合同单号、业务员姓名、甲方名称、合同状态、创建时间范围筛选，支持viewScope数据范围过滤")
    public Result<IPage<BusinessContractPageResponse>> getPage(@Valid BusinessContractPageRequest request) {
        try {
            IPage<BusinessContractPageResponse> page = businessContractService.getPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询业务合作合同列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增业务合作合同（支持上传合同文件）
     */
    @RequirePagePermission("合同管理:业务合作合同:业务合同:页面")
    @PostMapping(consumes = {"multipart/form-data"})
    @ApiOperation(value = "新增业务合作合同", notes = "创建业务合作合同，可同时上传合同扫描件")
    public Result<java.util.Map<String, Object>> create(
            @Valid @RequestPart("contract") com.erp.controller.contract.dto.BusinessContractCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Integer contractId = businessContractService.create(request, file);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("contractId", contractId);
            logRecordService.recordOperationLog("业务合作合同", "新增",
                    "新增业务合作合同，合同ID=" + contractId + "，甲方=" + request.getPartyAName(),
                    userId, ipAddress, true, null);
            return Result.success("新增合同成功", data);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务合作合同", "新增",
                    "新增业务合作合同失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增业务合作合同失败", e);
            logRecordService.recordOperationLog("业务合作合同", "新增",
                    "新增业务合作合同失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询业务合作合同详情
     */
    @GetMapping("/{contractId}")
    @ApiOperation(value = "查询业务合作合同详情", notes = "支持viewScope数据范围校验")
    public Result<com.erp.controller.contract.dto.BusinessContractDetailResponse> getDetail(
            @PathVariable Integer contractId) {
        try {
            com.erp.controller.contract.dto.BusinessContractDetailResponse detail =
                    businessContractService.getDetail(contractId);
            return Result.success("查询成功", detail);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询业务合作合同详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新业务合作合同（支持重新上传合同文件）
     */
    @RequirePagePermission("合同管理:业务合作合同:业务合同:页面")
    @PostMapping(value = "/{contractId}/update", consumes = {"multipart/form-data"})
    @ApiOperation(value = "更新业务合作合同", notes = "更新合同信息，可同时更新合同扫描件，支持operateScope操作范围校验")
    public Result<Void> update(
            @PathVariable Integer contractId,
            @Valid @RequestPart("contract") com.erp.controller.contract.dto.BusinessContractCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            businessContractService.update(contractId, request, file);
            logRecordService.recordOperationLog("业务合作合同", "更新",
                    "更新业务合作合同，合同ID=" + contractId + "，甲方=" + request.getPartyAName(),
                    userId, ipAddress, true, null);
            return Result.success("更新合同成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务合作合同", "更新",
                    "更新业务合作合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新业务合作合同失败", e);
            logRecordService.recordOperationLog("业务合作合同", "更新",
                    "更新业务合作合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    /**
     * 更新合同状态
     */
    @RequirePagePermission("合同管理:业务合作合同:业务合同:页面")
    @PostMapping("/{contractId}/status")
    @ApiOperation(value = "更新业务合作合同状态", notes = "支持operateScope操作范围校验")
    public Result<Void> updateStatus(
            @PathVariable Integer contractId,
            @RequestBody com.erp.controller.contract.dto.BusinessContractStatusRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            businessContractService.updateStatus(contractId, request);
            String actionType = "审核中".equals(request.getStatus()) ? "提交审核" :
                    "执行中".equals(request.getStatus()) ? "审核通过" :
                    "已驳回".equals(request.getStatus()) ? "审核拒绝" :
                    "已完结".equals(request.getStatus()) ? "标记完结" :
                    "已归档".equals(request.getStatus()) ? "归档" : "状态更新";
            logRecordService.recordOperationLog("业务合作合同", actionType,
                    actionType + "，合同ID=" + contractId + "，新状态=" + request.getStatus() +
                            (request.getAuditOpinion() != null ? "，审核意见=" + request.getAuditOpinion() : ""),
                    userId, ipAddress, true, null);
            return Result.success("状态更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务合作合同", "状态更新",
                    "更新合同状态失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新业务合作合同状态失败", e);
            logRecordService.recordOperationLog("业务合作合同", "状态更新",
                    "更新合同状态失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 逻辑删除合同（仅待审核状态可删除）
     */
    @RequirePagePermission("合同管理:业务合作合同:业务合同:页面")
    @PostMapping("/{contractId}/delete")
    @ApiOperation(value = "删除业务合作合同", notes = "仅待审核状态的合同可删除，支持operateScope操作范围校验")
    public Result<Void> delete(@PathVariable Integer contractId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            businessContractService.delete(contractId);
            logRecordService.recordOperationLog("业务合作合同", "删除",
                    "删除业务合作合同，合同ID=" + contractId,
                    userId, ipAddress, true, null);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务合作合同", "删除",
                    "删除业务合作合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除业务合作合同失败", e);
            logRecordService.recordOperationLog("业务合作合同", "删除",
                    "删除业务合作合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新合同状态
     * 支持批量提交审核（待审核/已驳回 → 审核中）、批量撤回（审核中 → 待审核）
     */
    @RequirePagePermission("合同管理:业务合作合同:业务合同:页面")
    @PostMapping("/batch/status")
    @ApiOperation(value = "批量更新业务合作合同状态", notes = "支持operateScope操作范围校验")
    public Result<com.erp.service.contract.dto.BusinessContractBatchUpdateResponse> batchUpdateStatus(
            @RequestBody com.erp.controller.contract.dto.BusinessContractStatusRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            if (request.getContractIds() == null || request.getContractIds().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "合同ID列表不能为空");
            }
            if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "目标状态不能为空");
            }
            com.erp.service.contract.dto.BusinessContractBatchUpdateResponse result =
                    businessContractService.batchUpdateStatus(request.getContractIds(), request.getStatus());

            String actionType = "审核中".equals(request.getStatus()) ? "批量提交审核" :
                    "待审核".equals(request.getStatus()) ? "批量撤回" : "批量状态更新";
            logRecordService.recordOperationLog("业务合作合同", actionType,
                    actionType + "，目标状态=" + request.getStatus() +
                            "，成功=" + result.getSuccessIds().size() + "，失败=" + result.getFailedIds().size(),
                    userId, ipAddress, result.getAllSuccess(), null);

            return Result.success(actionType + "完成", result);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("业务合作合同", "批量状态更新",
                    "批量状态更新失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量更新业务合作合同状态失败", e);
            logRecordService.recordOperationLog("业务合作合同", "批量状态更新",
                    "批量状态更新失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量操作失败：" + e.getMessage());
        }
    }
}
