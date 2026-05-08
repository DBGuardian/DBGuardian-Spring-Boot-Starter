package com.erp.service.finance.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundDiaryImportResult;
import com.erp.controller.finance.dto.FundDiaryRequest;
import com.erp.controller.finance.dto.FundDiaryResponse;
import com.erp.entity.common.File;
import com.erp.entity.finance.FundAccount;
import com.erp.entity.finance.FundAccountInitialBalance;
import com.erp.entity.finance.FundPeriod;
import com.erp.entity.finance.FundTransaction;
import com.erp.entity.system.SysConfig;
import com.erp.mapper.common.FileMapper;
import com.erp.service.common.FileService;
import com.erp.mapper.finance.FundAccountInitialBalanceMapper;
import com.erp.mapper.finance.FundAccountMapper;
import com.erp.mapper.finance.FundPeriodMapper;
import com.erp.mapper.finance.FundSubjectMapper;
import com.erp.mapper.finance.FundTransactionMapper;
import com.erp.service.finance.FundDiaryService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.SysConfigService;
import com.erp.util.ZipFileProcessor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日记账服务实现类
 */
@Slf4j
@Service
public class FundDiaryServiceImpl implements FundDiaryService {

    @Autowired
    private FundAccountMapper fundAccountMapper;

    @Autowired
    private FundPeriodMapper fundPeriodMapper;

    @Autowired
    private FundAccountInitialBalanceMapper initialBalanceMapper;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private FundTransactionMapper transactionMapper;

    @Autowired
    private FundSubjectMapper subjectMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private ZipFileProcessor zipFileProcessor;

    @Autowired(required = false)
    private ILogRecordService logRecordService;

    @org.springframework.beans.factory.annotation.Value("${file.storage.local.path:D:/erp}")
    private String localStoragePath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String FUND_DIARY_BUSINESS_TYPE = "FUND_DIARY";
    private static final String FUND_DIARY_BUSINESS_MODULE = "资金管理";

    @Override
    public FundDiaryResponse getDiary(FundDiaryRequest request) {
        // 1. 查询账户信息
        FundAccount account = fundAccountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw new RuntimeException("账户不存在，ID：" + request.getAccountId());
        }

        // 2. 查询账期信息
        FundPeriod period = null;
        if (request.getPeriodId() != null) {
            period = fundPeriodMapper.selectById(request.getPeriodId());
        } else if (request.getYear() != null && request.getMonth() != null) {
            QueryWrapper<FundPeriod> periodQuery = new QueryWrapper<>();
            periodQuery.eq("年份", request.getYear());
            periodQuery.eq("月份", request.getMonth());
            periodQuery.eq("组织编号", request.getOrganizationId());
            period = fundPeriodMapper.selectOne(periodQuery);
        }

        if (period == null) {
            throw new RuntimeException("账期不存在");
        }

        // 3. 查询期初余额
        FundAccountInitialBalance initialBalanceRecord = initialBalanceMapper.selectByAccountAndPeriod(
                request.getAccountId(), period.getPeriodId());

        FundDiaryResponse.InitialBalanceInfo initialBalance = null;
        if (initialBalanceRecord != null) {
            initialBalance = new FundDiaryResponse.InitialBalanceInfo();
            initialBalance.setAmount(initialBalanceRecord.getInitialBalance());
            initialBalance.setDirection(initialBalanceRecord.getBalanceDirection());
        } else {
            // 如果没有期初余额记录，默认为0，方向为"收入"
            initialBalance = new FundDiaryResponse.InitialBalanceInfo();
            initialBalance.setAmount(BigDecimal.ZERO);
            initialBalance.setDirection("收入");
        }

        // 4. 查询流水明细
        QueryWrapper<FundTransaction> transactionQuery = new QueryWrapper<>();
        transactionQuery.eq("账户编号", request.getAccountId());
        transactionQuery.ge("交易日期", period.getStartDate());
        transactionQuery.le("交易日期", period.getEndDate());

        // 可选筛选条件
        if (request.getDateRangeStart() != null) {
            transactionQuery.ge("交易日期", LocalDate.parse(request.getDateRangeStart()));
        }
        if (request.getDateRangeEnd() != null) {
            transactionQuery.le("交易日期", LocalDate.parse(request.getDateRangeEnd()));
        }
        if (request.getSummary() != null && !request.getSummary().trim().isEmpty()) {
            transactionQuery.like("摘要", request.getSummary().trim());
        }
        if (request.getCounterpartyName() != null && !request.getCounterpartyName().trim().isEmpty()) {
            transactionQuery.like("往来单位账户名称", request.getCounterpartyName().trim());
        }

        // 排序：按交易日期、创建时间、流水编号
        transactionQuery.orderByAsc("交易日期", "创建时间", "流水编号");

        List<FundTransaction> transactions = transactionMapper.selectList(transactionQuery);

        // 5. 计算每笔流水后的余额
        BigDecimal runningBalance = initialBalance.getAmount();
        String balanceDirection = initialBalance.getDirection();
        List<FundDiaryResponse.TransactionDetailInfo> transactionDetails = new ArrayList<>();

        for (FundTransaction tx : transactions) {
            FundDiaryResponse.TransactionDetailInfo detail = new FundDiaryResponse.TransactionDetailInfo();
            detail.setTransactionId(tx.getTransactionId());
            detail.setTransactionCode(tx.getTransactionCode());
            detail.setAccountId(tx.getAccountId());
            detail.setTransactionDate(tx.getTransactionDate().format(DATE_FORMATTER));
            detail.setCounterpartyAccount(tx.getCounterpartyAccount());
            detail.setCounterpartyName(tx.getCounterpartyName());
            detail.setCounterpartyBank(tx.getCounterpartyBank());
            detail.setPurpose(tx.getPurpose());
            detail.setSummary(tx.getSummary());
            detail.setSubjectId(tx.getSubjectId());
            detail.setRemark(tx.getRemark());
            detail.setTimestamp(tx.getTimestamp());

            // 如果有科目ID，查询科目信息
            if (tx.getSubjectId() != null) {
                // 查询科目信息
                var subject = subjectMapper.selectById(tx.getSubjectId());
                if (subject != null) {
                    detail.setSubjectCode(subject.getSubjectCode());
                    detail.setSubjectName(subject.getSubjectName());
                }
            }
            detail.setInternalTransfer(Boolean.TRUE.equals(tx.getInternalTransfer()));
            detail.setRelatedAccountId(tx.getRelatedAccountId());
            detail.setRelatedTransactionId(tx.getRelatedTransactionId());
            // 如果有关联流水ID，查询该流水的编码以便前端显示
            if (tx.getRelatedTransactionId() != null) {
                FundTransaction relatedTx = transactionMapper.selectById(tx.getRelatedTransactionId());
                if (relatedTx != null) {
                    detail.setRelatedTransactionCode(relatedTx.getTransactionCode());
                }
            }

            if ("INCOME".equals(tx.getTransactionType())) {
                detail.setIncome(tx.getAmount());
                detail.setExpenditure(null);
                runningBalance = runningBalance.add(tx.getAmount());
            } else if ("EXPENDITURE".equals(tx.getTransactionType())) {
                detail.setIncome(null);
                detail.setExpenditure(tx.getAmount());
                runningBalance = runningBalance.subtract(tx.getAmount());
            }

            // 判断余额方向
            if (runningBalance.compareTo(BigDecimal.ZERO) >= 0) {
                balanceDirection = "收入";
            } else {
                balanceDirection = "支出";
            }

            detail.setDirection(balanceDirection);
            detail.setBalance(runningBalance.abs());
            // 回单信息
            if (tx.getReceiptFile() != null) {
                com.erp.entity.common.File fileEntity = fileMapper.selectById(tx.getReceiptFile());
                if (fileEntity != null) {
                    detail.setReceiptFileId(fileEntity.getFileId());
                    detail.setReceiptFileUrl(fileEntity.getFileUrl());
                    detail.setReceiptFileName(fileEntity.getFileName());
                }
            }
            detail.setReceiptNo(tx.getReceiptNo());
            detail.setTimestamp(tx.getTimestamp());
            transactionDetails.add(detail);
        }

        // 6. 计算本期合计
        BigDecimal periodIncome = transactions.stream()
                .filter(tx -> "INCOME".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodExpenditure = transactions.stream()
                .filter(tx -> "EXPENDITURE".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalBalance = initialBalance.getAmount().add(periodIncome).subtract(periodExpenditure);
        String finalDirection = finalBalance.compareTo(BigDecimal.ZERO) >= 0 ? "收入" : "支出";

        FundDiaryResponse.PeriodTotalInfo periodTotal = new FundDiaryResponse.PeriodTotalInfo();
        periodTotal.setIncome(periodIncome);
        periodTotal.setExpenditure(periodExpenditure);
        periodTotal.setDirection(finalDirection);
        periodTotal.setBalance(finalBalance.abs());

        // 7. 计算本年累计（从年初到账期结束）
        LocalDate yearStart = LocalDate.of(period.getYear(), 1, 1);
        QueryWrapper<FundTransaction> yearTransactionQuery = new QueryWrapper<>();
        yearTransactionQuery.eq("账户编号", request.getAccountId());
        yearTransactionQuery.ge("交易日期", yearStart);
        yearTransactionQuery.le("交易日期", period.getEndDate());
        List<FundTransaction> yearTransactions = transactionMapper.selectList(yearTransactionQuery);

        BigDecimal yearIncome = yearTransactions.stream()
                .filter(tx -> "INCOME".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearExpenditure = yearTransactions.stream()
                .filter(tx -> "EXPENDITURE".equals(tx.getTransactionType()))
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算年初期初余额（如果有第一个账期的期初余额）
        BigDecimal yearInitialBalance = BigDecimal.ZERO;
        QueryWrapper<FundPeriod> firstPeriodQuery = new QueryWrapper<>();
        firstPeriodQuery.eq("年份", period.getYear());
        firstPeriodQuery.eq("月份", 1);
        firstPeriodQuery.eq("组织编号", request.getOrganizationId());
        FundPeriod firstPeriod = fundPeriodMapper.selectOne(firstPeriodQuery);
        if (firstPeriod != null) {
            FundAccountInitialBalance firstPeriodBalance = initialBalanceMapper.selectByAccountAndPeriod(
                    request.getAccountId(), firstPeriod.getPeriodId());
            if (firstPeriodBalance != null) {
                yearInitialBalance = firstPeriodBalance.getInitialBalance();
            }
        }

        BigDecimal yearFinalBalance = yearInitialBalance.add(yearIncome).subtract(yearExpenditure);
        String yearFinalDirection = yearFinalBalance.compareTo(BigDecimal.ZERO) >= 0 ? "收入" : "支出";

        FundDiaryResponse.YearTotalInfo yearTotal = new FundDiaryResponse.YearTotalInfo();
        yearTotal.setIncome(yearIncome);
        yearTotal.setExpenditure(yearExpenditure);
        yearTotal.setDirection(yearFinalDirection);
        yearTotal.setBalance(yearFinalBalance.abs());

        // 8. 构建响应
        FundDiaryResponse response = new FundDiaryResponse();
        response.setAccountId(account.getAccountId());
        response.setAccountName(account.getAccountName());
        response.setAccountCode(account.getAccountCode());
        response.setPeriodId(period.getPeriodId());
        response.setPeriodCode(period.getPeriodCode());
        response.setYear(period.getYear());
        response.setMonth(period.getMonth());
        response.setInitialBalance(initialBalance);
        response.setTransactions(transactionDetails);
        response.setPeriodTotal(periodTotal);
        response.setYearTotal(yearTotal);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateDiaryPdf(FundDiaryRequest request) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        if (request.getAccountId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户ID不能为空");
        }

        // 获取日记账数据
        FundDiaryResponse diaryResponse = getDiary(request);

        // 查询该账户和账期的所有PDF文件记录
        FundPeriod period = null;
        if (request.getPeriodId() != null) {
            period = fundPeriodMapper.selectById(request.getPeriodId());
        } else if (request.getYear() != null && request.getMonth() != null) {
            QueryWrapper<FundPeriod> periodQuery = new QueryWrapper<>();
            periodQuery.eq("年份", request.getYear());
            periodQuery.eq("月份", request.getMonth());
            periodQuery.eq("组织编号", request.getOrganizationId());
            period = fundPeriodMapper.selectOne(periodQuery);
        }

        if (period == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "账期不存在");
        }

        // 查询该账户和账期的所有PDF文件记录（使用业务类型和业务ID）
        // 使用组合ID：accountId * 10000 + periodId（确保在Integer范围内）
        Integer combinedBusinessId = (int)(request.getAccountId() * 10000L + period.getPeriodId());
        List<File> existingFiles = fileMapper.selectByBusinessTypeAndId(
                FUND_DIARY_BUSINESS_TYPE, combinedBusinessId);

        // 物理删除所有旧记录和文件
        if (existingFiles != null && !existingFiles.isEmpty()) {
            for (File file : existingFiles) {
                // 删除实际文件
                if (file.getLocalPath() != null && !file.getLocalPath().trim().isEmpty()) {
                    try {
                        String fullPath = localStoragePath + "/" + file.getLocalPath();
                        java.io.File fileObj = new java.io.File(fullPath);
                        if (fileObj.exists() && fileObj.isFile()) {
                            boolean deleted = fileObj.delete();
                            if (deleted) {
                                log.info("删除日记账PDF文件成功：fileId={}, filePath={}", file.getFileId(), fullPath);
                            }
                        }
                    } catch (Exception e) {
                        log.error("删除日记账PDF文件异常：fileId={}, filePath={}", file.getFileId(), file.getLocalPath(), e);
                    }
                }
                // 物理删除数据库记录
                try {
                    int delRows = fileMapper.deleteById(file.getFileId());
                    if (delRows == 0) {
                        log.warn("删除日记账PDF数据库记录失败（记录不存在），fileId={}", file.getFileId());
                    }
                } catch (Exception e) {
                    log.error("删除日记账PDF数据库记录异常：fileId={}", file.getFileId(), e);
                }
            }
        }

        // 生成PDF文件名：账户名称_账期编码.pdf
        String pdfFileName = diaryResponse.getAccountName() + "_" + diaryResponse.getPeriodCode() + ".pdf";
        // 替换文件名中的特殊字符
        pdfFileName = pdfFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 构建文件存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = FUND_DIARY_BUSINESS_TYPE + "/" + datePath + "/" + pdfFileName;
        String fullPath = localStoragePath + "/" + relativePath;

        // 创建目录
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
            java.nio.file.Files.createDirectories(filePath.getParent());

            // 生成PDF文件
            com.erp.util.FundDiaryPdfGenerator.generatePdf(diaryResponse, fullPath);

            // 获取文件大小
            java.io.File pdfFileObj = new java.io.File(fullPath);
            long fileSize = pdfFileObj.length();

            // 创建文件记录
            File pdfFile = new File();
            pdfFile.setFileName(pdfFileName);
            pdfFile.setFileType("PDF");
            pdfFile.setFileSize(fileSize);
            pdfFile.setStorageType("本地");
            pdfFile.setLocalPath(relativePath);
            pdfFile.setFileUrl(""); // 先设置为空，插入后更新
            pdfFile.setBusinessModule(FUND_DIARY_BUSINESS_MODULE);
            pdfFile.setBusinessId(combinedBusinessId); // 使用组合ID
            pdfFile.setBusinessType(FUND_DIARY_BUSINESS_TYPE);
            pdfFile.setFileStatus("正常");
            pdfFile.setUploadTime(LocalDateTime.now());
            pdfFile.setUploaderId(currentUserId);
            pdfFile.setCreateTime(LocalDateTime.now());
            pdfFile.setUpdateTime(LocalDateTime.now());
            fileMapper.insert(pdfFile);

            // 更新fileUrl
            pdfFile.setFileUrl("/api/file/preview/" + pdfFile.getFileId());
            int updateRows = fileMapper.updateById(pdfFile);
            if (updateRows == 0) {
                log.warn("更新PDF fileUrl失败（乐观锁冲突），fileId={}", pdfFile.getFileId());
            }

            log.info("日记账PDF生成成功：accountId={}, periodId={}, pdfFileId={}, filePath={}, fileSize={}, operator={}",
                    request.getAccountId(), period.getPeriodId(), pdfFile.getFileId(), fullPath, fileSize, currentUserId);

            // 返回打印URL（预览URL，用于打印）
            return "/api/file/preview/" + pdfFile.getFileId();

        } catch (Exception e) {
            log.error("生成日记账PDF失败：accountId={}, periodId={}", request.getAccountId(), period.getPeriodId(), e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "生成PDF失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateBatchDiaryPdf(List<FundDiaryRequest> requests) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "账户列表不能为空");
        }

        // 查询所有日记账数据
        List<FundDiaryResponse> diaryResponses = new java.util.ArrayList<>();
        for (FundDiaryRequest request : requests) {
            if (request.getAccountId() == null) {
                continue;
            }
            try {
                FundDiaryResponse diaryResponse = getDiary(request);
                diaryResponses.add(diaryResponse);
            } catch (Exception e) {
                log.warn("获取日记账数据失败：accountId={}, periodId={}", request.getAccountId(), request.getPeriodId(), e);
                // 继续处理其他账户，不中断整个流程
            }
        }

        if (diaryResponses.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "没有找到有效的日记账数据");
        }

        // 获取账期信息（用于文件名）
        FundPeriod period = null;
        FundDiaryRequest firstRequest = requests.get(0);
        if (firstRequest.getPeriodId() != null) {
            period = fundPeriodMapper.selectById(firstRequest.getPeriodId());
        } else if (firstRequest.getYear() != null && firstRequest.getMonth() != null) {
            QueryWrapper<FundPeriod> periodQuery = new QueryWrapper<>();
            periodQuery.eq("年份", firstRequest.getYear());
            periodQuery.eq("月份", firstRequest.getMonth());
            periodQuery.eq("组织编号", firstRequest.getOrganizationId());
            period = fundPeriodMapper.selectOne(periodQuery);
        }

        if (period == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "账期不存在");
        }

        // 生成合并后的PDF文件名：账期编码_连续打印_时间戳.pdf
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String pdfFileName = period.getPeriodCode() + "_连续打印_" + timestamp + ".pdf";
        // 替换文件名中的特殊字符
        pdfFileName = pdfFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 构建文件存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = FUND_DIARY_BUSINESS_TYPE + "/batch/" + datePath + "/" + pdfFileName;
        String fullPath = localStoragePath + "/" + relativePath;

        // 创建目录
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(fullPath);
            java.nio.file.Files.createDirectories(filePath.getParent());

            // 批量生成PDF文件（合并）
            com.erp.util.FundDiaryPdfGenerator.generateBatchPdf(diaryResponses, fullPath);

            // 获取文件大小
            java.io.File pdfFileObj = new java.io.File(fullPath);
            long fileSize = pdfFileObj.length();

            // 创建文件记录（批量打印不关联具体的账户，businessId设为null）
            File pdfFile = new File();
            pdfFile.setFileName(pdfFileName);
            pdfFile.setFileType("PDF");
            pdfFile.setFileSize(fileSize);
            pdfFile.setStorageType("本地");
            pdfFile.setLocalPath(relativePath);
            pdfFile.setFileUrl(""); // 先设置为空，插入后更新
            pdfFile.setBusinessModule(FUND_DIARY_BUSINESS_MODULE);
            pdfFile.setBusinessId(null); // 批量打印不关联具体业务ID
            pdfFile.setBusinessType(FUND_DIARY_BUSINESS_TYPE);
            pdfFile.setFileStatus("正常");
            pdfFile.setUploadTime(LocalDateTime.now());
            pdfFile.setUploaderId(currentUserId);
            pdfFile.setCreateTime(LocalDateTime.now());
            pdfFile.setUpdateTime(LocalDateTime.now());
            fileMapper.insert(pdfFile);

            // 更新fileUrl
            pdfFile.setFileUrl("/api/file/preview/" + pdfFile.getFileId());
            int updateRows = fileMapper.updateById(pdfFile);
            if (updateRows == 0) {
                log.warn("更新PDF fileUrl失败（乐观锁冲突），fileId={}", pdfFile.getFileId());
            }

            log.info("批量日记账PDF生成成功：accountIds={}, periodId={}, pdfFileId={}, filePath={}, fileSize={}, operator={}",
                    requests.stream().map(r -> String.valueOf(r.getAccountId())).collect(java.util.stream.Collectors.joining(",")),
                    period.getPeriodId(), pdfFile.getFileId(), fullPath, fileSize, currentUserId);

            // 返回打印URL（预览URL，用于打印）
            return "/api/file/preview/" + pdfFile.getFileId();

        } catch (Exception e) {
            log.error("批量生成日记账PDF失败：accountIds={}, periodId={}",
                    requests.stream().map(r -> String.valueOf(r.getAccountId())).collect(java.util.stream.Collectors.joining(",")),
                    period.getPeriodId(), e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "批量生成PDF失败：" + e.getMessage());
        }
    }

    /**
     * 下载日记账导入Excel模板
     *
     * @param response HTTP响应对象
     */
    @Override
    public void downloadFundDiaryTemplate(HttpServletResponse response) {
        Workbook workbook = null;
        OutputStream outputStream = null;

        try {
            // 创建工作簿
            workbook = new XSSFWorkbook();

            // 创建工作表
            Sheet sheet = workbook.createSheet("日记账导入模板");

            // 创建样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 创建标题行
            Row headerRow = sheet.createRow(0);

            // 定义列标题
            String[] headers = {
                "交易日期",
                "付方银行账号", "付方单位账户名称", "付方单位开户银行名称",
                "收方银行账号", "收方单位账户名称", "收方单位开户银行名称",
                "交易金额", "用途", "摘要", "科目编码", "回单编号", "备注"
            };

            // 创建标题行
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 设置列宽
            for (int i = 0; i < headers.length; i++) {
                if (i == 0) { // 交易日期
                    sheet.setColumnWidth(i, 12 * 256);
                } else if (i >= 1 && i <= 6) { // 往来单位字段（付方3个 + 收方3个）
                    sheet.setColumnWidth(i, 25 * 256);
                } else if (i == 7) { // 交易金额
                    sheet.setColumnWidth(i, 15 * 256);
                } else { // 用途、摘要、科目编号、回单编号、备注
                    sheet.setColumnWidth(i, 20 * 256);
                }
            }

            // 创建示例数据行
            Row exampleRow = sheet.createRow(1);

            // 设置示例数据
            exampleRow.createCell(0).setCellValue("2025-01-15"); // 交易日期

            // 付方信息（示例：对方公司）
            exampleRow.createCell(1).setCellValue("对方公司账号"); // 付方银行账号
            exampleRow.createCell(2).setCellValue("对方公司名称"); // 付方单位账户名称
            exampleRow.createCell(3).setCellValue("对方开户银行"); // 付方单位开户银行名称

            // 收方信息（示例：本公司）
            exampleRow.createCell(4).setCellValue("本公司账号"); // 收方银行账号
            exampleRow.createCell(5).setCellValue("本公司名称"); // 收方单位账户名称
            exampleRow.createCell(6).setCellValue("本公司开户银行"); // 收方单位开户银行名称

            exampleRow.createCell(7).setCellValue("10000.00"); // 交易金额
            exampleRow.createCell(8).setCellValue("销售货款"); // 用途
            exampleRow.createCell(9).setCellValue("收到客户货款"); // 摘要
            exampleRow.createCell(10).setCellValue("1001"); // 科目编码
            exampleRow.createCell(11).setCellValue("R20250115001"); // 回单编号
            exampleRow.createCell(12).setCellValue("正常业务收款"); // 备注

            // 为示例行设置样式
            for (int i = 0; i < headers.length; i++) {
                Cell cell = exampleRow.getCell(i);
                if (cell != null) {
                    cell.setCellStyle(dataStyle);
                }
            }

            // 设置响应头（兼容多浏览器，包含 filename 和 filename*）
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = "日记账导入模板.xlsx";
            try {
                String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString());
                // 同时设置 filename 和 filename* 以提高兼容性
                response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            } catch (UnsupportedEncodingException e) {
                log.warn("文件名编码失败，使用英文文件名", e);
                response.setHeader("Content-Disposition", "attachment; filename=Fund_Diary_Template.xlsx");
            }
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 将工作簿先写入内存，便于校验与设置 Content-Length
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();

            // 记录字节长度与前4字节（用于确认是ZIP/XLSX格式，前2字节应为 'P' 'K'）
            log.info("日记账导入模板 bytes length={}", bytes.length);
            if (bytes.length >= 4) {
                log.info("template prefix bytes: {} {} {} {}", bytes[0], bytes[1], bytes[2], bytes[3]);
            }

            // 设置响应长度并输出
            response.setContentLength(bytes.length);
            ServletOutputStream out = response.getOutputStream();
            out.write(bytes);
            out.flush();

            log.info("日记账导入模板下载成功");

        } catch (Exception e) {
            log.error("下载日记账导入模板失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "下载模板失败：" + e.getMessage());
        } finally {
            // 清理资源
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.warn("关闭Excel工作簿失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输出流失败", e);
                }
            }
        }
    }

    /**
     * Excel导入日记账数据
     *
     * @param excelFile Excel文件
     * @param accountId 账户ID
     * @param periodId 账期ID
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundDiaryImportResult excelImportFundDiary(MultipartFile excelFile, Long accountId, Long periodId) {
        List<FundDiaryImportResult.FailDetail> failDetails = new ArrayList<>();
        int successCount = 0;
        int totalCount = 0;

        Workbook workbook = null;
        try {
            // 验证文件
            if (excelFile.isEmpty()) {
                throw new BusinessException("上传的文件为空");
            }

            // 验证账户是否存在
            FundAccount account = fundAccountMapper.selectById(accountId);
            if (account == null) {
                throw new BusinessException("指定的账户不存在");
            }

            // 验证账期是否存在
            FundPeriod period = fundPeriodMapper.selectById(periodId);
            if (period == null) {
                throw new BusinessException("指定的账期不存在");
            }

            // 获取企业信息用于判断交易类型（从 SYS_CONFIG -> INVOICE_COMPANY_INFO）
            String companyInfoJson = getCompanyInfo();
            String enterpriseName = null;
            String unifiedSocialCreditCode = null;
            String bankAccount = null;
            String bankName = null;

            if (companyInfoJson != null) {
                try {
                    JSONObject companyJson = JSONUtil.parseObj(companyInfoJson);
                    enterpriseName = companyJson.getStr("enterpriseName");
                    unifiedSocialCreditCode = companyJson.getStr("unifiedSocialCreditCode");
                    bankAccount = companyJson.getStr("bankAccount");
                    bankName = companyJson.getStr("bankName");
                } catch (Exception e) {
                    log.warn("解析企业信息失败，使用默认判断逻辑", e);
                }
            }

            // 解析Excel文件
            workbook = new XSSFWorkbook(excelFile.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null) {
                throw new BusinessException("Excel文件格式错误，未找到工作表");
            }

            List<FundTransaction> transactions = new ArrayList<>();

            // 从第2行开始读取数据（第1行是标题，第2行是示例）
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                totalCount++;

                // 遇到任何一行解析或校验异常都应立即抛出以触发事务回滚（全量回滚）
                FundTransaction transaction;
                try {
                    transaction = parseTransactionFromRow(row, accountId, periodId,
                        enterpriseName, unifiedSocialCreditCode, bankAccount, bankName, rowIndex + 1);
                } catch (Exception e) {
                    log.error("导入第{}行失败，已终止导入：{}", rowIndex + 1, e.getMessage(), e);
                    throw new BusinessException("第" + (rowIndex + 1) + "行导入失败：" + e.getMessage());
                }

                // 成功解析则加入待插入列表
                transactions.add(transaction);
                successCount++;
            }

            // 批量插入数据
            if (!transactions.isEmpty()) {
                for (FundTransaction transaction : transactions) {
                    transactionMapper.insert(transaction);
                }
            }

            log.info("Excel导入日记账数据完成，accountId={}, periodId={}, total={}, success={}, fail={}",
                accountId, periodId, totalCount, successCount, failDetails.size());

            // 记录数据变更日志（批量导入）
            try {
                if (logRecordService != null) {
                    FundTransaction dummyOld = null;
                    FundTransaction dummyNew = new FundTransaction();
                    dummyNew.setAccountId(accountId);
                    logRecordService.recordDataChangeLog("日记账管理", "FUND_TRANSACTION", String.valueOf(accountId),
                            "批量导入", String.format("Excel批量导入日记账：成功%d条，失败%d条，跳过%d条",
                                    successCount, failDetails.size(), totalCount - successCount - failDetails.size()),
                            dummyOld, dummyNew, SecurityUtil.getCurrentUserId(), null, true, null);
                }
            } catch (Exception logEx) {
                log.warn("记录日记账批量导入数据变更日志失败，accountId={}", accountId, logEx);
            }

            return FundDiaryImportResult.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failDetails.size())
                .failDetails(failDetails)
                .build();

        } catch (Exception e) {
            log.error("Excel导入日记账数据失败，accountId={}, periodId={}", accountId, periodId, e);
            throw new BusinessException("导入失败：" + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.warn("关闭Excel工作簿失败", e);
                }
            }
        }
    }

    /**
     * 从Excel行解析交易记录
     */
    private FundTransaction parseTransactionFromRow(Row row, Long accountId, Long periodId,
            String enterpriseName, String unifiedSocialCreditCode, String bankAccount, String companyBankName, int rowNumber) {

        // 获取单元格值
        String transactionDate = getCellValueAsString(row.getCell(0)); // 交易日期

        // 往来单位信息
        String payerAccount = getCellValueAsString(row.getCell(1)); // 付方银行账号
        String payerName = getCellValueAsString(row.getCell(2)); // 付方单位账户名称
        String payerBank = getCellValueAsString(row.getCell(3)); // 付方单位开户银行名称
        String receiverAccount = getCellValueAsString(row.getCell(4)); // 收方银行账号
        String receiverName = getCellValueAsString(row.getCell(5)); // 收方单位账户名称
        String receiverBank = getCellValueAsString(row.getCell(6)); // 收方单位开户银行名称

        String amountStr = getCellValueAsString(row.getCell(7)); // 交易金额
        String purpose = getCellValueAsString(row.getCell(8)); // 用途
        String summary = getCellValueAsString(row.getCell(9)); // 摘要
        String subjectCode = getCellValueAsString(row.getCell(10)); // 科目编码
        String receiptNo = getCellValueAsString(row.getCell(11)); // 回单编号
        String remark = getCellValueAsString(row.getCell(12)); // 备注

        // 验证必填字段
        if (transactionDate == null || transactionDate.trim().isEmpty()) {
            throw new IllegalArgumentException("交易日期不能为空");
        }
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new IllegalArgumentException("交易金额不能为空");
        }
        if (summary == null || summary.trim().isEmpty()) {
            throw new IllegalArgumentException("摘要不能为空");
        }

        // 解析交易日期
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(transactionDate.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("交易日期格式错误，应为YYYY-MM-DD格式");
        }

        // 解析交易金额
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("交易金额必须大于0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("交易金额格式错误");
        }

        // 校验交易日期对应的账期是否存在且未结账（按交易账户所属组织、交易日期的年月查找账期）
        com.erp.entity.finance.FundAccount accountEntity = fundAccountMapper.selectById(accountId);
        if (accountEntity == null) {
            throw new BusinessException("账户不存在，ID：" + accountId);
        }
        int txYear = parsedDate.getYear();
        int txMonth = parsedDate.getMonthValue();
        var periodQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.finance.FundPeriod>();
        periodQuery.eq("组织编号", accountEntity.getOrganizationId());
        periodQuery.eq("年份", txYear);
        periodQuery.eq("月份", txMonth);
        com.erp.entity.finance.FundPeriod rowPeriod = fundPeriodMapper.selectOne(periodQuery);
        if (rowPeriod == null) {
            throw new BusinessException("第" + rowNumber + "行：对应账期未创建（" + txYear + "年" + txMonth + "月），导入已终止");
        }
        if (Boolean.TRUE.equals(rowPeriod.getIsSettled())) {
            throw new BusinessException("第" + rowNumber + "行：对应账期已结账，不能导入该日期的流水，导入已终止");
        }
        if (parsedDate.isBefore(rowPeriod.getStartDate()) || parsedDate.isAfter(rowPeriod.getEndDate())) {
            throw new BusinessException("第" + rowNumber + "行：交易日期不在对应账期范围内（" + rowPeriod.getStartDate() + " 至 " + rowPeriod.getEndDate() + "），导入已终止");
        }

        // 判断交易类型（通过sysconfig企业信息匹配）
        String finalTransactionType = determineTransactionType(
            null, payerName, payerAccount, payerBank, receiverName, receiverAccount, receiverBank,
            enterpriseName, unifiedSocialCreditCode, bankAccount, companyBankName, rowNumber
        );

        // 设置往来单位信息
        String counterpartyAccount;
        String counterpartyName;
        String counterpartyBank;

        if ("INCOME".equals(finalTransactionType)) {
            // 收入：收方是本公司，付方是对方
            counterpartyAccount = payerAccount;
            counterpartyName = payerName;
            counterpartyBank = payerBank;
        } else {
            // 支出：付方是本公司，收方是对方
            counterpartyAccount = receiverAccount;
            counterpartyName = receiverName;
            counterpartyBank = receiverBank;
        }

        // 获取科目ID
        Long subjectId = null;
        if (subjectCode != null && !subjectCode.trim().isEmpty()) {
            var subjectQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.erp.entity.finance.FundSubject>();
            subjectQuery.eq("科目编码", subjectCode.trim());
            // 数据库字段为 `是否启用`（1 表示启用），避免使用错误的列名 `启用状态`
            subjectQuery.eq("是否启用", 1); // 只查询启用的科目
            com.erp.entity.finance.FundSubject subject = subjectMapper.selectOne(subjectQuery);
            if (subject != null) {
                subjectId = subject.getSubjectId();
            }
        }

        // 生成流水编码
        String transactionCode = generateTransactionCode(parsedDate);

        // 创建交易记录
        FundTransaction transaction = new FundTransaction();
        transaction.setTransactionCode(transactionCode);
        transaction.setAccountId(accountId);
        transaction.setTransactionDate(parsedDate);
        transaction.setTransactionType(finalTransactionType);
        transaction.setAmount(amount);
        transaction.setCounterpartyAccount(counterpartyAccount);
        transaction.setCounterpartyName(counterpartyName);
        transaction.setCounterpartyBank(counterpartyBank);
        transaction.setPurpose(purpose);
        transaction.setSummary(summary);
        transaction.setSubjectId(subjectId);
        transaction.setInternalTransfer(false); // 默认非内部往来
        transaction.setReceiptNo(receiptNo);
        transaction.setRemark(remark);
        transaction.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transaction.setCreateBy(SecurityUtil.getCurrentUserId().longValue());
        transaction.setUpdateBy(SecurityUtil.getCurrentUserId().longValue());

        return transaction;
    }

    /**
     * 根据往来单位信息判断交易类型
     */
    private String determineTransactionType(String specifiedType, String payerName, String payerAccount, String payerBank,
            String receiverName, String receiverAccount, String receiverBank, String enterpriseName,
            String unifiedSocialCreditCode, String bankAccount, String companyBankName, int rowNumber) {

        // 如果明确指定了交易类型，直接使用
        if ("INCOME".equalsIgnoreCase(specifiedType) || "EXPENDITURE".equalsIgnoreCase(specifiedType)) {
            return specifiedType.toUpperCase();
        }

        // 通过企业信息匹配判断
        if (enterpriseName != null && unifiedSocialCreditCode != null) {
            // 检查付方是否是本公司
            boolean payerIsCompany = isCompanyInfo(payerName, payerAccount, payerBank, enterpriseName, unifiedSocialCreditCode, bankAccount, companyBankName);
            boolean receiverIsCompany = isCompanyInfo(receiverName, receiverAccount, receiverBank, enterpriseName, unifiedSocialCreditCode, bankAccount, companyBankName);

            if (payerIsCompany && !receiverIsCompany) {
                return "EXPENDITURE";
            } else if (!payerIsCompany && receiverIsCompany) {
                return "INCOME";
            } else {
                if (payerIsCompany && receiverIsCompany) {
                    // 业务口径：内部往来/自己对自己禁止导入
                    throw new BusinessException("第" + rowNumber + "行：付方与收方均匹配本公司信息（内部往来/自己对自己），系统不允许导入该行，导入已终止");
                }
                throw new BusinessException("第" + rowNumber + "行：付方与收方均未匹配本公司信息，无法判断交易方向。请检查：收/付方名称、账号、开户行是否与系统配置 INVOICE_COMPANY_INFO 一致；银行账号单元格是否为文本格式，导入已终止");
            }
        }

        // 配置缺失或不完整
        throw new BusinessException("第" + rowNumber + "行：系统配置 INVOICE_COMPANY_INFO 缺失或格式不完整（enterpriseName/unifiedSocialCreditCode），无法判断交易方向，导入已终止");
    }

    /**
     * 检查是否是本公司信息
     */
    private boolean isCompanyInfo(String name, String account, String bankName, String enterpriseName,
            String unifiedSocialCreditCode, String bankAccount, String companyBankName) {
        if ((name == null || name.trim().isEmpty()) && (account == null || account.trim().isEmpty()) && (bankName == null || bankName.trim().isEmpty())) {
            return false;
        }

        String normalizedEnterpriseName = normalizeTextForMatch(enterpriseName);
        String normalizedName = normalizeTextForMatch(name);

        String normalizedCompanyBankName = normalizeTextForMatch(companyBankName);
        String normalizedBankName = normalizeTextForMatch(bankName);

        String normalizedBankAccount = normalizeDigitsForMatch(bankAccount);
        String normalizedAccount = normalizeDigitsForMatch(account);

        // 名称匹配（包含关系，忽略大小写，去空白）
        boolean nameMatch = normalizedEnterpriseName != null && normalizedName != null &&
            (normalizedName.contains(normalizedEnterpriseName) || normalizedEnterpriseName.contains(normalizedName));

        // 账号匹配（仅保留数字后比较）
        boolean accountMatch = normalizedBankAccount != null && normalizedAccount != null &&
            normalizedBankAccount.equals(normalizedAccount);

        // 银行名称匹配（包含关系，忽略大小写，去空白）
        boolean bankNameMatch = normalizedCompanyBankName != null && normalizedBankName != null &&
            (normalizedCompanyBankName.contains(normalizedBankName) || normalizedBankName.contains(normalizedCompanyBankName));

        return nameMatch || accountMatch || bankNameMatch;
    }

    private String normalizeTextForMatch(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("\\s+", "").toLowerCase();
    }

    private String normalizeDigitsForMatch(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D+", "");
        return digits.isEmpty() ? null : digits;
    }

    /**
     * 生成流水编码
     */
    private String generateTransactionCode(LocalDate date) {
        // 新格式：LS-YYYYMMDD-0001（日期+4位序号，按天递增）
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "LS-" + dateStr + "-";

        // 查询当天最大流水编码
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FundTransaction>();
        queryWrapper.like("流水编码", prefix + "%");
        queryWrapper.orderByDesc("流水编码");
        queryWrapper.last("limit 1");

        FundTransaction lastTransaction = transactionMapper.selectOne(queryWrapper);
        int sequence = 1;

        if (lastTransaction != null && lastTransaction.getTransactionCode() != null) {
            String lastCode = lastTransaction.getTransactionCode();
            if (lastCode.startsWith(prefix)) {
                try {
                    String[] parts = lastCode.split("-");
                    String seqPart = parts[parts.length - 1];
                    sequence = Integer.parseInt(seqPart) + 1;
                } catch (Exception e) {
                    // 解析失败则从1开始
                }
            }
        }

        return String.format("%s%04d", prefix, sequence);
    }

    /**
     * 获取企业信息
     */
    private String getCompanyInfo() {
        try {
            // 使用 INVOICE_COMPANY_INFO 配置项作为发票/导入时的企业信息来源
            SysConfig config = sysConfigService.getByName("INVOICE_COMPANY_INFO");
            return config != null ? config.getValue() : null;
        } catch (Exception e) {
            log.warn("获取企业信息失败", e);
            return null;
        }
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    // 对于数字类型，优先按单元格显示格式转为文本，避免长数字精度丢失/科学计数法问题
                    try {
                        DataFormatter formatter = new DataFormatter();
                        String formatted = formatter.formatCellValue(cell);
                        if (formatted != null) {
                            formatted = formatted.trim();
                        }
                        if (formatted != null && !formatted.isEmpty()) {
                            return formatted;
                        }
                    } catch (Exception ignore) {
                        // fallback
                    }
                    return NumberToTextConverter.toText(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 计算公式结果
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    @Override
    public void exportFundDiaryExcel(FundDiaryRequest request, HttpServletResponse response) {
        try {
            log.info("开始导出日记账Excel，accountId={}", request.getAccountId());

            // 1. 获取日记账数据（复用现有的查询逻辑）
            FundDiaryResponse diaryResponse = getDiary(request);

            // 2. 获取企业信息用于处理往来单位字段
            JSONObject companyInfo = getCompanyInfoJson();
            String enterpriseName = companyInfo != null ? companyInfo.getStr("enterpriseName") : null;
            String bankAccount = companyInfo != null ? companyInfo.getStr("bankAccount") : null;
            String bankName = companyInfo != null ? companyInfo.getStr("bankName") : null;

            // 3. 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("日记账");

            // 4. 创建表头
            Row headerRow = sheet.createRow(0);

            // 基础字段
            String[] headers = {
                "交易日期",
                "付方银行账号", "付方单位账户名称", "付方单位开户银行名称",
                "收方银行账号", "收方单位账户名称", "收方单位开户银行名称",
                "交易金额", "用途", "摘要", "科目编码", "回单编号", "备注"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            // 5. 填充数据
            List<FundDiaryResponse.TransactionDetailInfo> transactions = diaryResponse.getTransactions();
            int rowNum = 1;

            for (FundDiaryResponse.TransactionDetailInfo tx : transactions) {
                Row row = sheet.createRow(rowNum++);

                // 处理往来单位字段分配
                String transactionType = tx.getIncome() != null ? "INCOME" : "EXPENDITURE";
                String counterpartyAccount = tx.getCounterpartyAccount() != null ? tx.getCounterpartyAccount() : "";
                String counterpartyName = tx.getCounterpartyName() != null ? tx.getCounterpartyName() : "";
                String counterpartyBank = tx.getCounterpartyBank() != null ? tx.getCounterpartyBank() : "";

                String payerAccount, payerName, payerBank, receiverAccount, receiverName, receiverBank;
                if ("INCOME".equals(transactionType)) {
                    // 收入：往来单位赋值给付方，企业信息赋值给收方
                    payerAccount = counterpartyAccount;
                    payerName = counterpartyName;
                    payerBank = counterpartyBank;
                    receiverAccount = bankAccount != null ? bankAccount : "";
                    receiverName = enterpriseName != null ? enterpriseName : "";
                    receiverBank = bankName != null ? bankName : "";
                } else {
                    // 支出：往来单位赋值给收方，企业信息赋值给付方
                    payerAccount = bankAccount != null ? bankAccount : "";
                    payerName = enterpriseName != null ? enterpriseName : "";
                    payerBank = bankName != null ? bankName : "";
                    receiverAccount = counterpartyAccount;
                    receiverName = counterpartyName;
                    receiverBank = counterpartyBank;
                }

                // 按照统一后的表头顺序填充数据（仅保留13列）
                row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate() : ""); // 交易日期
                row.createCell(1).setCellValue(payerAccount); // 付方银行账号
                row.createCell(2).setCellValue(payerName); // 付方单位账户名称
                row.createCell(3).setCellValue(payerBank); // 付方单位开户银行名称
                row.createCell(4).setCellValue(receiverAccount); // 收方银行账号
                row.createCell(5).setCellValue(receiverName); // 收方单位账户名称
                row.createCell(6).setCellValue(receiverBank); // 收方单位开户银行名称
                String amountValue = "";
                if (tx.getIncome() != null) {
                    amountValue = tx.getIncome().toString();
                } else if (tx.getExpenditure() != null) {
                    amountValue = tx.getExpenditure().toString();
                }
                row.createCell(7).setCellValue(amountValue); // 交易金额
                row.createCell(8).setCellValue(tx.getPurpose() != null ? tx.getPurpose() : ""); // 用途
                row.createCell(9).setCellValue(tx.getSummary() != null ? tx.getSummary() : ""); // 摘要
                row.createCell(10).setCellValue(tx.getSubjectCode() != null ? tx.getSubjectCode() : ""); // 科目编码
                row.createCell(11).setCellValue(tx.getReceiptNo() != null ? tx.getReceiptNo() : ""); // 回单编号
                row.createCell(12).setCellValue(tx.getRemark() != null ? tx.getRemark() : ""); // 备注
            }

            // 6. 设置列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256); // 设置列宽为20个字符宽度
            }

            // 7. 设置响应头
            String fileName = "日记账_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
            response.setCharacterEncoding("UTF-8");

            // 8. 写入响应流
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            log.info("日记账Excel导出完成，共导出{}条记录", transactions.size());

        } catch (Exception e) {
            log.error("导出日记账Excel失败，accountId={}", request.getAccountId(), e);
            throw new BusinessException("导出日记账Excel失败：" + e.getMessage());
        }
    }

    /**
     * 获取企业信息JSON对象
     */
    private JSONObject getCompanyInfoJson() {
        try {
            String companyInfoJson = getCompanyInfo();
            return companyInfoJson != null ? JSONUtil.parseObj(companyInfoJson) : null;
        } catch (Exception e) {
            log.warn("解析企业信息失败", e);
            return null;
        }
    }

    /**
     * 转换交易类型显示名称
     */
    private String convertTransactionType(String transactionType) {
        if ("INCOME".equals(transactionType)) {
            return "收入";
        } else if ("EXPENDITURE".equals(transactionType)) {
            return "支出";
        }
        return transactionType;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    @Override
    public void exportFundDiaryReceipts(Long periodId, Long organizationId, HttpServletResponse response) throws IOException {
        // 1. 参数校验
        if (periodId == null) {
            throw new BusinessException("账期ID不能为空");
        }
        if (organizationId == null) {
            throw new BusinessException("组织ID不能为空");
        }

        log.info("开始导出日记账回单文件，periodId={}, organizationId={}", periodId, organizationId);

        try {
            // 2. 查询账期信息
            FundPeriod fundPeriod = fundPeriodMapper.selectById(periodId);
            if (fundPeriod == null) {
                throw new BusinessException("账期不存在，ID：" + periodId);
            }

            // 3. 查询当前账期内所有有回单文件编号的流水
            QueryWrapper<FundTransaction> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("交易日期", fundPeriod.getStartDate())
                       .le("交易日期", fundPeriod.getEndDate())
                       .isNotNull("回单文件编号")
                       .ne("回单文件编号", 0); // 确保回单文件编号不为0

            List<FundTransaction> transactions = transactionMapper.selectList(queryWrapper);
            log.info("找到 {} 条有回单文件的流水记录", transactions.size());

            if (transactions.isEmpty()) {
                throw new BusinessException("当前账期内没有找到任何有回单文件的流水记录");
            }

            // 4. 收集所有回单文件ID
            List<Integer> receiptFileIds = transactions.stream()
                    .map(FundTransaction::getReceiptFile)
                    .filter(id -> id != null && id > 0)
                    .distinct()
                    .collect(Collectors.toList());

            log.info("需要处理的回单文件ID数量：{}", receiptFileIds.size());

            // 5. 查询文件信息并验证文件存在性
            Map<String, byte[]> filesToZip = new HashMap<>();
            int processedFiles = 0;

            for (Integer fileId : receiptFileIds) {
                try {
                    File file = fileMapper.selectById(fileId);
                    if (file == null) {
                        log.warn("回单文件不存在，文件ID：{}", fileId);
                        continue;
                    }

                    // 检查文件状态
                    if (!"正常".equals(file.getFileStatus())) {
                        log.warn("回单文件状态异常，文件ID：{}，状态：{}", fileId, file.getFileStatus());
                        continue;
                    }

                    // 根据存储类型读取文件
                    byte[] fileData = null;
                    if ("本地".equals(file.getStorageType())) {
                        // 本地存储
                        if (file.getLocalPath() != null) {
                            try {
                                // 使用FileService读取本地文件
                                fileData = fileService.readFile(file);
                            } catch (Exception e) {
                                log.warn("读取本地文件失败，文件ID：{}，路径：{}", fileId, file.getLocalPath(), e);
                                continue;
                            }
                        }
                    } else if ("云端".equals(file.getStorageType())) {
                        // 云端存储
                        try {
                            fileData = fileService.readFile(file);
                        } catch (Exception e) {
                            log.warn("读取云端文件失败，文件ID：{}，objectKey：{}", fileId, file.getObjectKey(), e);
                            continue;
                        }
                    } else {
                        log.warn("未知的存储类型：{}，文件ID：{}", file.getStorageType(), fileId);
                        continue;
                    }

                    if (fileData != null && fileData.length > 0) {
                        // 生成文件名：流水编码_回单编号_原始文件名
                        String fileName = generateReceiptFileName(transactions, fileId, file.getFileName());
                        filesToZip.put(fileName, fileData);
                        processedFiles++;
                        log.debug("成功添加文件到ZIP：{}，大小：{} bytes", fileName, fileData.length);
                    }

                } catch (Exception e) {
                    log.warn("处理回单文件失败，文件ID：{}", fileId, e);
                    // 继续处理下一个文件
                }
            }

            log.info("成功处理 {} 个回单文件", processedFiles);

            // 6. 检查是否有文件可以导出
            if (filesToZip.isEmpty()) {
                throw new BusinessException("没有找到可用的回单文件，请检查文件是否存在或状态是否正常");
            }

            // 7. 设置响应头
            String zipFileName = String.format("日记账回单_%s_%s.zip",
                    fundPeriod.getPeriodCode(),
                    DATE_FORMATTER.format(java.time.LocalDate.now()));

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + zipFileName + "\"");
            response.setCharacterEncoding("UTF-8");

            // 8. 创建ZIP文件并输出
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                zipFileProcessor.createZipFile(filesToZip, outputStream);
                log.info("日记账回单ZIP文件导出成功，共包含 {} 个文件", filesToZip.size());
            }

        } catch (BusinessException e) {
            log.error("导出日记账回单失败：{}", e.getMessage());
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":400,\"message\":\"" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        } catch (Exception e) {
            log.error("导出日记账回单失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\",\"data\":null}");
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 读取本地文件
     *
     * @param filePath 文件相对路径
     * @return 文件字节数组
     */
    private byte[] readFileFromLocal(String filePath) throws IOException {
        // 构建完整路径
        java.nio.file.Path fullPath = java.nio.file.Paths.get(localStoragePath, filePath);
        if (!java.nio.file.Files.exists(fullPath)) {
            throw new IOException("文件不存在：" + fullPath);
        }
        return java.nio.file.Files.readAllBytes(fullPath);
    }

    /**
     * 生成回单文件名
     *
     * @param transactions 流水列表
     * @param fileId 文件ID
     * @param originalFileName 原始文件名
     * @return 生成的文件名
     */
    private String generateReceiptFileName(List<FundTransaction> transactions, Integer fileId, String originalFileName) {
        // 查找对应的流水记录
        FundTransaction transaction = transactions.stream()
                .filter(t -> fileId.equals(t.getReceiptFile()))
                .findFirst()
                .orElse(null);

        if (transaction != null && transaction.getReceiptNo() != null) {
            // 格式：流水编码_回单编号_原始文件名
            return String.format("%s_%s_%s",
                    transaction.getTransactionCode(),
                    transaction.getReceiptNo(),
                    originalFileName);
        } else {
            // 如果找不到对应的流水或回单编号为空，使用文件ID作为标识
            return String.format("FILE_%d_%s", fileId, originalFileName);
        }
    }
}

