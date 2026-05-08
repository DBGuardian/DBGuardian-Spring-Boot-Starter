package com.erp.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.controller.finance.dto.InvoicePageRequest;
import com.erp.controller.finance.dto.InvoicePageResponse;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.entity.system.Permission;
import com.erp.entity.system.EmployeePermission;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.entity.common.File;
import com.erp.entity.finance.Invoice;
import com.erp.entity.finance.InvoiceItem;
import com.erp.entity.finance.InvoiceNoticeInvoice;
import com.erp.entity.settlement.Settlement;
import com.erp.entity.settlement.SettlementFundTransactionRel;
import com.erp.entity.settlement.SettlementInvoiceRel;
import com.erp.mapper.finance.InvoiceMapper;
import com.erp.mapper.common.FileMapper;
import com.erp.common.exception.BusinessException;
import com.erp.entity.system.SysConfig;
import com.erp.service.common.FileService;
import com.erp.service.auth.AuthService;
import com.erp.service.finance.InvoiceService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.SysConfigService;
import com.erp.service.system.MessageNotificationService;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.util.FileValidator;
import com.erp.util.FileValidator.ValidationResult;
import com.erp.util.ZipFileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.convert.NumberChineseFormatter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发票服务实现类
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private ZipFileProcessor zipFileProcessor;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired(required = false)
    private ILogRecordService logRecordService;
    
    @Autowired
    private com.erp.mapper.finance.InvoiceNoticeInvoiceMapper invoiceNoticeInvoiceMapper;

    @Autowired
    private com.erp.mapper.finance.SettlementInvoiceRelMapper settlementInvoiceRelMapper;

    @Autowired
    private com.erp.mapper.finance.SettlementFundTransactionRelMapper settlementFundTransactionRelMapper;

    @Autowired
    private com.erp.mapper.finance.SettlementMapper settlementMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从ZIP文件批量导入发票
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportFromZip(MultipartFile zipFile, String invoiceStatus) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> errorList = new ArrayList<>();

        try {
            // 1. 解压ZIP文件
            Map<String, byte[]> files = zipFileProcessor.extractZipFile(zipFile);

            // 2. 查找并解析Excel统计文件
            String excelFileName = findExcelFile(files.keySet());
            if (excelFileName == null) {
                throw new RuntimeException("未找到Excel统计文件（格式：发票批量下载情况统计_时间戳.xlsx）");
            }

            // 3. 从Excel中读取发票号码列表并去重
            List<String> invoiceNumbersRaw = parseExcelFile(files.get(excelFileName));
            Set<String> uniqueInvoiceNumbers = new LinkedHashSet<>(invoiceNumbersRaw);
            List<String> invoiceNumbers = new ArrayList<>(uniqueInvoiceNumbers);
            if (invoiceNumbersRaw.size() != invoiceNumbers.size()) {
                log.warn("从Excel中读取到{}个发票号码，去重后{}个，已自动去除重复项", invoiceNumbersRaw.size(), invoiceNumbers.size());
            }

            // 4. 仅处理PDF文件：先按PDF内容匹配发票号，失败后回退到文件名匹配
            Map<String, String> pdfFileNameMap = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String fileName = entry.getKey();
                if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                    continue;
                }

                String resolvedInvoiceNumber = resolveInvoiceNumberForZipPdf(entry.getValue(), fileName);
                if (resolvedInvoiceNumber == null || resolvedInvoiceNumber.trim().isEmpty()) {
                    log.warn("ZIP中的PDF文件无法匹配发票号，已跳过：{}", fileName);
                    continue;
                }

                resolvedInvoiceNumber = resolvedInvoiceNumber.trim();
                if (!pdfFileNameMap.containsKey(resolvedInvoiceNumber)) {
                    pdfFileNameMap.put(resolvedInvoiceNumber, fileName);
                } else {
                    log.warn("ZIP中存在多个PDF匹配到同一发票号，保留第一个：invoiceNumber={}, kept={}, ignored={}",
                            resolvedInvoiceNumber, pdfFileNameMap.get(resolvedInvoiceNumber), fileName);
                }
            }

            // 5. 按Excel中的发票号补充PDF附件（仅支持PDF，不支持XML/OFD）
            for (String invoiceNumber : invoiceNumbers) {
                try {
                    Invoice existingInvoice = invoiceMapper.selectByInvoiceNumber(invoiceNumber);
                    if (existingInvoice == null) {
                        errorList.add(createErrorRecord(invoiceNumber, "未找到对应发票记录"));
                        continue;
                    }

                    String pdfFileName = pdfFileNameMap.get(invoiceNumber);
                    if (pdfFileName == null) {
                        errorList.add(createErrorRecord(invoiceNumber, "未找到匹配的PDF文件"));
                        continue;
                    }

                    Integer pdfFileId = uploadInvoiceFile(files.get(pdfFileName), pdfFileName, invoiceNumber, "PDF");
                    if (pdfFileId == null) {
                        errorList.add(createErrorRecord(invoiceNumber, "PDF文件上传失败"));
                        continue;
                    }

                    existingInvoice.setPdfFileId(pdfFileId);
                    int updated = invoiceMapper.updateById(existingInvoice);
                    if (updated <= 0) {
                        errorList.add(createErrorRecord(invoiceNumber, "更新发票PDF附件失败"));
                        continue;
                    }

                    successList.add(createSuccessRecord(invoiceNumber, existingInvoice.getInvoiceId()));
                } catch (Exception e) {
                    log.error("处理ZIP导入PDF失败：{}", invoiceNumber, e);
                    errorList.add(createErrorRecord(invoiceNumber, e.getMessage()));
                }
            }

            result.put("total", invoiceNumbers.size());
            result.put("success", successList.size());
            result.put("error", errorList.size());
            result.put("successList", successList);
            result.put("errorList", errorList);
            return result;

        } catch (Exception e) {
            log.error("批量导入发票失败", e);
            throw new RuntimeException("批量导入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建发票
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createInvoice(Map<String, Object> invoiceData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 构建发票实体
            Invoice invoice = buildInvoiceFromMap(invoiceData);

            // 1.1 设置创建人编码（从请求数据或当前登录用户获取）
            Integer creatorId = null;
            if (invoiceData.containsKey("creatorId")) {
                Object creatorIdObj = invoiceData.get("creatorId");
                if (creatorIdObj != null) {
                    if (creatorIdObj instanceof Integer) {
                        creatorId = (Integer) creatorIdObj;
                    } else if (creatorIdObj instanceof Number) {
                        creatorId = ((Number) creatorIdObj).intValue();
                    }
                }
            }
            // 如果请求中没有提供，则使用当前登录用户
            if (creatorId == null) {
                creatorId = SecurityUtil.getCurrentUserId();
            }
            invoice.setCreatorId(creatorId);
            
            // 2. 验证发票号码是否已存在
            Invoice existingInvoice = invoiceMapper.selectByInvoiceNumber(invoice.getInvoiceNumber());
            if (existingInvoice != null) {
                throw new RuntimeException("发票号码已存在：" + invoice.getInvoiceNumber());
            }
            
            // 3. 处理金额字段
            if (invoice.getRecordDetails() == null || !invoice.getRecordDetails()) {
                // 不录入明细时，使用表单中的金额
                BigDecimal taxExcludedAmount = parseBigDecimalFromObject(invoiceData.get("taxExcludedAmount"));
                BigDecimal taxAmount = parseBigDecimalFromObject(invoiceData.get("taxAmount"));
                BigDecimal totalAmount = parseBigDecimalFromObject(invoiceData.get("totalAmount"));
                
                invoice.setTaxExcludedAmount(taxExcludedAmount);
                invoice.setTaxAmount(taxAmount);
                invoice.setTotalAmount(totalAmount);
                invoice.setAmount(taxExcludedAmount); // 金额 = 不含税金额
            } else {
                // 录入明细时，从明细计算金额（前端已计算好，这里直接使用）
                BigDecimal taxExcludedAmount = parseBigDecimalFromObject(invoiceData.get("taxExcludedAmount"));
                BigDecimal taxAmount = parseBigDecimalFromObject(invoiceData.get("taxAmount"));
                BigDecimal totalAmount = parseBigDecimalFromObject(invoiceData.get("totalAmount"));
                
                invoice.setTaxExcludedAmount(taxExcludedAmount);
                invoice.setTaxAmount(taxAmount);
                invoice.setTotalAmount(totalAmount);
                invoice.setAmount(taxExcludedAmount); // 金额 = 不含税金额
            }

            if (invoice.getRecordDetails() == null) {
                invoice.setRecordDetails(false); // 默认不录入明细
            }
            
            // 6. 保存发票
            invoiceMapper.insert(invoice);
            
            // 7. 保存发票明细（如果录入明细）
            if (invoice.getRecordDetails() != null && invoice.getRecordDetails() && invoiceData.containsKey("details")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detailsList = (List<Map<String, Object>>) invoiceData.get("details");
                if (detailsList != null && !detailsList.isEmpty()) {
                    for (Map<String, Object> itemData : detailsList) {
                        InvoiceItem item = buildInvoiceItemFromMap(itemData, invoice.getInvoiceId());
                        invoiceMapper.insertItem(item);
                    }
                }
            }
            
            // 8. 构建返回结果
            result.put("invoiceId", invoice.getInvoiceId());
            result.put("invoiceNumber", invoice.getInvoiceNumber());
            result.put("message", "发票创建成功");

            // 9. 记录数据变更日志
            try {
                Integer currentUserId = SecurityUtil.getCurrentUserId();
                // 创建后查询完整数据
                Map<String, Object> newDetail = getInvoiceDetail(invoice.getInvoiceId());
                String logContent = "创建发票：发票号码=" + invoice.getInvoiceNumber();
                if (logRecordService != null) {
                    logRecordService.recordDataChangeLog("发票管理", "INVOICE", String.valueOf(invoice.getInvoiceId()),
                            "新增", logContent, null, newDetail, currentUserId, null, true, null);
                }
            } catch (Exception logException) {
                log.warn("记录发票创建数据变更日志失败", logException);
            }

            return result;
            
        } catch (Exception e) {
            log.error("创建发票失败", e);
            throw new RuntimeException("创建发票失败：" + e.getMessage(), e);
        }
    }

    /**
     * Excel导入发票（表格导入）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> excelImportInvoice(MultipartFile excelFile, String invoiceStatus) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> errorList = new ArrayList<>();
        List<Map<String, Object>> warningList = new ArrayList<>();

        try {
            // 1. 解析Excel文件，读取所有行数据
            List<Map<String, Object>> allRows = parseExcelFileForImport(excelFile);
            if (allRows == null || allRows.isEmpty()) {
                throw new BusinessException("Excel文件为空或没有数据行");
            }

            // 2. 按发票号码分组（优先使用数电发票号码）
            Map<String, List<Map<String, Object>>> groupedData = groupInvoiceDataByInvoiceNumber(allRows);

            // 3. 处理每个发票组（全量事务处理：一错全部回滚）
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
                String invoiceNumber = entry.getKey();
                List<Map<String, Object>> invoiceRows = entry.getValue();

                try {
                    // 3.1 构建发票头数据（使用第一行数据）
                    Map<String, Object> invoiceHeaderData = buildInvoiceHeaderFromExcelRows(invoiceRows.get(0), invoiceRows, invoiceStatus);
                    
                    // 3.2 公司信息验证（在构建发票头后立即验证）
                    validateCompanyInfoForExcel(invoiceHeaderData, invoiceStatus, invoiceNumber);

                    // 3.3 检查发票号码是否已存在
                    Invoice existingInvoice = invoiceMapper.selectByInvoiceNumber(invoiceNumber);
                    if (existingInvoice != null) {
                        // 发票号码已存在，立即抛出异常，触发全部回滚
                        throw new BusinessException(
                            String.format("Excel导入失败：发票号码已存在（发票号码：%s），已回滚全部导入结果", invoiceNumber)
                        );
                    }

                    // 3.4 构建发票实体
                    Invoice invoice = buildInvoiceFromExcelData(invoiceHeaderData, invoiceStatus);

                    // 3.4.1 设置创建人编码
                    Integer creatorId = SecurityUtil.getCurrentUserId();
                    invoice.setCreatorId(creatorId);

                    // 3.5 保存发票头
                    invoiceMapper.insert(invoice);

                    // 3.6 构建并保存发票明细
                    for (Map<String, Object> rowData : invoiceRows) {
                        InvoiceItem item = buildInvoiceItemFromExcelRow(rowData, invoice.getInvoiceId());
                        if (item != null) {
                            invoiceMapper.insertItem(item);
                        }
                    }

                    successList.add(createSuccessRecord(invoiceNumber, invoice.getInvoiceId()));

                } catch (BusinessException e) {
                    // 任何业务异常（包括公司信息验证失败、发票号码重复等），立即抛出，触发全部回滚
                    log.error("Excel导入失败，发票号码：{}，错误：{}，已回滚全部导入结果", invoiceNumber, e.getMessage());
                    throw e;
                } catch (Exception e) {
                    // 任何其他异常（解析错误、数据库错误等），立即抛出，触发全部回滚
                    log.error("Excel导入失败，发票号码：{}，错误：{}，已回滚全部导入结果", invoiceNumber, e.getMessage(), e);
                    throw new BusinessException(
                        String.format("Excel导入失败：处理发票时发生错误（发票号码：%s，错误：%s），已回滚全部导入结果", 
                            invoiceNumber, e.getMessage())
                    );
                }
            }

            // 4. 构建返回结果（只有所有发票都成功时才会执行到这里）
            result.put("total", groupedData.size());
            result.put("success", successList.size());
            result.put("error", 0);
            result.put("warning", warningList.size());
            result.put("successList", successList);
            result.put("errorList", new ArrayList<>());
            result.put("warningList", warningList);

            return result;

        } catch (BusinessException e) {
            // 业务异常统一向上抛出，由上层捕获并返回友好提示，事务将回滚
            log.error("Excel导入发票失败（业务异常触发回滚）：{}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Excel导入发票失败", e);
            throw new RuntimeException("Excel导入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 更新发票
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateInvoice(Integer invoiceId, Map<String, Object> invoiceData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 查询发票是否存在（保存旧数据用于日志记录）
            Map<String, Object> oldDetail = null;
            try {
                oldDetail = getInvoiceDetail(invoiceId);
            } catch (Exception e) {
                log.warn("查询发票旧数据失败，invoiceId={}", invoiceId, e);
            }
            
            Invoice existingInvoice = invoiceMapper.selectById(invoiceId);
            if (existingInvoice == null) {
                throw new RuntimeException("发票不存在，发票ID：" + invoiceId);
            }
            
            // 2. 检查发票是否被锁定
            if (existingInvoice.getIsLocked() != null && existingInvoice.getIsLocked()) {
                throw new RuntimeException("发票已被锁定，无法修改。锁定原因：" + 
                    (existingInvoice.getLockReason() != null ? existingInvoice.getLockReason() : "未知"));
            }
            
            // 3. 如果修改了发票号码，检查新发票号码是否重复
            String newInvoiceNumber = (String) invoiceData.get("invoiceNumber");
            if (newInvoiceNumber != null && !newInvoiceNumber.equals(existingInvoice.getInvoiceNumber())) {
                Invoice duplicateInvoice = invoiceMapper.selectByInvoiceNumber(newInvoiceNumber);
                if (duplicateInvoice != null && !duplicateInvoice.getInvoiceId().equals(invoiceId)) {
                    throw new RuntimeException("发票号码已存在：" + newInvoiceNumber);
                }
            }
            
            // 4. 构建发票实体（从Map数据）
            Invoice invoice = buildInvoiceFromMap(invoiceData);
            invoice.setInvoiceId(invoiceId); // 设置发票ID
            
            // 5. 保留原有字段（这些字段不应该被前端修改）
            // 开票人名称：如果前端没有传入，保留原有值；如果传入了，使用新值
            if (!invoiceData.containsKey("issuerName")) {
                invoice.setIssuerName(existingInvoice.getIssuerName());
            }
            invoice.setIsLocked(existingInvoice.getIsLocked()); // 保留锁定状态
            invoice.setLockTime(existingInvoice.getLockTime()); // 保留锁定时间
            invoice.setLockerId(existingInvoice.getLockerId()); // 保留锁定人编码
            invoice.setLockReason(existingInvoice.getLockReason()); // 保留锁定原因
            invoice.setCreateTime(existingInvoice.getCreateTime()); // 保留创建时间

            // 如果前端没有传入文件ID字段（字段不存在），保留原有文件ID（防止清空已上传的文件）
            // 注意：如果前端明确传入null，则允许清空文件关联
            if (!invoiceData.containsKey("pdfFileId")) {
                invoice.setPdfFileId(existingInvoice.getPdfFileId());
            }
            if (!invoiceData.containsKey("imageFileId")) {
                invoice.setImageFileId(existingInvoice.getImageFileId());
            }
            
            // 6. 处理金额字段
            if (invoice.getRecordDetails() == null || !invoice.getRecordDetails()) {
                // 不录入明细时，使用表单中的金额
                BigDecimal taxExcludedAmount = parseBigDecimalFromObject(invoiceData.get("taxExcludedAmount"));
                BigDecimal taxAmount = parseBigDecimalFromObject(invoiceData.get("taxAmount"));
                BigDecimal totalAmount = parseBigDecimalFromObject(invoiceData.get("totalAmount"));
                
                invoice.setTaxExcludedAmount(taxExcludedAmount);
                invoice.setTaxAmount(taxAmount);
                invoice.setTotalAmount(totalAmount);
                invoice.setAmount(taxExcludedAmount); // 金额 = 不含税金额
            } else {
                // 录入明细时，从明细计算金额（前端已计算好，这里直接使用）
                BigDecimal taxExcludedAmount = parseBigDecimalFromObject(invoiceData.get("taxExcludedAmount"));
                BigDecimal taxAmount = parseBigDecimalFromObject(invoiceData.get("taxAmount"));
                BigDecimal totalAmount = parseBigDecimalFromObject(invoiceData.get("totalAmount"));
                
                invoice.setTaxExcludedAmount(taxExcludedAmount);
                invoice.setTaxAmount(taxAmount);
                invoice.setTotalAmount(totalAmount);
                invoice.setAmount(taxExcludedAmount); // 金额 = 不含税金额
            }

            // 7. 更新发票主表
            int rows = invoiceMapper.updateById(invoice);
            if (rows == 0) {
                log.warn("更新发票主表失败（乐观锁冲突），invoiceId={}", invoiceId);
            }

            // 8. 处理发票明细：差分更新（只删除被删除的，只新增被新增的，只更新被修改的）
            if (invoice.getRecordDetails() != null && invoice.getRecordDetails()) {
                // 8.1 查询现有的明细列表
                List<InvoiceItem> existingItems = invoiceMapper.selectItemsByInvoiceId(invoiceId);
                Set<Integer> existingItemIds = new HashSet<>();
                Map<Integer, InvoiceItem> existingItemMap = new HashMap<>();
                if (existingItems != null) {
                    for (InvoiceItem existingItem : existingItems) {
                        if (existingItem.getItemId() != null) {
                            existingItemIds.add(existingItem.getItemId());
                            existingItemMap.put(existingItem.getItemId(), existingItem);
                        }
                    }
                }
                
                // 8.2 处理新明细列表
                Set<Integer> newItemIds = new HashSet<>();
                if (invoiceData.containsKey("details")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> detailsList = (List<Map<String, Object>>) invoiceData.get("details");
                    if (detailsList != null && !detailsList.isEmpty()) {
                        for (Map<String, Object> itemData : detailsList) {
                            Object itemIdObj = itemData.get("itemId");
                            Integer itemId = null;
                            if (itemIdObj != null) {
                                if (itemIdObj instanceof Integer) {
                                    itemId = (Integer) itemIdObj;
                                } else if (itemIdObj instanceof Number) {
                                    itemId = ((Number) itemIdObj).intValue();
                                } else if (itemIdObj instanceof String) {
                                    try {
                                        itemId = Integer.valueOf((String) itemIdObj);
                                    } catch (NumberFormatException e) {
                                        log.warn("解析明细ID失败：{}", itemIdObj);
                                    }
                                }
                            }
                            
                            InvoiceItem item = buildInvoiceItemFromMap(itemData, invoiceId);
                            
                            if (itemId != null && existingItemIds.contains(itemId)) {
                                // 已存在的明细，需要更新
                                item.setItemId(itemId);
                                invoiceMapper.updateItem(item);
                                newItemIds.add(itemId);
                            } else {
                                // 新增的明细（没有itemId或itemId不在现有列表中）
                                invoiceMapper.insertItem(item);
                                if (item.getItemId() != null) {
                                    newItemIds.add(item.getItemId());
                                }
                            }
                        }
                    }
                }
                
                // 8.3 删除不在新明细列表中的旧明细
                for (Integer existingItemId : existingItemIds) {
                    if (!newItemIds.contains(existingItemId)) {
                        invoiceMapper.deleteItemById(existingItemId);
                    }
                }
            } else {
                // 如果不录入明细，删除所有旧明细
                invoiceMapper.deleteItemsByInvoiceId(invoiceId);
            }
            
            // 9. 记录数据变更日志
            try {
                Integer currentUserId = SecurityUtil.getCurrentUserId();
                // 更新后重新查询新数据
                Map<String, Object> newDetail = getInvoiceDetail(invoiceId);
                String invoiceNumber = invoice.getInvoiceNumber();
                String logContent = invoiceNumber != null ? "更新发票：发票号码=" + invoiceNumber : "更新发票：发票ID=" + invoiceId;
                if (logRecordService != null && oldDetail != null) {
                    logRecordService.recordDataChangeLog("发票管理", "INVOICE", String.valueOf(invoiceId), 
                            "更新", logContent, oldDetail, newDetail, currentUserId, null, true, null);
                }
            } catch (Exception logException) {
                // 日志记录失败不应该影响主业务流程
                log.warn("记录发票更新数据变更日志失败", logException);
            }
            
            // 10. 构建返回结果
            result.put("invoiceId", invoice.getInvoiceId());
            result.put("invoiceNumber", invoice.getInvoiceNumber());
            result.put("message", "发票更新成功");

            return result;
            
        } catch (Exception e) {
            log.error("更新发票失败，invoiceId={}", invoiceId, e);
            throw new RuntimeException("更新发票失败：" + e.getMessage(), e);
        }
    }

    /**
     * 发票分页查询
     * 根据员工的viewScope配置进行数据范围过滤
     */
    @Override
    public IPage<InvoicePageResponse> getInvoicePage(InvoicePageRequest request) {
        // 兜底处理分页参数
        int page = request.getPage() == null || request.getPage() <= 0 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() <= 0 ? 10 : request.getSize();

        // 根据发票类型确定页面权限编码
        String pageCode = "销项发票".equals(request.getInvoiceStatus())
            ? "财务管理:发票管理:销项发票:页面"
            : "财务管理:发票管理:进项发票:页面";

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(pageCode, request.getViewScope());

        // 数据范围过滤
        Integer creatorFilter = null;
        // SELF 模式需要添加创建人过滤条件
        if (ViewScopeHelper.isSelfScope(viewScope)) {
            Integer currentUserId = SecurityUtil.getCurrentUserId();
            creatorFilter = currentUserId;
        }
        // ALL 模式不添加限制，查询全部数据

        // 将creatorFilter设置到请求对象中，供Mapper使用
        request.setCreatorFilter(creatorFilter);

        Page<InvoicePageResponse> pageObj = new Page<>(page, size);
        return invoiceMapper.selectInvoicePage(pageObj, request);
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


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> fileSupplement(MultipartFile[] files, String invoiceStatus) {
        if (files == null || files.length == 0) {
            throw new BusinessException("文件列表不能为空");
        }
        if (files.length > 50) {
            throw new BusinessException("文件数量超出上限（最多50个），已终止并回滚");
        }
        if (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
            throw new BusinessException("发票状态必须是'进项发票'或'销项发票'");
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> errorList = new ArrayList<>();

        // 按发票号缓存要更新的发票，避免重复查询
        Map<String, Invoice> invoiceCache = new HashMap<>();

        try {
            for (MultipartFile file : files) {
                String originalName = file.getOriginalFilename();
                // 1. 文件基础校验与类型校验
                ValidationResult validation = FileValidator.validate(
                        file,
                        FileValidator.FileType.PDF,
                        FileValidator.FileType.JPEG,
                        FileValidator.FileType.PNG,
                        FileValidator.FileType.GIF,
                        FileValidator.FileType.BMP
                );
                if (!validation.isValid()) {
                    throw new BusinessException("文件补充失败，已全部回滚：" + validation.getMessage());
                }

                // 2. 提取发票号：PDF优先按文件内容匹配，失败后回退到文件名匹配
                String invoiceNumber = resolveInvoiceNumberForSupplement(file, validation.getFileType(), originalName);
                if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                    throw new BusinessException("文件名格式错误，无法提取发票号：" + originalName);
                }
                invoiceNumber = invoiceNumber.trim();

                // 3. 获取发票并缓存
                Invoice invoice = invoiceCache.get(invoiceNumber);
                if (invoice == null) {
                    invoice = invoiceMapper.selectByInvoiceNumber(invoiceNumber);
                    if (invoice == null) {
                        throw new BusinessException("未找到匹配的发票记录：发票号 " + invoiceNumber);
                    }
                    // 兜底：保证发票号、主键不为空
                    if (invoice.getInvoiceNumber() == null) {
                        invoice.setInvoiceNumber(invoiceNumber);
                    }
                    if (invoice.getInvoiceId() == null) {
                        throw new BusinessException("文件补充失败：发票记录缺少主键，发票号：" + invoiceNumber);
                    }
                    invoiceCache.put(invoiceNumber, invoice);
                }

                // 4. 上传文件
                File fileEntity = fileService.uploadAndSave(file, "INVOICE_SUPPLEMENT", null);
                if (fileEntity == null || fileEntity.getFileId() == null) {
                    throw new BusinessException("文件上传失败：" + originalName);
                }

                // 5. 按类型写入对应字段
                FileValidator.FileType fileType = validation.getFileType();
                if (fileType == FileValidator.FileType.PDF) {
                    invoice.setPdfFileId(fileEntity.getFileId());
                } else {
                    // 图片类
                    invoice.setImageFileId(fileEntity.getFileId());
                }

                // 6. 记录成功项
                Map<String, Object> success = new HashMap<>();
                success.put("fileName", originalName);
                success.put("invoiceNumber", invoiceNumber);
                success.put("fileType", getFileTypeName(fileType));
                successList.add(success);
            }

            // 7. 更新所有发票（记录数据变更日志与发送通知）
            for (Map.Entry<String, Invoice> entry : invoiceCache.entrySet()) {
                String num = entry.getKey();
                Invoice invoice = entry.getValue();
                // 确保发票号不为空
                if (invoice.getInvoiceNumber() == null) {
                    invoice.setInvoiceNumber(num);
                }

                // 记录旧数据（用于数据变更日志）
                Map<String, Object> oldDetail = null;
                try {
                    oldDetail = getInvoiceDetail(invoice.getInvoiceId());
                } catch (Exception e) {
                    log.warn("获取发票旧数据失败（用于文件补充日志），invoiceId={}", invoice.getInvoiceId(), e);
                }

                int updated = invoiceMapper.updateById(invoice);
                if (updated <= 0) {
                    String displayNo = invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : num;
                    throw new BusinessException("文件补充失败：更新发票附件失败（发票号：" + displayNo + "）");
                }

                // 记录数据变更日志（不影响主流程）
                try {
                    if (logRecordService != null && oldDetail != null) {
                        Map<String, Object> newDetail = getInvoiceDetail(invoice.getInvoiceId());
                        String content = "批量文件补充：发票号=" + invoice.getInvoiceNumber();
                        logRecordService.recordDataChangeLog("发票管理", "INVOICE", String.valueOf(invoice.getInvoiceId()),
                                "更新", content, oldDetail, newDetail, SecurityUtil.getCurrentUserId(), null, true, null);
                    }
                } catch (Exception logEx) {
                    log.warn("记录发票文件补充数据变更日志失败，invoiceId={}", invoice.getInvoiceId(), logEx);
                }
            }

            result.put("total", files.length);
            result.put("success", successList.size());
            result.put("error", 0);
            result.put("successList", successList);
            result.put("errorList", errorList);
            result.put("rollbackMessage", null);
            return result;
        } catch (BusinessException e) {
            // 业务异常触发回滚
            throw e;
        } catch (Exception e) {
            log.error("批量文件补充失败", e);
            throw new BusinessException("文件补充失败，已全部回滚：" + e.getMessage());
        }
    }

    /**
     * ZIP批量全导时解析PDF对应的发票号：优先读取PDF内容中的纯数字匹配，失败后回退到文件名
     */
    private String resolveInvoiceNumberForZipPdf(byte[] fileData, String originalName) {
        String invoiceNumberFromPdf = extractInvoiceNumberFromPdf(fileData, originalName);
        if (invoiceNumberFromPdf != null && !invoiceNumberFromPdf.trim().isEmpty()) {
            return invoiceNumberFromPdf.trim();
        }
        return extractInvoiceNumberFromFilename(originalName);
    }

    /**
     * 批量补充文件时解析发票号：PDF 优先从内容中的纯数字匹配，失败时回退到文件名
     */
    private String resolveInvoiceNumberForSupplement(MultipartFile file, FileValidator.FileType fileType, String originalName) {
        if (fileType == FileValidator.FileType.PDF) {
            String invoiceNumberFromPdf = extractInvoiceNumberFromPdf(file);
            if (invoiceNumberFromPdf != null && !invoiceNumberFromPdf.trim().isEmpty()) {
                return invoiceNumberFromPdf.trim();
            }
        }
        return extractInvoiceNumberFromFilename(originalName);
    }

    /**
     * 使用 PDFBox 从 PDF 文本中提取发票号码
     */
    private String extractInvoiceNumberFromPdf(MultipartFile file) {
        try {
            return extractInvoiceNumberFromPdf(file.getBytes(), file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("读取PDF内容匹配发票号失败，fileName={}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * 使用 PDFBox 从 PDF 文本中提取发票号码
     */
    private String extractInvoiceNumberFromPdf(byte[] fileData, String fileName) {
        try (PDDocument document = PDDocument.load(fileData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                return null;
            }

            java.util.regex.Matcher pureNumberMatcher = java.util.regex.Pattern
                    .compile("(?<!\\d)([0-9]{8,20})(?!\\d)")
                    .matcher(text);
            while (pureNumberMatcher.find()) {
                String candidate = pureNumberMatcher.group(1);
                if (candidate != null && invoiceMapper.selectByInvoiceNumber(candidate.trim()) != null) {
                    return candidate.trim();
                }
            }
        } catch (Exception e) {
            log.warn("读取PDF内容匹配发票号失败，fileName={}", fileName, e);
        }
        return null;
    }

    /**
     * 从文件名提取发票号，规则：dzfp_发票号_...
     */
    private String extractInvoiceNumberFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        String name = filename;
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (slash >= 0 && slash < filename.length() - 1) {
            name = filename.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        if (!name.startsWith("dzfp_")) {
            return null;
        }
        String[] parts = name.split("_");
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }

    /**
     * 映射FileType到前端展示名称
     */
    private String getFileTypeName(FileValidator.FileType fileType) {
        switch (fileType) {
            case PDF:
                return "PDF";
            case JPEG:
            case PNG:
            case GIF:
            case BMP:
                return "IMAGE";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 从Map数据构建Invoice实体
     */
    private Invoice buildInvoiceFromMap(Map<String, Object> data) {
        Invoice invoice = new Invoice();
        
        // 基本信息
        invoice.setInvoiceType((String) data.get("invoiceType"));
        invoice.setInvoiceForm((String) data.get("invoiceForm"));
        invoice.setInvoiceNature((String) data.get("invoiceNature"));
        invoice.setInvoiceStatus((String) data.get("invoiceStatus"));
        invoice.setInvoiceNumber((String) data.get("invoiceNumber"));
        invoice.setInvoiceCode((String) data.get("invoiceCode"));
        
        // 开票日期
        String invoiceDateStr = (String) data.get("invoiceDate");
        if (invoiceDateStr != null && !invoiceDateStr.isEmpty()) {
            invoice.setInvoiceDate(parseDateTime(invoiceDateStr));
        }
        
        // 购买方信息
        invoice.setBuyerName((String) data.get("buyerName"));
        invoice.setBuyerCreditCode((String) data.get("buyerCreditCode"));
        invoice.setBuyerAddress((String) data.get("buyerAddress"));
        invoice.setBuyerPhone((String) data.get("buyerPhone"));
        invoice.setBuyerBankName((String) data.get("buyerBankName"));
        invoice.setBuyerBankAccount((String) data.get("buyerBankAccount"));
        
        // 销售方信息
        invoice.setSellerName((String) data.get("sellerName"));
        invoice.setSellerCreditCode((String) data.get("sellerCreditCode"));
        invoice.setSellerAddress((String) data.get("sellerAddress"));
        invoice.setSellerPhone((String) data.get("sellerPhone"));
        invoice.setSellerBankName((String) data.get("sellerBankName"));
        invoice.setSellerBankAccount((String) data.get("sellerBankAccount"));
        
        // 发票数据
        invoice.setRecordDetails((Boolean) data.get("recordDetails"));
        invoice.setTaxExcludedAmount(parseBigDecimalFromObject(data.get("taxExcludedAmount")));
        invoice.setTaxAmount(parseBigDecimalFromObject(data.get("taxAmount")));
        invoice.setTotalAmount(parseBigDecimalFromObject(data.get("totalAmount")));
        invoice.setAmount(parseBigDecimalFromObject(data.get("taxExcludedAmount"))); // 金额 = 不含税金额
        // 价税合计大写：从前端传入的中文大写文字
        String totalAmountInChinese = (String) data.get("totalAmountInChinese");
        if (totalAmountInChinese != null && !totalAmountInChinese.trim().isEmpty()) {
            invoice.setTotalAmountInChinese(totalAmountInChinese.trim());
        } else {
            // 如果前端未传入，设置为null
            invoice.setTotalAmountInChinese(null);
        }
        
        // 文件ID：如果字段存在（包括null值），则设置；如果字段不存在，则不设置（保留原值）
        if (data.containsKey("pdfFileId")) {
            Object pdfFileIdObj = data.get("pdfFileId");
            if (pdfFileIdObj != null) {
                invoice.setPdfFileId(pdfFileIdObj instanceof Integer ? (Integer) pdfFileIdObj : Integer.valueOf(pdfFileIdObj.toString()));
            } else {
                invoice.setPdfFileId(null); // 明确传入null，清空文件关联
            }
        }
        if (data.containsKey("imageFileId")) {
            Object imageFileIdObj = data.get("imageFileId");
            if (imageFileIdObj != null) {
                invoice.setImageFileId(imageFileIdObj instanceof Integer ? (Integer) imageFileIdObj : Integer.valueOf(imageFileIdObj.toString()));
            } else {
                invoice.setImageFileId(null); // 明确传入null，清空文件关联
            }
        }
        
        // 备注
        invoice.setRemark((String) data.get("remark"));
        
        // 开票人名称
        String issuerName = (String) data.get("issuerName");
        if (issuerName != null) {
            invoice.setIssuerName(issuerName.trim());
        } else {
            invoice.setIssuerName(null);
        }
        
        return invoice;
    }

    /**
     * 从Map数据构建InvoiceItem实体
     */
    private InvoiceItem buildInvoiceItemFromMap(Map<String, Object> itemData, Integer invoiceId) {
        InvoiceItem item = new InvoiceItem();
        
        item.setInvoiceId(invoiceId);
        item.setProductName((String) itemData.get("productName"));
        item.setSpecification((String) itemData.get("specification"));
        item.setUnit((String) itemData.get("unit"));
        item.setQuantity(parseBigDecimalFromObject(itemData.get("quantity")));
        item.setUnitPrice(parseBigDecimalFromObject(itemData.get("unitPrice")));
        item.setAmount(parseBigDecimalFromObject(itemData.get("amount")));
        
        // 税率处理：前端传的是百分比（如13表示13%），数据库存的是小数（0.13）
        BigDecimal taxRate = parseBigDecimalFromObject(itemData.get("taxRate"));
        if (taxRate.compareTo(BigDecimal.ONE) > 0) {
            // 如果税率大于1，说明是百分比格式，需要转换为小数
            taxRate = taxRate.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP);
        }
        item.setTaxRate(taxRate);
        
        item.setProductTaxAmount(parseBigDecimalFromObject(itemData.get("productTaxAmount")));
        item.setTaxClassificationCode((String) itemData.get("taxClassificationCode"));
        item.setTaxIncludedAmount(parseBigDecimalFromObject(itemData.get("taxIncludedAmount")));
        item.setRemark((String) itemData.get("remark"));
        
        return item;
    }

    /**
     * 解析BigDecimal（从Object类型）
     */
    private BigDecimal parseBigDecimalFromObject(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                log.warn("解析BigDecimal失败：{}", value);
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 查找Excel统计文件
     * 文件名格式：发票批量下载情况统计_时间戳.xlsx 或 发票批量下载情况统计_时间戳.xls
     * ZIP包中应该只包含一个这样的文件
     */
    private String findExcelFile(Set<String> fileNames) {
        List<String> matchedFiles = new ArrayList<>();
        for (String fileName : fileNames) {
            if (fileName.toLowerCase().startsWith("发票批量下载情况统计")
                && (fileName.toLowerCase().endsWith(".xlsx") || fileName.toLowerCase().endsWith(".xls"))) {
                matchedFiles.add(fileName);
            }
        }

        if (matchedFiles.isEmpty()) {
            return null;
        } else if (matchedFiles.size() > 1) {
            throw new RuntimeException("找到多个Excel统计文件：" + matchedFiles.toString() + "，请确保ZIP包中只有一个统计文件");
        } else {
            return matchedFiles.get(0);
        }
    }

    /**
     * 解析Excel文件，提取发票号码
     */
    private List<String> parseExcelFile(byte[] excelData) throws Exception {
        List<String> invoiceNumbers = new ArrayList<>();
        
        try (InputStream is = new java.io.ByteArrayInputStream(excelData);
             Workbook workbook = WorkbookFactory.create(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // 查找发票号码列（第一列）
            int invoiceNumberColIndex = 0; // 假设第一列是发票号码
            
            // 从第二行开始读取（第一行是表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell cell = row.getCell(invoiceNumberColIndex);
                if (cell == null) continue;
                
                String invoiceNumber = getCellValueAsString(cell);
                if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
                    invoiceNumbers.add(invoiceNumber.trim());
                }
            }
        }
        
        return invoiceNumbers;
    }

    /**
     * 获取单元格值（字符串）
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字格式，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 上传发票文件到FILE表
     */
    private Integer uploadInvoiceFile(byte[] fileData, String fileName, String invoiceNumber, String fileType) {
        try {
            // 创建临时MultipartFile
            MultipartFile multipartFile = new InMemoryMultipartFile(fileName, fileData);
            
            // 调用FileService上传文件
            File fileEntity = fileService.uploadAndSave(
                multipartFile,
                "INVOICE",
                null
            );
            
            return fileEntity.getFileId();
        } catch (Exception e) {
            log.error("上传发票文件失败：{}", fileName, e);
            return null;
        }
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // 尝试多种日期格式
            String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd"
            };
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    if (pattern.contains("HH:mm:ss")) {
                        return LocalDateTime.parse(dateTimeStr, formatter);
                    } else {
                        return LocalDateTime.parse(dateTimeStr + " 00:00:00", 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                } catch (Exception e) {
                    // 继续尝试下一个格式
                }
            }
        } catch (Exception e) {
            log.warn("解析日期失败：{}", dateTimeStr, e);
        }
        return LocalDateTime.now();
    }

    /**
     * 创建成功记录
     */
    private Map<String, Object> createSuccessRecord(String invoiceNumber, Integer invoiceId) {
        Map<String, Object> record = new HashMap<>();
        record.put("invoiceNumber", invoiceNumber);
        record.put("invoiceId", invoiceId);
        return record;
    }

    /**
     * 创建错误记录
     */
    private Map<String, Object> createErrorRecord(String invoiceNumber, String errorMessage) {
        Map<String, Object> record = new HashMap<>();
        record.put("invoiceNumber", invoiceNumber);
        record.put("errorMessage", errorMessage);
        return record;
    }

    /**
     * 内存中的MultipartFile实现
     */
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final byte[] content;

        public InMemoryMultipartFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            // 根据文件名推断MIME类型
            return FileValidator.getMimeTypeByFilename(name);
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IllegalStateException, IOException {
            if (content == null || content.length == 0) {
                throw new IllegalStateException("文件内容为空");
            }
            // 确保父目录存在
            java.io.File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            // 将内存中的字节数组写入文件
            Files.write(dest.toPath(), content);
        }
    }

    /**
     * 根据发票ID查询发票详情（包含所有字段和明细）
     * 使用连表查询一次性获取发票信息和明细列表
     */
    @Override
    public Map<String, Object> getInvoiceDetail(Integer invoiceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 使用连表查询一次性获取发票信息和明细列表
            com.erp.controller.finance.dto.InvoiceDetailDTO invoiceDetail = invoiceMapper.selectInvoiceDetailWithItems(invoiceId);
            if (invoiceDetail == null) {
                throw new RuntimeException("发票不存在，发票ID：" + invoiceId);
            }
            
            // 将DTO转换为Map，包含所有字段
            result.put("invoiceId", invoiceDetail.getInvoiceId());
            result.put("invoiceType", invoiceDetail.getInvoiceType());
            result.put("invoiceForm", invoiceDetail.getInvoiceForm());
            result.put("invoiceNature", invoiceDetail.getInvoiceNature());
            result.put("invoiceStatus", invoiceDetail.getInvoiceStatus());
            result.put("invoiceNumber", invoiceDetail.getInvoiceNumber());
            result.put("invoiceCode", invoiceDetail.getInvoiceCode());
            result.put("invoiceDate", invoiceDetail.getInvoiceDate() != null ? 
                invoiceDetail.getInvoiceDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
            result.put("amount", invoiceDetail.getAmount());
            result.put("taxAmount", invoiceDetail.getTaxAmount());
            result.put("totalAmount", invoiceDetail.getTotalAmount());
            result.put("totalAmountInChinese", invoiceDetail.getTotalAmountInChinese());
            result.put("taxExcludedAmount", invoiceDetail.getTaxExcludedAmount());
            result.put("recordDetails", invoiceDetail.getRecordDetails());
            result.put("remark", invoiceDetail.getRemark());
            result.put("issuerName", invoiceDetail.getIssuerName());
            result.put("pdfFileId", invoiceDetail.getPdfFileId());
            result.put("imageFileId", invoiceDetail.getImageFileId());
            result.put("isLocked", invoiceDetail.getIsLocked());
            result.put("lockTime", invoiceDetail.getLockTime() != null ? 
                invoiceDetail.getLockTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            result.put("lockerId", invoiceDetail.getLockerId());
            result.put("lockReason", invoiceDetail.getLockReason());
            
            // 购买方信息
            result.put("buyerName", invoiceDetail.getBuyerName());
            result.put("buyerCreditCode", invoiceDetail.getBuyerCreditCode());
            result.put("buyerAddress", invoiceDetail.getBuyerAddress());
            result.put("buyerPhone", invoiceDetail.getBuyerPhone());
            result.put("buyerBankName", invoiceDetail.getBuyerBankName());
            result.put("buyerBankAccount", invoiceDetail.getBuyerBankAccount());
            
            // 销售方信息
            result.put("sellerName", invoiceDetail.getSellerName());
            result.put("sellerCreditCode", invoiceDetail.getSellerCreditCode());
            result.put("sellerAddress", invoiceDetail.getSellerAddress());
            result.put("sellerPhone", invoiceDetail.getSellerPhone());
            result.put("sellerBankName", invoiceDetail.getSellerBankName());
            result.put("sellerBankAccount", invoiceDetail.getSellerBankAccount());
            
            // 将明细列表转换为Map列表
            List<Map<String, Object>> detailsList = new ArrayList<>();
            if (invoiceDetail.getDetails() != null && !invoiceDetail.getDetails().isEmpty()) {
                for (InvoiceItem item : invoiceDetail.getDetails()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("itemId", item.getItemId());
                    itemMap.put("productName", item.getProductName());
                    itemMap.put("specification", item.getSpecification());
                    itemMap.put("unit", item.getUnit());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("amount", item.getAmount());
                    itemMap.put("taxRate", item.getTaxRate());
                    itemMap.put("productTaxAmount", item.getProductTaxAmount());
                    itemMap.put("taxClassificationCode", item.getTaxClassificationCode());
                    itemMap.put("taxIncludedAmount", item.getTaxIncludedAmount());
                    itemMap.put("remark", item.getRemark());
                    detailsList.add(itemMap);
                }
            }
            result.put("details", detailsList);
            
            return result;
            
        } catch (Exception e) {
            log.error("查询发票详情失败，invoiceId={}", invoiceId, e);
            throw new RuntimeException("查询发票详情失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询发票文件编号
     * 查询发票是否有PDF、OFD、XML、图片文件
     *
     * @param invoiceId 发票ID
     * @return 文件编号信息（pdfFileId, imageFileId）
     */
    @Override
    public Map<String, Object> getInvoiceFileIds(Integer invoiceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询发票基本信息
            Invoice invoice = invoiceMapper.selectById(invoiceId);
            if (invoice == null) {
                throw new RuntimeException("发票不存在，发票ID：" + invoiceId);
            }
            
            // 返回文件编号信息
            result.put("pdfFileId", invoice.getPdfFileId());
            result.put("imageFileId", invoice.getImageFileId());
            
            return result;
            
        } catch (Exception e) {
            log.error("查询发票文件编号失败，invoiceId={}", invoiceId, e);
            throw new RuntimeException("查询发票文件编号失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析Excel文件，读取所有行数据（用于Excel导入）
     */
    private List<Map<String, Object>> parseExcelFileForImport(MultipartFile excelFile) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel中没有可用的Sheet");
            }

            // 读取表头行（第一行）
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("Excel表头行为空");
            }

            // 构建列名映射（列索引 -> 列名）
            Map<Integer, String> columnMap = new HashMap<>();
            for (int i = 0; i <= headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String columnName = getCellValueAsString(cell);
                    if (columnName != null && !columnName.trim().isEmpty()) {
                        columnMap.put(i, columnName.trim());
                    }
                }
            }

            // 从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // 检查是否为空行
                boolean isEmptyRow = true;
                for (int j = 0; j <= row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        String value = getCellValueAsString(cell);
                        if (value != null && !value.trim().isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                }
                if (isEmptyRow) {
                    continue;
                }

                // 构建行数据Map
                Map<String, Object> rowData = new HashMap<>();
                for (Map.Entry<Integer, String> entry : columnMap.entrySet()) {
                    int colIndex = entry.getKey();
                    String columnName = entry.getValue();
                    Cell cell = row.getCell(colIndex);
                    String cellValue = getCellValueAsString(cell);
                    rowData.put(columnName, cellValue);
                }
                rows.add(rowData);
            }
        }
        
        return rows;
    }

    /**
     * 按发票号码分组数据（优先使用数电发票号码）
     */
    private Map<String, List<Map<String, Object>>> groupInvoiceDataByInvoiceNumber(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
        
        for (Map<String, Object> row : rows) {
            String invoiceNumber = getInvoiceNumberForGrouping(row);
            if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                continue; // 跳过没有发票号码的行
            }
            
            groupedData.computeIfAbsent(invoiceNumber, k -> new ArrayList<>()).add(row);
        }
        
        return groupedData;
    }

    /**
     * 获取用于分组的发票号码（优先使用数电发票号码）
     */
    private String getInvoiceNumberForGrouping(Map<String, Object> rowData) {
        String digitalInvoiceNumber = (String) rowData.get("数电发票号码");
        String invoiceNumber = (String) rowData.get("发票号码");
        
        // 优先使用数电发票号码
        if (digitalInvoiceNumber != null && !digitalInvoiceNumber.trim().isEmpty()) {
            return digitalInvoiceNumber.trim();
        }
        
        // 如果数电发票号码为空，使用发票号码
        if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
            return invoiceNumber.trim();
        }
        
        return null;
    }

    /**
     * 从Excel行数据构建发票头数据
     */
    private Map<String, Object> buildInvoiceHeaderFromExcelRows(Map<String, Object> firstRow, 
                                                                 List<Map<String, Object>> allRows, 
                                                                 String invoiceStatus) {
        Map<String, Object> headerData = new HashMap<>();
        
        // 发票号码和发票代码处理（优先使用数电发票号码）
        String digitalInvoiceNumber = (String) firstRow.get("数电发票号码");
        String invoiceNumber = (String) firstRow.get("发票号码");
        String invoiceCode = (String) firstRow.get("发票代码");
        
        if (digitalInvoiceNumber != null && !digitalInvoiceNumber.trim().isEmpty()) {
            // 数电发票：使用数电发票号码，发票代码为空或从数电发票号码提取
            headerData.put("invoiceNumber", digitalInvoiceNumber.trim());
            if (digitalInvoiceNumber.trim().length() >= 10) {
                headerData.put("invoiceCode", digitalInvoiceNumber.trim().substring(0, 10));
            } else {
                headerData.put("invoiceCode", null);
            }
        } else {
            // 普通发票：使用发票号码和发票代码
            headerData.put("invoiceNumber", invoiceNumber != null ? invoiceNumber.trim() : null);
            headerData.put("invoiceCode", invoiceCode != null ? invoiceCode.trim() : null);
        }
        
        // 销方信息
        headerData.put("sellerCreditCode", firstRow.get("销方识别号"));
        headerData.put("sellerName", firstRow.get("销方名称"));
        
        // 购方信息
        headerData.put("buyerCreditCode", firstRow.get("购方识别号"));
        headerData.put("buyerName", firstRow.get("购买方名称"));
        // 备注（如果有）
        headerData.put("remark", firstRow.get("备注"));
        
        // 开票日期
        headerData.put("invoiceDate", firstRow.get("开票日期"));
        
        // 发票票种解析（如果存在）
        String invoiceTypeText = (String) firstRow.get("发票票种");
        if (invoiceTypeText != null && !invoiceTypeText.trim().isEmpty()) {
            Map<String, String> parsedType = parseInvoiceTypeText(invoiceTypeText);
            headerData.put("invoiceForm", parsedType.get("invoiceForm"));
            headerData.put("invoiceType", parsedType.get("invoiceType"));
        } else {
            // 默认值
            headerData.put("invoiceForm", "数电发票");
            headerData.put("invoiceType", "增值税专用发票");
        }
        
        // 发票性质处理
        String invoiceNature = processInvoiceNatureFromExcel(firstRow);
        headerData.put("invoiceNature", invoiceNature);
        
        // 开票人名称
        headerData.put("issuerName", firstRow.get("开票人"));
        
        // 发票状态
        headerData.put("invoiceStatus", invoiceStatus);
        
        // 计算发票总金额（从明细行汇总）
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        BigDecimal totalTaxIncludedAmount = BigDecimal.ZERO;
        
        for (Map<String, Object> row : allRows) {
            // 金额相关字段允许为空，使用通用解析方法，空值按0处理
            BigDecimal amount = parseBigDecimalFromObject(row.get("金额"));
            BigDecimal taxAmount = parseBigDecimalFromObject(row.get("税额"));
            BigDecimal taxIncludedAmount = parseBigDecimalFromObject(row.get("价税合计"));
            
            if (amount != null) {
                totalAmount = totalAmount.add(amount);
            }
            if (taxAmount != null) {
                totalTaxAmount = totalTaxAmount.add(taxAmount);
            }
            if (taxIncludedAmount != null) {
                totalTaxIncludedAmount = totalTaxIncludedAmount.add(taxIncludedAmount);
            }
        }
        
        headerData.put("taxExcludedAmount", totalAmount);
        headerData.put("taxAmount", totalTaxAmount);
        headerData.put("totalAmount", totalTaxIncludedAmount);
        headerData.put("amount", totalAmount); // 金额 = 不含税金额
        // 价税合计大写：金额汇总完成后进行中文大写转换
        headerData.put("totalAmountInChinese",
            NumberChineseFormatter.format(totalTaxIncludedAmount.doubleValue(), true, true));

        // 必填校验：发票基础字段不能为空
        String headerInvoiceNumber = (String) headerData.get("invoiceNumber");
        if (headerInvoiceNumber == null || headerInvoiceNumber.trim().isEmpty()) {
            throw new BusinessException("Excel导入失败：发票号码为空，已回滚全部导入结果");
        }
        String buyerName = (String) headerData.get("buyerName");
        String sellerName = (String) headerData.get("sellerName");
        if (buyerName == null || buyerName.toString().trim().isEmpty()) {
            throw new BusinessException(String.format("Excel导入失败：发票号码 %s 的购方名称为空，已回滚全部导入结果", headerInvoiceNumber));
        }
        if (sellerName == null || sellerName.toString().trim().isEmpty()) {
            throw new BusinessException(String.format("Excel导入失败：发票号码 %s 的销方名称为空，已回滚全部导入结果", headerInvoiceNumber));
        }
        
        return headerData;
    }

    /**
     * 从Excel数据构建Invoice实体
     */
    private Invoice buildInvoiceFromExcelData(Map<String, Object> headerData, String invoiceStatus) {
        Invoice invoice = new Invoice();
        
        // 基本信息
        invoice.setInvoiceNumber((String) headerData.get("invoiceNumber"));
        invoice.setInvoiceCode((String) headerData.get("invoiceCode"));
        invoice.setInvoiceType((String) headerData.get("invoiceType"));
        invoice.setInvoiceForm((String) headerData.get("invoiceForm"));
        invoice.setInvoiceNature((String) headerData.get("invoiceNature"));
        invoice.setInvoiceStatus(invoiceStatus);
        
        // 开票日期
        String invoiceDateStr = (String) headerData.get("invoiceDate");
        if (invoiceDateStr != null && !invoiceDateStr.isEmpty()) {
            invoice.setInvoiceDate(parseDateTime(invoiceDateStr));
        }
        
        // 购买方信息
        invoice.setBuyerName((String) headerData.get("buyerName"));
        invoice.setBuyerCreditCode((String) headerData.get("buyerCreditCode"));
        
        // 销售方信息
        invoice.setSellerName((String) headerData.get("sellerName"));
        invoice.setSellerCreditCode((String) headerData.get("sellerCreditCode"));
        invoice.setRemark((String) headerData.get("remark"));
        
        // 金额信息
        invoice.setTaxExcludedAmount((BigDecimal) headerData.get("taxExcludedAmount"));
        invoice.setTaxAmount((BigDecimal) headerData.get("taxAmount"));
        invoice.setTotalAmount((BigDecimal) headerData.get("totalAmount"));
        invoice.setAmount((BigDecimal) headerData.get("amount"));
        invoice.setTotalAmountInChinese((String) headerData.get("totalAmountInChinese"));
        
        // 开票人名称
        String issuerName = (String) headerData.get("issuerName");
        if (issuerName != null && !issuerName.trim().isEmpty()) {
            invoice.setIssuerName(issuerName.trim());
        } else {
            invoice.setIssuerName(null);
        }
        
        // 其他字段
        invoice.setRecordDetails(true);
        
        return invoice;
    }

    /**
     * 从Excel行数据构建InvoiceItem实体
     */
    private InvoiceItem buildInvoiceItemFromExcelRow(Map<String, Object> rowData, Integer invoiceId) {
        InvoiceItem item = new InvoiceItem();
        
        item.setInvoiceId(invoiceId);
        item.setTaxClassificationCode((String) rowData.get("税收分类编码"));
        
        // 商品名称（货物或应税劳务名称）是必填字段，不能为空
        String productName = (String) rowData.get("货物或应税劳务名称");
        if (productName == null || productName.trim().isEmpty()) {
            throw new BusinessException("Excel导入失败：明细行中'货物或应税劳务名称'字段不能为空");
        }
        item.setProductName(productName.trim());
        
        item.setSpecification((String) rowData.get("规格型号"));
        item.setUnit((String) rowData.get("单位"));
        // 数量、单价、金额等金额字段允许为空，使用通用解析方法，空值按0处理
        item.setQuantity(parseBigDecimalFromObject(rowData.get("数量")));
        item.setUnitPrice(parseBigDecimalFromObject(rowData.get("单价")));
        item.setAmount(parseBigDecimalFromObject(rowData.get("金额")));
        
        // 税率处理：Excel中的税率可能是百分比格式（如6%或3%），需要转换为小数
        BigDecimal taxRate = convertTaxRateFromExcel(rowData.get("税率"));
        item.setTaxRate(taxRate);
        
        item.setProductTaxAmount(parseBigDecimalFromObject(rowData.get("税额")));
        item.setTaxIncludedAmount(parseBigDecimalFromObject(rowData.get("价税合计")));
        
        return item;
    }

    /**
     * 解析发票票种字段
     */
    private Map<String, String> parseInvoiceTypeText(String invoiceTypeText) {
        Map<String, String> result = new HashMap<>();
        
        if (invoiceTypeText == null || invoiceTypeText.trim().isEmpty()) {
            result.put("invoiceForm", "数电发票");
            result.put("invoiceType", "增值税专用发票");
            return result;
        }
        
        String text = invoiceTypeText.trim();
        
        // 检查是否包含括号
        int leftBracketIndex = text.indexOf('(');
        int rightBracketIndex = text.indexOf(')');
        
        if (leftBracketIndex > 0 && rightBracketIndex > leftBracketIndex) {
            // 提取发票形式（括号前的部分）
            String invoiceForm = text.substring(0, leftBracketIndex).trim();
            // 提取发票类型（括号内的部分）
            String invoiceType = text.substring(leftBracketIndex + 1, rightBracketIndex).trim();
            
            result.put("invoiceForm", invoiceForm);
            result.put("invoiceType", invoiceType);
        } else {
            // 没有括号，根据关键词判断
            String invoiceForm = "数电发票"; // 默认
            String invoiceType = "增值税专用发票"; // 默认
            
            if (text.contains("专用发票")) {
                invoiceType = "增值税专用发票";
            } else if (text.contains("普通发票")) {
                invoiceType = "普通发票";
            }
            
            if (text.contains("数电发票")) {
                invoiceForm = "数电发票";
            } else if (text.contains("电子发票")) {
                invoiceForm = "电子发票";
            } else if (text.contains("纸质发票")) {
                invoiceForm = "纸质发票";
            }
            
            result.put("invoiceForm", invoiceForm);
            result.put("invoiceType", invoiceType);
        }
        
        return result;
    }

    /**
     * 处理发票性质（从Excel数据）
     */
    private String processInvoiceNatureFromExcel(Map<String, Object> rowData) {
        Object isPositiveInvoice = rowData.get("是否是正数发票");
        if (isPositiveInvoice != null) {
            String value = isPositiveInvoice.toString().trim();
            if ("是".equals(value) || "true".equalsIgnoreCase(value) || "1".equals(value)) {
                return "蓝字";
            } else if ("否".equals(value) || "false".equalsIgnoreCase(value) || "0".equals(value)) {
                return "红字";
            }
        }
        
        // 如果"是否是正数发票"列不存在，尝试从发票票种中解析
        String invoiceTypeText = (String) rowData.get("发票票种");
        if (invoiceTypeText != null && invoiceTypeText.contains("红字")) {
            return "红字";
        }
        
        // 默认值
        return "蓝字";
    }

    /**
     * 转换税率为小数格式（从Excel数据）
     */
    private BigDecimal convertTaxRateFromExcel(Object taxRateValue) {
        if (taxRateValue == null) {
            return BigDecimal.ZERO;
        }
        
        // 如果是字符串，可能需要去除百分号
        String taxRateStr = taxRateValue.toString().trim();
        if (taxRateStr.endsWith("%")) {
            taxRateStr = taxRateStr.substring(0, taxRateStr.length() - 1);
        }
        
        BigDecimal taxRate = parseBigDecimalFromObject(taxRateStr);
        if (taxRate == null) {
            return BigDecimal.ZERO;
        }
        
        if (taxRate.compareTo(BigDecimal.ONE) >= 0) {
            // 如果税率大于等于1，说明是百分比格式，需要转换为小数
            taxRate = taxRate.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP);
        }
        
        return taxRate;
    }

    /**
     * 验证公司信息（用于Excel导入）
     */
    private void validateCompanyInfoForExcel(Map<String, Object> invoiceHeaderData, String invoiceStatus, String invoiceNumber) {
        try {
            // 1. 从 SYS_CONFIG 表读取配置
            SysConfig companyConfig = sysConfigService.getByName("INVOICE_COMPANY_INFO");
            if (companyConfig == null || companyConfig.getValue() == null) {
                throw new BusinessException("系统配置缺失：INVOICE_COMPANY_INFO");
            }

            // 2. 解析 JSON 配置
            Map<String, Object> configJson = objectMapper.readValue(companyConfig.getValue(), Map.class);
            String enterpriseName = normalizeName((String) configJson.get("enterpriseName"));
            String unifiedSocialCreditCode = ((String) configJson.get("unifiedSocialCreditCode")).trim();

            if (enterpriseName == null || unifiedSocialCreditCode == null) {
                throw new BusinessException("系统配置格式错误：INVOICE_COMPANY_INFO 缺少必要字段（enterpriseName 或 unifiedSocialCreditCode）");
            }

            // 3. 根据发票状态进行验证
            if ("进项发票".equals(invoiceStatus)) {
                // 进项发票：验证购买方信息
                String buyerName = (String) invoiceHeaderData.get("buyerName");
                String buyerCreditCode = (String) invoiceHeaderData.get("buyerCreditCode");

                if (buyerName == null || buyerCreditCode == null) {
                    throw new BusinessException("进项发票验证失败：Excel中缺少购买方信息");
                }

                buyerName = normalizeName(buyerName);
                buyerCreditCode = buyerCreditCode.trim();

                if (!enterpriseName.equals(buyerName) || !unifiedSocialCreditCode.equals(buyerCreditCode)) {
                    throw new BusinessException(
                        String.format("进项发票验证失败：购买方信息与系统配置不一致（发票号码：%s，购买方名称：%s，统一社会信用代码：%s），已回滚全部导入结果",
                            invoiceNumber, buyerName, buyerCreditCode)
                    );
                }
            } else if ("销项发票".equals(invoiceStatus)) {
                // 销项发票：验证销售方信息
                String sellerName = (String) invoiceHeaderData.get("sellerName");
                String sellerCreditCode = (String) invoiceHeaderData.get("sellerCreditCode");

                if (sellerName == null || sellerCreditCode == null) {
                    throw new BusinessException("销项发票验证失败：Excel中缺少销售方信息，已回滚全部导入结果");
                }

                sellerName = normalizeName(sellerName);
                sellerCreditCode = sellerCreditCode.trim();

                if (!enterpriseName.equals(sellerName) || !unifiedSocialCreditCode.equals(sellerCreditCode)) {
                    throw new BusinessException(
                        String.format("销项发票验证失败：销售方信息与系统配置不一致（发票号码：%s，销售方名称：%s，统一社会信用代码：%s），已回滚全部导入结果",
                            invoiceNumber, sellerName, sellerCreditCode)
                    );
                }
            }
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("验证公司信息失败，invoiceNumber={}, invoiceStatus={}", invoiceNumber, invoiceStatus, e);
            throw new BusinessException("验证公司信息失败：" + e.getMessage());
        }
    }

    /**
     * 标准化名称，去除前后空格，并将全角括号替换为半角括号，避免因符号差异导致校验失败
     */
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replace('（', '(')
                .replace('）', ')');
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadInvoiceFile(Integer invoiceId, MultipartFile file, String fileType) {
        // 1. 校验发票是否存在
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BusinessException("发票不存在：ID=" + invoiceId);
        }

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException("文件名不能为空");
        }

        // 3. 根据文件类型进行校验和处理
        ValidationResult validation;
        com.erp.entity.common.File fileEntity;
        FileValidator.FileType validatedFileType;

        switch (fileType) {
            case "pdf":
                validation = FileValidator.validate(file, FileValidator.FileType.PDF);
                if (!validation.isValid()) {
                    throw new BusinessException("文件校验失败：" + validation.getMessage());
                }
                validatedFileType = validation.getFileType();
                break;
            case "image":
                validation = FileValidator.validate(file,
                    FileValidator.FileType.JPEG,
                    FileValidator.FileType.PNG,
                    FileValidator.FileType.GIF,
                    FileValidator.FileType.BMP);
                if (!validation.isValid()) {
                    throw new BusinessException("文件校验失败：" + validation.getMessage());
                }
                validatedFileType = validation.getFileType();
                break;
            default:
                throw new BusinessException("不支持的文件类型：" + fileType);
        }

        // 4. 上传并保存文件
        fileEntity = fileService.uploadAndSave(file, "INVOICE_FILE", null);
        if (fileEntity == null || fileEntity.getFileId() == null) {
            throw new BusinessException("文件上传失败：" + originalName);
        }

        // 5. 更新发票文件字段
        if (validatedFileType == FileValidator.FileType.PDF) {
            invoice.setPdfFileId(fileEntity.getFileId());
        } else {
            // 图片类
            invoice.setImageFileId(fileEntity.getFileId());
        }

        // 6. 更新发票记录（保存旧数据用于数据变更日志）
        Map<String, Object> oldDetail = null;
        try {
            oldDetail = getInvoiceDetail(invoiceId);
        } catch (Exception e) {
            log.warn("获取发票旧数据失败，invoiceId={}", invoiceId, e);
        }

        invoice.setUpdateTime(LocalDateTime.now());
        int updated = invoiceMapper.updateById(invoice);
        if (updated <= 0) {
            throw new BusinessException("更新发票文件信息失败");
        }

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("invoiceId", invoiceId);
        result.put("invoiceNumber", invoice.getInvoiceNumber());
        result.put("fileId", fileEntity.getFileId());
        result.put("fileName", originalName);
        result.put("fileType", getFileTypeName(validatedFileType));
        result.put("fileSize", file.getSize());

        // 8. 记录数据变更日志与发送通知（不影响主流程）
        try {
            if (logRecordService != null && oldDetail != null) {
                Map<String, Object> newDetail = getInvoiceDetail(invoiceId);
                String content = "上传发票文件：" + originalName + "（" + getFileTypeName(validatedFileType) + "）";
                logRecordService.recordDataChangeLog("发票管理", "INVOICE", String.valueOf(invoiceId),
                        "更新", content, oldDetail, newDetail, SecurityUtil.getCurrentUserId(), null, true, null);
            }
        } catch (Exception logEx) {
            log.warn("记录发票文件变更日志失败，invoiceId={}", invoiceId, logEx);
        }

        return result;
    }

    /**
     * 导出发票Excel
     * 根据发票ID列表导出发票数据为Excel文件，包含发票基本信息和明细信息（明细分行显示）
     *
     * @param invoiceIds    发票ID列表
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return Excel文件字节数组
     */
    @Override
    public byte[] exportInvoiceExcel(List<Integer> invoiceIds, String invoiceStatus) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new BusinessException("发票ID列表不能为空");
        }
        if (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
            throw new BusinessException("发票状态必须是'进项发票'或'销项发票'");
        }

        log.info("开始导出发票Excel，发票状态：{}，发票数量：{}", invoiceStatus, invoiceIds.size());

        try {
            // 查询发票数据及其明细
            List<Invoice> invoices = invoiceMapper.selectBatchIds(invoiceIds);
            if (invoices.isEmpty()) {
                throw new BusinessException("未找到指定的发票数据");
            }

            // 获取所有发票的明细数据
            List<InvoiceItem> allItems = invoiceMapper.selectItemsByInvoiceIds(invoiceIds);
            log.info("查询到发票明细数据：{}条", allItems.size());

            // 将明细按发票ID分组
            Map<Integer, List<InvoiceItem>> itemsMap = allItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(InvoiceItem::getInvoiceId));

            log.info("分组后的发票明细映射：{}个发票有明细", itemsMap.size());

            // 使用Apache POI创建Excel
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("发票数据");

            // 创建标题样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // 创建数据样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            // 创建金额样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.cloneStyleFrom(dataStyle);
            org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
            amountStyle.setDataFormat(format.getFormat("#,##0.00"));

            // 创建标题行
            String[] headers = {
                "发票编号", "发票号码", "发票代码", "发票类型", "发票形式", "发票性质",
                "购买方", "购买方统一社会信用代码", "销售方", "销售方统一社会信用代码",
                "开票人", "金额", "税额", "价税合计", "价税合计大写", "开票日期",
                "创建时间", "更新时间", "备注",
                // 明细字段
                "商品名称", "规格型号", "单位", "数量", "单价", "金额（明细）",
                "税率", "商品税额", "含税金额", "税收分类编码", "备注（明细）"
            };

            org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            int rowIndex = 1;
            for (Invoice invoice : invoices) {
                List<InvoiceItem> items = itemsMap.get(invoice.getInvoiceId());
                if (items == null || items.isEmpty()) {
                    // 无明细的发票，创建一行基本信息
                    org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowIndex++);
                    fillInvoiceRow(row, invoice, null, dataStyle, amountStyle);
                } else {
                    // 有明细的发票，每条明细创建一行
                    for (InvoiceItem item : items) {
                        org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowIndex++);
                        fillInvoiceRow(row, invoice, item, dataStyle, amountStyle);
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最大列宽
                if (sheet.getColumnWidth(i) > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            // 转换为字节数组
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            log.info("导出发票Excel完成，发票状态：{}，总行数：{}", invoiceStatus, rowIndex - 1);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("导出发票Excel失败", e);
            throw new BusinessException("导出Excel失败：" + e.getMessage());
        }
    }

    /**
     * 填充发票数据行
     */
    private void fillInvoiceRow(org.apache.poi.xssf.usermodel.XSSFRow row, Invoice invoice,
                               InvoiceItem item, org.apache.poi.xssf.usermodel.XSSFCellStyle dataStyle,
                               org.apache.poi.xssf.usermodel.XSSFCellStyle amountStyle) {

        int colIndex = 0;

        // 发票基本信息
        setCellValue(row, colIndex++, invoice.getInvoiceId(), dataStyle);
        setCellValue(row, colIndex++, invoice.getInvoiceNumber(), dataStyle);
        setCellValue(row, colIndex++, invoice.getInvoiceCode(), dataStyle);
        setCellValue(row, colIndex++, invoice.getInvoiceType(), dataStyle);
        setCellValue(row, colIndex++, invoice.getInvoiceForm(), dataStyle);
        setCellValue(row, colIndex++, invoice.getInvoiceNature(), dataStyle);
        setCellValue(row, colIndex++, invoice.getBuyerName(), dataStyle);
        setCellValue(row, colIndex++, invoice.getBuyerCreditCode(), dataStyle);
        setCellValue(row, colIndex++, invoice.getSellerName(), dataStyle);
        setCellValue(row, colIndex++, invoice.getSellerCreditCode(), dataStyle);
        setCellValue(row, colIndex++, invoice.getIssuerName(), dataStyle);

        // 金额字段
        setCellValue(row, colIndex++, invoice.getAmount(), amountStyle);
        setCellValue(row, colIndex++, invoice.getTaxAmount(), amountStyle);
        setCellValue(row, colIndex++, invoice.getTotalAmount(), amountStyle);
        setCellValue(row, colIndex++, invoice.getTotalAmountInChinese(), dataStyle);

        // 日期字段
        setCellValue(row, colIndex++, invoice.getInvoiceDate(), dataStyle);
        setCellValue(row, colIndex++, invoice.getCreateTime(), dataStyle);
        setCellValue(row, colIndex++, invoice.getUpdateTime(), dataStyle);
        setCellValue(row, colIndex++, invoice.getRemark(), dataStyle);

        // 明细信息
        if (item != null) {
            setCellValue(row, colIndex++, item.getProductName(), dataStyle);
            setCellValue(row, colIndex++, item.getSpecification(), dataStyle);
            setCellValue(row, colIndex++, item.getUnit(), dataStyle);
            setCellValue(row, colIndex++, item.getQuantity(), dataStyle);
            setCellValue(row, colIndex++, item.getUnitPrice(), amountStyle);
            setCellValue(row, colIndex++, item.getAmount(), amountStyle);
            setCellValue(row, colIndex++, item.getTaxRate(), dataStyle);
            setCellValue(row, colIndex++, item.getProductTaxAmount(), amountStyle);
            setCellValue(row, colIndex++, item.getTaxIncludedAmount(), amountStyle);
            setCellValue(row, colIndex++, item.getTaxClassificationCode(), dataStyle);
            setCellValue(row, colIndex++, item.getRemark(), dataStyle);
        } else {
            // 无明细时填充空值
            for (int i = 0; i < 11; i++) {
                setCellValue(row, colIndex++, null, dataStyle);
            }
        }
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(org.apache.poi.xssf.usermodel.XSSFRow row, int columnIndex,
                             Object value, org.apache.poi.xssf.usermodel.XSSFCellStyle style) {
        org.apache.poi.xssf.usermodel.XSSFCell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);

        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof java.time.LocalDateTime) {
            cell.setCellValue(((java.time.LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else if (value instanceof java.time.LocalDate) {
            cell.setCellValue(((java.time.LocalDate) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 导出发票文件
     * 根据发票ID列表导出发票相关文件为ZIP压缩包
     *
     * @param invoiceIds    发票ID列表
     * @param invoiceStatus 发票状态（进项发票/销项发票）
     * @return ZIP文件字节数组
     */
    @Override
    public byte[] exportInvoiceFiles(List<Integer> invoiceIds, String invoiceStatus) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new BusinessException("发票ID列表不能为空");
        }
        if (!"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
            throw new BusinessException("发票状态必须是'进项发票'或'销项发票'");
        }

        log.info("开始导出发票文件，状态：{}，发票数量：{}", invoiceStatus, invoiceIds.size());

        try {
            // 查询所有发票的文件信息
            List<Map<String, Object>> allFileInfos = new ArrayList<>();
            for (Integer invoiceId : invoiceIds) {
                Map<String, Object> fileIds = getInvoiceFileIds(invoiceId);
                if (fileIds != null && !fileIds.isEmpty()) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("invoiceId", invoiceId);

                    // 查询发票基本信息用于文件名
                    Invoice invoice = getById(invoiceId);
                    if (invoice != null) {
                        fileInfo.put("invoiceNumber", invoice.getInvoiceNumber());
                        fileInfo.put("buyerName", invoice.getBuyerName());
                    }

                    fileInfo.put("fileIds", fileIds);
                    allFileInfos.add(fileInfo);
                }
            }

            if (allFileInfos.isEmpty()) {
                throw new BusinessException("选中的发票都没有相关文件");
            }

            log.info("找到{}个发票有相关文件", allFileInfos.size());

            // 复用 createInvoiceFilesZip 公共方法打包，该方法内部的 addFileToZip
            // 会根据 FILE 表中的 storageType 自动选择本地或 OSS 存储服务读取文件
            log.info("导出发票文件完成，状态：{}", invoiceStatus);
            return createInvoiceFilesZip(allFileInfos);

        } catch (Exception e) {
            log.error("导出发票文件失败", e);
            throw new BusinessException("导出文件失败：" + e.getMessage());
        }
    }

    /**
     * 全部导出发票Excel
     * 直接导出系统中的全部发票数据为Excel文件
     *
     * @param invoiceStatus 发票状态（进项发票/销项发票，可选，不传则导出全部）
     * @return Excel文件字节数组
     */
    @Override
    public byte[] exportAllInvoiceExcel(String invoiceStatus, Integer creatorId) {
        // 验证参数（invoiceStatus可选）
        if (invoiceStatus != null && !"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
            throw new BusinessException("发票状态必须是'进项发票'或'销项发票'");
        }

        log.info("开始全部导出发票Excel，发票状态：{}，创建人过滤：{}", invoiceStatus != null ? invoiceStatus : "全部", creatorId);

        try {
        // 构建查询条件
        QueryWrapper<Invoice> queryWrapper = new QueryWrapper<>();
        if (invoiceStatus != null) {
            queryWrapper.eq("发票状态", invoiceStatus);
        }
        // viewScope=SELF 时只导出当前员工创建的发票
        if (creatorId != null) {
            queryWrapper.eq("创建人编码", creatorId);
        }
        queryWrapper.orderByDesc("创建时间");

            // 查询所有符合条件的发票数据（分页查询以避免内存溢出）
            int pageSize = 1000; // 每次查询1000条，避免一次性查询过多数据
            int currentPage = 1;
            List<Invoice> allInvoices = new ArrayList<>();

            while (true) {
                IPage<Invoice> page = invoiceMapper.selectPage(new Page<>(currentPage, pageSize), queryWrapper);
                if (page.getRecords().isEmpty()) {
                    break;
                }
                allInvoices.addAll(page.getRecords());
                currentPage++;

                // 防止无限循环，限制最多查询100页
                if (currentPage > 100) {
                    log.warn("查询发票数据超过限制，已查询{}条记录", allInvoices.size());
                    break;
                }
            }

            if (allInvoices.isEmpty()) {
                throw new BusinessException("未找到符合条件的发票数据");
            }

            log.info("查询到全部发票数据：{}条", allInvoices.size());

            // 获取所有发票ID列表，用于查询明细数据
            List<Integer> invoiceIds = allInvoices.stream()
                .map(Invoice::getInvoiceId)
                .collect(java.util.stream.Collectors.toList());

            // 分批查询明细数据
            List<InvoiceItem> allItems = new ArrayList<>();
            int batchSize = 500; // 每批查询500个发票的明细
            for (int i = 0; i < invoiceIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, invoiceIds.size());
                List<Integer> batchIds = invoiceIds.subList(i, endIndex);
                List<InvoiceItem> batchItems = invoiceMapper.selectItemsByInvoiceIds(batchIds);
                allItems.addAll(batchItems);
            }

            log.info("查询到发票明细数据：{}条", allItems.size());

            // 将明细按发票ID分组
            Map<Integer, List<InvoiceItem>> itemsMap = allItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(InvoiceItem::getInvoiceId));

            // 使用Apache POI创建Excel（复用现有的Excel生成逻辑）
            return createInvoiceExcel(allInvoices, itemsMap);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("全部导出发票Excel失败", e);
            throw new BusinessException("全部导出Excel失败：" + e.getMessage());
        }
    }

    /**
     * 全部导出发票文件
     * 直接导出系统中的全部发票相关文件为ZIP压缩包
     *
     * @param invoiceStatus 发票状态（进项发票/销项发票，可选，不传则导出全部）
     * @return ZIP文件字节数组
     */
    @Override
    public byte[] exportAllInvoiceFiles(String invoiceStatus, Integer creatorId) {
        // 验证参数（invoiceStatus可选）
        if (invoiceStatus != null && !"进项发票".equals(invoiceStatus) && !"销项发票".equals(invoiceStatus)) {
            throw new BusinessException("发票状态必须是'进项发票'或'销项发票'");
        }

        log.info("开始全部导出发票文件，发票状态：{}，创建人过滤：{}", invoiceStatus != null ? invoiceStatus : "全部", creatorId);

        try {
            // 构建查询条件
            QueryWrapper<Invoice> queryWrapper = new QueryWrapper<>();
            if (invoiceStatus != null) {
                queryWrapper.eq("发票状态", invoiceStatus);
            }
            // viewScope=SELF 时只导出当前员工创建的发票文件
            if (creatorId != null) {
                queryWrapper.eq("创建人编码", creatorId);
            }
            queryWrapper.select("发票编号"); // 只查询发票ID字段

            // 分页查询所有符合条件的发票ID（优化版本：直接查询Integer列表）
            int pageSize = 1000; // 每次查询1000条，避免内存溢出
            int currentPage = 1;
            List<Integer> allInvoiceIds = new ArrayList<>();

            while (true) {
                // 使用selectObjs直接查询Integer列表，更高效
                IPage<Map<String, Object>> page = invoiceMapper.selectMapsPage(new Page<>(currentPage, pageSize), queryWrapper);
                if (page.getRecords().isEmpty()) {
                    break;
                }

                // 提取发票ID
                List<Integer> invoiceIds = page.getRecords().stream()
                    .filter(map -> map != null && map.get("发票编号") != null)
                    .map(map -> (Integer) map.get("发票编号"))
                    .collect(java.util.stream.Collectors.toList());

                allInvoiceIds.addAll(invoiceIds);
                currentPage++;

                // 防止无限循环，限制最多查询200页
                if (currentPage > 200) {
                    log.warn("查询发票数据超过限制，已查询{}条记录", allInvoiceIds.size());
                    break;
                }
            }

            if (allInvoiceIds.isEmpty()) {
                throw new BusinessException("未找到符合条件的发票数据");
            }

            log.info("查询到全部发票ID：{}条", allInvoiceIds.size());

            // 复用批量导出文件的逻辑
            return exportInvoiceFiles(allInvoiceIds, invoiceStatus);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("全部导出发票文件失败", e);
            throw new BusinessException("全部导出文件失败：" + e.getMessage());
        }
    }

    /**
     * 创建发票Excel文件（提取的公共方法）
     */
    private byte[] createInvoiceExcel(List<Invoice> invoices, Map<Integer, List<InvoiceItem>> itemsMap) throws Exception {
        // 使用Apache POI创建Excel
        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("发票数据");

        // 创建标题样式
        org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);

        // 创建数据样式
        org.apache.poi.xssf.usermodel.XSSFCellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);

        // 创建标题行
        org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
        String[] headers = {
            "发票号码", "发票代码", "发票类型", "发票形式", "发票性质",
            "购买方", "购买方统一社会信用代码", "销售方", "销售方统一社会信用代码",
            "开票人", "金额", "税额", "价税合计", "价税合计大写", "开票日期",
            "备注", "创建时间", "更新时间",
            // 明细字段
            "商品名称", "规格型号", "单位", "数量", "单价", "金额（明细）",
            "税率", "商品税额", "含税金额", "税收分类编码", "备注（明细）"
        };

        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000); // 设置列宽
        }

        // 填充数据
        int rowNum = 1;
        for (Invoice invoice : invoices) {
            List<InvoiceItem> items = itemsMap.get(invoice.getInvoiceId());
            if (items == null || items.isEmpty()) {
                // 没有明细的发票，创建一行数据
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowNum++);
                fillInvoiceRow(row, invoice, null, dataStyle);
            } else {
                // 有明细的发票，为每个明细创建一行
                for (InvoiceItem item : items) {
                    org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowNum++);
                    fillInvoiceRow(row, invoice, item, dataStyle);
                }
            }
        }

        // 写入到字节数组
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 填充发票数据行
     */
    private void fillInvoiceRow(org.apache.poi.xssf.usermodel.XSSFRow row, Invoice invoice, InvoiceItem item,
                               org.apache.poi.xssf.usermodel.XSSFCellStyle dataStyle) {
        int columnIndex = 0;

        // 发票基本信息
        setCellValue(row, columnIndex++, invoice.getInvoiceNumber(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getInvoiceCode(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getInvoiceType(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getInvoiceForm(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getInvoiceNature(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getBuyerName(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getBuyerCreditCode(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getSellerName(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getSellerCreditCode(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getIssuerName(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getAmount(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getTaxAmount(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getTotalAmount(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getTotalAmountInChinese(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getInvoiceDate() != null ? cn.hutool.core.date.DateUtil.format(invoice.getInvoiceDate(), "yyyy-MM-dd") : null, dataStyle);
        setCellValue(row, columnIndex++, invoice.getRemark(), dataStyle);
        setCellValue(row, columnIndex++, invoice.getCreateTime() != null ? cn.hutool.core.date.DateUtil.format(invoice.getCreateTime(), "yyyy-MM-dd HH:mm:ss") : null, dataStyle);
        setCellValue(row, columnIndex++, invoice.getUpdateTime() != null ? cn.hutool.core.date.DateUtil.format(invoice.getUpdateTime(), "yyyy-MM-dd HH:mm:ss") : null, dataStyle);

        // 明细信息
        if (item != null) {
            setCellValue(row, columnIndex++, item.getProductName(), dataStyle);
            setCellValue(row, columnIndex++, item.getSpecification(), dataStyle);
            setCellValue(row, columnIndex++, item.getUnit(), dataStyle);
            setCellValue(row, columnIndex++, item.getQuantity(), dataStyle);
            setCellValue(row, columnIndex++, item.getUnitPrice(), dataStyle);
            setCellValue(row, columnIndex++, item.getAmount(), dataStyle);
            setCellValue(row, columnIndex++, item.getTaxRate(), dataStyle);
            setCellValue(row, columnIndex++, item.getProductTaxAmount(), dataStyle);
            setCellValue(row, columnIndex++, item.getTaxIncludedAmount(), dataStyle);
            setCellValue(row, columnIndex++, item.getTaxClassificationCode(), dataStyle);
            setCellValue(row, columnIndex++, item.getRemark(), dataStyle);
        } else {
            // 没有明细时填充空值
            for (int i = 0; i < 11; i++) {
                setCellValue(row, columnIndex++, null, dataStyle);
            }
        }
    }


    /**
     * 创建发票文件ZIP包（提取的公共方法）
     */
    private byte[] createInvoiceFilesZip(List<Map<String, Object>> allFileInfos) throws Exception {
        // 创建ZIP文件
        java.io.ByteArrayOutputStream zipOutputStream = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(zipOutputStream);

        try {
            // 统计文件数量
            int totalFiles = 0;

            // 处理每个发票的文件
            for (Map<String, Object> fileInfo : allFileInfos) {
                Integer invoiceId = (Integer) fileInfo.get("invoiceId");
                String invoiceNumber = (String) fileInfo.get("invoiceNumber");
                String buyerName = (String) fileInfo.get("buyerName");
                @SuppressWarnings("unchecked")
                Map<String, Object> fileIds = (Map<String, Object>) fileInfo.get("fileIds");

                // 处理不同类型的文件
                if (fileIds.get("pdfFileId") != null) {
                    addFileToZip(zipOut, (Integer) fileIds.get("pdfFileId"),
                               generateFileName(invoiceNumber, buyerName, "pdf"), "PDF文件");
                    totalFiles++;
                }
                if (fileIds.get("imageFileId") != null) {
                    addFileToZip(zipOut, (Integer) fileIds.get("imageFileId"),
                               generateFileName(invoiceNumber, buyerName, "jpg"), "图片文件");
                    totalFiles++;
                }
            }

            log.info("ZIP文件包含总文件数：{}", totalFiles);

            if (totalFiles == 0) {
                throw new BusinessException("没有找到任何可导出的文件");
            }

        } finally {
            zipOut.close();
        }

        return zipOutputStream.toByteArray();
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String invoiceNumber, String buyerName, String extension) {
        String safeBuyerName = buyerName != null ? buyerName.replaceAll("[\\\\/:*?\"<>|]", "_") : "";
        return String.format("dzfp_%s_%s.%s", invoiceNumber, safeBuyerName, extension);
    }

    /**
     * 添加文件到ZIP包
     * 根据 FILE 表中的 storageType 字段选择对应的存储服务读取文件内容，
     * 同时兼容本地存储和 OSS（云端）存储。
     */
    private void addFileToZip(java.util.zip.ZipOutputStream zipOut, Integer fileId, String fileName, String fileType) throws Exception {
        try {
            // 通过fileId查询文件信息
            com.erp.entity.common.File fileEntity = fileMapper.selectById(fileId);
            if (fileEntity == null) {
                log.warn("文件记录不存在，跳过：{} (ID:{})", fileName, fileId);
                return;
            }

            // 根据 FILE 表中的 storageType 选择存储服务和文件路径
            // 而非依赖全局配置，确保历史本地文件与新上传的 OSS 文件都能正确读取
            com.erp.service.common.FileStorageService storageService;
            String filePath;
            if ("本地".equals(fileEntity.getStorageType())) {
                storageService = fileService.getLocalFileStorageService();
                filePath = fileEntity.getLocalPath();
            } else {
                // 云端（OSS）存储
                storageService = fileService.getOssFileStorageService();
                filePath = fileEntity.getObjectKey();
                if (storageService == null) {
                    log.warn("OSS 存储服务未配置，无法读取云端文件，跳过：{} (ID:{})", fileName, fileId);
                    return;
                }
            }

            if (filePath == null || filePath.trim().isEmpty()) {
                log.warn("文件路径为空，跳过：{} (ID:{})", fileName, fileId);
                return;
            }

            // 从对应存储服务读取文件内容
            byte[] fileContent = storageService.readFile(filePath);
            if (fileContent == null || fileContent.length == 0) {
                log.warn("文件内容为空，跳过：{} (ID:{})", fileName, fileId);
                return;
            }

            // 创建ZIP条目
            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);

            // 写入文件内容
            zipOut.write(fileContent);
            zipOut.closeEntry();

            log.debug("成功添加{}到ZIP：{}", fileType, fileName);

        } catch (Exception e) {
            log.warn("添加文件到ZIP失败：{} (ID:{})，错误：{}", fileName, fileId, e.getMessage());
            // 继续处理其他文件，不抛出异常
        }
    }

    @Override
    public Map<String, Object> getInvoiceAssociations(Integer invoiceId) {
        log.info("查询发票关联记录：invoiceId={}", invoiceId);

        // 验证发票存在
        Invoice invoice = this.getById(invoiceId);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }

        Map<String, Object> result = new HashMap<>();

        // 设置发票基本信息
        result.put("invoiceId", invoice.getInvoiceId());
        result.put("invoiceNumber", invoice.getInvoiceNumber());
        result.put("invoiceNature", invoice.getInvoiceNature());

        // 查询关联记录
        List<Map<String, Object>> associations = invoiceMapper.selectInvoiceAssociations(invoiceId);
        result.put("associations", associations);

        log.info("查询到{}个关联记录", associations.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoiceAssociations(Integer invoiceId, Map<String, Object> data) {
        log.info("更新发票关联信息：invoiceId={}, payload={}", invoiceId, data);

        /**
         * 行为文档（summary）：
         * - 前端应在需要“删除所有结算关联”的场景下，显式传入 settlementIds = []（空数组）。
         * - 后端语义：
         *    - settlementIds == null: 前端未提供结算信息，后端按现状差分处理（不会自动删除所有关联）。
         *    - settlementIds == [] (空数组): 表示“删除所有结算关联”，后端会删除 SETTLEMENT_INVOICE_REL 中关于该发票的所有记录；若检测到已有关联并发生变更，还会删除 INVOICE_NOTICE_INVOICE 的登记。
         *    - settlementIds 非空数组: 按差分（existing vs new）删除/新增 rel 记录。
         */
        // 验证发票存在
        Invoice invoice = this.getById(invoiceId);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }

        // 解析入参
        Integer newContractId = null;
        Object contractObj = data != null ? data.get("contractId") : null;
        if (contractObj instanceof Number) {
            newContractId = ((Number) contractObj).intValue();
        } else if (contractObj instanceof String) {
            try {
                newContractId = Integer.parseInt((String) contractObj);
            } catch (Exception ignored) {}
        }

        List<Integer> newSettlementIds = null;
        Object settlementsObj = data != null ? data.get("settlementIds") : null;
        if (settlementsObj instanceof List) {
            newSettlementIds = ((List<?>) settlementsObj).stream()
                .filter(elt -> elt != null)
                .map(elt -> {
                    if (elt instanceof Number) return ((Number) elt).intValue();
                    try { return Integer.parseInt(String.valueOf(elt)); } catch (Exception e) { return null; }
                })
                .filter(i -> i != null)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        }

        // 查询现有关联记录
        List<Map<String, Object>> existing = invoiceMapper.selectInvoiceAssociations(invoiceId);

        boolean hasExisting = existing != null && !existing.isEmpty();
        boolean changed = false;

        if (hasExisting) {
            Map<String, Object> first = existing.get(0);
            Integer existContractId = first.get("contractId") instanceof Number ? ((Number) first.get("contractId")).intValue() : null;
            Integer existSettlementId = first.get("settlementId") instanceof Number ? ((Number) first.get("settlementId")).intValue() : null;
            // 如果前端显式传入空的 settlementIds（表示删除所有关联），也应视为变更
            if ((newContractId != null && !newContractId.equals(existContractId)) ||
                (newSettlementIds != null && (newSettlementIds.isEmpty() || !newSettlementIds.contains(existSettlementId)))) {
                changed = true;
            }
        } else {
            // no existing associations and new ones provided -> treat as change
            if (newContractId != null || (newSettlementIds != null && !newSettlementIds.isEmpty())) {
                changed = true;
            }
        }

        Integer currentUserId = SecurityUtil.getCurrentUserId();

        // 如果已有现有关联并且合同或结算单被修改，则删除发票在通知单登记表里的记录（发票已被修改）
        if (hasExisting && changed) {
            QueryWrapper<InvoiceNoticeInvoice> delWrapper = new QueryWrapper<>();
            delWrapper.eq("发票编号", invoiceId);
            invoiceNoticeInvoiceMapper.delete(delWrapper);
            log.info("检测到发票关联变更，已删除开票通知单中的发票登记记录：invoiceId={}", invoiceId);
        }

        // 处理结算单-发票关系：如果没有关联记录则新增；如果有则更新结算单编号
        QueryWrapper<SettlementInvoiceRel> relWrapper = new QueryWrapper<>();
        relWrapper.eq("发票编号", invoiceId);
        List<SettlementInvoiceRel> rels = settlementInvoiceRelMapper.selectList(relWrapper);

        // 差分处理结算单-发票关联关系
        java.util.Set<Integer> existingSettlementIds = new java.util.HashSet<>();
        if (rels != null && !rels.isEmpty()) {
            for (SettlementInvoiceRel rel : rels) {
                if (rel.getSettlementId() != null) {
                    existingSettlementIds.add(rel.getSettlementId());
                }
            }
        }

        if (newSettlementIds == null || newSettlementIds.isEmpty()) {
            // 如果前端未提供任何结算单，且数据库中有现有关联，则删除这些关系
            if (!existingSettlementIds.isEmpty()) {
                QueryWrapper<SettlementInvoiceRel> delAllWrapper = new QueryWrapper<>();
                delAllWrapper.eq("发票编号", invoiceId);
                settlementInvoiceRelMapper.delete(delAllWrapper);
                log.info("前端未提供结算单，删除该发票所有结算单关联：invoiceId={}", invoiceId);
            } else {
                log.info("无现有关联且前端未提供结算单，跳过结算关联变更：invoiceId={}", invoiceId);
            }
        } else {
            // 计算需要删除和新增的结算单关联（按 settlementId 差分）
            java.util.Set<Integer> newSet = new java.util.HashSet<>(newSettlementIds);

            // 需要删除的：存在于 existing but not in newSet
            java.util.Set<Integer> toDelete = new java.util.HashSet<>(existingSettlementIds);
            toDelete.removeAll(newSet);

            // 需要新增的：存在于 newSet but not in existing
            java.util.Set<Integer> toAdd = new java.util.HashSet<>(newSet);
            toAdd.removeAll(existingSettlementIds);

            if (!toDelete.isEmpty()) {
                QueryWrapper<SettlementInvoiceRel> delWrapper2 = new QueryWrapper<>();
                delWrapper2.eq("发票编号", invoiceId).in("结算单编号", toDelete);
                settlementInvoiceRelMapper.delete(delWrapper2);
                log.info("删除结算单-发票关联：invoiceId={}, removedSettlementIds={}", invoiceId, toDelete);
            }

            if (!toAdd.isEmpty()) {
                for (Integer sid : toAdd) {
                    SettlementInvoiceRel rel = new SettlementInvoiceRel();
                    rel.setSettlementId(sid);
                    rel.setInvoiceId(invoiceId);
                    rel.setNoticeId(null);
                    rel.setRelAmount(invoice.getTotalAmount());
                    rel.setRelType("INVOICE");
                    rel.setRelTime(LocalDateTime.now());
                    rel.setCreateUserId(currentUserId);
                    rel.setRemark("前端更新关联自动新增");
                    rel.setStatus("BOUND");
                    rel.setVersion(0);
                    settlementInvoiceRelMapper.insert(rel);
                    log.info("新增结算单-发票关系：settlementId={}, invoiceId={}", sid, invoiceId);
                }
            } else {
                log.info("结算单关联无新增项：invoiceId={}", invoiceId);
            }
        }

        log.info("更新发票关联处理完成：invoiceId={}, changed={}", invoiceId, changed);

        // 更新已收金额（如果有资金流水关联）
        try {
            updateSettlementsReceivedAmount(newSettlementIds);
        } catch (Exception e) {
            log.warn("更新结算单已收金额失败，invoiceId={}", invoiceId, e);
            // 不影响主流程，只记录警告
        }

        // 记录数据变更日志并发送通知（若发生变更）
        if (changed) {
            try {
                if (logRecordService != null) {
                    Map<String, Object> oldData = new HashMap<>();
                    oldData.put("associations", existing != null ? existing : new ArrayList<>());
                    List<Map<String, Object>> newAssociations = invoiceMapper.selectInvoiceAssociations(invoiceId);
                    Map<String, Object> newData = new HashMap<>();
                    newData.put("associations", newAssociations != null ? newAssociations : new ArrayList<>());
                    String content = "更新发票关联：发票ID=" + invoiceId;
                    logRecordService.recordDataChangeLog("发票管理", "INVOICE", String.valueOf(invoiceId),
                            "更新", content, oldData, newData, SecurityUtil.getCurrentUserId(), null, true, null);
                }
            } catch (Exception logEx) {
                log.warn("记录发票关联变更数据日志失败，invoiceId={}", invoiceId, logEx);
            }
        }
    }

    /**
     * 更新结算单的已收金额
     * 逻辑：
     * 1. 首先检查 SETTLEMENT_FUND_TRANSACTION_REL 表是否有该结算单的资金流水关联
     * 2. 如果没有关联，不做任何操作
     * 3. 如果有关联，查询 SETTLEMENT_INVOICE_REL 表中该结算单所有发票的关联金额
     *    蓝字相加，红字相减，得到净额
     * 4. 更新结算单的已收金额字段
     * 注意：使用 FinanceController.updateSettlement 接口进行更新，无论结算单是什么状态
     *
     * @param settlementIds 结算单ID列表（可能为null或空）
     */
    private void updateSettlementsReceivedAmount(List<Integer> settlementIds) {
        if (settlementIds == null || settlementIds.isEmpty()) {
            log.debug("结算单ID列表为空，跳过更新已收金额");
            return;
        }

        log.info("开始更新结算单已收金额，settlementIds={}", settlementIds);

        for (Integer settlementId : settlementIds) {
            if (settlementId == null) {
                continue;
            }

            try {
                // 1. 检查 SETTLEMENT_FUND_TRANSACTION_REL 表是否有该结算单的资金流水关联
                QueryWrapper<SettlementFundTransactionRel> fundRelWrapper = new QueryWrapper<>();
                fundRelWrapper.eq("结算单编号", settlementId.longValue());
                Long fundRelCount = settlementFundTransactionRelMapper.selectCount(fundRelWrapper);

                if (fundRelCount == null || fundRelCount == 0) {
                    log.info("结算单没有资金流水关联，跳过更新已收金额，settlementId={}", settlementId);
                    continue;
                }

                log.info("结算单有资金流水关联，开始更新已收金额，settlementId={}, 关联记录数={}",
                    settlementId, fundRelCount);

                // 2. 查询 SETTLEMENT_INVOICE_REL 表中该结算单所有发票的关联金额（蓝字 - 红字）
                BigDecimal netAmount = settlementInvoiceRelMapper.selectNetAmountBySettlementId(settlementId.longValue());
                if (netAmount == null) {
                    netAmount = BigDecimal.ZERO;
                }

                log.info("结算单发票关联净额计算完成，settlementId={}, netAmount={}", settlementId, netAmount);

                // 3. 查询结算单信息
                Settlement settlement = settlementMapper.selectById(settlementId);
                if (settlement == null) {
                    log.warn("结算单不存在，无法更新已收金额，settlementId={}", settlementId);
                    continue;
                }

                // 4. 使用 field 模式更新结算单的已收金额字段（通过 updateSettlement 接口的内部逻辑）
                // 这里直接调用 Mapper 更新已收金额，模拟 updateSettlement 接口的 field 模式行为
                settlement.setReceivedAmount(netAmount);
                settlement.setUpdateTime(LocalDateTime.now());
                settlement.setUpdateUserId(SecurityUtil.getCurrentUserId());

                int updateResult = settlementMapper.updateById(settlement);
                if (updateResult > 0) {
                    log.info("结算单已收金额更新成功，settlementId={}, receivedAmount={}", settlementId, netAmount);
                } else {
                    log.warn("结算单已收金额更新失败，settlementId={}", settlementId);
                }
            } catch (Exception e) {
                log.error("更新结算单已收金额异常，settlementId={}", settlementId, e);
                // 继续处理其他结算单
            }
        }
    }
}

