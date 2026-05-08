package com.erp.controller.transport;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.aspect.RateLimit;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.transport.dto.*;
import com.erp.service.transport.TransportContractService;
import com.erp.service.transport.TransportContractVehicleService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 运输合同控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/transport-contract")
@Api(tags = "运输合同管理")
public class TransportContractController {

    @Autowired
    private TransportContractService transportContractService;

    @Autowired
    private TransportContractVehicleService transportContractVehicleService;

    @Autowired
    private ILogRecordService logRecordService;

    /**
     * 批量更新运输合同状态
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @PostMapping("/batch/status")
    @ApiOperation(value = "批量更新运输合同状态", notes = "支持operateScope操作范围校验")
    public Result<TransportContractBatchStatusResponse> batchUpdateStatus(
            @Valid @RequestBody TransportContractBatchStatusRequest request,
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
            TransportContractBatchStatusResponse result = transportContractService.batchUpdateStatus(request.getContractIds(), request.getStatus());

            String actionType = "审核中".equals(request.getStatus()) ? "批量提交审核" :
                    "待审核".equals(request.getStatus()) ? "批量撤回" : "批量状态更新";
            logRecordService.recordOperationLog("委外运输合同", actionType,
                    actionType + "，目标状态=" + request.getStatus() +
                            "，成功=" + result.getSuccessIds().size() + "，失败=" + result.getFailedIds().size(),
                    userId, ipAddress, result.isAllSuccess(), null);

            return Result.success(actionType + "完成", result);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("委外运输合同", "批量状态更新",
                    "批量状态更新失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量更新委外运输合同状态失败", e);
            logRecordService.recordOperationLog("委外运输合同", "批量状态更新",
                    "批量状态更新失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量操作失败：" + e.getMessage());
        }
    }

    /**
     * 查询各状态合同数量统计
     */
    @GetMapping("/statistics")
    @RateLimit(key = "transport:statistics", permitsPerSecond = 50, message = "查询过于频繁，请稍后再试")
    @ApiOperation(value = "查询运输合同各状态统计数量")
    public Result<java.util.Map<String, Long>> getStatistics() {
        try {
            return Result.success("查询成功", transportContractService.getStatistics());
        } catch (Exception e) {
            log.error("查询运输合同统计数量失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询运输合同列表
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @GetMapping("/list")
    @RateLimit(key = "transport:list", permitsPerSecond = 100, message = "查询过于频繁，请稍后再试")
    @ApiOperation(value = "运输合同分页查询", notes = "支持viewScope数据范围过滤")
    public Result<IPage<TransportContractPageResponse>> getPage(@Valid TransportContractPageRequest request) {
        try {
            IPage<TransportContractPageResponse> page = transportContractService.getPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询运输合同列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询运输合同详情
     */
    @GetMapping("/{contractId}")
    @ApiOperation(value = "查询运输合同详情", notes = "支持viewScope数据范围校验")
    public Result<TransportContractDetailResponse> getDetail(@PathVariable Integer contractId) {
        try {
            TransportContractDetailResponse detail = transportContractService.getDetail(contractId);
            return Result.success("查询成功", detail);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询运输合同详情失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增运输合同
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @PostMapping
    @ApiOperation(value = "新增运输合同")
    public Result<java.util.Map<String, Object>> create(
            @Valid @RequestBody TransportContractSaveRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Integer contractId = transportContractService.create(request);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("contractId", contractId);
            logRecordService.recordOperationLog("运输合同", "新增",
                    "新增运输合同，合同ID=" + contractId + "，承运方=" + request.getCarrierName(),
                    userId, ipAddress, true, null);
            return Result.success("新增合同成功", data);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输合同", "新增",
                    "新增运输合同失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增运输合同失败", e);
            logRecordService.recordOperationLog("运输合同", "新增",
                    "新增运输合同失败：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增失败：" + e.getMessage());
        }
    }

    /**
     * 更新运输合同
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @PostMapping("/{contractId}/update")
    @ApiOperation(value = "更新运输合同", notes = "支持operateScope操作范围校验")
    public Result<Void> update(
            @PathVariable Integer contractId,
            @Valid @RequestBody TransportContractSaveRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            transportContractService.update(contractId, request);
            logRecordService.recordOperationLog("运输合同", "更新",
                    "更新运输合同，合同ID=" + contractId + "，承运方=" + request.getCarrierName(),
                    userId, ipAddress, true, null);
            return Result.success("更新合同成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输合同", "更新",
                    "更新运输合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新运输合同失败", e);
            logRecordService.recordOperationLog("运输合同", "更新",
                    "更新运输合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    /**
     * 审核通过（包含完整字段验证）
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @PostMapping("/{contractId}/audit")
    @ApiOperation(value = "审核通过运输合同", notes = "支持operateScope操作范围校验")
    public Result<Void> audit(
            @PathVariable Integer contractId,
            @Valid @RequestBody TransportContractAuditRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            transportContractService.audit(contractId, request);
            logRecordService.recordOperationLog("运输合同", "审核通过",
                    "审核通过运输合同，合同ID=" + contractId + "，承运方=" + request.getCarrierName(),
                    userId, ipAddress, true, null);
            return Result.success("审核通过，合同已进入执行阶段", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输合同", "审核通过",
                    "审核通过失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核通过运输合同失败", e);
            logRecordService.recordOperationLog("运输合同", "审核通过",
                    "审核通过失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "审核失败：" + e.getMessage());
        }
    }

    /**
     * 更新合同状态
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @PostMapping("/{contractId}/status")
    @ApiOperation(value = "更新运输合同状态", notes = "支持operateScope操作范围校验")
    public Result<Void> updateStatus(
            @PathVariable Integer contractId,
            @RequestBody TransportContractStatusRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            transportContractService.updateStatus(contractId, request);
            String actionType = "审核中".equals(request.getStatus()) ? "提交审核" :
                    "执行中".equals(request.getStatus()) ? "审核通过" :
                    "已驳回".equals(request.getStatus()) ? "审核拒绝" :
                    "已完结".equals(request.getStatus()) ? "标记完结" : "状态更新";
            logRecordService.recordOperationLog("运输合同", actionType,
                    actionType + "，合同ID=" + contractId + "，新状态=" + request.getStatus() +
                            (request.getAuditOpinion() != null ? "，审核意见=" + request.getAuditOpinion() : ""),
                    userId, ipAddress, true, null);
            return Result.success("状态更新成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输合同", "状态更新",
                    "更新合同状态失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新运输合同状态失败", e);
            logRecordService.recordOperationLog("运输合同", "状态更新",
                    "更新合同状态失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 逻辑删除合同（仅待审核状态可删除）
     */
    @RequirePagePermission("合同管理:委托运输合同:页面")
    @PostMapping("/{contractId}/delete")
    @ApiOperation(value = "删除运输合同", notes = "仅待审核状态的合同可删除，支持operateScope操作范围校验")
    public Result<Void> delete(@PathVariable Integer contractId, HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            transportContractService.delete(contractId);
            logRecordService.recordOperationLog("运输合同", "删除",
                    "删除运输合同，合同ID=" + contractId,
                    userId, ipAddress, true, null);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("运输合同", "删除",
                    "删除运输合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除运输合同失败", e);
            logRecordService.recordOperationLog("运输合同", "删除",
                    "删除运输合同失败，合同ID=" + contractId + "：" + e.getMessage(),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取合同关联的车辆列表
     */
    @GetMapping("/{contractId}/vehicles")
    @ApiOperation(value = "获取合同关联的车辆列表")
    public Result<List<ContractVehicleResponse>> getContractVehicles(@PathVariable Integer contractId) {
        try {
            List<ContractVehicleResponse> vehicles = transportContractVehicleService.getVehiclesByContractId(contractId);
            return Result.success("查询成功", vehicles);
        } catch (Exception e) {
            log.error("获取合同关联车辆列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 运输合同查询（关联车辆统计）
     * 查询 TRANSPORT_CONTRACT 表的合同编号、合同单号、承运方名称
     * 关联查询 TRANSPORT_CONTRACT_VEHICLE 车辆编号关联 VEHICLE 获取车牌号
     * 统计 DISPATCH_ORDER 中运输车辆号牌与合同关联车辆号牌相等但车辆编号为空的记录数量
     * 统计 OUTSOURCE_TRANSPORT_SETTLEMENT 中合同编号为空的记录数量
     */
    @GetMapping("/query-with-vehicles")
    @ApiOperation(value = "运输合同查询（关联车辆统计）")
    public Result<List<TransportContractQueryResponse>> queryWithVehicles() {
        try {
            List<TransportContractQueryResponse> result = transportContractService.getContractWithVehicleList();
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("运输合同查询失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 搜索运输合同（下拉框用）
     * 支持按合同单号、承运方名称搜索，不分页
     */
    @GetMapping("/search")
    @ApiOperation(value = "搜索运输合同（下拉框用）")
    public Result<List<Map<String, Object>>> searchContracts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String viewScope) {
        try {
            List<Map<String, Object>> result = transportContractService.searchContracts(keyword, viewScope);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("搜索运输合同失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "搜索失败：" + e.getMessage());
        }
    }
}
