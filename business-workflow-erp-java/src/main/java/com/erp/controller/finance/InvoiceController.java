package com.erp.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.InvoicePageRequest;
import com.erp.controller.finance.dto.InvoicePageResponse;
import com.erp.service.auth.AuthService;
import com.erp.service.finance.InvoiceService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
import com.erp.common.annotation.RequireActionPermission;
import com.erp.entity.finance.Invoice;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.enums.ResultCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 发票管理控制器
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/finance/invoice")
@Api(tags = "发票管理")
@Validated
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private com.erp.mapper.finance.InvoiceMapper invoiceMapper;

    @Autowired
    private com.erp.mapper.system.EmployeeMapper employeeMapper;

    @Autowired
    private AuthService authService;

    /**
     * 操作范围权限校验
     * 当operateScope为SELF时，检查当前用户是否是发票的创建人
     */
    private void checkOperateScope(Invoice invoice, Integer currentUserId, String operation) {
        if (invoice == null || currentUserId == null) {
            throw new BusinessException("参数不完整");
        }

        // 判断是否为超级管理员
        if (authService.isAdmin(currentUserId)) {
            return;
        }

        // 根据发票状态确定页面权限编码
        String pageCode = "销项发票".equals(invoice.getInvoiceStatus())
            ? "财务管理:发票管理:销项发票:页面"
            : "财务管理:发票管理:进项发票:页面";

        // 获取员工的页面权限配置
        EmployeePermission permission = getEmployeePagePermission(currentUserId, pageCode);
        if (permission == null) {
            throw new BusinessException("您没有该页面的操作权限");
        }

        // 检查operateScope配置
        String operateScope = permission.getOperateScope();
        if ("SELF".equalsIgnoreCase(operateScope)) {
            // 仅能操作自己创建的数据
            if (!currentUserId.equals(invoice.getCreatorId())) {
                throw new BusinessException("您只能操作自己创建的发票");
            }
        }
        // operateScope为ALL或null时，不限制操作
    }


    /**
     * 获取员工的页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            // 从数据库查询页面权限ID
            Permission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionCode, pageCode)
                    .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );

            if (permission == null) {
                return null;
            }

            // 查询员工页面权限配置
            EmployeePermission employeePermission = employeePermissionMapper.selectOne(
                new LambdaQueryWrapper<EmployeePermission>()
                    .eq(EmployeePermission::getEmployeeId, employeeId)
                    .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );

            return employeePermission;
        } catch (Exception e) {
            log.error("获取员工页面权限配置失败：employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }

    /**
     * 批量导入发票（ZIP文件）
     * 
     * 接口地址：POST /api/finance/invoice/batch-import
     * 功能描述：上传ZIP压缩包，批量导入发票数据，仅处理PDF与图片文件
     * 入参：{ file: MultipartFile, invoiceStatus: String }
     * 返回参数：批量导入结果统计信息
     * 请求方式：POST
     * 
     * 数据验证规则：
     * 1. 文件格式验证：只支持ZIP格式，文件大小不超过100MB
     * 2. 发票状态验证：必须是"进项发票"或"销项发票"
     * 3. ZIP内文件处理规则（Service层实现）：
     *    - 仅处理PDF文件，不支持XML、OFD文件导入
     *    - PDF文件匹配发票号：先读取PDF内容提取发票号码进行匹配
     *    - 当PDF内容匹配失败时，回退到按文件名方式匹配（格式：dzfp_发票号_xxx.pdf）
     * 4. 公司信息验证（Service层实现）：
     *    - 进项发票：验证购买方名称和购买方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
     *    - 销项发票：验证销售方名称和销售方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
     *    - 如果验证失败，整个导入失败并回滚所有操作
     */
    @RequireActionPermission({
            "财务管理:发票管理:销项发票:批量导入ZIP",
            "财务管理:发票管理:进项发票:批量导入ZIP"
    })
    @PostMapping("/batch-import")
    @ApiOperation(value = "批量导入发票", notes = "上传ZIP压缩包，批量导入发票数据，仅处理PDF与图片文件；PDF先按文件内容匹配发票号，失败后再按文件名匹配")
    public Result<Map<String, Object>> batchImportInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam("invoiceStatus") @NotBlank(message = "发票状态不能为空") String invoiceStatus,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 验证文件格式
            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".zip"))) {
                return Result.error("只支持ZIP格式文件");
            }

            // 验证文件大小（100MB）
            long maxSize = 100 * 1024 * 1024; // 100MB
            if (file.getSize() > maxSize) {
                return Result.error("文件大小不能超过100MB");
            }

            // 验证发票状态
            if (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
                return Result.error("发票状态必须是'进项发票'或'销项发票'");
            }

            // 调用Service进行批量导入
            Map<String, Object> result = invoiceService.batchImportFromZip(file, invoiceStatus);

            // 检查是否有错误
            Integer errorCount = (Integer) result.get("error");
            Integer successCount = (Integer) result.get("success");
            Integer totalCount = (Integer) result.get("total");
            String logContent = String.format("批量导入发票（ZIP）：发票状态=%s，总数=%d，成功=%d，失败=%d", 
                    invoiceStatus, totalCount != null ? totalCount : 0, 
                    successCount != null ? successCount : 0, errorCount != null ? errorCount : 0);
            
            if (errorCount != null && errorCount > 0) {
                logRecordService.recordOperationLog("发票管理", "导入", logContent, userId, ipAddress, false, "部分发票导入失败");
                // 使用带code、message、data的重载，返回业务错误但附带导入结果明细
                return Result.error(500, "批量导入完成，但有部分发票导入失败", result);
            }

            logRecordService.recordOperationLog("发票管理", "导入", logContent, userId, ipAddress, true, null);
            return Result.success("批量导入成功", result);

        } catch (Exception e) {
            log.error("批量导入发票失败", e);
            logRecordService.recordOperationLog("发票管理", "导入", 
                    "批量导入发票（ZIP）失败：发票状态=" + invoiceStatus, userId, ipAddress, false, e.getMessage());
            return Result.error("批量导入失败：" + e.getMessage());
        }
    }

    /**
     * Excel导入发票（表格导入）
     *
     * 接口地址：POST /api/finance/invoice/excel-import
     * 功能描述：上传Excel文件，解析发票数据并批量导入
     * 入参：{ excelFile: MultipartFile, invoiceStatus: String }
     * 返回参数：批量导入结果统计信息
     * 请求方式：POST
     *
     * 数据验证与事务规则：
     * 1. 文件格式验证：只支持Excel格式（.xlsx、.xls），文件大小不超过100MB
     * 2. 发票状态验证：必须是"进项发票"或"销项发票"
     * 3. 公司信息验证（Service层实现）：
     *    - 进项发票：验证购买方名称和购买方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
     *    - 销项发票：验证销售方名称和销售方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
     * 4. 导入事务规则：只要当前Excel中存在任意一张发票解析/校验/入库失败，本次导入操作将整体回滚，不会写入任何发票或明细数据
     */
    @RequireActionPermission({
            "财务管理:发票管理:销项发票:批量导入Excel",
            "财务管理:发票管理:进项发票:批量导入Excel"
    })
    @PostMapping("/excel-import")
    @ApiOperation(value = "Excel导入发票", notes = "上传Excel文件，解析发票数据并批量导入")
    public Result<Map<String, Object>> excelImportInvoice(
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam("invoiceStatus") @NotBlank(message = "发票状态不能为空") String invoiceStatus,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 验证文件格式
            if (excelFile == null || excelFile.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String fileName = excelFile.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                return Result.error("只支持Excel格式文件（.xlsx、.xls）");
            }

            // 验证文件大小（100MB）
            long maxSize = 100 * 1024 * 1024; // 100MB
            if (excelFile.getSize() > maxSize) {
                return Result.error("文件大小不能超过100MB");
            }

            // 验证发票状态
            if (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
                return Result.error("发票状态必须是'进项发票'或'销项发票'");
            }

            // 调用Service进行Excel导入（内部使用事务控制：任意一条失败将整体回滚）
            Map<String, Object> result = invoiceService.excelImportInvoice(excelFile, invoiceStatus);
            
            Integer successCount = (Integer) result.get("success");
            Integer totalCount = (Integer) result.get("total");
            String logContent = String.format("Excel导入发票：发票状态=%s，总数=%d，成功=%d", 
                    invoiceStatus, totalCount != null ? totalCount : 0, successCount != null ? successCount : 0);
            
            logRecordService.recordOperationLog("发票管理", "导入", logContent, userId, ipAddress, true, null);
            return Result.success("Excel导入成功", result);

        } catch (com.erp.common.exception.BusinessException e) {
            // 业务异常（包括任意一条发票失败导致的整体回滚）
            log.error("Excel导入发票失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "导入", 
                    "Excel导入发票失败：发票状态=" + invoiceStatus, userId, ipAddress, false, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Excel导入发票失败", e);
            logRecordService.recordOperationLog("发票管理", "导入", 
                    "Excel导入发票失败：发票状态=" + invoiceStatus, userId, ipAddress, false, e.getMessage());
            return Result.error("Excel导入失败：" + e.getMessage());
        }
    }

    /**
     * 批量文件补充（PDF/图片）
     *
     * 接口地址：POST /api/finance/invoice/file-supplement
     * 功能描述：上传多个文件，根据文件名中的发票号匹配已存在的发票记录，补充文件附件
     * 入参：files，invoiceStatus（进项发票/销项发票）
     * 返回参数：补充结果统计信息
     * 请求方式：POST
     *
     * 规则：
     * 1. 文件类型限制：仅支持 PDF/图片（jpg、jpeg、png、gif、bmp）
     * 2. 全量回滚：任意文件失败则整体回滚
     */
    @RequireActionPermission({
            "财务管理:发票管理:销项发票:上传",
            "财务管理:发票管理:进项发票:上传"
    })
    @PostMapping("/file-supplement")
    @ApiOperation(value = "批量文件补充", notes = "上传多个文件，根据文件名中的发票号匹配已存在的发票记录，补充附件")
    public Result<Map<String, Object>> fileSupplement(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("invoiceStatus") @NotBlank(message = "发票状态不能为空") String invoiceStatus,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            if (files == null || files.length == 0) {
                return Result.error("文件列表不能为空");
            }
            if (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
                return Result.error("发票状态必须是'进项发票'或'销项发票'");
            }

            Map<String, Object> result = invoiceService.fileSupplement(files, invoiceStatus);
            Integer errorCount = (Integer) result.get("error");
            Integer successCount = (Integer) result.get("success");
            Integer totalCount = (Integer) result.get("total");
            String logContent = String.format("批量文件补充：发票状态=%s，文件数=%d，成功=%d，失败=%d", 
                    invoiceStatus, totalCount != null ? totalCount : files.length, 
                    successCount != null ? successCount : 0, errorCount != null ? errorCount : 0);
            
            if (errorCount != null && errorCount > 0) {
                logRecordService.recordOperationLog("发票管理", "导入", logContent, userId, ipAddress, false, "文件补充失败，已全部回滚");
                return Result.error(500, "文件补充失败，已全部回滚", result);
            }
            logRecordService.recordOperationLog("发票管理", "导入", logContent, userId, ipAddress, true, null);

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        "INVOICE_FILE_SUPPLEMENT", null, "发票文件已补充", "文件补充", userId);
            } catch (Exception msgEx) {
                log.warn("发送文件补充通知失败", msgEx);
            }

            return Result.success("文件补充成功", result);
        } catch (com.erp.common.exception.BusinessException e) {
            log.error("批量文件补充失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "导入", 
                    "批量文件补充失败：发票状态=" + invoiceStatus + "，文件数=" + files.length, 
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("批量文件补充失败", e);
            logRecordService.recordOperationLog("发票管理", "导入", 
                    "批量文件补充失败：发票状态=" + invoiceStatus + "，文件数=" + files.length, 
                    userId, ipAddress, false, e.getMessage());
            return Result.error("批量文件补充失败：" + e.getMessage());
        }
    }

    /**
     * 创建发票
     * 
     * 接口地址：POST /api/finance/invoice
     * 功能描述：创建发票记录
     * 入参：发票数据Map
     * 返回参数：创建结果
     * 请求方式：POST
     */
    @PostMapping
    @ApiOperation(value = "创建发票", notes = "创建发票记录")
    public Result<Map<String, Object>> createInvoice(@RequestBody Map<String, Object> invoiceData,
                                                      HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            // 将创建人编码放入请求数据
            invoiceData.put("creatorId", userId);

            Map<String, Object> result = invoiceService.createInvoice(invoiceData);
            String invoiceNumber = (String) result.get("invoiceNumber");
            String logContent = invoiceNumber != null ? "创建发票：发票号码=" + invoiceNumber : "创建发票：发票ID=" + result.get("invoiceId");
            logRecordService.recordOperationLog("发票管理", "新增", logContent, userId, ipAddress, true, null);

            // 获取发票数据用于确定业务类型
            Integer invoiceId = (Integer) result.get("invoiceId");
            String invoiceStatus = (String) invoiceData.get("invoiceStatus");
            String businessType = "销项发票".equals(invoiceStatus) ? "INVOICE_OUTPUT_CREATE" : "INVOICE_INPUT_CREATE";

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        businessType, invoiceId, "发票已创建", "新增", userId);
            } catch (Exception msgEx) {
                log.warn("发送发票创建通知失败", msgEx);
            }

            return Result.success("发票创建成功", result);
        } catch (Exception e) {
            log.error("创建发票失败", e);
            logRecordService.recordOperationLog("发票管理", "新增", "创建发票失败", userId, ipAddress, false, e.getMessage());
            return Result.error("创建发票失败：" + e.getMessage());
        }
    }

    /**
     * 发票分页查询（按进项/销项分表查询）
     *
     * 接口地址：GET /api/finance/invoice/list
     * 功能描述：根据发票状态（进项发票/销项发票）以及其他筛选条件分页查询发票列表
     * 入参：
     *  - page, size：分页参数
     *  - invoiceStatus：发票状态（进项发票/销项发票，必填）
     *  - invoiceType：发票类型（增值税专用发票/普通发票）
     *  - invoiceForm：发票形式（数电发票/电子发票/纸质发票）
     *  - invoiceNature：发票性质（红字/蓝字）
     *  - invoiceDateStart/invoiceDateEnd：开票日期范围
     *  - invoiceNumber：发票号码（模糊）
     *  - sellerName/sellerCreditCode：销售方名称/统一社会信用代码（模糊）
     *  - buyerName/buyerCreditCode：购买方名称/统一社会信用代码（模糊）
     * 返回参数：发票分页结果
     * 请求方式：GET
     */
    @RequirePagePermission({
            "财务管理:发票管理:进项发票:页面",
            "财务管理:发票管理:销项发票:页面"
    })
    @GetMapping("/list")
    @ApiOperation(value = "发票分页查询", notes = "根据发票状态（进项/销项）及多条件筛选发票列表")
    public Result<IPage<?>> getInvoicePage(
            @Valid InvoicePageRequest request) {
        try {
            IPage<InvoicePageResponse> page = invoiceService.getInvoicePage(request);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("发票分页查询失败，request={}", request, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取发票详情
     * 
     * 接口地址：GET /api/finance/invoice/{invoiceId}
     * 功能描述：根据发票ID查询发票详情，包含所有字段和明细
     * 入参：invoiceId（路径参数）
     * 返回参数：发票详情数据（包含所有字段）
     * 请求方式：GET
     */
    @GetMapping("/{invoiceId}")
    @ApiOperation(value = "获取发票详情", notes = "根据发票ID查询发票详情，包含所有字段和明细")
    public Result<Map<String, Object>> getInvoiceDetail(@PathVariable("invoiceId") Integer invoiceId) {
        try {
            Map<String, Object> data = invoiceService.getInvoiceDetail(invoiceId);
            return Result.success("查询成功", data);
        } catch (Exception e) {
            log.error("获取发票详情失败，invoiceId={}", invoiceId, e);
            return Result.error("获取发票详情失败：" + e.getMessage());
        }
    }

    /**
     * 更新发票
     * 
     * 接口地址：PUT /api/finance/invoice/{invoiceId}
     * 功能描述：更新发票记录，包含所有字段和明细
     * 入参：invoiceId（路径参数），发票数据Map（请求体）
     * 返回参数：更新结果
     * 请求方式：PUT
     */
    @PutMapping("/{invoiceId}")
    @ApiOperation(value = "更新发票", notes = "更新发票记录，包含所有字段和明细")
    public Result<Map<String, Object>> updateInvoice(
            @PathVariable("invoiceId") Integer invoiceId,
            @RequestBody Map<String, Object> invoiceData,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            Map<String, Object> result = invoiceService.updateInvoice(invoiceId, invoiceData);
            String invoiceNumber = (String) result.get("invoiceNumber");
            String logContent = invoiceNumber != null ? "更新发票：发票号码=" + invoiceNumber : "更新发票：发票ID=" + invoiceId;
            logRecordService.recordOperationLog("发票管理", "编辑", logContent, userId, ipAddress, true, null);

            // 确定业务类型
            String invoiceStatus = (String) invoiceData.get("invoiceStatus");
            String businessType = "销项发票".equals(invoiceStatus) ? "INVOICE_OUTPUT_UPDATE" : "INVOICE_INPUT_UPDATE";

            // 发送消息通知
            try {
                messageNotificationService.sendBusinessOperationNotification(
                        businessType, invoiceId, "发票已更新", "编辑", userId);
            } catch (Exception msgEx) {
                log.warn("发送发票更新通知失败", msgEx);
            }

            return Result.success("发票更新成功", result);
        } catch (Exception e) {
            log.error("更新发票失败，invoiceId={}", invoiceId, e);
            logRecordService.recordOperationLog("发票管理", "编辑", "更新发票失败：发票ID=" + invoiceId, userId, ipAddress, false, e.getMessage());
            return Result.error("更新发票失败：" + e.getMessage());
        }
    }

    /**
     * 查询发票文件编号
     *
     * 接口地址：GET /api/finance/invoice/{invoiceId}/files
     * 功能描述：查询发票是否有PDF、图片文件
     * 入参：invoiceId（路径参数）
     * 返回参数：文件编号信息（pdfFileId, imageFileId）
     * 请求方式：GET
     */
    @GetMapping("/{invoiceId}/files")
    @ApiOperation(value = "查询发票文件编号", notes = "查询发票是否有PDF、图片文件")
    public Result<Map<String, Object>> getInvoiceFileIds(@PathVariable("invoiceId") Integer invoiceId) {
        try {
            Map<String, Object> data = invoiceService.getInvoiceFileIds(invoiceId);
            return Result.success("查询成功", data);
        } catch (Exception e) {
            log.error("查询发票文件编号失败，invoiceId={}", invoiceId, e);
            return Result.error("查询发票文件编号失败：" + e.getMessage());
        }
    }

    /**
     * 上传单个发票文件
     *
     * 接口地址：POST /api/finance/invoice/{invoiceId}/upload-file
     * 功能描述：为指定发票上传单个PDF或图片文件
     * 入参：
     *  - invoiceId：发票ID（路径参数）
     *  - file：文件（必填，支持PDF、图片格式）
     *  - fileType：文件类型（pdf/image）
     * 返回参数：上传结果
     * 请求方式：POST
     */
    @PostMapping("/{invoiceId}/upload-file")
    @ApiOperation(value = "上传单个发票文件", notes = "为指定发票上传单个PDF或图片文件")
    public Result<Map<String, Object>> uploadInvoiceFile(
            @PathVariable("invoiceId") Integer invoiceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") @NotBlank(message = "文件类型不能为空") String fileType,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        String fileName = null;
        try {
            // 1. 校验发票是否存在及操作权限
            Invoice invoice = invoiceMapper.selectById(invoiceId);
            if (invoice == null) {
                return Result.error("发票不存在");
            }

            // 2. 操作范围权限校验（operateScope）
            // 检查当前用户是否能够操作该发票
            checkOperateScope(invoice, userId, "上传发票文件");

            // 基础校验
            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            fileName = file.getOriginalFilename();
            if (fileName == null) {
                return Result.error("文件名不能为空");
            }

            // 验证文件类型
            if (!"pdf".equals(fileType) && !"image".equals(fileType)) {
                return Result.error("文件类型必须是pdf或image");
            }

            // 验证文件格式
            String extension = getFileExtension(fileName).toLowerCase();
            switch (fileType) {
                case "pdf":
                    if (!"pdf".equals(extension)) {
                        return Result.error("PDF文件格式不正确");
                    }
                    break;
                case "image":
                    if (!"jpg".equals(extension) && !"jpeg".equals(extension) && !"png".equals(extension)) {
                        return Result.error("图片文件格式不正确，支持jpg、jpeg、png格式");
                    }
                    break;
            }

            // 验证文件大小（10MB）
            long maxSize = 10 * 1024 * 1024L;
            if (file.getSize() > maxSize) {
                return Result.error("文件大小不能超过10MB");
            }

            // 调用Service进行文件上传
            Map<String, Object> result = invoiceService.uploadInvoiceFile(invoiceId, file, fileType);

            logRecordService.recordOperationLog("发票管理", "上传",
                    "上传发票文件：" + fileName + "，类型：" + fileType + "，发票ID：" + invoiceId,
                    userId, ipAddress, true, null);

            // 发送消息通知
            try {
                // 根据发票状态确定业务类型
                String businessType = "销项发票".equals(invoice.getInvoiceStatus()) ? "INVOICE_OUTPUT_UPDATE" : "INVOICE_INPUT_UPDATE";
                messageNotificationService.sendBusinessOperationNotification(
                        businessType, invoiceId, "发票文件已上传", "上传", userId);
            } catch (Exception msgEx) {
                log.warn("发送发票文件上传通知失败", msgEx);
            }

            return Result.success("文件上传成功", result);

        } catch (com.erp.common.exception.BusinessException e) {
            log.error("上传发票文件失败，业务异常：发票ID={}，文件={}，类型={}", invoiceId, fileName, fileType, e);
            logRecordService.recordOperationLog("发票管理", "上传",
                    "上传发票文件失败：发票ID=" + invoiceId + "，文件=" + fileName + "，类型=" + fileType,
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传发票文件失败，发票ID={}，文件={}，类型={}", invoiceId, fileName, fileType, e);
            logRecordService.recordOperationLog("发票管理", "上传",
                    "上传发票文件失败：发票ID=" + invoiceId + "，文件=" + fileName + "，类型=" + fileType,
                    userId, ipAddress, false, e.getMessage());
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 导出发票Excel
     * 根据发票ID列表导出发票数据为Excel文件，包含发票基本信息和明细信息（明细分行显示）
     *
     * 接口地址：POST /api/finance/invoice/export-excel
     * 功能描述：根据选择的发票ID列表导出Excel文件，包含所有字段，明细分行显示
     * 入参：
     *  - invoiceIds：发票ID列表（必填）
     *  - invoiceStatus：发票状态（进项发票/销项发票，必填）
     * 返回参数：Excel文件流
     * 请求方式：POST
     */
    @PostMapping("/export-excel")
    @ApiOperation(value = "导出发票Excel", notes = "根据发票ID列表导出Excel文件，包含发票基本信息和明细信息")
    public void exportInvoiceExcel(
            @RequestBody Map<String, Object> requestBody,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            // 参数校验
            @SuppressWarnings("unchecked")
            List<Integer> invoiceIds = (List<Integer>) requestBody.get("invoiceIds");
            String invoiceStatus = (String) requestBody.get("invoiceStatus");

            if (invoiceIds == null || invoiceIds.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "发票ID列表不能为空");
                return;
            }
            if (invoiceStatus == null || (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "发票状态必须是'进项发票'或'销项发票'");
                return;
            }

            // 调用Service进行导出
            byte[] excelData = invoiceService.exportInvoiceExcel(invoiceIds, invoiceStatus);

            // 设置响应头
            String fileName = "发票导出_" + java.time.LocalDate.now().toString().replace("-", "") + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 写入响应
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();

            // 记录操作日志
            String logContent = String.format("导出发票Excel：发票状态=%s，发票数量=%d", invoiceStatus, invoiceIds.size());
            logRecordService.recordOperationLog("发票管理", "导出", logContent, userId, ipAddress, true, null);

        } catch (com.erp.common.exception.BusinessException e) {
            log.error("导出发票Excel失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "导出发票Excel失败：发票状态=" + requestBody.get("invoiceStatus"), userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        } catch (Exception e) {
            log.error("导出发票Excel失败", e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "导出发票Excel失败：发票状态=" + requestBody.get("invoiceStatus"), userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 导出发票文件（ZIP压缩包）
     * 根据发票ID列表导出发票相关文件为ZIP压缩包，包含PDF、图片等文件
     *
     * 接口地址：POST /api/finance/invoice/export-files
     * 功能描述：将勾选的发票的所有相关文件（PDF、图片）进行打包，只要有的就打包成ZIP文件统一下载
     * 入参：
     *  - invoiceIds：发票ID列表（必填）
     *  - invoiceStatus：发票状态（进项发票/销项发票，必填）
     * 返回参数：ZIP文件流
     * 请求方式：POST
     */
    @PostMapping("/export-files")
    @ApiOperation(value = "导出发票文件", notes = "将勾选的发票的所有相关文件打包成ZIP文件统一下载")
    public void exportInvoiceFiles(
            @RequestBody Map<String, Object> requestBody,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            // 参数校验
            @SuppressWarnings("unchecked")
            List<Integer> invoiceIds = (List<Integer>) requestBody.get("invoiceIds");
            String invoiceStatus = (String) requestBody.get("invoiceStatus");

            if (invoiceIds == null || invoiceIds.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "发票ID列表不能为空");
                return;
            }
            if (invoiceStatus == null || (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus))) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "发票状态必须是'进项发票'或'销项发票'");
                return;
            }

            // 调用Service进行导出
            byte[] zipData = invoiceService.exportInvoiceFiles(invoiceIds, invoiceStatus);

            // 设置响应头
            String fileName = "发票文件导出_" + java.time.LocalDate.now().toString().replace("-", "") + ".zip";
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 写入响应
            response.getOutputStream().write(zipData);
            response.getOutputStream().flush();

            // 记录操作日志
            String logContent = String.format("导出发票文件（ZIP）：发票状态=%s，发票数量=%d", invoiceStatus, invoiceIds.size());
            logRecordService.recordOperationLog("发票管理", "导出", logContent, userId, ipAddress, true, null);

        } catch (com.erp.common.exception.BusinessException e) {
            log.error("导出发票文件失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "导出发票文件失败：发票状态=" + requestBody.get("invoiceStatus"), userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        } catch (Exception e) {
            log.error("导出发票文件失败", e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "导出发票文件失败：发票状态=" + requestBody.get("invoiceStatus"), userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 全部导出发票Excel
     * 直接导出系统中的全部发票数据为Excel文件
     *
     * 接口地址：POST /api/finance/invoice/export-all-excel
     * 功能描述：直接导出系统中的全部发票数据为Excel文件
     * 入参：
     *  - invoiceStatus：发票状态（进项发票/销项发票，可选，不传则导出全部）
     * 返回参数：Excel文件流
     * 请求方式：POST
     */
    @PostMapping("/export-all-excel")
    @ApiOperation(value = "全部导出发票Excel", notes = "直接导出系统中的全部发票数据为Excel文件")
    public void exportAllInvoiceExcel(
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            // 获取参数（invoiceStatus可选）
            String invoiceStatus = requestBody != null ? (String) requestBody.get("invoiceStatus") : null;

            // 验证参数（允许为null）
            if (invoiceStatus != null && !"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "发票状态必须是'进项发票'或'销项发票'");
                return;
            }

            // 根据发票状态确定页面权限编码，进行 viewScope 安全校验
            // 确定页面权限编码：优先用 invoiceStatus 匹配，否则默认两个页面都适用（此处取销项页面兜底）
            String pageCode;
            if ("进项发票".equals(invoiceStatus)) {
                pageCode = "财务管理:发票管理:进项发票:页面";
            } else {
                pageCode = "财务管理:发票管理:销项发票:页面";
            }

            // 后端强制校验 viewScope，防止前端绕过
            Integer creatorId = null;
            if (!authService.isAdmin(userId)) {
                EmployeePermission permission = getEmployeePagePermission(userId, pageCode);
                if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                    // viewScope=SELF：强制只导出当前员工创建的数据
                    creatorId = userId;
                }
                // viewScope=ALL 或无配置：creatorId 保持 null，导出全部
            }
            // 超级管理员：creatorId=null，导出全部

            // 调用Service进行导出
            byte[] excelData = invoiceService.exportAllInvoiceExcel(invoiceStatus, creatorId);

            // 设置响应头
            String fileName = "发票全部导出_" + java.time.LocalDate.now().toString().replace("-", "") + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 写入响应
            response.getOutputStream().write(excelData);
            response.getOutputStream().flush();

            // 记录操作日志
            String logContent = String.format("全部导出发票Excel：发票状态=%s", invoiceStatus != null ? invoiceStatus : "全部");
            logRecordService.recordOperationLog("发票管理", "导出", logContent, userId, ipAddress, true, null);

        } catch (com.erp.common.exception.BusinessException e) {
            log.error("全部导出发票Excel失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "全部导出发票Excel失败：发票状态=" + (requestBody != null ? requestBody.get("invoiceStatus") : "全部"),
                    userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        } catch (Exception e) {
            log.error("全部导出发票Excel失败", e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "全部导出发票Excel失败：发票状态=" + (requestBody != null ? requestBody.get("invoiceStatus") : "全部"),
                    userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 全部导出发票文件（ZIP压缩包）
     * 直接导出系统中的全部发票相关文件为ZIP压缩包
     *
     * 接口地址：POST /api/finance/invoice/export-all-files
     * 功能描述：将系统中的全部发票相关文件（PDF、图片）进行打包，只要有的就打包成ZIP文件统一下载
     * 入参：
     *  - invoiceStatus：发票状态（进项发票/销项发票，可选，不传则导出全部）
     * 返回参数：ZIP文件流
     * 请求方式：POST
     */
    @PostMapping("/export-all-files")
    @ApiOperation(value = "全部导出发票文件", notes = "将系统中的全部发票相关文件打包成ZIP文件统一下载")
    public void exportAllInvoiceFiles(
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletResponse response,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            // 获取参数（invoiceStatus可选）
            String invoiceStatus = requestBody != null ? (String) requestBody.get("invoiceStatus") : null;

            // 验证参数（允许为null）
            if (invoiceStatus != null && !"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "发票状态必须是'进项发票'或'销项发票'");
                return;
            }

            // 根据发票状态确定页面权限编码，进行 viewScope 安全校验
            String pageCode;
            if ("进项发票".equals(invoiceStatus)) {
                pageCode = "财务管理:发票管理:进项发票:页面";
            } else {
                pageCode = "财务管理:发票管理:销项发票:页面";
            }

            // 后端强制校验 viewScope，防止前端绕过
            Integer creatorId = null;
            if (!authService.isAdmin(userId)) {
                EmployeePermission permission = getEmployeePagePermission(userId, pageCode);
                if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
                    // viewScope=SELF：强制只导出当前员工创建的数据
                    creatorId = userId;
                }
                // viewScope=ALL 或无配置：creatorId 保持 null，导出全部
            }
            // 超级管理员：creatorId=null，导出全部

            // 调用Service进行导出
            byte[] zipData = invoiceService.exportAllInvoiceFiles(invoiceStatus, creatorId);

            // 设置响应头
            String fileName = "发票全部文件导出_" + java.time.LocalDate.now().toString().replace("-", "") + ".zip";
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 写入响应
            response.getOutputStream().write(zipData);
            response.getOutputStream().flush();

            // 记录操作日志
            String logContent = String.format("全部导出发票文件（ZIP）：发票状态=%s", invoiceStatus != null ? invoiceStatus : "全部");
            logRecordService.recordOperationLog("发票管理", "导出", logContent, userId, ipAddress, true, null);

        } catch (com.erp.common.exception.BusinessException e) {
            log.error("全部导出发票文件失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "全部导出发票文件失败：发票状态=" + (requestBody != null ? requestBody.get("invoiceStatus") : "全部"),
                    userId, ipAddress, false, e.getMessage());

            // 对于业务异常（如没有相关文件），返回正常的JSON响应而不是500错误
            try {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");

                // 构建错误响应JSON
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 200); // 业务级别的错误码
                errorResponse.put("message", e.getMessage());
                errorResponse.put("data", null);
                errorResponse.put("timestamp", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                String jsonResponse = objectMapper.writeValueAsString(errorResponse);

                response.getWriter().write(jsonResponse);
                response.getWriter().flush();
            } catch (Exception ex) {
                log.error("发送JSON错误响应失败", ex);
                // 如果JSON响应失败，回退到HTTP错误
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                } catch (Exception ex2) {
                    log.error("发送错误响应失败", ex2);
                }
            }
        } catch (Exception e) {
            log.error("全部导出发票文件失败", e);
            logRecordService.recordOperationLog("发票管理", "导出",
                    "全部导出发票文件失败：发票状态=" + (requestBody != null ? requestBody.get("invoiceStatus") : "全部"),
                    userId, ipAddress, false, e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    /**
     * 获取发票关联记录
     * 请求方式：GET
     */
    @GetMapping("/{invoiceId}/associations")
    @ApiOperation(value = "获取发票关联记录", notes = "查询指定发票的结算单和合同关联记录")
    public Result<?> getInvoiceAssociations(
            @PathVariable Integer invoiceId,
            HttpServletRequest httpRequest) {

        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);

        try {
            log.info("查询发票关联记录：invoiceId={}", invoiceId);

            // 调用Service查询关联记录
            Map<String, Object> associations = invoiceService.getInvoiceAssociations(invoiceId);

            // 记录操作日志
            logRecordService.recordOperationLog("发票管理", "查询",
                    String.format("查询发票关联记录：发票ID=%s", invoiceId),
                    userId, ipAddress, true, null);

            return Result.success(associations);

        } catch (com.erp.common.exception.BusinessException e) {
            log.error("查询发票关联记录失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "查询",
                    String.format("查询发票关联记录失败：发票ID=%s", invoiceId),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询发票关联记录失败，系统异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "查询",
                    String.format("查询发票关联记录失败：发票ID=%s", invoiceId),
                    userId, ipAddress, false, e.getMessage());
            return Result.error("查询发票关联记录失败：" + e.getMessage());
        }
    }

    /**
     * 更新发票的合同/结算单关联
     * 接口地址：POST /api/finance/invoice/{invoiceId}/associations
     */
    @PostMapping("/{invoiceId}/associations")
    @ApiOperation(value = "更新发票关联记录", notes = "更新指定发票的合同/结算单关联信息，后端会根据传入数据处理新增/删除/修改")
    public Result<Void> updateInvoiceAssociations(
            @PathVariable Integer invoiceId,
            @RequestBody Map<String, Object> data,
            HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        try {
            log.info("更新发票关联：invoiceId={}, payload={}", invoiceId, data);

            // 1. 校验发票是否存在及操作权限
            Invoice invoice = invoiceMapper.selectById(invoiceId);
            if (invoice == null) {
                return Result.error("发票不存在");
            }

            // 2. 操作范围权限校验（operateScope）
            checkOperateScope(invoice, userId, "更新发票关联");

            invoiceService.updateInvoiceAssociations(invoiceId, data);
            logRecordService.recordOperationLog("发票管理", "更新关联",
                    String.format("更新发票关联：发票ID=%s", invoiceId),
                    userId, ipAddress, true, null);

            // 发送消息通知
            try {
                // 根据发票状态确定业务类型
                String businessType = "销项发票".equals(invoice.getInvoiceStatus()) ? "INVOICE_OUTPUT_UPDATE" : "INVOICE_INPUT_UPDATE";
                messageNotificationService.sendBusinessOperationNotification(
                        businessType, invoiceId, "发票关联已更新", "编辑", userId);
            } catch (Exception msgEx) {
                log.warn("发送发票关联更新通知失败", msgEx);
            }

            return Result.success("更新成功", null);
        } catch (com.erp.common.exception.BusinessException e) {
            log.error("更新发票关联失败，业务异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "更新关联",
                    String.format("更新发票关联失败：发票ID=%s", invoiceId),
                    userId, ipAddress, false, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新发票关联失败，系统异常：{}", e.getMessage(), e);
            logRecordService.recordOperationLog("发票管理", "更新关联",
                    String.format("更新发票关联失败：发票ID=%s", invoiceId),
                    userId, ipAddress, false, e.getMessage());
            return Result.error("更新发票关联失败：" + e.getMessage());
        }
    }


    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

}

