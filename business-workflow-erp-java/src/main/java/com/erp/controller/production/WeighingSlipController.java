package com.erp.controller.production;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.controller.production.dto.CreateWeighingSlipRequest;
import com.erp.controller.production.dto.UpdateWeighingSlipRequest;
import com.erp.controller.production.dto.WeighingSlipInfoResponse;
import com.erp.controller.production.dto.WeighingSlipListResponse;
import com.erp.controller.production.dto.WeighingSlipPageRequest;
import com.erp.service.production.WeighingSlipService;
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
 * 总磅单管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/warehouse/inbound/weighing-slip")
@Api(tags = "总磅单管理")
public class WeighingSlipController {

    @Autowired
    private WeighingSlipService weighingSlipService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 创建总磅单
     */
    @RequireActionPermission("仓库管理:入库管理:总磅单:创建入库单")
    @PostMapping("/create")
    @ApiOperation(value = "创建总磅单", notes = "创建新的总磅单，支持关联多个运输单号，后端自动设置状态为'待细分'")
    public Result<WeighingSlipInfoResponse> createWeighingSlip(@Valid @RequestBody CreateWeighingSlipRequest request,
                                                                HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            WeighingSlipInfoResponse response = weighingSlipService.createWeighingSlip(request);
            // 记录操作日志
            logRecordService.recordOperationLog("总磅单管理", "新增",
                    "创建总磅单：总磅单号=" + response.getWeighingSlipNo(), userId, ipAddress, true, null);
            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "WEIGHING_SLIP_CREATE", response.getWeighingSlipId(), "总磅单创建成功", "新增", userId);
            } catch (Exception msgEx) {
                log.warn("发送总磅单创建通知失败", msgEx);
            }
            return Result.success("创建总磅单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("总磅单管理", "新增",
                    "创建总磅单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建总磅单失败", e);
            logRecordService.recordOperationLog("总磅单管理", "新增",
                    "创建总磅单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "创建总磅单失败：" + e.getMessage());
        }
    }

    /**
     * 获取总磅单信息
     */
    @RequireActionPermission("仓库管理:入库管理:总磅单:查看")
    @GetMapping("/info")
    @ApiOperation(value = "获取总磅单信息", notes = "根据总磅单号获取总磅单详细信息，包括关联的运输单号列表")
    public Result<WeighingSlipInfoResponse> getWeighingSlipInfo(@RequestParam String weighingSlipNo) {
        try {
            WeighingSlipInfoResponse response = weighingSlipService.getWeighingSlipInfo(weighingSlipNo);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询总磅单信息失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新总磅单
     */
    @RequireActionPermission("仓库管理:入库管理:总磅单:修改")
    @PostMapping("/update")
    @ApiOperation(value = "更新总磅单", notes = "更新总磅单信息，支持修改基本信息、称重信息、关联运输单等，已细分状态不能修改")
    public Result<WeighingSlipInfoResponse> updateWeighingSlip(@Valid @RequestBody UpdateWeighingSlipRequest request,
                                                                HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            WeighingSlipInfoResponse response = weighingSlipService.updateWeighingSlip(request);
            // 记录操作日志（照片字段只记录路径部分）
            String logContent = buildUpdateLogContent(request, response);
            logRecordService.recordOperationLog("总磅单管理", "修改",
                    logContent, userId, ipAddress, true, null);
            // 发送消息通知给创建人（如果修改人不是创建人）
            try {
                Integer creatorId = response.getCreatorId();
                if (creatorId != null && !creatorId.equals(userId)) {
                    messageNotificationService.sendBusinessOperationNotification(
                            "WEIGHING_SLIP_UPDATE", response.getWeighingSlipId(), "总磅单已修改", "修改", creatorId);
                }
            } catch (Exception msgEx) {
                log.warn("发送总磅单修改通知失败", msgEx);
            }
            return Result.success("更新总磅单成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("总磅单管理", "修改",
                    "更新总磅单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("更新总磅单失败", e);
            logRecordService.recordOperationLog("总磅单管理", "修改",
                    "更新总磅单失败", userId, ipAddress, false, e.getMessage());
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新总磅单失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询总磅单列表
     *
     * @param request                  查询条件
     */
    @RequirePagePermission("仓库管理:入库-总磅单:页面")
    @GetMapping("/list")
    @ApiOperation(value = "分页查询总磅单列表", notes = "支持按关键字、总磅单号、序号、车号、状态、日期范围等条件筛选")
    public Result<WeighingSlipListResponse> getWeighingSlipPage(
            WeighingSlipPageRequest request) {
        try {
            // 设置默认值
            if (request == null) {
                request = new WeighingSlipPageRequest();
            }
            if (request.getCurrent() == null || request.getCurrent() <= 0) {
                request.setCurrent(1L);
            }
            if (request.getSize() == null || request.getSize() <= 0) {
                request.setSize(10L);
            }
            
            WeighingSlipListResponse response = weighingSlipService.getWeighingSlipPage(request);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            log.error("查询总磅单列表失败，业务异常：{}", e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询总磅单列表失败，异常类型：{}，异常信息：{}，堆栈：", 
                    e.getClass().getName(), 
                    e.getMessage() != null ? e.getMessage() : "null", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : 
                    (e.getClass().getSimpleName() + (e.getCause() != null ? "：" + e.getCause().getMessage() : ""));
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + errorMessage);
        }
    }

    /**
     * 构建更新日志内容（照片字段只记录路径部分）
     */
    private String buildUpdateLogContent(UpdateWeighingSlipRequest request, WeighingSlipInfoResponse response) {
        StringBuilder content = new StringBuilder("更新总磅单：总磅单号=" + response.getWeighingSlipNo());
        
        // 记录修改的字段
        boolean hasChanges = false;
        if (request.getSequence() != null) {
            content.append("，序号=").append(request.getSequence());
            hasChanges = true;
        }
        if (request.getDate() != null) {
            content.append("，日期=").append(request.getDate());
            hasChanges = true;
        }
        if (request.getPlateNo() != null) {
            content.append("，车号=").append(request.getPlateNo());
            hasChanges = true;
        }
        if (request.getGrossWeight() != null) {
            content.append("，总重=").append(request.getGrossWeight()).append("kg");
            hasChanges = true;
        }
        if (request.getTareWeight() != null) {
            content.append("，空重=").append(request.getTareWeight()).append("kg");
            hasChanges = true;
        }
        if (request.getNetWeight() != null) {
            content.append("，净重=").append(request.getNetWeight()).append("kg");
            hasChanges = true;
        }
        if (request.getStatus() != null) {
            content.append("，状态=").append(request.getStatus());
            hasChanges = true;
        }
        if (request.getRemark() != null) {
            content.append("，备注=").append(request.getRemark());
            hasChanges = true;
        }
        if (request.getDispatchCodes() != null && !request.getDispatchCodes().isEmpty()) {
            content.append("，关联运输单数=").append(request.getDispatchCodes().size());
            hasChanges = true;
        }
        // 照片字段的修改：只记录文件路径部分
        if (request.getPhotoUrl() != null) {
            String photoPath = extractPhotoPath(request.getPhotoUrl());
            content.append("，总磅单照片=").append(photoPath);
            hasChanges = true;
        }
        
        if (!hasChanges) {
            content.append("（无字段变更）");
        }
        
        return content.toString();
    }

    /**
     * 从照片URL中提取文件路径部分
     */
    private String extractPhotoPath(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return "";
        }
        
        // 如果包含查询参数，提取path参数的值
        if (photoUrl.contains("?path=")) {
            String pathParam = photoUrl.substring(photoUrl.indexOf("?path=") + 6);
            // 如果path参数后面还有其他参数，只取第一个参数的值
            if (pathParam.contains("&")) {
                pathParam = pathParam.substring(0, pathParam.indexOf("&"));
            }
            // URL解码（如果有编码）
            try {
                pathParam = java.net.URLDecoder.decode(pathParam, "UTF-8");
            } catch (Exception e) {
                // 解码失败，使用原始值
            }
            return pathParam;
        } else if (photoUrl.startsWith("/api/file/download")) {
            // 如果只是路径部分，去掉前缀
            return photoUrl.replace("/api/file/download", "");
        } else {
            // 直接返回原始值（已经是路径）
            return photoUrl;
        }
    }
}



