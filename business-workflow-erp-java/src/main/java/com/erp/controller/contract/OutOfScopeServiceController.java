package com.erp.controller.contract;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.contract.dto.OutOfScopeServiceCreateDTO;
import com.erp.entity.contract.OutOfScopeService;
import com.erp.common.util.SecurityUtil;
import com.erp.service.contract.OutOfScopeServiceService;
import com.erp.service.system.ILogRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 价外服务（OutOfScopeService）管理接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping
@Api(tags = "价外服务管理")
public class OutOfScopeServiceController {

    @Autowired
    private OutOfScopeServiceService outOfScopeServiceService;

    @Autowired
    private ILogRecordService logRecordService;


    @GetMapping("/quotation/{quotationId}/out-of-scope-services")
    @ApiOperation("查询指定报价单的价外服务列表")
    public Result<List<OutOfScopeService>> listByQuotation(@PathVariable("quotationId") Integer quotationId) {
        try {
            List<OutOfScopeService> list = outOfScopeServiceService.listByQuotation(quotationId);
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("查询价外服务失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/quotation/{quotationId}/out-of-scope-service")
    @ApiOperation("为报价单新增价外服务（批量）")
    public Result<List<OutOfScopeService>> createForQuotation(@PathVariable("quotationId") Integer quotationId,
                                                              @RequestBody List<OutOfScopeServiceCreateDTO> services,
                                                              HttpServletRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(request);
        try {
            // 设置创建者信息到第一个服务实体（Service层会处理）
            if (services != null && !services.isEmpty()) {
                services.get(0).setCreatedBy(userId);
            }
            List<OutOfScopeService> created = outOfScopeServiceService.createForQuotation(quotationId, services, userId);
            logRecordService.recordOperationLog("价外服务", "新增", "为报价单新增价外服务，quotationId=" + quotationId, userId, ip, true, null);
            return Result.success("新增成功", created);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("价外服务", "新增", "新增失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增价外服务失败", e);
            logRecordService.recordOperationLog("价外服务", "新增", "新增失败", userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增失败：" + e.getMessage());
        }
    }

    @GetMapping("/contract/{contractId}/out-of-scope-services")
    @ApiOperation("查询指定合同的价外服务列表")
    public Result<List<OutOfScopeService>> listByContract(@PathVariable("contractId") Integer contractId) {
        try {
            List<OutOfScopeService> list = outOfScopeServiceService.listByContract(contractId);
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("查询合同价外服务失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/contract/{contractId}/out-of-scope-service")
    @ApiOperation("为合同新增价外服务（批量）")
    public Result<List<OutOfScopeService>> createForContract(@PathVariable("contractId") Integer contractId,
                                                             @RequestBody List<OutOfScopeServiceCreateDTO> services,
                                                             HttpServletRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(request);
        try {
            // 设置创建者信息到第一个服务实体（Service层会处理）
            if (services != null && !services.isEmpty()) {
                services.get(0).setCreatedBy(userId);
            }
            List<OutOfScopeService> created = outOfScopeServiceService.createForContract(contractId, services, userId);
            logRecordService.recordOperationLog("价外服务", "新增", "为合同新增价外服务，contractId=" + contractId, userId, ip, true, null);
            return Result.success("新增成功", created);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("价外服务", "新增", "新增失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新增合同价外服务失败", e);
            logRecordService.recordOperationLog("价外服务", "新增", "新增失败", userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/out-of-scope-service/{id}")
    @ApiOperation("更新指定价外服务")
    public Result<OutOfScopeService> update(@PathVariable("id") Integer id, @RequestBody OutOfScopeServiceCreateDTO dto,
                                            HttpServletRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(request);
        try {
            OutOfScopeService updated = outOfScopeServiceService.update(id, dto, userId);
            logRecordService.recordOperationLog("价外服务", "更新", "更新价外服务ID=" + id, userId, ip, true, null);
            return Result.success("更新成功", updated);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("价外服务", "更新", "更新失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新价外服务失败", e);
            logRecordService.recordOperationLog("价外服务", "更新", "更新失败", userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/out-of-scope-service/{id}")
    @ApiOperation("删除指定价外服务")
    public Result<Void> delete(@PathVariable("id") Integer id, HttpServletRequest request) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ip = logRecordService.getClientIp(request);
        try {
            outOfScopeServiceService.delete(id);
            logRecordService.recordOperationLog("价外服务", "删除", "删除价外服务ID=" + id, userId, ip, true, null);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("价外服务", "删除", "删除失败", userId, ip, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除价外服务失败", e);
            logRecordService.recordOperationLog("价外服务", "删除", "删除失败", userId, ip, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }
}


