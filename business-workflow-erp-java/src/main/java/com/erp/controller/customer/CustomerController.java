package com.erp.controller.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.Result;
import com.erp.controller.customer.dto.CustomerCreateRequest;
import com.erp.controller.customer.dto.CustomerDetailResponse;
import com.erp.controller.customer.dto.CustomerImportResponse;
import com.erp.controller.customer.dto.CustomerPageRequest;
import com.erp.controller.customer.dto.CustomerPageResponse;
import com.erp.controller.customer.dto.CustomerUpdateRequest;
import com.erp.controller.customer.dto.CustomerQuotationHierarchicalResponse;
import com.erp.controller.customer.dto.CustomerContractResponse;
import com.erp.controller.customer.dto.CustomerFollowResponse;
import com.erp.controller.customer.dto.CustomerFollowUpCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpPageRequest;
import com.erp.controller.customer.dto.CustomerFollowUpResponse;
import com.erp.controller.customer.dto.CustomerFollowUpUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpUpdateWithDetailsRequest;
import com.erp.controller.customer.dto.CustomerFollowUpDetailResponse;
import com.erp.controller.customer.dto.CustomerFollowUpDetailCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpDetailUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpWithDetailsResponse;
import com.erp.service.customer.CustomerFollowUpDetailService;
import com.erp.common.util.SecurityUtil;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.service.customer.CustomerService;
import com.erp.service.customer.CustomerFollowUpService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 客户管理控制器
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/customer")
@Api(tags = "客户管理")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerFollowUpService customerFollowUpService;

    @Autowired
    private CustomerFollowUpDetailService customerFollowUpDetailService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @PostMapping
    @ApiOperation(value = "新增客户", notes = "创建单个客户信息")
    @RequireActionPermission("档案管理:客户档案:新增")
    public Result<CustomerDetailResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerDetailResponse response = customerService.createCustomer(request);
            // 记录操作日志和数据变更日志（在Service层已记录数据变更日志）
            logRecordService.recordOperationLog("客户管理", "新增",
                    "新增客户：" + response.getEnterpriseName(), userId, ipAddress, true, null);
            // 发送消息通知（基于权限发送给有客户档案权限的人员）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "CUSTOMER_CREATE",                           // 业务类型
                        response.getCustomerId(),                     // 业务ID
                        response.getEnterpriseName(),                 // 业务标题
                        "新增",                                       // 操作类型
                        userId                                       // 发送人ID（会自动排除）
                );
            } catch (Exception msgEx) {
                log.warn("发送新增客户通知失败", msgEx);
            }
            return Result.success("新增客户成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "新增",
                    "新增客户：" + request.getEnterpriseName(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/{customerId}")
    @ApiOperation(value = "更新客户", notes = "更新客户基础信息")
    @RequireActionPermission("档案管理:客户档案:编辑")
    public Result<Void> updateCustomer(@PathVariable Integer customerId,
            @Valid @RequestBody CustomerUpdateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            request.setCustomerId(customerId);
            // 获取客户名称用于日志
            String customerName = null;
            try {
                CustomerDetailResponse detail = customerService.getCustomerDetail(customerId);
                if (detail != null) {
                    customerName = detail.getEnterpriseName();
                }
            } catch (Exception ignored) {
            }
            customerService.updateCustomer(request);
            // 记录操作日志（数据变更日志在Service层已记录）
            logRecordService.recordOperationLog("客户管理", "编辑",
                    "更新客户：ID=" + customerId, userId, ipAddress, true, null);
            // 发送消息通知（基于权限发送给有客户档案权限的人员）
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "CUSTOMER_UPDATE",                           // 业务类型
                        customerId,                                   // 业务ID
                        customerName != null ? customerName : "客户(ID:" + customerId + ")", // 业务标题
                        "修改",                                       // 操作类型
                        userId                                       // 发送人ID（会自动排除）
                );
            } catch (Exception msgEx) {
                log.warn("发送客户更新通知失败", msgEx);
            }
            return Result.success("更新客户成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "编辑",
                    "更新客户：ID=" + customerId, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/{customerId}")
    @ApiOperation(value = "客户详情", notes = "根据ID查询客户详情")
    public Result<CustomerDetailResponse> getCustomerDetail(@PathVariable Integer customerId) {
        try {
            CustomerDetailResponse response = customerService.getCustomerDetail(customerId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/{customerId}/quotations")
    @ApiOperation(value = "获取客户报价记录", notes = "根据客户ID查询该客户的所有报价单（层级结构）")
    public Result<List<CustomerQuotationHierarchicalResponse>> getCustomerQuotations(@PathVariable Integer customerId) {
        try {
            List<CustomerQuotationHierarchicalResponse> list = customerService.getCustomerQuotations(customerId);
            return Result.success("查询成功", list);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/{customerId}/contracts")
    @ApiOperation(value = "获取客户合同记录", notes = "根据客户ID查询该客户的所有合同")
    public Result<List<CustomerContractResponse>> getCustomerContracts(@PathVariable Integer customerId) {
        try {
            List<CustomerContractResponse> list = customerService.getCustomerContracts(customerId);
            return Result.success("查询成功", list);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/{customerId}/follows")
    @ApiOperation(value = "获取客户跟进记录", notes = "根据客户ID查询该客户的跟进记录（旧接口，保留兼容）")
    public Result<List<CustomerFollowResponse>> getCustomerFollows(@PathVariable Integer customerId) {
        try {
            List<CustomerFollowResponse> list = customerService.getCustomerFollows(customerId);
            return Result.success("查询成功", list);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/{customerId}/follow-ups")
    @ApiOperation(value = "获取客户跟进记录（新接口）", notes = "根据客户ID查询该客户的所有跟进记录")
    public Result<List<CustomerFollowUpResponse>> getCustomerFollowUps(@PathVariable Integer customerId) {
        try {
            List<CustomerFollowUpResponse> list = customerFollowUpService.getFollowUpsByCustomerId(customerId);
            return Result.success("查询成功", list);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("查询客户跟进记录失败", e);
            return Result.error(ResultCodeEnum.OPERATION_FAILED.getCode(), "查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/follow-up/list")
    @ApiOperation(value = "查询当前用户的客户跟进记录", notes = "查询当前登录用户的所有客户跟进记录")
    public Result<List<CustomerFollowUpResponse>> getCurrentUserFollowUps() {
        try {
            List<CustomerFollowUpResponse> list = customerFollowUpService.getCurrentUserFollowUps();
            return Result.success("查询成功", list);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @RequirePagePermission("业务管理:客户跟进:页面")
    @GetMapping("/follow-up/page")
    @ApiOperation(value = "分页查询当前用户的客户跟进记录", notes = "支持多条件筛选、模糊查询、排序，查询的业务员必须为当前登录用户")
    public Result<IPage<CustomerFollowUpResponse>> getFollowUpPage(CustomerFollowUpPageRequest request) {
        try {
            IPage<CustomerFollowUpResponse> page = customerFollowUpService.getFollowUpPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PostMapping("/follow-up")
    @ApiOperation(value = "新增客户跟进记录", notes = "手动新增一条客户跟进记录")
    @RequireActionPermission("业务管理:客户跟进:新增")
    public Result<CustomerFollowUpResponse> createFollowUp(@Valid @RequestBody CustomerFollowUpCreateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerFollowUpResponse response = customerFollowUpService.createFollowUp(request);
            // 记录操作日志和数据变更日志（在Service层已记录数据变更日志）
            logRecordService.recordOperationLog("客户管理", "新增",
                    "新增客户跟进记录：客户ID=" + request.getCustomerId(), userId, ipAddress, true, null);
            // 发送消息通知
            try {
                String title = "新增客户跟进记录";
                String content = "您新增了一条客户跟进记录，客户ID=" + request.getCustomerId();
                messageNotificationService.sendBusinessNotification("客户管理", title, content, null, userId, "CUSTOMER_FOLLOW_UP",
                        response.getFollowUpId() != null ? response.getFollowUpId().intValue() : null);
            } catch (Exception msgEx) {
                log.warn("发送新增客户跟进记录通知失败", msgEx);
            }
            return Result.success("新增客户跟进记录成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "新增",
                    "新增客户跟进记录：客户ID=" + request.getCustomerId(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/follow-up")
    @ApiOperation(value = "更新客户跟进记录", notes = "更新一条客户跟进记录")
    @RequireActionPermission("业务管理:客户跟进:编辑")
    public Result<CustomerFollowUpResponse> updateFollowUp(@Valid @RequestBody CustomerFollowUpUpdateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerFollowUpResponse response = customerFollowUpService.updateFollowUp(request);
            logRecordService.recordOperationLog("客户管理", "更新",
                    "更新客户跟进记录：ID=" + request.getFollowUpId(), userId, ipAddress, true, null);
            // 发送消息通知
            try {
                String title = "更新客户跟进记录";
                String content = "您更新了客户跟进记录，ID=" + request.getFollowUpId();
                messageNotificationService.sendBusinessNotification("客户管理", title, content, null, userId, "CUSTOMER_FOLLOW_UP",
                        request.getFollowUpId() != null ? request.getFollowUpId().intValue() : null);
            } catch (Exception msgEx) {
                log.warn("发送更新客户跟进记录通知失败", msgEx);
            }
            return Result.success("更新客户跟进记录成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "更新",
                    "更新客户跟进记录：ID=" + request.getFollowUpId() + "，失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/follow-up/with-details")
    @ApiOperation(value = "更新客户跟进记录（包含明细差分修改）", notes = "更新客户跟进记录及其明细，支持差分修改：新增、更新、删除明细")
    @RequireActionPermission("业务管理:客户跟进:编辑")
    public Result<CustomerFollowUpWithDetailsResponse> updateFollowUpWithDetails(
            @Valid @RequestBody CustomerFollowUpUpdateWithDetailsRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerFollowUpWithDetailsResponse response = customerFollowUpService.updateFollowUpWithDetails(request);
            logRecordService.recordOperationLog("客户管理", "更新",
                    "更新客户跟进记录（包含明细）：ID=" + request.getFollowUpId(), userId, ipAddress, true, null);
            return Result.success("更新客户跟进记录成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "更新",
                    "更新客户跟进记录（包含明细）：ID=" + request.getFollowUpId() + "，失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/follow-up/{followUpId}/details")
    @ApiOperation(value = "获取跟进记录明细列表", notes = "根据跟进记录ID查询所有明细")
    public Result<CustomerFollowUpWithDetailsResponse> getFollowUpWithDetails(@PathVariable Integer followUpId) {
        try {
            CustomerFollowUpWithDetailsResponse response = customerFollowUpDetailService.getFollowUpWithDetails(followUpId);
            return Result.success("查询成功", response);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PostMapping("/follow-up-detail")
    @ApiOperation(value = "新增跟进明细", notes = "为跟进记录添加一条明细")
    @RequireActionPermission("业务管理:客户跟进:编辑")
    public Result<CustomerFollowUpDetailResponse> createFollowUpDetail(
            @Valid @RequestBody CustomerFollowUpDetailCreateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerFollowUpDetailResponse response = customerFollowUpDetailService.createDetail(request);
            logRecordService.recordOperationLog("客户管理", "新增",
                    "新增跟进明细：跟进记录ID=" + request.getFollowUpId(), userId, ipAddress, true, null);
            return Result.success("新增跟进明细成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "新增",
                    "新增跟进明细：跟进记录ID=" + request.getFollowUpId() + "，失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/follow-up-detail")
    @ApiOperation(value = "更新跟进明细", notes = "更新一条跟进明细")
    @RequireActionPermission("业务管理:客户跟进:编辑")
    public Result<CustomerFollowUpDetailResponse> updateFollowUpDetail(
            @Valid @RequestBody CustomerFollowUpDetailUpdateRequest request,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerFollowUpDetailResponse response = customerFollowUpDetailService.updateDetail(request);
            logRecordService.recordOperationLog("客户管理", "更新",
                    "更新跟进明细：ID=" + request.getDetailId(), userId, ipAddress, true, null);
            return Result.success("更新跟进明细成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "更新",
                    "更新跟进明细：ID=" + request.getDetailId() + "，失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PutMapping("/follow-up-detail/{detailId}/complete")
    @ApiOperation(value = "完成跟进明细", notes = "将跟进明细状态标记为已完成")
    @RequireActionPermission("业务管理:客户跟进:编辑")
    public Result<CustomerFollowUpDetailResponse> completeFollowUpDetail(@PathVariable Integer detailId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerFollowUpDetailResponse response = customerFollowUpDetailService.completeDetail(detailId);
            logRecordService.recordOperationLog("客户管理", "完成",
                    "完成跟进明细：ID=" + detailId, userId, ipAddress, true, null);
            return Result.success("完成跟进明细成功", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "完成",
                    "完成跟进明细：ID=" + detailId + "，失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @DeleteMapping("/follow-up-detail/{detailId}")
    @ApiOperation(value = "删除跟进明细", notes = "删除一条跟进明细")
    @RequireActionPermission("业务管理:客户跟进:编辑")
    public Result<Void> deleteFollowUpDetail(@PathVariable Integer detailId,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            customerFollowUpDetailService.deleteDetail(detailId);
            logRecordService.recordOperationLog("客户管理", "删除",
                    "删除跟进明细：ID=" + detailId, userId, ipAddress, true, null);
            return Result.success("删除跟进明细成功", null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "删除",
                    "删除跟进明细：ID=" + detailId + "，失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @DeleteMapping("/follow-up/batch")
    @ApiOperation(value = "批量删除客户跟进记录", notes = "只能删除当前登录用户创建的跟进记录")
    @RequireActionPermission("业务管理:客户跟进:批量删除")
    public Result<Integer> deleteFollowUpBatch(@RequestBody List<Integer> followUpIds,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            int deletedCount = customerFollowUpService.deleteBatchByIds(followUpIds);
            logRecordService.recordOperationLog("客户管理", "删除",
                    "批量删除客户跟进记录：" + deletedCount + "条，ID列表=" + followUpIds, userId, ipAddress, true, null);
            // 发送消息通知
            try {
                String title = "批量删除客户跟进记录";
                String content = "您批量删除了" + deletedCount + "条客户跟进记录";
                messageNotificationService.sendBusinessNotification("客户管理", title, content, null, userId, "CUSTOMER_FOLLOW_UP", null);
            } catch (Exception msgEx) {
                log.warn("发送批量删除客户跟进记录通知失败", msgEx);
            }
            return Result.success("批量删除客户跟进记录成功", deletedCount);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "删除",
                    "批量删除客户跟进记录失败：" + e.getMessage(), userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/follow-up/export")
    @ApiOperation(value = "导出客户跟进记录", notes = "根据筛选条件导出客户跟进记录Excel")
    @RequireActionPermission("业务管理:客户跟进:导出")
    public void exportFollowUps(@Valid CustomerFollowUpPageRequest request, HttpServletResponse response,
                                 HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            List<CustomerFollowUpWithDetailsResponse> list = customerFollowUpService.exportFollowUps(request);
            writeFollowUpExport(list, response);
            logRecordService.recordOperationLog("客户管理", "导出",
                    "导出客户跟进记录，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "导出",
                    "导出客户跟进记录失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出客户跟进记录失败", e);
            logRecordService.recordOperationLog("客户管理", "导出",
                    "导出客户跟进记录失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    @RequirePagePermission({
            "档案管理:客户档案:页面",
            "业务管理:客户跟进:页面",
            "业务管理:客户报价:页面",
            "合同管理:合同订立:页面",
            "合同管理:危险废物合同:页面"
    })
    @GetMapping("/list")
    @ApiOperation(value = "客户分页查询", notes = "支持按企业名称、信用代码、状态、业务员筛选")
    public Result<IPage<CustomerPageResponse>> getCustomerPage(
            @Valid CustomerPageRequest request) {
        try {
            IPage<CustomerPageResponse> page = customerService.getCustomerPage(request);
            return Result.success("查询成功", page);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @PostMapping("/import")
    @ApiOperation(value = "批量导入客户", notes = "支持上传Excel文件批量创建客户信息")
    @RequireActionPermission("档案管理:客户档案:批量导入")
    public Result<CustomerImportResponse> importCustomers(@RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            CustomerImportResponse response = customerService.importCustomers(file);
            // 记录导入操作日志
            String content = String.format("批量导入客户：总记录数=%d，成功=%d，失败=%d",
                    response.getTotalCount(), response.getSuccessCount(), response.getFailCount());
            logRecordService.recordOperationLog("客户管理", "导入", content, userId, ipAddress, true, null);
            return Result.success("导入完成", response);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "导入",
                    "批量导入客户失败", userId, ipAddress, false, e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/import/template")
    @ApiOperation(value = "下载客户导入模板", notes = "下载前导入格式一致的Excel模板")
    public void exportImportTemplate(HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户导入模板");
            // 与 CustomerServiceImpl.buildCustomerFromRow 中列顺序保持一致
            String[] headers = {
                    "企业名称",
                    "统一社会信用代码",
                    "地址",
                    "电话",
                    "法定代表人",
                    "联系人",
                    "联系电话",
                    "曾用名",
                    "备注"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                if (i == 1 || i == 2 || i == 8) {
                    sheet.setColumnWidth(i, 25 * 256);
                } else {
                    sheet.setColumnWidth(i, 20 * 256);
                }
            }
            String fileName = URLEncoder.encode("客户导入模板.xlsx", StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("下载客户导入模板失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出模板失败：" + e.getMessage());
        }
    }

    @GetMapping("/export")
    @ApiOperation(value = "导出客户", notes = "根据筛选条件导出客户Excel")
    @RequireActionPermission("档案管理:客户档案:导出")
    public void exportCustomers(@Valid CustomerPageRequest request, HttpServletResponse response,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            List<CustomerDetailResponse> list = customerService.listCustomersForExport(request);
            writeExport(list, response);
            // 记录导出操作日志
            logRecordService.recordOperationLog("客户管理", "导出",
                    "导出客户列表，共" + list.size() + "条记录", userId, ipAddress, true, null);
        } catch (BusinessException e) {
            logRecordService.recordOperationLog("客户管理", "导出",
                    "导出客户列表失败", userId, ipAddress, false, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("导出客户失败", e);
            logRecordService.recordOperationLog("客户管理", "导出",
                    "导出客户列表失败", userId, ipAddress, false, e.getMessage());
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导出失败：" + e.getMessage());
        }
    }

    private void writeExport(List<CustomerDetailResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户信息");
            String[] headers = { "企业名称", "统一社会信用代码", "地址", "电话",
                    "法定代表人", "联系人", "联系电话", "曾用名", "备注" };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // 为较长的字段设置更大的列宽
                if (i == 1 || i == 2 || i == 8) { // 信用代码、地址、备注列
                    sheet.setColumnWidth(i, 25 * 256);
                } else {
                    sheet.setColumnWidth(i, 15 * 256);
                }
            }
            for (int i = 0; i < data.size(); i++) {
                CustomerDetailResponse item = data.get(i);
                Row row = sheet.createRow(i + 1);
                int col = 0;
                row.createCell(col++).setCellValue(item.getEnterpriseName() == null ? "" : item.getEnterpriseName());
                row.createCell(col++).setCellValue(item.getCreditCode() == null ? "" : item.getCreditCode());
                row.createCell(col++).setCellValue(item.getAddress() == null ? "" : item.getAddress());
                row.createCell(col++).setCellValue(item.getPhone() == null ? "" : item.getPhone());
                row.createCell(col++)
                        .setCellValue(item.getLegalRepresentative() == null ? "" : item.getLegalRepresentative());
                row.createCell(col++).setCellValue(item.getContactPerson() == null ? "" : item.getContactPerson());
                row.createCell(col++).setCellValue(item.getContactPhone() == null ? "" : item.getContactPhone());
                row.createCell(col++).setCellValue(item.getFormerNames() == null ? "" : item.getFormerNames());
                row.createCell(col).setCellValue(item.getRemark() == null ? "" : item.getRemark());
            }
            String fileName = URLEncoder.encode("客户信息导出.xlsx", StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        }
    }

    /**
     * 写入导出Excel（客户跟进记录）
     * 如果有多条明细，则每行Excel数据前面的父表字段内容相同，子表明细字段内容不同
     */
    private void writeFollowUpExport(List<CustomerFollowUpWithDetailsResponse> data, HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户跟进记录");
            String[] headers = {
                "客户名称",
                "联系人姓名",
                "联系人电话",
                "跟进人",
                "创建人",
                "创建时间",
                "备注",
                "跟进时间",
                "跟进内容",
                "跟进状态",
                "明细创建人",
                "明细更新时间"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowIndex = 1;
            for (CustomerFollowUpWithDetailsResponse item : data) {
                List<CustomerFollowUpDetailResponse> details = item.getDetails();
                // 如果没有明细，则创建一行，父表字段有值，明细字段为空
                if (details == null || details.isEmpty()) {
                    Row row = sheet.createRow(rowIndex++);
                    int col = 0;
                    row.createCell(col++).setCellValue(item.getCustomerName() == null ? "" : item.getCustomerName());
                    row.createCell(col++).setCellValue(item.getContactName() == null ? "" : item.getContactName());
                    row.createCell(col++).setCellValue(item.getContactPhone() == null ? "" : item.getContactPhone());
                    row.createCell(col++).setCellValue(item.getEmployeeName() == null ? "" : item.getEmployeeName());
                    row.createCell(col++).setCellValue(item.getCreatorName() == null ? "" : item.getCreatorName());
                    row.createCell(col++).setCellValue(item.getCreateTime() == null ? "" : formatter.format(item.getCreateTime()));
                    row.createCell(col++).setCellValue(item.getRemark() == null ? "" : item.getRemark());
                    row.createCell(col++).setCellValue("");
                    row.createCell(col++).setCellValue("");
                    row.createCell(col++).setCellValue("");
                    row.createCell(col++).setCellValue("");
                    row.createCell(col).setCellValue("");
                } else {
                    // 有明细时，每条明细一行，父表字段重复
                    for (CustomerFollowUpDetailResponse detail : details) {
                        Row row = sheet.createRow(rowIndex++);
                        int col = 0;
                        row.createCell(col++).setCellValue(item.getCustomerName() == null ? "" : item.getCustomerName());
                        row.createCell(col++).setCellValue(item.getContactName() == null ? "" : item.getContactName());
                        row.createCell(col++).setCellValue(item.getContactPhone() == null ? "" : item.getContactPhone());
                        row.createCell(col++).setCellValue(item.getEmployeeName() == null ? "" : item.getEmployeeName());
                        row.createCell(col++).setCellValue(item.getCreatorName() == null ? "" : item.getCreatorName());
                        row.createCell(col++).setCellValue(item.getCreateTime() == null ? "" : formatter.format(item.getCreateTime()));
                        row.createCell(col++).setCellValue(item.getRemark() == null ? "" : item.getRemark());
                        row.createCell(col++).setCellValue(detail.getFollowTime() == null ? "" : formatter.format(detail.getFollowTime()));
                        row.createCell(col++).setCellValue(detail.getFollowContent() == null ? "" : detail.getFollowContent());
                        row.createCell(col++).setCellValue(detail.getFollowStatus() == null ? "" : detail.getFollowStatus());
                        row.createCell(col++).setCellValue(detail.getCreatorName() == null ? "" : detail.getCreatorName());
                        row.createCell(col++).setCellValue(detail.getUpdateTime() == null ? "" : formatter.format(detail.getUpdateTime()));
                    }
                }
            }
            String fileName = URLEncoder.encode("客户跟进记录导出.xlsx", StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        }
    }
}
