package com.erp.controller.production;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.production.dto.BatchCreateWarehousingRequest;
import com.erp.controller.production.dto.BatchCreateWarehousingResponse;
import com.erp.controller.production.dto.UpdateWarehousingRequest;
import com.erp.controller.production.dto.WarehousingDetailResponse;
import com.erp.controller.production.dto.WarehousingListResponse;
import com.erp.controller.production.dto.WarehousingPageRequest;
import com.erp.controller.production.dto.WarehousingWithSettlementVO;
import com.erp.mapper.production.WarehousingMapper;
import com.erp.service.production.WarehousingService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 入库单管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/warehouse/inbound")
@Api(tags = "入库单管理")
public class WarehousingController {

    @Autowired
    private WarehousingService warehousingService;

    @Autowired
    private WarehousingMapper warehousingMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    private static final String WAREHOUSING_BUSINESS_TYPE = "WAREHOUSING";

    /**
     * 批量创建入库单
     */
    @RequireActionPermission("仓库管理:入库管理:总磅单:创建入库单")
    @PostMapping("/batch-create")
    @ApiOperation(value = "批量创建入库单", notes = "根据总磅单批量创建入库单，为每个关联的收运运输单生成一个入库单")
    public Result<BatchCreateWarehousingResponse> batchCreateWarehousing(
            @Valid @RequestBody BatchCreateWarehousingRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            BatchCreateWarehousingResponse response = warehousingService.batchCreateWarehousing(request);
            // 记录操作日志
            int count = response.getWarehousingList() != null ? response.getWarehousingList().size() : 0;
            String logContent = String.format("批量创建入库单：总磅单号=%s，成功创建=%d个入库单",
                    request.getWeighingSlipNo(), count);
            logRecordService.recordOperationLog("入库单管理", "新增", logContent, userId, ipAddress, true, null);

            // 发送消息通知给仓管员
            if (response.getWarehousingList() != null && !response.getWarehousingList().isEmpty()) {
                for (BatchCreateWarehousingResponse.WarehousingInfo info : response.getWarehousingList()) {
                    try {
                        // 获取入库单详情获取仓管员ID
                        com.erp.entity.production.Warehousing entity = warehousingMapper.selectByWarehousingNo(info.getWarehousingNo());
                        if (entity != null && entity.getWarehouseKeeperId() != null) {
                            messageNotificationService.sendBusinessOperationNotification(
                                    "WAREHOUSING_CREATE", entity.getWarehousingId(), "入库单创建成功", "新增", entity.getWarehouseKeeperId());
                        }
                    } catch (Exception e) {
                        log.warn("发送入库单创建通知失败：warehousingNo={}", info.getWarehousingNo(), e);
                    }
                }
            }

            return Result.success("批量创建入库单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("入库单管理", "新增",
                    "批量创建入库单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批量创建入库单失败", e);
            logRecordService.recordOperationLog("入库单管理", "新增",
                    "批量创建入库单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量创建入库单失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询入库单列表
     *
     * @param request                  查询条件
     */
    @RequirePagePermission("仓库管理:入库-入库单:页面")
    @GetMapping("/list")
    @ApiOperation(value = "分页查询入库单列表", notes = "支持按关键字、总磅单号、收运运输单号、状态、时间范围等条件筛选")
    public Result<WarehousingListResponse> getWarehousingPage(
            WarehousingPageRequest request) {
        try {
            // 设置默认值
            if (request == null) {
                request = new WarehousingPageRequest();
            }
            if (request.getPage() == null || request.getPage() <= 0) {
                request.setPage(1);
            }
            if (request.getSize() == null || request.getSize() <= 0) {
                request.setSize(20);
            }

            WarehousingListResponse response = warehousingService.getWarehousingPage(request);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            log.error("查询入库单列表失败，业务异常：{}", e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询入库单列表失败，异常类型：{}，异常信息：{}，堆栈：",
                    e.getClass().getName(),
                    e.getMessage() != null ? e.getMessage() : "null", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() :
                    (e.getClass().getSimpleName() + (e.getCause() != null ? "：" + e.getCause().getMessage() : ""));
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + errorMessage);
        }
    }

    /**
     * 获取入库单详情
     */
    @RequireActionPermission("仓库管理:入库管理:入库单:查看")
    @GetMapping("/detail")
    @ApiOperation(value = "获取入库单详情", notes = "根据入库单编号或入库单号获取入库单详细信息")
    public Result<WarehousingDetailResponse> getWarehousingDetail(
            @RequestParam(required = false) Integer warehousingId,
            @RequestParam(required = false) String warehousingNo,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            if (warehousingId == null && (warehousingNo == null || warehousingNo.trim().isEmpty())) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "入库单编号或入库单号至少提供一个");
            }

            // 如果只提供了入库单号，需要先查询入库单编号
            if (warehousingId == null) {
                com.erp.entity.production.Warehousing warehousing = warehousingMapper.selectByWarehousingNo(warehousingNo);
                if (warehousing == null) {
                    return Result.error(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "入库单不存在");
                }
                warehousingId = warehousing.getWarehousingId();
            }

            WarehousingDetailResponse response = warehousingService.getWarehousingDetail(warehousingId);
            // 查看操作不计入日志（仅失败时记录）
            return Result.success("获取入库单详情成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("入库单管理", "查看",
                    "获取入库单详情失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取入库单详情失败", e);
            logRecordService.recordOperationLog("入库单管理", "查看",
                    "获取入库单详情失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "获取入库单详情失败：" + e.getMessage());
        }
    }

    /**
     * 更新入库单
     */
    @RequireActionPermission("仓库管理:入库管理:入库单:编辑")
    @PostMapping("/update")
    @ApiOperation(value = "更新入库单", notes = "更新已存在的入库单信息（仅限未锁定状态）")
    public Result<WarehousingDetailResponse> updateWarehousing(
            @Valid @RequestBody UpdateWarehousingRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 调试：打印请求中的明细，便于排查辅助核算字段是否到达后端
            if (log.isInfoEnabled()) {
                log.info("接收到更新入库单请求 items: {}", request.getItems());
            }
            WarehousingDetailResponse response = warehousingService.updateWarehousing(request);
            logRecordService.recordOperationLog("入库单管理", "修改",
                    String.format("更新入库单：入库单号=%s", response.getWarehousingNo()),
                    userId, ipAddress, true, null);

            // 发送消息通知给仓管员（如果修改人不是仓管员）
            try {
                Integer warehouseKeeperId = response.getWarehouseKeeperId();
                if (warehouseKeeperId != null && !warehouseKeeperId.equals(userId)) {
                    messageNotificationService.sendBusinessOperationNotification(
                            "WAREHOUSING_UPDATE", response.getWarehousingId(), "入库单已修改", "修改", warehouseKeeperId);
                }
            } catch (Exception e) {
                log.warn("发送入库单修改通知失败：warehousingId={}", request.getWarehousingId(), e);
            }

            return Result.success("更新入库单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("入库单管理", "修改",
                    "更新入库单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新入库单失败", e);
            logRecordService.recordOperationLog("入库单管理", "修改",
                    "更新入库单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新入库单失败：" + e.getMessage());
        }
    }


    /**
     * 删除入库单
     */
    @DeleteMapping("/{warehousingId}")
    @ApiOperation(value = "删除入库单", notes = "删除入库单及其关联的危废明细。只有未锁定的入库单才能删除")
    public Result<Void> deleteWarehousing(
            @PathVariable Integer warehousingId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 获取删除前的数据用于日志记录
            WarehousingDetailResponse oldData = null;
            try {
                oldData = warehousingService.getWarehousingDetail(warehousingId);
            } catch (Exception e) {
                // 如果获取详情失败，不影响删除操作，只记录警告
                log.warn("删除前获取入库单详情失败：warehousingId={}", warehousingId, e);
            }
            
            warehousingService.deleteWarehousing(warehousingId);
            
            // 记录操作日志
            String logContent = String.format("删除入库单：入库单编号=%d", warehousingId);
            if (oldData != null && oldData.getWarehousingNo() != null) {
                logContent += "，入库单号=" + oldData.getWarehousingNo();
            }
            logRecordService.recordOperationLog("入库单管理", "删除", logContent, userId, ipAddress, true, null);

            // 发送消息通知给仓管员
            if (oldData != null && oldData.getWarehouseKeeperId() != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "WAREHOUSING_DELETE", warehousingId, "入库单已删除", "删除", oldData.getWarehouseKeeperId());
                } catch (Exception e) {
                    log.warn("发送入库单删除通知失败：warehousingId={}", warehousingId, e);
                }
            }

            return Result.success("删除入库单成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("入库单管理", "删除", 
                    "删除入库单：入库单编号=" + warehousingId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除入库单失败", e);
            logRecordService.recordOperationLog("入库单管理", "删除", 
                    "删除入库单：入库单编号=" + warehousingId, userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除失败：" + e.getMessage());
        }
    }

    /**
     * 根据合同号获取入库单列表（含业务链和结算状态）
     * 业务链：收运通知单 → 运输单 → 入库单
     */
    @GetMapping("/contract/{contractCode}")
    @ApiOperation(value = "根据合同号获取入库单列表", notes = "根据合同号获取关联的通知单→运输单→入库单列表，并表明当前入库单是否已经结算")
    public Result<java.util.List<WarehousingWithSettlementVO>> getWarehousingByContract(
            @PathVariable String contractCode) {
        try {
            if (contractCode == null || contractCode.trim().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "合同号不能为空");
            }
            java.util.List<WarehousingWithSettlementVO> result = warehousingService.getWarehousingWithChainByContract(contractCode.trim());
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("根据合同号获取入库单列表失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }
}


